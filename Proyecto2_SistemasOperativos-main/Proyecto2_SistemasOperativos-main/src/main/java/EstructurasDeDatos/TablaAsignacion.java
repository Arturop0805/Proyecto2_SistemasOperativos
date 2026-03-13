/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package EstructurasDeDatos;

/**
 * Tabla de Asignación de Archivos (FAT)
 * Controla qué archivos están en el disco y dónde inician.
 * 
 * @author Artur
 */
public class TablaAsignacion {
    private ListaSimple<EntradaFAT> entradas;

    public TablaAsignacion() {
        this.entradas = new ListaSimple<EntradaFAT>();
    }

    /**
     * Registra un nuevo archivo en la FAT.
     */
    public void registrarArchivo(String nombreArchivo, int cantidadBloques, int direccionPrimerBloque, String propietario) {
        EntradaFAT nuevaEntrada = new EntradaFAT(nombreArchivo, cantidadBloques, direccionPrimerBloque, propietario);
        entradas.insertarAlFinal(nuevaEntrada);
    }

    /**
     * Elimina el registro de un archivo en la FAT por su nombre.
     */
    public void eliminarArchivo(String nombreArchivo) {
        // Necesitamos iterar manualmente porque nuestra ListaSimple evalúa por objeto
        // exacto
        int tamano = entradas.obtenerTamano();
        for (int i = 0; i < tamano; i++) {
            EntradaFAT entrada = entradas.obtener(i);
            if (entrada != null && entrada.getNombreArchivo().equals(nombreArchivo)) {
                entradas.eliminarPorIndice(i);
                return; // Archivo encontrado y eliminado
            }
        }
    }

    /**
     * Busca y retorna la entrada de un archivo en la FAT.
     */
    public EntradaFAT buscarArchivo(String nombreArchivo) {
        int tamano = entradas.obtenerTamano();
        for (int i = 0; i < tamano; i++) {
            EntradaFAT entrada = entradas.obtener(i);
            if (entrada != null && entrada.getNombreArchivo().equals(nombreArchivo)) {
                return entrada;
            }
        }
        return null;
    }

    public ListaSimple<EntradaFAT> getEntradas() {
        return entradas;
    }
}
