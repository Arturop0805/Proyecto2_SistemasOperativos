/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package EstructurasDeDatos;

/**
 *
 * @author Artur
 */
public class Cola<T> {
    private Nodo<T> frente;
    private Nodo<T> finalCola;

    public Cola() {
        this.frente = null;
        this.finalCola = null;
    }

    /**
     * Agrega un elemento al final de la cola.
     */
    public void encolar(T dato) {
        Nodo<T> nuevoNodo = new Nodo<>(dato);

        // Si la cola está vacía, el frente y el final son el mismo nodo
        if (finalCola == null) {
            frente = nuevoNodo;
            finalCola = nuevoNodo;
            return;
        }

        // Conectar el nuevo nodo al final y actualizar el puntero 'finalCola'
        finalCola.setSiguiente(nuevoNodo);
        finalCola = nuevoNodo;
    }

    /**
     * Retira y devuelve el elemento al frente de la cola.
     * Retorna null si la cola está vacía.
     */
    public T desencolar() {
        // Validar si la cola está vacía
        if (frente == null) {
            System.out.println("La cola está vacía, no hay nada que desencolar.");
            return null; 
        }

        // Extraer el dato del frente
        T dato = frente.getDato();
        
        // Mover el frente al siguiente nodo
        frente = frente.getSiguiente();

        // Si el frente se volvió nulo, significa que la cola quedó vacía,
        // por lo tanto, el final también debe ser nulo.
        if (frente == null) {
            finalCola = null;
        }

        return dato;
    }
}
