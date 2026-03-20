/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Controlador;

/**
 * Parámetros globales y constantes de la simulación del Sistema de Archivos.
 * Centraliza la configuración para evitar "magic numbers" y "hardcoded strings".
 */
public class Config {
    
    // ==========================================
    // CONFIGURACIÓN DEL DISCO (SD)
    // ==========================================
    public static final int CAPACIDAD_DISCO_DEFECTO = 512; // Cantidad de bloques
    public static final int POSICION_INICIAL_CABEZAL = 0;   // Bloque donde inicia el lector
    
    // ==========================================
    // SISTEMA DE ARCHIVOS
    // ==========================================
    public static final String NOMBRE_DIRECTORIO_RAIZ = "SSD";
    
    // ==========================================
    // ROLES Y PERMISOS
    // ==========================================
    public static final String ROL_ADMIN = "Administrador";
    public static final String ROL_USER = "Usuario";
    
    // ==========================================
    // OPERACIONES DE E/S (CRUD)
    // ==========================================
    public static final String OP_CREATE = "CREATE";
    public static final String OP_READ   = "READ";
    public static final String OP_UPDATE = "UPDATE";
    public static final String OP_DELETE = "DELETE";
    
    // ==========================================
    // PLANIFICADOR Y SIMULACIÓN
    // ==========================================
    public static final String POLITICA_DEFECTO = "FIFO";
    // Tiempo base de ciclo (ms)
    public static final int VELOCIDAD_RELOJ_MS  = 1000; // Valor inicial del timer (1 seg)
    // Rango permitido para el slider de velocidad (en ms)
    public static final int VELOCIDAD_MIN_MS = 200; // Más rápido (0.2 seg)
    public static final int VELOCIDAD_MAX_MS = 3000; // Más lento (3 seg)
    
    // ==========================================
    // PERSISTENCIA Y JOURNALING
    // ==========================================
    public static final String RUTA_JOURNAL = "journal.json"; // Archivo para recuperación ante fallos
}
