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
    private ArbolNario arbolDirectorios;
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
        this.arbolDirectorios = new ArbolNario(new MetadatoArchivo("SSD", true, 0)); 
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
        // 1. Completar I/O en ejecución si existe
        Proceso ioActual = planificador.getIOEnEjecucion();
        if (ioActual != null) {
            String resultado = ejecutarProceso(ioActual);
            ioActual.terminar();
            planificador.getColaBloqueados().eliminar(ioActual);
            planificador.limpiarIOEnEjecucion();

            System.out.println("I/O completado: " + ioActual.getIdProceso() + " -> " + resultado);
            return "I/O completado: " + ioActual.getIdProceso() + " - " + resultado;
        }

        // 2. Completar CPU en ejecución si existe
        Proceso cpuActual = planificador.getCPUEnEjecucion();
        if (cpuActual != null) {
            if (Config.OP_CREATE.equals(cpuActual.getOperacion())) {
                String resultado = ejecutarProceso(cpuActual);
                cpuActual.terminar();
                planificador.liberarCPU();

                System.out.println("CPU completó: " + cpuActual.getIdProceso() + " -> " + resultado);
                return "CPU completó: " + cpuActual.getIdProceso() + " - " + resultado;
            }

            // Otros ops van a I/O después de pasar por CPU
            planificador.encolarBloqueado(cpuActual);
            planificador.encolarIO(cpuActual);
            planificador.liberarCPU();

            String mensaje = "CPU liberada: Proceso " + cpuActual.getIdProceso() + " movido a I/O (" + cpuActual.getOperacion() + ")";
            System.out.println(mensaje);
            return mensaje;
        }

        // 3. Iniciar próxima operación de I/O si hay pendiente
        if (planificador.getColaIO().obtenerTamano() > 0) {
            Proceso proximoIO = planificador.getColaIO().obtener(0);
            planificador.getColaIO().eliminarPorIndice(0);
            planificador.definirIOEnEjecucion(proximoIO);
            return "I/O en ejecución: " + proximoIO.getIdProceso() + " (" + proximoIO.getOperacion() + ")";
        }

        // 4. Tomar siguiente proceso listo para CPU
        Proceso siguiente = planificador.obtenerSiguienteProceso();

        if (siguiente == null) {
            return "No hay procesos en la cola.";
        }

        return "CPU en ejecución: " + siguiente.getIdProceso() + " (" + siguiente.getOperacion() + ")";
    }

    /**
     * Formatea completamente el disco virtual y sistema de archivos.
     * Solo el usuario administrador puede ejecutar esta acción.
     */
    public String formatearDisco() {
        if (usuarioActivo == null || !usuarioActivo.puedeFormatearDisco()) {
            return "Error: No tienes permisos para formatear el disco.";
        }

        this.disco = new DiscoV(Config.CAPACIDAD_DISCO_DEFECTO);
        this.fat = new TablaAsignacion();
        this.planificador = new Planificador(0);
        this.arbolDirectorios = new ArbolNario(new MetadatoArchivo("SSD", true, 0));
        this.journal = new ListaSimple<>();
        this.txCounter = 1;
        this.contadorProcesos = 1;

        return "Disco formateado. Solo la carpeta raíz 'SSD' permanece.";
    }

    public String renombrarArchivo(String rutaActual, String nuevoNombre) {
        if (usuarioActivo == null || !usuarioActivo.esAdministrador()) {
            return "Error: Solo el administrador puede renombrar archivos.";
        }
        if (rutaActual == null || rutaActual.trim().isEmpty() || nuevoNombre == null || nuevoNombre.trim().isEmpty()) {
            return "Error: Ruta o nombre nuevo inválido.";
        }

        EntradaFAT entrada = fat.buscarArchivo(rutaActual);
        if (entrada == null) {
            return "Error: El archivo no existe para renombrar.";
        }

        // Nombre libre
        int idxSlash = rutaActual.lastIndexOf('/');
        String rutaPadre = idxSlash >= 0 ? rutaActual.substring(0, idxSlash) : "";
        String nuevaRuta = (rutaPadre.isEmpty() ? "" : rutaPadre + "/") + nuevoNombre;

        if (fat.buscarArchivo(nuevaRuta) != null) {
            return "Error: Ya existe un archivo con el nuevo nombre en dicha ruta.";
        }

        // Cambiar registro FAT
        entrada.setNombreArchivo(nuevaRuta);

        // Actualizar bloques del disco con la nueva ruta
        disco.actualizarNombreArchivo(rutaActual, nuevaRuta);

        // Actualizar árbol de directorios
        arbolDirectorios.eliminarArchivo(rutaActual);
        arbolDirectorios.insertarArchivo(nuevaRuta, entrada.getCantidadBloques());

        return "Éxito: Archivo renombrado a '" + nuevaRuta + "'.";
    }

    public String renombrarCarpeta(String rutaActual, String nuevoNombre) {
        if (usuarioActivo == null || !usuarioActivo.esAdministrador()) {
            return "Error: Solo el administrador puede renombrar directorios.";
        }
        if (rutaActual == null || rutaActual.trim().isEmpty() || nuevoNombre == null || nuevoNombre.trim().isEmpty()) {
            return "Error: Ruta o nombre nuevo inválido.";
        }
        if (rutaActual.equalsIgnoreCase(arbolDirectorios.getRaiz().getDato().getNombre())) {
            return "Error: No se puede renombrar la carpeta raíz.";
        }

        EstructurasDeDatos.NodoArbol nodo = arbolDirectorios.encontrarNodoPorRuta(rutaActual);
        if (nodo == null) {
            return "Error: Directorio no encontrado.";
        }

        // Validar nombre no repetido entre hermanos
        String rutaPadre = rutaActual.substring(0, rutaActual.lastIndexOf('/'));
        EstructurasDeDatos.NodoArbol nodoPadre = arbolDirectorios.encontrarNodoPorRuta(rutaPadre);
        if (nodoPadre == null) {
            return "Error: Ruta padre no encontrada.";
        }
        for (int i = 0; i < nodoPadre.getHijos().obtenerTamano(); i++) {
            EstructurasDeDatos.NodoArbol hijo = nodoPadre.getHijos().obtener(i);
            if (hijo.getDato().isEsDirectorio() && hijo.getDato().getNombre().equalsIgnoreCase(nuevoNombre)) {
                return "Error: Ya existe un directorio con ese nombre en el mismo nivel.";
            }
        }

        String nuevaRuta = rutaPadre + "/" + nuevoNombre;

        // Cambiar nombre en el nodo
        nodo.getDato().setNombre(nuevoNombre);

        // Actualizar todas las rutas de archivos dentro de este directorio en FAT y Disco
        String prefijo = rutaActual + "/";
        for (int i = 0; i < fat.getEntradas().obtenerTamano(); i++) {
            EntradaFAT entrada = fat.getEntradas().obtener(i);
            if (entrada != null) {
                String nombreArchivo = entrada.getNombreArchivo();
                if (nombreArchivo.startsWith(prefijo)) {
                    String restante = nombreArchivo.substring(prefijo.length());
                    String rutaNuevaCompleta = nuevaRuta + "/" + restante;
                    entrada.setNombreArchivo(rutaNuevaCompleta);
                }
            }
        }

        disco.actualizarNombreArchivo(rutaActual, nuevaRuta);

        return "Éxito: Directorio renombrado a '" + nuevaRuta + "'.";
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

        // Mantener el árbol de directorios sincronizado con el estado real del sistema
        arbolDirectorios.insertarArchivo(nombreArchivo, tamano);

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

        Transaccion txRead = new Transaccion(txCounter++, Config.OP_READ, nombreArchivo, entrada.getCantidadBloques(), entrada.getDireccionPrimerBloque(), entrada.getPropietario());
        journal.insertarAlFinal(txRead);

        entrada.adquirirLockLectura();
        try {
            txRead.setEstado(Transaccion.ESTADO_CONFIRMADA);
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

            // Sincronizar árbol de directorios
            arbolDirectorios.eliminarArchivo(nombreArchivo);

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

    public void crearCarpeta(String ruta) {
        arbolDirectorios.insertarCarpeta(ruta);
    }

    /**
     * Elimina un directorio y todo su contenido (subcarpetas + archivos) de forma recursiva.
     * Mantiene sincronizados el árbol de directorios, la FAT y el disco.
     */
    public boolean eliminarCarpeta(String rutaCarpeta) {
        if (rutaCarpeta == null || rutaCarpeta.trim().isEmpty()) {
            return false;
        }

        // No eliminar la raíz
        if (rutaCarpeta.equalsIgnoreCase(arbolDirectorios.getRaiz().getDato().getNombre())) {
            return false;
        }

        EstructurasDeDatos.NodoArbol nodoCarpeta = arbolDirectorios.encontrarNodoPorRuta(rutaCarpeta);
        if (nodoCarpeta == null || !nodoCarpeta.getDato().isEsDirectorio()) {
            return false;
        }

        // Eliminar archivos en ruta recursivamente (de FAT y disco)
        java.util.List<String> archivosAEliminar = new java.util.ArrayList<>();
        recolectarArchivosRecursivos(rutaCarpeta, nodoCarpeta, archivosAEliminar);
        for (String rutaArchivo : archivosAEliminar) {
            EntradaFAT entrada = fat.buscarArchivo(rutaArchivo);
            if (entrada != null) {
                Bloque bloqueInicial = disco.getBloque(entrada.getDireccionPrimerBloque());
                if (bloqueInicial != null) {
                    disco.liberarBloques(bloqueInicial);
                }
                fat.eliminarArchivo(rutaArchivo);
            }
        }

        // Eliminar nodo del árbol padre
        int idxUltimaBarra = rutaCarpeta.lastIndexOf('/');
        String rutaPadre = idxUltimaBarra > 0 ? rutaCarpeta.substring(0, idxUltimaBarra) : arbolDirectorios.getRaiz().getDato().getNombre();
        EstructurasDeDatos.NodoArbol nodoPadre = arbolDirectorios.encontrarNodoPorRuta(rutaPadre);
        if (nodoPadre == null) {
            return false;
        }

        return nodoPadre.eliminarHijo(nodoCarpeta.getDato());
    }

    private void recolectarArchivosRecursivos(String rutaBase, EstructurasDeDatos.NodoArbol nodo, java.util.List<String> archivos) {
        EstructurasDeDatos.ListaSimple<EstructurasDeDatos.NodoArbol> hijos = nodo.getHijos();
        for (int i = 0; i < hijos.obtenerTamano(); i++) {
            EstructurasDeDatos.NodoArbol hijo = hijos.obtener(i);
            String rutaHijo = rutaBase + "/" + hijo.getDato().getNombre();
            if (hijo.getDato().isEsDirectorio()) {
                recolectarArchivosRecursivos(rutaHijo, hijo, archivos);
            } else {
                archivos.add(rutaHijo);
            }
        }
    }

    public String crearArchivoSistema(String nombreArchivo, int tamano) {
        if (tamano <= 0) return "Error: El tamaño debe ser mayor a 0.";
        if (fat.buscarArchivo(nombreArchivo) != null) return "Error: El archivo ya existe.";
        if (disco.getBloquesLibres() < tamano) return "Error: No hay espacio suficiente en el disco.";
        Bloque bloqueInicial = disco.asignarBloques(nombreArchivo, tamano);
        if (bloqueInicial == null) {
            return "Error crítico: Fallo al asignar bloques en el disco.";
        }
        fat.registrarArchivo(nombreArchivo, tamano, bloqueInicial.getNumeroBloque(), "system");

        // Sincronizar el árbol de directorios para que el JTree refleje el contenido del disco
        arbolDirectorios.insertarArchivo(nombreArchivo, tamano);

        return "Éxito: Archivo '" + nombreArchivo + "' creado.";
    }

    public ArbolNario getArbolDirectorios() {
        return arbolDirectorios;
    }

    public ListaSimple<MetadatoArchivo> getSubcarpetas(String ruta) {
        return arbolDirectorios.getSubcarpetas(ruta);
    }
}
