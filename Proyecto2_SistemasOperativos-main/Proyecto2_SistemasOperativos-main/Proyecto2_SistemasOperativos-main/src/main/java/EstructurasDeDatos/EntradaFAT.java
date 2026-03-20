/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package EstructurasDeDatos;

/**
 * Representa una entrada individual en la Tabla de Asignación de Archivos
 * (FAT).
 * 
 * @author Artur
 */
public class EntradaFAT {
    private String nombreArchivo;
    private int cantidadBloques;
    private int direccionPrimerBloque;
    private String propietario;

    public EntradaFAT(String nombreArchivo, int cantidadBloques, int direccionPrimerBloque, String propietario) {
        this.nombreArchivo = nombreArchivo;
        this.cantidadBloques = cantidadBloques;
        this.direccionPrimerBloque = direccionPrimerBloque;
        this.propietario = propietario;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public int getCantidadBloques() {
        return cantidadBloques;
    }

    public void setCantidadBloques(int cantidadBloques) {
        this.cantidadBloques = cantidadBloques;
    }

    public int getDireccionPrimerBloque() {
        return direccionPrimerBloque;
    }

    public void setDireccionPrimerBloque(int direccionPrimerBloque) {
        this.direccionPrimerBloque = direccionPrimerBloque;
    }

    public String getPropietario() {
        return propietario;
    }

    public void setPropietario(String propietario) {
        this.propietario = propietario;
    }
}
