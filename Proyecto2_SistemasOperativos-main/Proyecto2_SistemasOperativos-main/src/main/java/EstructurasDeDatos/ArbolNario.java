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
}
