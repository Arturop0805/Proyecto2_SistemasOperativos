/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.so_proyecto2;

import Controlador.ControladorSistema;
import Controlador.Config;
import Vista.VentanaInicio;
import Modelo.Usuario;

public class SO_Proyecto2 {
    public static void main(String[] args) {
        // 1. Crear el orquestador del sistema con la capacidad del disco (ej. 1024 bloques)
<<<<<<< HEAD
        ControladorSistema controlador = new ControladorSistema(Config.CAPACIDAD_DISCO_DEFECTO);
=======
        ControladorSistema controlador = new ControladorSistema();
>>>>>>> develop

        // 2. Agregar un usuario administrador por defecto
        controlador.agregarUsuario(new Usuario("Admin_1", "", Config.ROL_ADMIN));

        // 3. Iniciar la interfaz gráfica en el hilo de eventos de Swing
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                // Se lanza la ventana de inicio, inyectando el controlador
                new VentanaInicio(controlador).setVisible(true);
            }
        });
    }
}