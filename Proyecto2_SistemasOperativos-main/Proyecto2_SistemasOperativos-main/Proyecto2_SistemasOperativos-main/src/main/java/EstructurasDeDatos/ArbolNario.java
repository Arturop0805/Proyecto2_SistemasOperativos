/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package EstructurasDeDatos;
/**
 * Estructura de Árbol N-ario genérica para la jerarquía de directorios.
 * 
 * @author Artur
 */
public class ArbolNario<T> {
    private NodoArbol<T> raiz;
    
    public ArbolNario() {
        this.raiz = null;
    }

    public ArbolNario(T datoRaiz) {
        this.raiz = new NodoArbol<T>(datoRaiz);
    }

    public NodoArbol<T> getRaiz() {
        return raiz;
    }

    public void setRaiz(NodoArbol<T> raiz) {
        this.raiz = raiz;
    }

    public boolean estaVacio() {
        return raiz == null;
    }

    /**
     * Busca un nodo en el árbol mediante DFS (Búsqueda en Profundidad).
     * Retorna null si no lo encuentra.
     */
    public NodoArbol<T> buscarNodo(T datoBuscado) {
        if (estaVacio()) return null;
        return buscarRecursivo(raiz, datoBuscado);
    }

    private NodoArbol<T> buscarRecursivo(NodoArbol<T> actual, T datoBuscado) {
        if (actual.getDato().equals(datoBuscado)) {
            return actual;
        }

        ListaSimple<NodoArbol<T>> hijos = actual.getHijos();
        int tamano = hijos.obtenerTamano();
        for (int i = 0; i < tamano; i++) {
            NodoArbol<T> resultado = buscarRecursivo(hijos.obtener(i), datoBuscado);
            if (resultado != null) {
                return resultado;
            }
        }
        return null;
    }

    /**
     * Elimina todos los descendientes de un nodo dado.
     */
    public void eliminarSubarbol(NodoArbol<T> nodo) {
        if (nodo == null) return;
        
        ListaSimple<NodoArbol<T>> hijos = nodo.getHijos();
        int tamano = hijos.obtenerTamano();
        for (int i = 0; i < tamano; i++) {
            eliminarSubarbol(hijos.obtener(i));
        }
        hijos.vaciar();
    }
}
