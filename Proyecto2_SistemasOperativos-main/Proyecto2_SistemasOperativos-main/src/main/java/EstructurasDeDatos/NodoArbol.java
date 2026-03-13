/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package EstructurasDeDatos;

/**
 * Representa un nodo en un árbol N-ario, capaz de tener múltiples hijos.
 * 
 * @author Artur
 */
public class NodoArbol<T> {
    private T dato;
    private ListaSimple<NodoArbol<T>> hijos;

    public NodoArbol(T dato) {
        this.dato = dato;
        this.hijos = new ListaSimple<NodoArbol<T>>();
    }

    public T getDato() {
        return dato;
    }

    public void setDato(T dato) {
        this.dato = dato;
    }

    public ListaSimple<NodoArbol<T>> getHijos() {
        return hijos;
    }

    public void agregarHijo(NodoArbol<T> hijo) {
        this.hijos.insertarAlFinal(hijo);
    }
}
