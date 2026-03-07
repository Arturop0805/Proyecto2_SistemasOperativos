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

    public ListaSimple() {
        this.cabeza = null;
    }

    /**
     * Agrega un nuevo elemento al inicio de la lista.
     * Complejidad de tiempo: O(1)
     */
    public void agregarAlInicio(T dato) {
        Nodo<T> nuevoNodo = new Nodo<>(dato);
        nuevoNodo.setSiguiente(cabeza);
        cabeza = nuevoNodo;
    }

    /**
     * Elimina un nodo basándose en su posición (índice empezando en 0).
     * Complejidad de tiempo: O(n)
     */
    public void eliminarPorIndice(int indice) {
        // Validación básica: Si la lista está vacía o el índice es inválido
        if (cabeza == null || indice < 0) {
            System.out.println("No se puede eliminar: Índice inválido o lista vacía.");
            return;
        }

        // Caso especial: Eliminar el primer elemento (índice 0)
        if (indice == 0) {
            cabeza = cabeza.getSiguiente();
            return;
        }

        // Recorrer la lista para encontrar el nodo anterior al que queremos eliminar
        Nodo<T> actual = cabeza;
        for (int i = 0; i < indice - 1; i++) {
            if (actual.getSiguiente() == null) {
                System.out.println("No se puede eliminar: El índice excede el tamaño de la lista.");
                return; // Índice fuera de los límites
            }
            actual = actual.getSiguiente();
        }

        // Si el siguiente nodo existe, lo "saltamos" para eliminarlo
        if (actual.getSiguiente() != null) {
            actual.setSiguiente(actual.getSiguiente().getSiguiente());
        }
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
