package EstructurasDeDatos;

import Modelo.MetadatoArchivo;

public class MainEstructurasTest {

    public static void main(String[] args) {
        System.out.println("=== TEST FASE 1: Estructuras y Disco ===\n");

        // 1. Probar el Disco y FAT
        DiscoV disco = new DiscoV(100); // 100 bloques
        TablaAsignacion fat = new TablaAsignacion();

        System.out.println("Capacidad inicial del disco: " + disco.getCapacidadTotal());
        System.out.println("Bloques libres: " + disco.getBloquesLibres());

        // Simulando creación de un archivo
        String nombreArch1 = "documento.txt";
        int tamanoArch1 = 5;
        Bloque bInicial = disco.asignarBloques(nombreArch1, tamanoArch1);
        
        if (bInicial != null) {
            fat.registrarArchivo(nombreArch1, tamanoArch1, bInicial.getNumeroBloque(), "Admin");
            System.out.println("Archivo " + nombreArch1 + " asignado en disco iniciando en bloque: " + bInicial.getNumeroBloque());
        }
        System.out.println("Bloques libres tras asignación: " + disco.getBloquesLibres() + "\n");

        // 2. Probar el Árbol N-Ario
        ArbolNario arbol = new ArbolNario(new MetadatoArchivo("Raiz", true, 0));
        NodoArbol carpetaDocs = new NodoArbol(new MetadatoArchivo("Documentos", true, 0));
        NodoArbol archivoTxt = new NodoArbol(new MetadatoArchivo(nombreArch1, false, tamanoArch1));

        arbol.getRaiz().agregarHijo(carpetaDocs);
        carpetaDocs.agregarHijo(archivoTxt);

        System.out.println("Árbol construido con Raiz -> Documentos -> documento.txt");

        // 3. Probar Búsqueda DFS
        System.out.println("Buscando 'documento.txt'...");
        MetadatoArchivo target = new MetadatoArchivo("documento.txt", false, 0); // El equals evalúa por nombre
        NodoArbol encontrado = arbol.buscarNodo(target);
        
        if (encontrado != null) {
            System.out.println("ÉXITO: Encontrado el nodo: " + encontrado.getDato().toString());
        } else {
            System.out.println("FALLO: No se encontró el nodo.");
        }

        // 4. Probar Borrado Recursivo y Liberación en Disco
        System.out.println("\nSimulando borrar la carpeta 'Documentos'...");
        MetadatoArchivo targetCarpeta = new MetadatoArchivo("Documentos", true, 0);
        NodoArbol nodoCarpeta = arbol.buscarNodo(targetCarpeta);

        if (nodoCarpeta != null) {
            // Eliminamos todos los hijos recursivamente de la estructura
            arbol.eliminarSubarbol(nodoCarpeta);
            // Lo desenlazamos del padre (Raiz)
            arbol.getRaiz().eliminarHijo(targetCarpeta);
            
            // Simular limpieza en disco para los archivos eliminados (requeriría interactuar con FAT en código real)
            EntradaFAT entrada = fat.buscarArchivo(nombreArch1);
            if (entrada != null) {
                disco.liberarBloques(disco.getBloque(entrada.getDireccionPrimerBloque()));
                fat.eliminarArchivo(nombreArch1);
                System.out.println("Archivo eliminado del Disco y FAT.");
            }
        }

        System.out.println("Bloques libres al final: " + disco.getBloquesLibres());
        System.out.println("Buscando 'documento.txt' tras el borrado: " + (arbol.buscarNodo(target) == null ? "No existe (Correcto)" : "Aún existe (Fallo)"));
    }
}
