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
import EstructurasDeDatos.ListaSimple;
import EstructurasDeDatos.Nodo;
import EstructurasDeDatos.Transaccion;
import Modelo.MetadatoArchivo;
import Controlador.Config;
import Modelo.Usuario;
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
    private ListaSimple<Transaccion> journal;
    private int txCounter = 1;
    // --- Variables de control ---
    private int contadorProcesos; // Para generar IDs únicos

    /**
     * Inicializa el sistema con la capacidad del disco especificada.
     */
    public ControladorSistema() {
        this.disco = new DiscoV(Config.CAPACIDAD_DISCO_DEFECTO);
        this.fat = new TablaAsignacion();
        this.planificador = new Planificador(0); // El cabezal inicia en el bloque 0
        this.arbolDirectorios = new ArbolNario<>(new MetadatoArchivo("Raiz", true, 0)); 
        this.usuariosSistema = new ListaSimple<>();
        this.journal = new ListaSimple<>();
        this.contadorProcesos = 1;
    }

    private boolean forzarFallo = false;
    public void setForzarFallo(boolean val) { this.forzarFallo = val; }
    public boolean isForzarFallo() { return forzarFallo; }
    public ListaSimple<Transaccion> getJournal() { return journal; }

    public String recuperarFallos() {
        int recuperadas = 0;
        Nodo<Transaccion> actual = journal.getInicio();
        while (actual != null) {
            Transaccion tx = actual.getDato();
            if (Transaccion.ESTADO_PENDIENTE.equals(tx.getEstado())) {
                if (Config.OP_CREATE.equals(tx.getOperacion())) {
                    EntradaFAT entrada = fat.buscarArchivo(tx.getArchivo());
                    if (entrada != null) {
                        Bloque b = disco.getBloque(entrada.getDireccionPrimerBloque());
                        disco.liberarBloques(b);
                        fat.eliminarArchivo(tx.getArchivo());
                    } else if (tx.getBloqueAnterior() > 0) {
                        Bloque b = disco.getBloque(tx.getBloqueAnterior());
                        disco.liberarBloques(b);
                    }
                } else if (Config.OP_DELETE.equals(tx.getOperacion())) {
                    EntradaFAT entrada = fat.buscarArchivo(tx.getArchivo());
                    if (entrada == null && tx.getBloqueAnterior() != 0) {
                       fat.registrarArchivo(tx.getArchivo(), tx.getTamanoAnterior(), tx.getBloqueAnterior(), tx.getPropietario());
                    }
                } else if (Config.OP_UPDATE.equals(tx.getOperacion())) {
                    EntradaFAT entrada = fat.buscarArchivo(tx.getArchivo());
                    if (entrada != null) {
                       Bloque nuevoB = disco.getBloque(entrada.getDireccionPrimerBloque());
                       disco.liberarBloques(nuevoB);
                       fat.eliminarArchivo(tx.getArchivo());
                    }
                    if (tx.getBloqueAnterior() > 0) {
                       fat.registrarArchivo(tx.getArchivo(), tx.getTamanoAnterior(), tx.getBloqueAnterior(), tx.getPropietario());
                    }
                }
                tx.setEstado("RECUPERADA");
                recuperadas++;
            }
            actual = actual.getSiguiente();
        }
        return "Éxito: Se recuperaron " + recuperadas + " transacciones incompletas.";
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

    public String ejecutarCrearProceso(Proceso proceso) {
        String nombreArchivo = proceso.getArchivoObjetivo();
        int tamano = proceso.getCantidadBloques();

        if (fat.buscarArchivo(nombreArchivo) != null) {
            return "Error: Ya existe un archivo con el nombre '" + nombreArchivo + "'.";
        }

        if (disco.getBloquesLibres() < tamano) {
            return "Error: Espacio insuficiente en el disco. Se requieren " + tamano + " bloques.";
        }

        Transaccion tx = new Transaccion(txCounter++, Config.OP_CREATE, nombreArchivo, 0, 0, proceso.getPropietario());
        journal.insertarAlFinal(tx);

        Bloque bloqueInicial = disco.asignarBloques(nombreArchivo, tamano);
        if (bloqueInicial == null) {
            tx.setEstado("FALLIDA");
            return "Error crítico: Fallo al asignar bloques en el disco.";
        }
        
        tx.setBloqueAnterior(bloqueInicial.getNumeroBloque());
        
        if (forzarFallo) {
            forzarFallo = false;
            throw new RuntimeException("Fallo Simulado: Interrupción en creación (Journaling pendiente)");
        }

        fat.registrarArchivo(nombreArchivo, tamano, bloqueInicial.getNumeroBloque(), proceso.getPropietario());
        tx.setEstado(Transaccion.ESTADO_CONFIRMADA);
        
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

        entrada.adquirirLockLectura();
        try {
            return "Éxito: Lectura completa de '" + nombreArchivo + "' (" + entrada.getCantidadBloques() + " bloques).";
        } finally {
            entrada.liberarLockLectura();
        }
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

        entrada.adquirirLockEscritura();
        try {
            Transaccion tx = new Transaccion(txCounter++, Config.OP_DELETE, nombreArchivo, entrada.getCantidadBloques(), entrada.getDireccionPrimerBloque(), entrada.getPropietario());
            journal.insertarAlFinal(tx);
            
            if (forzarFallo) {
                forzarFallo = false;
                throw new RuntimeException("Fallo Simulado: Interrupción antes de eliminar (Journaling pendiente)");
            }

            Bloque bloqueInicial = disco.getBloque(entrada.getDireccionPrimerBloque());
            disco.liberarBloques(bloqueInicial);
            fat.eliminarArchivo(nombreArchivo);

            tx.setEstado(Transaccion.ESTADO_CONFIRMADA);
            return "Éxito: Archivo '" + nombreArchivo + "' eliminado correctamente.";
        } finally {
            entrada.liberarLockEscritura();
        }
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

        entrada.adquirirLockEscritura();
        try {
            int nuevoTamano = entrada.getCantidadBloques() + variacionTamano;
            if (nuevoTamano <= 0) {
                Transaccion txDel = new Transaccion(txCounter++, Config.OP_DELETE, nombreArchivo, entrada.getCantidadBloques(), entrada.getDireccionPrimerBloque(), entrada.getPropietario());
                journal.insertarAlFinal(txDel);
                
                Bloque bloqueInicial = disco.getBloque(entrada.getDireccionPrimerBloque());
                disco.liberarBloques(bloqueInicial);
                fat.eliminarArchivo(nombreArchivo);
                
                txDel.setEstado(Transaccion.ESTADO_CONFIRMADA);
                return "Éxito: Archivo '" + nombreArchivo + "' eliminado (tamaño pasó a 0 o menor).";
            }

            int bloquesDisponibles = disco.getBloquesLibres() + entrada.getCantidadBloques();
            if (bloquesDisponibles < nuevoTamano) {
                return "Error: No hay espacio suficiente para la modificación. Se requieren " + nuevoTamano + " bloques en total.";
            }

            Transaccion txUpd = new Transaccion(txCounter++, Config.OP_UPDATE, nombreArchivo, entrada.getCantidadBloques(), entrada.getDireccionPrimerBloque(), entrada.getPropietario());
            journal.insertarAlFinal(txUpd);

            Bloque bloqueInicialViejo = disco.getBloque(entrada.getDireccionPrimerBloque());
            disco.liberarBloques(bloqueInicialViejo);
            fat.eliminarArchivo(nombreArchivo);

            Bloque bloqueInicialNuevo = disco.asignarBloques(nombreArchivo, nuevoTamano);
            if (bloqueInicialNuevo == null) {
                // Rollback: intentar restaurar tamaño anterior
                Bloque restaurado = disco.asignarBloques(nombreArchivo, entrada.getCantidadBloques());
                fat.registrarArchivo(nombreArchivo, entrada.getCantidadBloques(), restaurado.getNumeroBloque(), entrada.getPropietario());
                txUpd.setEstado("FALLIDA");
                return "Error: Falla interna al modificar. Se restauró el tamaño anterior.";
            }

            if (forzarFallo) {
                forzarFallo = false;
                throw new RuntimeException("Fallo Simulado: Interrupción en modficación (Journaling pendiente)");
            }

            fat.registrarArchivo(nombreArchivo, nuevoTamano, bloqueInicialNuevo.getNumeroBloque(), entrada.getPropietario());
            txUpd.setEstado(Transaccion.ESTADO_CONFIRMADA);
            return "Éxito: Archivo modificado. Nuevo tamaño: " + nuevoTamano + " bloques.";
        } finally {
            entrada.liberarLockEscritura();
        }
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
