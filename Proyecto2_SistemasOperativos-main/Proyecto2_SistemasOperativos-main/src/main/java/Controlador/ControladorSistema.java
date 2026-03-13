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
    private ArbolNario<String> arbolDirectorios; // Asumiendo String para nombres de carpetas por ahora
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
        this.arbolDirectorios = new ArbolNario<>("Raiz"); 
        this.usuariosSistema = new ListaSimple<>();
        this.contadorProcesos = 1;
    }

    // =========================================================================
    // FUNCIONES BÁSICAS DE OPERACIÓN (CRUD)
    // =========================================================================

    public String solicitarCreacionArchivo(String nombreArchivo, int cantidadBloques, String propietario) {
        
        // 1. Validar que no exista un archivo con el mismo nombre en la FAT
        if (fat.buscarArchivo(nombreArchivo) != null) {
            return "Error: Ya existe un archivo con el nombre '" + nombreArchivo + "'.";
        }
        
        // 2. Validar espacio en disco
        if (disco.getBloquesLibres() < cantidadBloques) {
            return "Error: Espacio insuficiente en el disco. Se requieren " + cantidadBloques + " bloques.";
        }

        // 3. Asignar los bloques en el Disco (Asignación encadenada)
        Bloque bloqueInicial = disco.asignarBloques(nombreArchivo, cantidadBloques);
        if (bloqueInicial == null) {
            return "Error crítico: Fallo al asignar bloques en el disco.";
        }

        // 4. Registrar en la Tabla de Asignación (FAT)
        fat.registrarArchivo(nombreArchivo, cantidadBloques, bloqueInicial.getNumeroBloque(), propietario);

        // 5. Crear la solicitud de Entrada/Salida (Proceso) y enviarla al planificador
        String idProceso = "P" + contadorProcesos++;
        Proceso nuevoProceso = new Proceso(
                idProceso, 
                propietario, 
                Config.OP_CREATE, 
                nombreArchivo, 
                bloqueInicial.getNumeroBloque(), 
                cantidadBloques
        );
        
        planificador.agregarProceso(nuevoProceso);
        

        return "Éxito: Solicitud de creación encolada (Proceso " + idProceso + ").";
    }

    public String solicitarLecturaArchivo(String nombreArchivo, String propietario) {
        
        // 1. Buscar en la FAT
        EntradaFAT entrada = fat.buscarArchivo(nombreArchivo);
        if (entrada == null) {
            return "Error: El archivo '" + nombreArchivo + "' no existe.";
        }

        // NOTA DE PERMISOS: Aquí a futuro debemos validar si el 'propietario' tiene
        // permisos para leer este archivo basándonos en si es dueño o Administrador.

        // 2. Crear solicitud de E/S
        String idProceso = "P" + contadorProcesos++;
        Proceso nuevoProceso = new Proceso(
                idProceso, 
                propietario, 
                Config.OP_READ, 
                nombreArchivo, 
                entrada.getDireccionPrimerBloque(), // El cabezal debe ir al inicio del archivo
                entrada.getCantidadBloques()
        );
        
        planificador.agregarProceso(nuevoProceso);
        return "Éxito: Solicitud de lectura encolada (Proceso " + idProceso + ").";
    }
    public Proceso procesarSiguienteOperacion() {
        Proceso procesoEjecutado = planificador.obtenerSiguienteProceso();
        
        if (procesoEjecutado != null) {
            // Aquí el proceso ya pasó a estado EJECUCION y la posición del cabezal cambió.
            // Para el alcance de este simulador, asumimos que al extraerlo, la operación de E/S 
            // finaliza inmediatamente.
            procesoEjecutado.terminar();
            
            // NOTA JOURNALING: Si la operación era un CREATE o DELETE, 
            // aquí deberíamos buscar la transacción en el Journal y marcarla como CONFIRMADA.
            
            System.out.println("Se ejecutó: " + procesoEjecutado.getOperacion() + 
                               " sobre " + procesoEjecutado.getArchivoObjetivo() + 
                               " | Nuevo cabezal: " + planificador.getPosicionCabezal());
        }
        
        return procesoEjecutado; // Se retorna a la Vista para que actualice tablas/gráficas
    }
    
    // =========================================================================
    // LÓGICA DE OPERACIONES CRUD (E/S)
    // =========================================================================

    /**
     * OPERACIÓN CREATE: Asigna bloques en el disco y registra en la FAT.
     */
    public String crearArchivo(String nombreArchivo, int tamano) {
        if (tamano <= 0) return "Error: El tamaño debe ser mayor a 0.";
        if (fat.buscarArchivo(nombreArchivo) != null) return "Error: El archivo ya existe.";
        if (disco.getBloquesLibres() < tamano) return "Error: No hay espacio suficiente en el disco.";
        
        // Asignación encadenada en el disco
        Bloque bloqueInicial = disco.asignarBloques(nombreArchivo, tamano);
        
        if (bloqueInicial != null) {
            // Registro en la FAT con propietario
            fat.registrarArchivo(nombreArchivo, tamano, bloqueInicial.getNumeroBloque(), usuarioActivo.getNombreUsuario());
            return "Éxito: Archivo '" + nombreArchivo + "' creado con " + tamano + " bloques.";
        }
        return "Error: Falla interna al asignar bloques.";
    }

    /**
     * OPERACIÓN READ: Verifica que el archivo exista para su lectura.
     */
    public String leerArchivo(String nombreArchivo) {
        EntradaFAT entrada = fat.buscarArchivo(nombreArchivo);
        if (entrada == null) return "Error: El archivo no existe.";
        
        // Validar permisos: solo propietario o admin pueden leer
        if (!usuarioActivo.getRol().equals(Config.ROL_ADMIN) && !entrada.getPropietario().equals(usuarioActivo.getNombreUsuario())) {
            return "Error: No tienes permisos para leer este archivo.";
        }
        
        // Aquí podrías validar permisos de usuario: usuarioActivo.puedeLeerArchivo(...)
        return "Éxito: Leyendo archivo '" + nombreArchivo + "' (Tamaño: " + entrada.getCantidadBloques() + " bloques).";
    }

    /**
     * OPERACIÓN DELETE: Libera los bloques encadenados y elimina el registro de la FAT.
     */
    public String eliminarArchivo(String nombreArchivo) {
        EntradaFAT entrada = fat.buscarArchivo(nombreArchivo);
        if (entrada == null) return "Error: El archivo no existe.";
        
        // Validar permisos: solo propietario o admin pueden eliminar
        if (!usuarioActivo.getRol().equals(Config.ROL_ADMIN) && !entrada.getPropietario().equals(usuarioActivo.getNombreUsuario())) {
            return "Error: No tienes permisos para eliminar este archivo.";
        }
        
        // Obtener el primer bloque y liberar toda la cadena en el disco
        Bloque bloqueInicial = disco.getBloque(entrada.getDireccionPrimerBloque());
        disco.liberarBloques(bloqueInicial);
        
        // Eliminar de la Tabla de Asignación
        fat.eliminarArchivo(nombreArchivo);
        
        return "Éxito: Archivo '" + nombreArchivo + "' eliminado correctamente.";
    }

    /**
     * OPERACIÓN UPDATE: Modifica el tamaño de un archivo.
     * En este simulador básico, lo implementaremos liberando y reasignando (Overwrite).
     */
    public String modificarArchivo(String nombreArchivo, int variacionTamano) {
        EntradaFAT entrada = fat.buscarArchivo(nombreArchivo);
        if (entrada == null) return "Error: El archivo no existe.";
        
        // Validar permisos: solo propietario o admin pueden modificar
        if (!usuarioActivo.getRol().equals(Config.ROL_ADMIN) && !entrada.getPropietario().equals(usuarioActivo.getNombreUsuario())) {
            return "Error: No tienes permisos para modificar este archivo.";
        }
        
        int nuevoTamano = entrada.getCantidadBloques() + variacionTamano;
        
        if (nuevoTamano <= 0) {
            return eliminarArchivo(nombreArchivo); // Si el tamaño es 0 o menor, se elimina
        }
        
        // 1. Liberar los bloques actuales para evitar fragmentación o falta de espacio fantasma
        Bloque bloqueInicialViejo = disco.getBloque(entrada.getDireccionPrimerBloque());
        disco.liberarBloques(bloqueInicialViejo);
        String propietario = entrada.getPropietario();
        fat.eliminarArchivo(nombreArchivo);
        
        // 2. Intentar crear con el nuevo tamaño
        if (disco.getBloquesLibres() >= nuevoTamano) {
            Bloque bloqueInicialNuevo = disco.asignarBloques(nombreArchivo, nuevoTamano);
            fat.registrarArchivo(nombreArchivo, nuevoTamano, bloqueInicialNuevo.getNumeroBloque(), propietario);
            return "Éxito: Archivo modificado. Nuevo tamaño: " + nuevoTamano + " bloques.";
        } else {
            // Rollback (Recuperación si no hubo espacio para la ampliación)
            Bloque restaurado = disco.asignarBloques(nombreArchivo, entrada.getCantidadBloques());
            fat.registrarArchivo(nombreArchivo, entrada.getCantidadBloques(), restaurado.getNumeroBloque(), propietario);
            return "Error: No hay espacio para la ampliación. Se conservó el tamaño original.";
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
