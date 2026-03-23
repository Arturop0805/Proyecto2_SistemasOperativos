/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package EstructurasDeDatos;

import Modelo.MetadatoArchivo;

/**
 * Representa un nodo en un árbol N-ario, capaz de tener múltiples hijos.
 * 
 * @author Artur
 */
public class NodoArbol {
    private MetadatoArchivo dato;
    private ListaSimple<NodoArbol> hijos;

    public NodoArbol(MetadatoArchivo dato) {
        this.dato = dato;
        this.hijos = new ListaSimple<NodoArbol>();
    }

    public MetadatoArchivo getDato() {
        return dato;
    }

    public void setDato(MetadatoArchivo dato) {
        this.dato = dato;
    }

    public ListaSimple<NodoArbol> getHijos() {
        return hijos;
    }

    public void agregarHijo(NodoArbol hijo) {
        this.hijos.insertarAlFinal(hijo);
    }

    /**
     * Elimina un hijo directo de este nodo en función del dato o metadato.
     * Necesita que el tipo T implemente equals adecuadamente.
     */
    public boolean eliminarHijo(MetadatoArchivo datoBuscado) {
        int tamanoLista = hijos.obtenerTamano();
        for (int i = 0; i < tamanoLista; i++) {
            NodoArbol hijoActual = hijos.obtener(i);
            if (hijoActual != null && hijoActual.getDato().equals(datoBuscado)) {
                hijos.eliminarPorIndice(i);
                return true;
            }
        }
        return false;
    }
}
