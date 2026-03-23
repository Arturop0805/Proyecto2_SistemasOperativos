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
import Modelo.MetadatoArchivo;
public class ArbolNario {
    private NodoArbol raiz;
    
    public ArbolNario() {
        this.raiz = null;
    }

    public ArbolNario(MetadatoArchivo datoRaiz) {
        this.raiz = new NodoArbol(datoRaiz);
    }

    public NodoArbol getRaiz() {
        return raiz;
    }

    public void setRaiz(NodoArbol raiz) {
        this.raiz = raiz;
    }

    public boolean estaVacio() {
        return raiz == null;
    }

    /**
     * Busca un nodo en el árbol mediante DFS (Búsqueda en Profundidad).
     * Retorna null si no lo encuentra.
     */
    public NodoArbol buscarNodo(MetadatoArchivo datoBuscado) {
        if (estaVacio()) return null;
        return buscarRecursivo(raiz, datoBuscado);
    }

    private NodoArbol buscarRecursivo(NodoArbol actual, MetadatoArchivo datoBuscado) {
        if (actual.getDato().equals(datoBuscado)) {
            return actual;
        }

        ListaSimple<NodoArbol> hijos = actual.getHijos();
        int tamano = hijos.obtenerTamano();
        for (int i = 0; i < tamano; i++) {
            NodoArbol resultado = buscarRecursivo(hijos.obtener(i), datoBuscado);
            if (resultado != null) {
                return resultado;
            }
        }
        return null;
    }

    /**
     * Elimina todos los descendientes de un nodo dado.
     */
    public void eliminarSubarbol(NodoArbol nodo) {
        if (nodo == null) return;
        
        ListaSimple<NodoArbol> hijos = nodo.getHijos();
        int tamano = hijos.obtenerTamano();
        for (int i = 0; i < tamano; i++) {
            eliminarSubarbol(hijos.obtener(i));
        }
        hijos.vaciar();
    }

    /**
     * Inserta una carpeta en la ruta especificada, creando carpetas intermedias si no existen.
     */
    public void insertarCarpeta(String ruta) {
        if (ruta == null || ruta.isEmpty() || raiz == null) return;

        // En el arbol tenemos una raíz 'SSD'. Queremos evitar crear un subdirectorio
        // adicional 'SSD' si la ruta también contiene el nombre de raíz.
        String raizNombre = raiz.getDato().getNombre();
        String[] partes = ruta.split("/");
        int inicio = 0;
        if (partes.length > 0 && partes[0].equalsIgnoreCase(raizNombre)) {
            inicio = 1;
        }

        NodoArbol current = raiz;
        for (int i = inicio; i < partes.length; i++) {
            String parte = partes[i];
            if (parte == null || parte.isEmpty()) continue;
            ListaSimple<NodoArbol> hijos = current.getHijos();
            NodoArbol found = null;
            for (int j = 0; j < hijos.obtenerTamano(); j++) {
                NodoArbol hijo = hijos.obtener(j);
                MetadatoArchivo meta = hijo.getDato();
                if (meta.getNombre().equals(parte) && meta.isEsDirectorio()) {
                    found = hijo;
                    break;
                }
            }
            if (found == null) {
                MetadatoArchivo nueva = new MetadatoArchivo(parte, true, 0);
                NodoArbol nuevoNodo = new NodoArbol(nueva);
                current.getHijos().insertarAlFinal(nuevoNodo);
                current = nuevoNodo;
            } else {
                current = found;
            }
        }
    }

