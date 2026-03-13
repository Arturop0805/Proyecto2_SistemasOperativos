package Modelo;


/**
 * Representa el Bloque de Control de Proceso (PCB) orientado a E/S.
 * Almacena toda la información de una solicitud al sistema de archivos.
 */
public class Proceso {
    
    // --- Identificación ---
    private String idProceso;
    private String propietario; // "Administrador" o "Usuario"
    
    // --- Información de la Solicitud de E/S ---
    private String operacion;       // "CREATE", "READ", "UPDATE", "DELETE"
    private String archivoObjetivo; // Nombre del archivo (ej. "A.txt")
    private int posicionDestino;    // Bloque inicial en el disco (Necesario para el Planificador)
    private int cantidadBloques;    // Tamaño en bloques (Útil principalmente para CREATE)
    
    // --- Estado ---
    private Estado estado;          // NUEVO, LISTO, EJECUTANDO, BLOQUEADO, TERMINADO
    
    /**
     * Constructor principal para inicializar una solicitud de proceso.
     */
    public Proceso(String idProceso, String propietario, String operacion, 
                   String archivoObjetivo, int posicionDestino, int cantidadBloques) {
        
        this.idProceso = idProceso;
        this.propietario = propietario;
        this.operacion = operacion;
        this.archivoObjetivo = archivoObjetivo;
        this.posicionDestino = posicionDestino;
        this.cantidadBloques = cantidadBloques;
        
        // Según los requerimientos, todo proceso inicia en estado NUEVO
        this.estado = Estado.NUEVO; 
    }

    // --- MÉTODOS DE TRANSICIÓN DE ESTADO ---
    
    // Estos métodos ayudan a que el Controlador cambie visualmente los estados
    public void pasarAListo() {
        this.estado = Estado.LISTO;
    }
    
    public void ejecutar() {
        this.estado = Estado.EJECUCION;
    }
    
    public void bloquear() {
        this.estado = Estado.BLOQUEADO;
    }
    
    public void terminar() {
        this.estado = Estado.TERMINADO;
    }

    // --- GETTERS Y SETTERS ---

    public String getIdProceso() { return idProceso; }
    public void setIdProceso(String idProceso) { this.idProceso = idProceso; }

    public String getPropietario() { return propietario; }
    public void setPropietario(String propietario) { this.propietario = propietario; }

    public String getOperacion() { return operacion; }
    public void setOperacion(String operacion) { this.operacion = operacion; }

    public String getArchivoObjetivo() { return archivoObjetivo; }
    public void setArchivoObjetivo(String archivoObjetivo) { this.archivoObjetivo = archivoObjetivo; }

    public int getPosicionDestino() { return posicionDestino; }
    public void setPosicionDestino(int posicionDestino) { this.posicionDestino = posicionDestino; }

    public int getCantidadBloques() { return cantidadBloques; }
    public void setCantidadBloques(int cantidadBloques) { this.cantidadBloques = cantidadBloques; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    
    @Override
    public String toString() {
        return "Proceso{" + "ID=" + idProceso + ", Op=" + operacion + 
               ", Archivo=" + archivoObjetivo + ", Pos=" + posicionDestino + 
               ", Estado=" + estado + '}';
    }
}