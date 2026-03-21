package EstructurasDeDatos;

public class Transaccion {
    public static final String ESTADO_PENDIENTE = "PENDIENTE";
    public static final String ESTADO_CONFIRMADA = "CONFIRMADA";

    private int id;
    private String operacion; // CREATE, DELETE, UPDATE
    private String archivo;
    private int tamanoAnterior;
    private int bloqueAnterior;
    private String estado;
    private String propietario;

    public Transaccion(int id, String operacion, String archivo, int tamanoAnterior, int bloqueAnterior, String propietario) {
        this.id = id;
        this.operacion = operacion;
        this.archivo = archivo;
        this.tamanoAnterior = tamanoAnterior;
        this.bloqueAnterior = bloqueAnterior;
        this.propietario = propietario;
        this.estado = ESTADO_PENDIENTE;
    }

    public int getId() { return id; }
    public String getOperacion() { return operacion; }
    public String getArchivo() { return archivo; }
    public int getTamanoAnterior() { return tamanoAnterior; }
    public int getBloqueAnterior() { return bloqueAnterior; }
    public void setBloqueAnterior(int bloqueAnterior) { this.bloqueAnterior = bloqueAnterior; }
    public void setTamanoAnterior(int tamanoAnterior) { this.tamanoAnterior = tamanoAnterior; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getPropietario() { return propietario; }

    @Override
    public String toString() {
        return "T" + id + ": " + operacion + " " + archivo + " [" + estado + "]";
    }
}