    public boolean insertarArchivo(String rutaArchivo, int tamano) {
        if (rutaArchivo == null || rutaArchivo.isEmpty() || raiz == null) return false;

        String raizNombre = raiz.getDato().getNombre();
        String[] partes = rutaArchivo.split("/");
        int inicio = 0;
        if (partes.length > 0 && partes[0].equalsIgnoreCase(raizNombre)) {
            inicio = 1;
        }
        if (partes.length - inicio < 1) {
            return false;
        }

        // Determinar carpeta padre y nombre de archivo
        String nombreArchivo = partes[partes.length - 1];
        StringBuilder rutaPadreBuilder = new StringBuilder(raizNombre);
        for (int i = inicio; i < partes.length - 1; i++) {
            String parte = partes[i];
            if (parte == null || parte.isEmpty()) continue;
            rutaPadreBuilder.append("/").append(parte);
        }
        String rutaPadre = rutaPadreBuilder.toString();

        // Asegurar creación de la carpeta padre
        insertarCarpeta(rutaPadre);
        NodoArbol padre = encontrarNodoPorRuta(rutaPadre);
        if (padre == null) return false;

        // Si el archivo ya existe en el nodo, no duplicar
        ListaSimple<NodoArbol> hijos = padre.getHijos();
        for (int i = 0; i < hijos.obtenerTamano(); i++) {
            NodoArbol hijo = hijos.obtener(i);
            MetadatoArchivo meta = hijo.getDato();
            if (!meta.isEsDirectorio() && meta.getNombre().equals(nombreArchivo)) {
                return false;
            }
        }

        NodoArbol archivoNodo = new NodoArbol(new MetadatoArchivo(nombreArchivo, false, tamano));
        padre.getHijos().insertarAlFinal(archivoNodo);
        return true;
    }

    public boolean eliminarArchivo(String rutaArchivo) {
        if (rutaArchivo == null || rutaArchivo.isEmpty() || raiz == null) return false;

        String raizNombre = raiz.getDato().getNombre();
        String[] partes = rutaArchivo.split("/");
        int inicio = 0;
        if (partes.length > 0 && partes[0].equalsIgnoreCase(raizNombre)) {
            inicio = 1;
        }
        if (partes.length - inicio < 1) {
            return false;
        }

        String nombreArchivo = partes[partes.length - 1];
        StringBuilder rutaPadreBuilder = new StringBuilder(raizNombre);
        for (int i = inicio; i < partes.length - 1; i++) {
            String parte = partes[i];
            if (parte == null || parte.isEmpty()) continue;
            rutaPadreBuilder.append("/").append(parte);
        }
        String rutaPadre = rutaPadreBuilder.toString();

        NodoArbol padre = encontrarNodoPorRuta(rutaPadre);
        if (padre == null) return false;

        ListaSimple<NodoArbol> hijos = padre.getHijos();
        for (int i = 0; i < hijos.obtenerTamano(); i++) {
            NodoArbol hijo = hijos.obtener(i);
            MetadatoArchivo meta = hijo.getDato();
            if (!meta.isEsDirectorio() && meta.getNombre().equals(nombreArchivo)) {
                hijos.eliminarPorIndice(i);
                return true;
            }
        }

        return false;
    }

    /**
     * Encuentra el nodo correspondiente a la ruta especificada.
     */
    public NodoArbol encontrarNodoPorRuta(String ruta) {
        if (ruta == null || ruta.isEmpty() || raiz == null) return raiz;

        String raizNombre = raiz.getDato().getNombre();

        if (ruta.equals(raizNombre)) {
            return raiz;
        }

        String rutaNormalizada = ruta;
        if (ruta.startsWith(raizNombre + "/")) {
            rutaNormalizada = ruta.substring((raizNombre + "/").length());
        }

        String[] partes = rutaNormalizada.split("/");
        NodoArbol current = raiz;
        for (String parte : partes) {
            if (parte == null || parte.isEmpty()) continue;

            ListaSimple<NodoArbol> hijos = current.getHijos();
            boolean found = false;
            for (int j = 0; j < hijos.obtenerTamano(); j++) {
                NodoArbol hijo = hijos.obtener(j);
                MetadatoArchivo meta = hijo.getDato();
                if (meta.getNombre().equals(parte) && meta.isEsDirectorio()) {
                    current = hijo;
                    found = true;
                    break;
                }
            }

            if (!found) {
                return null;
            }
        }

        return current;
    }

    /**
     * Obtiene las subcarpetas directas de la ruta especificada.
     */
    public ListaSimple<MetadatoArchivo> getSubcarpetas(String ruta) {
        NodoArbol nodo = encontrarNodoPorRuta(ruta);
        ListaSimple<MetadatoArchivo> subcarpetas = new ListaSimple<>();
        if (nodo != null) {
            ListaSimple<NodoArbol> hijos = nodo.getHijos();
            for (int i = 0; i < hijos.obtenerTamano(); i++) {
                MetadatoArchivo dato = hijos.obtener(i).getDato();
                if (dato.isEsDirectorio()) {
                    subcarpetas.insertarAlFinal(dato);
                }
            }
        }
        return subcarpetas;
    }
}
