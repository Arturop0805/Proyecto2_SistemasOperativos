/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Modelo;
import Controlador.Config;
import java.awt.Color;
/**
 * Representa a un usuario dentro del simulador de Sistema de Archivos.
 * Gestiona la autenticación y los permisos de acceso a diferentes funcionalidades.
 */
public class Usuario {

    private String nombreUsuario;
    private String contrasena;
    private String rol; // Utiliza Config.ROL_ADMIN o Config.ROL_USER
    private Color colorAsignado;

    // Contador estático para asignar colores únicos
    private static int indiceColor = 0;

    /**
     * Constructor principal del Usuario.
     */
    public Usuario(String nombreUsuario, String contrasena, String rol) {
        this.nombreUsuario = nombreUsuario;
        this.contrasena = contrasena;
        this.rol = rol;
        this.colorAsignado = generarColorUnico();
    }

    // =========================================================================
    // AUTENTICACIÓN
    // =========================================================================

    /**
     * Verifica si las credenciales ingresadas coinciden con las del usuario.
     * Útil para la ventana de Login.
     */
    public boolean validarCredenciales(String usuarioIngresado, String claveIngresada) {
        return this.nombreUsuario.equals(usuarioIngresado) && this.contrasena.equals(claveIngresada);
    }

    // =========================================================================
    // VALIDACIÓN DE ROLES (Útil para habilitar/deshabilitar botones en la GUI)
    // =========================================================================

    public boolean esAdministrador() {
        return Config.ROL_ADMIN.equals(this.rol);
    }

    /**
     * Verifica si el usuario puede modificar las políticas de planificación (SSTF, SCAN, etc.)
     */
    public boolean puedeCambiarPoliticasPlanificacion() {
        // Según el PDF, solo el admin gestiona los procesos y políticas a este nivel
        return esAdministrador();
    }

    /**
     * Verifica si el usuario tiene permiso para realizar un formateo completo del disco virtual.
     */
    public boolean puedeFormatearDisco() {
        return esAdministrador();
    }

    /**
     * Verifica si el usuario tiene permisos de edición (UPDATE/DELETE) 
     * sobre un archivo específico.
     * @param propietarioArchivo El dueño registrado en la FAT para ese archivo.
     */
    public boolean puedeModificarArchivo(String propietarioArchivo) {
        // Un administrador puede modificar cualquier archivo.
        // Un usuario estándar solo puede modificar los suyos.
        if (esAdministrador()) {
            return true;
        }
        return this.nombreUsuario.equals(propietarioArchivo);
    }

    /**
     * Verifica si el usuario tiene permisos de lectura (READ) sobre un archivo.
     * @param propietarioArchivo El dueño registrado en la FAT para ese archivo.
     * @param esPublico Flag que indica si el archivo está marcado como público.
     */
    public boolean puedeLeerArchivo(String propietarioArchivo, boolean esPublico) {
        // El admin lee todo. El usuario lee si es público o si es suyo.
        if (esAdministrador()) {
            return true;
        }
        return esPublico || this.nombreUsuario.equals(propietarioArchivo);
    }

    // =========================================================================
    // GETTERS Y SETTERS
    // =========================================================================

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public Color getColorAsignado() {
        return colorAsignado;
    }

    @Override
    public String toString() {
        return nombreUsuario + " (" + rol + ")";
    }

    private Color generarColorUnico() {
        Color[] paleta = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.ORANGE, Color.PINK, Color.CYAN, Color.MAGENTA, Color.LIGHT_GRAY, Color.DARK_GRAY};
        return paleta[indiceColor++ % paleta.length];
    }
}
