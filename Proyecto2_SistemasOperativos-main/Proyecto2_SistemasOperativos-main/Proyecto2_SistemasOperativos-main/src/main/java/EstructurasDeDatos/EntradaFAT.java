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
    
    // Control de Concurrencia (Reader-Writer Locks)
    private int contLectores = 0;
    private java.util.concurrent.Semaphore lockEscritura = new java.util.concurrent.Semaphore(1);
    private java.util.concurrent.Semaphore lockLectores = new java.util.concurrent.Semaphore(1);

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

    // ===================================
    // MÉTODOS DE CONCURRENCIA (SEMÁFOROS)
    // ===================================

    public void adquirirLockLectura() {
        try {
            lockLectores.acquire();
            contLectores++;
            if (contLectores == 1) {
                // El primer lector bloquea a los escritores
                lockEscritura.acquire();
            }
            lockLectores.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void liberarLockLectura() {
        try {
            lockLectores.acquire();
            contLectores--;
            if (contLectores == 0) {
                // El último lector desbloquea a los escritores
                lockEscritura.release();
            }
            lockLectores.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void adquirirLockEscritura() {
        try {
            lockEscritura.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void liberarLockEscritura() {
        lockEscritura.release();
    }
}
