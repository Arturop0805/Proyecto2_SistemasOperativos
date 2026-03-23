package Controlador;

import javax.swing.SwingUtilities;

public class HiloDisco extends Thread {
    
    private ControladorSistema controlador;
    private boolean corriendo;
    private boolean pausado;
    private int velocidadMs;
    private java.util.function.Consumer<String> onMensajeUI;
    private Runnable onRefrescarUI;

    public HiloDisco(ControladorSistema controlador, int velocidadMs, 
                     java.util.function.Consumer<String> onMensajeUI, 
                     Runnable onRefrescarUI) {
        this.controlador = controlador;
        this.velocidadMs = velocidadMs;
        this.onMensajeUI = onMensajeUI;
        this.onRefrescarUI = onRefrescarUI;
        this.corriendo = true;
        this.pausado = true; // Inicia pausado o como la UI determine
    }

    @Override
    public void run() {
        while (corriendo) {
            try {
                if (pausado) {
                    Thread.sleep(100);
                    continue;
                }
                
                String resultado = controlador.procesarSiguienteOperacion();
                
                if (!resultado.equals("No hay procesos en la cola.")) {
                    SwingUtilities.invokeLater(() -> {
                        if (onMensajeUI != null) onMensajeUI.accept(resultado);
                        if (onRefrescarUI != null) onRefrescarUI.run();
                    });
                    
                    Thread.sleep(velocidadMs);
                } else {
                    // Si la cola está vacía evitamos consumir CPU
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                // Interrupción abrupta (usado para simular fallos/crash)
                System.out.println("Hilo de Disco Interrumpido.");
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setVelocidadMs(int velocidadMs) {
        this.velocidadMs = velocidadMs;
    }

    public void pausar() {
        this.pausado = true;
    }

    public void reanudar() {
        this.pausado = false;
    }

    public boolean isPausado() {
        return pausado;
    }

    public void detener() {
        this.corriendo = false;
        this.interrupt();
    }
}
