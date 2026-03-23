/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package EstructurasDeDatos;

/**
 * Simulación del Disco (SD) que controla un arreglo de Bloques.
 * 
 * @author Artur
 */
public class DiscoV {
    private Bloque[] bloques;
    private int capacidadTotal;
    private int bloquesOcupados;

    public DiscoV(int capacidadTotal) {
        this.capacidadTotal = capacidadTotal;
        this.bloques = new Bloque[capacidadTotal];
        this.bloquesOcupados = 0;

        // Inicializar todos los bloques del disco
        for (int i = 0; i < capacidadTotal; i++) {
            bloques[i] = new Bloque(i);
        }
    }

    public int getCapacidadTotal() {
        return capacidadTotal;
    }

    public int getBloquesLibres() {
        return capacidadTotal - bloquesOcupados;
    }

    public Bloque getBloque(int indice) {
        if (indice >= 0 && indice < capacidadTotal) {
            return bloques[indice];
        }
        return null;
    }

    /**
     * Asigna una cantidad de bloques a un archivo usando asignación encadenada.
     * Retorna el bloque inicial si hay éxito, o null si no hay espacio.
     */
    public Bloque asignarBloques(String nombreArchivo, int cantidadBuscada) {
        if (cantidadBuscada > getBloquesLibres()) {
            System.out.println("Error: No hay espacio suficiente en el disco para " + nombreArchivo);
            return null; // No hay espacio suficiente
        }

        Bloque bloqueInicial = null;
        Bloque bloqueAnterior = null;
        int bloquesAsignados = 0;

        // Recorrer el disco buscando bloques libres
        for (int i = 0; i < capacidadTotal && bloquesAsignados < cantidadBuscada; i++) {
            if (!bloques[i].estaOcupado()) {
                // Si es el primer bloque que encontramos, lo guardamos como inicial
                if (bloqueInicial == null) {
                    bloqueInicial = bloques[i];
                    bloqueInicial.ocupar(nombreArchivo, null);
                    bloqueAnterior = bloqueInicial;
                } else {
                    // Enlazar el bloque anterior con este nuevo bloque (asignación encadenada)
                    bloques[i].ocupar(nombreArchivo, null);
                    bloqueAnterior.setSiguiente(bloques[i]);
                    bloqueAnterior = bloques[i];
                }
                bloquesAsignados++;
                bloquesOcupados++;
            }
        }

        return bloqueInicial;
    }

    /**
     * Libera todos los bloques encadenados a partir de un bloque inicial.
     */
    public void liberarBloques(Bloque bloqueInicial) {
        Bloque actual = bloqueInicial;
        while (actual != null) {
            Bloque siguiente = actual.getSiguiente();
            actual.liberar();
            bloquesOcupados--;
            actual = siguiente;
        }
    }
}
