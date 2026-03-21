/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Modelo;


import EstructurasDeDatos.ListaSimple;
import Controlador.Config;
/**
 * Planificador de disco que gestiona las solicitudes de E/S.
 * Determina el orden de ejecución basado en políticas de disco.
 */
public class Planificador {
    
    // Usamos ListaSimple en lugar de Cola para poder extraer procesos fuera de orden (necesario para SSTF, SCAN, etc.)
    private ListaSimple<Proceso> colaListos; 
    private int posicionCabezal;
    private String politicaActiva;
    private boolean direccionArriba; // true para SCAN/C-SCAN ascendente (moviéndose hacia bloques mayores)

    // Historial de movimientos del cabezal (para mostrar en la UI y calcular distancias)
    private ListaSimple<Integer> historialMovimientos;
    private int distanciaTotalRecorrida;

    public Planificador(int cabezalInicial) {
        this.colaListos = new ListaSimple<Proceso>();
        this.posicionCabezal = Config.POSICION_INICIAL_CABEZAL;
        this.politicaActiva = Config.POLITICA_DEFECTO;
        this.direccionArriba = true; // Por defecto el cabezal sube
        this.historialMovimientos = new ListaSimple<Integer>();
        this.distanciaTotalRecorrida = 0;
        // Registrar posición inicial
        this.historialMovimientos.insertarAlFinal(this.posicionCabezal);
    }


    public void agregarProceso(Proceso p) {
        p.pasarAListo();
        colaListos.insertarAlFinal(p);
    }

   
    public Proceso obtenerSiguienteProceso() {
        if (colaListos.obtenerTamano() == 0) {
            return null; // No hay procesos pendientes
        }

        int indiceSeleccionado = -1;

        switch (politicaActiva.toUpperCase()) {
            case "FIFO":
                indiceSeleccionado = 0; // Siempre el primero en llegar
                break;
            case "SSTF":
                indiceSeleccionado = buscarIndiceSSTF();
                break;
            case "SCAN":
                indiceSeleccionado = buscarIndiceSCAN();
                break;
            case "C-SCAN":
                indiceSeleccionado = buscarIndiceCSCAN();
                break;
            default:
                indiceSeleccionado = 0;
        }

        // Extraemos el proceso seleccionado
        Proceso procesoEjecutar = colaListos.obtener(indiceSeleccionado);
        colaListos.eliminarPorIndice(indiceSeleccionado);
        
        // Actualizamos la posición del cabezal y registramos el movimiento
        int posAnterior = this.posicionCabezal;
        this.posicionCabezal = procesoEjecutar.getPosicionDestino();
        int distancia = Math.abs(this.posicionCabezal - posAnterior);
        this.distanciaTotalRecorrida += distancia;
        this.historialMovimientos.insertarAlFinal(this.posicionCabezal);
        procesoEjecutar.ejecutar();
        
        return procesoEjecutar;
    }

    // --- ALGORITMOS DE PLANIFICACIÓN DE DISCO ---

    private int buscarIndiceSSTF() {
        int indiceMin = 0;
        int distanciaMinima = Integer.MAX_VALUE;

        for (int i = 0; i < colaListos.obtenerTamano(); i++) {
            Proceso p = colaListos.obtener(i);
            int distancia = Math.abs(p.getPosicionDestino() - posicionCabezal);
            if (distancia < distanciaMinima) {
                distanciaMinima = distancia;
                indiceMin = i;
            }
        }
        return indiceMin;
    }

  
    private int buscarIndiceSCAN() {
        int indiceSeleccionado = buscarMasCercanoEnDireccionActual();

        // Si no hay solicitudes en la dirección actual, invertimos la dirección y volvemos a buscar
        if (indiceSeleccionado == -1) {
            direccionArriba = !direccionArriba; // Cambiamos de sentido
            indiceSeleccionado = buscarMasCercanoEnDireccionActual();
        }
        return indiceSeleccionado != -1 ? indiceSeleccionado : 0; // Fallback de seguridad
    }

   
    private int buscarIndiceCSCAN() {
        int indiceSeleccionado = buscarMasCercanoEnDireccionActual();

        // Si no hay solicitudes en la dirección actual, simulamos que el cabezal "da la vuelta" al inicio del disco
        if (indiceSeleccionado == -1) {
            // Buscamos la posición más baja (como si el cabezal hubiera vuelto a 0)
            int indiceMin = 0;
            int posMinima = Integer.MAX_VALUE;
            for (int i = 0; i < colaListos.obtenerTamano(); i++) {
                Proceso p = colaListos.obtener(i);
                if (p.getPosicionDestino() < posMinima) {
                    posMinima = p.getPosicionDestino();
                    indiceMin = i;
                }
            }
            return indiceMin;
        }
        return indiceSeleccionado;
    }

  
    private int buscarMasCercanoEnDireccionActual() {
        int indiceMin = -1;
        int distanciaMinima = Integer.MAX_VALUE;

        for (int i = 0; i < colaListos.obtenerTamano(); i++) {
            Proceso p = colaListos.obtener(i);
            
            if (direccionArriba && p.getPosicionDestino() >= posicionCabezal) {
                int distancia = p.getPosicionDestino() - posicionCabezal;
                if (distancia < distanciaMinima) {
                    distanciaMinima = distancia;
                    indiceMin = i;
                }
            } else if (!direccionArriba && p.getPosicionDestino() <= posicionCabezal) {
                int distancia = posicionCabezal - p.getPosicionDestino();
                if (distancia < distanciaMinima) {
                    distanciaMinima = distancia;
                    indiceMin = i;
                }
            }
        }
        return indiceMin;
    }

    // --- GETTERS Y SETTERS ---
    public int getPosicionCabezal() { return posicionCabezal; }
    public void setPosicionCabezal(int posicionCabezal) { this.posicionCabezal = posicionCabezal; }
    
    public String getPoliticaActiva() { return politicaActiva; }
    public void setPoliticaActiva(String politicaActiva) {
        this.politicaActiva = politicaActiva;
        // Al cambiar política, resetear historial para mostrar resultados limpios
        resetearHistorial();
    }

    public boolean isDireccionArriba() { return direccionArriba; }
    public void setDireccionArriba(boolean direccionArriba) { this.direccionArriba = direccionArriba; }
    
    public ListaSimple<Proceso> getColaListos() { return colaListos; }

    // --- HISTORIAL DE MOVIMIENTOS ---
    public ListaSimple<Integer> getHistorialMovimientos() { return historialMovimientos; }
    public int getDistanciaTotalRecorrida() { return distanciaTotalRecorrida; }

    /**
     * Resetea el historial de movimientos para comenzar una nueva simulación de política.
     */
    public void resetearHistorial() {
        this.historialMovimientos = new ListaSimple<Integer>();
        this.distanciaTotalRecorrida = 0;
        this.historialMovimientos.insertarAlFinal(this.posicionCabezal);
    }

    /**
     * Genera un resumen legible del historial de movimientos del cabezal.
     */
    public String getResumenMovimientos() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Política: ").append(politicaActiva).append(" ===\n");
        sb.append("Secuencia: ");
        EstructurasDeDatos.Nodo<Integer> actual = historialMovimientos.getInicio();
        while (actual != null) {
            sb.append(actual.getDato());
            if (actual.getSiguiente() != null) sb.append(" → ");
            actual = actual.getSiguiente();
        }
        sb.append("\nDistancia total recorrida: ").append(distanciaTotalRecorrida).append(" bloques\n");
        return sb.toString();
    }
}