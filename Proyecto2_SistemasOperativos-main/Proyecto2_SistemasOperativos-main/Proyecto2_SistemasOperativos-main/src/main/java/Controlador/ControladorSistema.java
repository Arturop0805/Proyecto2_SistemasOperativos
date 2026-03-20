/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controlador;

import Modelo.Planificador;
import Modelo.Proceso;
import EstructurasDeDatos.DiscoV;
import EstructurasDeDatos.TablaAsignacion;
import EstructurasDeDatos.ArbolNario;
import EstructurasDeDatos.Bloque;
import EstructurasDeDatos.EntradaFAT;
import Modelo.MetadatoArchivo;
import Controlador.Config;
import Modelo.Usuario;
import EstructurasDeDatos.ListaSimple;
import java.awt.Color;
/**
 * Orquestador principal del Simulador de Sistema de Archivos.
 * Conecta la Vista (GUI) con los distintos Modelos lógicos.
 */
public class ControladorSistema {
    
    // --- Modelos ---
    private DiscoV disco;
    private TablaAsignacion fat; // File Allocation Table
    private Planificador planificador;
    private ArbolNario<MetadatoArchivo> arbolDirectorios;
    private Modelo.Usuario usuarioActivo;
    private ListaSimple<Modelo.Usuario> usuariosSistema;
    // --- Variables de control ---
    private int contadorProcesos; // Para generar IDs únicos

    /**
     * Inicializa el sistema con la capacidad del disco especificada.
     */
    public ControladorSistema(int CapacidadDisco) {
        this.disco = new DiscoV(CapacidadDisco);
        this.fat = new TablaAsignacion();
        this.planificador = new Planificador(0); // El cabezal inicia en el bloque 0
        this.arbolDirectorios = new ArbolNario<>(new MetadatoArchivo("Raiz", true, 0)); 
        this.usuariosSistema = new ListaSimple<>();
        this.contadorProcesos = 1;
    }

    // =========================================================================
    // FUNCIONES BÁSICAS DE OPERACIÓN (CRUD)
    // =========================================================================

    // =========================================================================
    // COLA DE PROCESOS (FIFO) Y EJECUCIÓN
    // =========================================================================

    /**
     * Encola una solicitud de creación de archivo.
     * La asignación de bloques se realiza cuando se atiende el proceso.
     */
    public String crearArchivo(String nombreArchivo, int tamano) {
        if (tamano <= 0) return "Error: El tamaño debe ser mayor a 0.";
        if (fat.buscarArchivo(nombreArchivo) != null) return "Error: El archivo ya existe.";
        if (disco.getBloquesLibres() < tamano) return "Error: No hay espacio suficiente en el disco.";

        String idProceso = "P" + contadorProcesos++;
        Proceso nuevoProceso = new Proceso(
                idProceso,
                usuarioActivo.getNombreUsuario(),
                Config.OP_CREATE,
                nombreArchivo,
                planificador.getPosicionCabezal(), // Se usa posición actual del cabezal para ordenar
                tamano
        );

        planificador.agregarProceso(nuevoProceso);
        return "Éxito: Solicitud de creación encolada (Proceso " + idProceso + ").";
    }

    /**
     * Encola una solicitud de lectura de archivo.
     */
    public String leerArchivo(String nombreArchivo) {
        EntradaFAT entrada = fat.buscarArchivo(nombreArchivo);
        if (entrada == null) return "Error: El archivo no existe.";

        if (!usuarioActivo.getRol().equals(Config.ROL_ADMIN)
                && !entrada.getPropietario().equals(usuarioActivo.getNombreUsuario())) {
            return "Error: No tienes permisos para leer este archivo.";
        }

        String idProceso = "P" + contadorProcesos++;
        Proceso nuevoProceso = new Proceso(
                idProceso,
                usuarioActivo.getNombreUsuario(),
                Config.OP_READ,
                nombreArchivo,
                entrada.getDireccionPrimerBloque(),
                entrada.getCantidadBloques()
        );

        planificador.agregarProceso(nuevoProceso);
        return "Éxito: Solicitud de lectura encolada (Proceso " + idProceso + ").";
    }

