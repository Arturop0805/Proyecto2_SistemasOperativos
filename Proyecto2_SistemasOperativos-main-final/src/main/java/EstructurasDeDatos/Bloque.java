/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package EstructurasDeDatos;

/**
 * Representa un Bloque individual dentro del Disco Simulado (SD).
 * Implementa la lógica para la asignación encadenada.
 * 
 * @author Artur
 */
public class Bloque {
    private int numeroBloque; // ID único del bloque en el disco (ej. 0 a 1023)
    private boolean ocupado; // Estado del bloque
    private String nombreArchivo;// Nombre del archivo al que pertenece (para la tabla FAT y colores GUI)
    private Bloque siguiente; // Puntero al siguiente bloque del archivo (asignación encadenada)

    public Bloque(int numeroBloque) {
        this.numeroBloque = numeroBloque;
        this.ocupado = false;
        this.nombreArchivo = null;
        this.siguiente = null;
    }

    public int getNumeroBloque() {
        return numeroBloque;
    }

    public boolean estaOcupado() {
        return ocupado;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public Bloque getSiguiente() {
        return siguiente;
    }

    /**
     * Ocupa el bloque con un archivo específico y un enlace al siguiente bloque.
     */
    public void ocupar(String nombreArchivo, Bloque siguiente) {
        this.ocupado = true;
        this.nombreArchivo = nombreArchivo;
        this.siguiente = siguiente;
    }

    /**
     * Limpia el bloque, dejándolo libre para futuros archivos.
     */
    public void liberar() {
        this.ocupado = false;
        this.nombreArchivo = null;
        this.siguiente = null;
    }

    // Setters manuales por si se necesita ajustar el enlace luego
    public void setSiguiente(Bloque siguiente) {
        this.siguiente = siguiente;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }
}
