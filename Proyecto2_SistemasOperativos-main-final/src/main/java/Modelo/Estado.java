package Modelo;

/**
 * Define los 6 estados obligatorios del sistema + Suspendidos.
 */
public enum Estado {
    NUEVO,              // Recién creado
    LISTO,              // En RAM, esperando CPU
    EJECUCION,          // Usando la CPU
    BLOQUEADO,          // Esperando E/S
    TERMINADO,          // Finalizó
}