    /**
     * Encola una solicitud de eliminación de archivo.
     */
    public String eliminarArchivo(String nombreArchivo) {
        EntradaFAT entrada = fat.buscarArchivo(nombreArchivo);
        if (entrada == null) return "Error: El archivo no existe.";

        if (!usuarioActivo.getRol().equals(Config.ROL_ADMIN)
                && !entrada.getPropietario().equals(usuarioActivo.getNombreUsuario())) {
            return "Error: No tienes permisos para eliminar este archivo.";
        }

        String idProceso = "P" + contadorProcesos++;
        Proceso nuevoProceso = new Proceso(
                idProceso,
                usuarioActivo.getNombreUsuario(),
                Config.OP_DELETE,
                nombreArchivo,
                entrada.getDireccionPrimerBloque(),
                entrada.getCantidadBloques()
        );

        planificador.agregarProceso(nuevoProceso);
        return "Éxito: Solicitud de eliminación encolada (Proceso " + idProceso + ").";
    }

    /**
     * Encola una solicitud de modificación de archivo (cambio de tamaño).
     */
    public String modificarArchivo(String nombreArchivo, int variacionTamano) {
        EntradaFAT entrada = fat.buscarArchivo(nombreArchivo);
        if (entrada == null) return "Error: El archivo no existe.";

        if (!usuarioActivo.getRol().equals(Config.ROL_ADMIN)
                && !entrada.getPropietario().equals(usuarioActivo.getNombreUsuario())) {
            return "Error: No tienes permisos para modificar este archivo.";
        }

        String idProceso = "P" + contadorProcesos++;
        Proceso nuevoProceso = new Proceso(
                idProceso,
                usuarioActivo.getNombreUsuario(),
                Config.OP_UPDATE,
                nombreArchivo,
                entrada.getDireccionPrimerBloque(),
                variacionTamano // Usamos el campo "cantidadBloques" para almacenar la variación
        );

        planificador.agregarProceso(nuevoProceso);
        return "Éxito: Solicitud de modificación encolada (Proceso " + idProceso + ").";
    }

    /**
     * Procesa el siguiente proceso de la cola (FIFO por defecto).
     */
    public String procesarSiguienteOperacion() {
        Proceso procesoEjecutado = planificador.obtenerSiguienteProceso();

        if (procesoEjecutado == null) {
            return "No hay procesos en la cola.";
        }

        String resultado = ejecutarProceso(procesoEjecutado);
        procesoEjecutado.terminar();

        // NOTA JOURNALING: Si la operación era un CREATE o DELETE,
        // aquí deberíamos buscar la transacción en el Journal y marcarla como CONFIRMADA.

        System.out.println("Se ejecutó: " + procesoEjecutado.getOperacion() +
                " sobre " + procesoEjecutado.getArchivoObjetivo() +
                " | Nuevo cabezal: " + planificador.getPosicionCabezal());

        return resultado;
    }

    private String ejecutarProceso(Proceso proceso) {
        switch (proceso.getOperacion()) {
            case Config.OP_CREATE:
                return ejecutarCrearProceso(proceso);
            case Config.OP_READ:
                return ejecutarLeerProceso(proceso);
            case Config.OP_DELETE:
                return ejecutarEliminarProceso(proceso);
            case Config.OP_UPDATE:
                return ejecutarModificarProceso(proceso);
            default:
                return "Error: Operación desconocida '" + proceso.getOperacion() + "'.";
        }
    }

    private String ejecutarCrearProceso(Proceso proceso) {
        String nombreArchivo = proceso.getArchivoObjetivo();
        int tamano = proceso.getCantidadBloques();

        if (fat.buscarArchivo(nombreArchivo) != null) {
            return "Error: Ya existe un archivo con el nombre '" + nombreArchivo + "'.";
        }

        if (disco.getBloquesLibres() < tamano) {
            return "Error: Espacio insuficiente en el disco. Se requieren " + tamano + " bloques.";
        }

        Bloque bloqueInicial = disco.asignarBloques(nombreArchivo, tamano);
        if (bloqueInicial == null) {
            return "Error crítico: Fallo al asignar bloques en el disco.";
        }

        fat.registrarArchivo(nombreArchivo, tamano, bloqueInicial.getNumeroBloque(), proceso.getPropietario());
        return "Éxito: Archivo '" + nombreArchivo + "' creado con " + tamano + " bloques.";
    }

