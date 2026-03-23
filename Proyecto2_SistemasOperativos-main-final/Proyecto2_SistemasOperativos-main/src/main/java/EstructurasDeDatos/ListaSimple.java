/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package EstructurasDeDatos;

/**
 *
 * @author Artur
 */
public class ListaSimple<T> {
    private Nodo<T> cabeza;
    private int tamano;

    public ListaSimple() {
        this.cabeza = null;
        this.tamano = 0;
    }

    /**
     * Agrega un nuevo elemento al inicio de la lista.
     * Complejidad de tiempo: O(1)
     */
    public void agregarAlInicio(T dato) {
        Nodo<T> nuevoNodo = new Nodo<T>(dato);
        nuevoNodo.setSiguiente(cabeza);
        cabeza = nuevoNodo;
        tamano++;
    }

    /**
     * Inserta un elemento al final de la lista.
     * Complejidad de tiempo: O(n)
     */
    public void insertarAlFinal(T dato) {
        Nodo<T> nuevoNodo = new Nodo<T>(dato);
        if (cabeza == null) {
            cabeza = nuevoNodo;
        } else {
            Nodo<T> actual = cabeza;
            while (actual.getSiguiente() != null) {
                actual = actual.getSiguiente();
            }
            actual.setSiguiente(nuevoNodo);
        }
        tamano++;
    }

    /**
     * Obtiene un elemento por su índice (0 a tamano-1).
     * Retorna null si el índice es inválido.
     */
    public T obtener(int indice) {
        if (indice < 0 || indice >= tamano) {
            return null; // Podría levantar excepción, pero null evita crasheos rápidos
        }
        Nodo<T> actual = cabeza;
        for (int i = 0; i < indice; i++) {
            actual = actual.getSiguiente();
        }
        return actual.getDato();
    }

    /**
     * Elimina el primer elemento que coincida con 'dato'.
     * Retorna true si encontró y eliminó, false en caso contrario.
     */
    public boolean eliminar(T dato) {
        if (cabeza == null)
            return false;

        if (cabeza.getDato().equals(dato)) {
            cabeza = cabeza.getSiguiente();
            tamano--;
            return true;
        }

        Nodo<T> actual = cabeza;
        while (actual.getSiguiente() != null && !actual.getSiguiente().getDato().equals(dato)) {
            actual = actual.getSiguiente();
        }

        if (actual.getSiguiente() != null) {
            actual.setSiguiente(actual.getSiguiente().getSiguiente());
            tamano--;
            return true;
        }
        return false;
    }

    /**
     * Elimina un nodo basándose en su posición (índice empezando en 0).
     * Complejidad de tiempo: O(n)
     */
    public void eliminarPorIndice(int indice) {
        if (cabeza == null || indice < 0 || indice >= tamano) {
            System.out.println("No se puede eliminar: Índice inválido o lista vacía.");
            return;
        }

        if (indice == 0) {
            cabeza = cabeza.getSiguiente();
            tamano--;
            return;
        }

        Nodo<T> actual = cabeza;
        for (int i = 0; i < indice - 1; i++) {
            actual = actual.getSiguiente();
        }

        if (actual.getSiguiente() != null) {
            actual.setSiguiente(actual.getSiguiente().getSiguiente());
            tamano--;
        }
    }

    public boolean estaVacia() {
        return cabeza == null;
    }

    public int obtenerTamano() {
        return tamano;
    }

    /**
     * Imprime todos los elementos de la lista en orden.
     */
    public void imprimir() {
        Nodo<T> actual = cabeza;
        if (actual == null) {
            System.out.println("La lista está vacía.");
            return;
        }

        while (actual != null) {
            System.out.print(actual.getDato() + " -> ");
            actual = actual.getSiguiente();
        }
        System.out.println("null");
    }
}
