package Modelo;

import java.util.Objects;

/**
 * Representa la información almacenada en cada nodo del Árbol N-ario.
 * Identifica si el nodo es un directorio o un archivo, su nombre y tamaño en bloques.
 */
public class MetadatoArchivo {
    private String nombre;
    private boolean esDirectorio;
    private int tamanoEnBloques; // 0 si es directorio

    public MetadatoArchivo(String nombre, boolean esDirectorio, int tamanoEnBloques) {
        this.nombre = nombre;
        this.esDirectorio = esDirectorio;
        this.tamanoEnBloques = esDirectorio ? 0 : tamanoEnBloques;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public boolean isEsDirectorio() {
        return esDirectorio;
    }

    public void setEsDirectorio(boolean esDirectorio) {
        this.esDirectorio = esDirectorio;
        if (esDirectorio) this.tamanoEnBloques = 0;
    }

    public int getTamanoEnBloques() {
        return tamanoEnBloques;
    }

    public void setTamanoEnBloques(int tamanoEnBloques) {
        if (!this.esDirectorio) {
            this.tamanoEnBloques = tamanoEnBloques;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetadatoArchivo that = (MetadatoArchivo) o;
        return nombre.equals(that.nombre);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(nombre);
    }

    @Override
    public String toString() {
        return nombre + (esDirectorio ? " (Dir)" : " (" + tamanoEnBloques + " bloques)");
    }
}