    private String ejecutarLeerProceso(Proceso proceso) {
        String nombreArchivo = proceso.getArchivoObjetivo();
        EntradaFAT entrada = fat.buscarArchivo(nombreArchivo);
        if (entrada == null) {
            return "Error: El archivo '" + nombreArchivo + "' no existe.";
        }

        if (!usuarioActivo.getRol().equals(Config.ROL_ADMIN)
                && !entrada.getPropietario().equals(usuarioActivo.getNombreUsuario())) {
            return "Error: No tienes permisos para leer este archivo.";
        }

        return "Éxito: Lectura completa de '" + nombreArchivo + "' (" + entrada.getCantidadBloques() + " bloques).";
    }

    private String ejecutarEliminarProceso(Proceso proceso) {
        String nombreArchivo = proceso.getArchivoObjetivo();
        EntradaFAT entrada = fat.buscarArchivo(nombreArchivo);
        if (entrada == null) {
            return "Error: El archivo '" + nombreArchivo + "' no existe.";
        }

        if (!usuarioActivo.getRol().equals(Config.ROL_ADMIN)
                && !entrada.getPropietario().equals(usuarioActivo.getNombreUsuario())) {
            return "Error: No tienes permisos para eliminar este archivo.";
        }

        Bloque bloqueInicial = disco.getBloque(entrada.getDireccionPrimerBloque());
        disco.liberarBloques(bloqueInicial);
        fat.eliminarArchivo(nombreArchivo);

        return "Éxito: Archivo '" + nombreArchivo + "' eliminado correctamente.";
    }

    private String ejecutarModificarProceso(Proceso proceso) {
        String nombreArchivo = proceso.getArchivoObjetivo();
        int variacionTamano = proceso.getCantidadBloques();

        EntradaFAT entrada = fat.buscarArchivo(nombreArchivo);
        if (entrada == null) {
            return "Error: El archivo '" + nombreArchivo + "' no existe.";
        }

        if (!usuarioActivo.getRol().equals(Config.ROL_ADMIN)
                && !entrada.getPropietario().equals(usuarioActivo.getNombreUsuario())) {
            return "Error: No tienes permisos para modificar este archivo.";
        }

        int nuevoTamano = entrada.getCantidadBloques() + variacionTamano;
        if (nuevoTamano <= 0) {
            return ejecutarEliminarProceso(proceso);
        }

        int bloquesDisponibles = disco.getBloquesLibres() + entrada.getCantidadBloques();
        if (bloquesDisponibles < nuevoTamano) {
            return "Error: No hay espacio suficiente para la modificación. Se requieren " + nuevoTamano + " bloques en total.";
        }

        Bloque bloqueInicialViejo = disco.getBloque(entrada.getDireccionPrimerBloque());
        disco.liberarBloques(bloqueInicialViejo);
        fat.eliminarArchivo(nombreArchivo);

        Bloque bloqueInicialNuevo = disco.asignarBloques(nombreArchivo, nuevoTamano);
        if (bloqueInicialNuevo == null) {
            // Rollback: intentar restaurar tamaño anterior
            Bloque restaurado = disco.asignarBloques(nombreArchivo, entrada.getCantidadBloques());
            fat.registrarArchivo(nombreArchivo, entrada.getCantidadBloques(), restaurado.getNumeroBloque(), entrada.getPropietario());
            return "Error: Falla interna al modificar. Se restauró el tamaño anterior.";
        }

        fat.registrarArchivo(nombreArchivo, nuevoTamano, bloqueInicialNuevo.getNumeroBloque(), entrada.getPropietario());
        return "Éxito: Archivo modificado. Nuevo tamaño: " + nuevoTamano + " bloques.";
    }

    // GETTERS
    
    
    public DiscoV getDisco() { return disco; }
    public TablaAsignacion getFat() { return fat; }
    public Planificador getPlanificador() { return planificador; }
    public Modelo.Usuario getUsuarioActivo() { return usuarioActivo;}
    public void setUsuarioActivo(Modelo.Usuario usuarioActivo) { this.usuarioActivo = usuarioActivo; }
    public ListaSimple<Modelo.Usuario> getUsuarios() { return usuariosSistema; }
    public void agregarUsuario(Modelo.Usuario u) { usuariosSistema.insertarAlFinal(u); }
    public Color getColorUsuario(String nombreUsuario) {
        int tamano = usuariosSistema.obtenerTamano();
        for (int i = 0; i < tamano; i++) {
            Usuario u = usuariosSistema.obtener(i);
            if (u.getNombreUsuario().equals(nombreUsuario)) {
                return u.getColorAsignado();
            }
        }
        return Color.GRAY; // default si no se encuentra
    }
}
