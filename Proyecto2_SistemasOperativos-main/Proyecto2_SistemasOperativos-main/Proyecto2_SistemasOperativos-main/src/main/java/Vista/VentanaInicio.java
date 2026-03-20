/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Vista;

import Controlador.ControladorSistema;
import Controlador.Config;
import Modelo.Usuario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class VentanaInicio extends JFrame {

    private ControladorSistema controlador;
    private JComboBox<String> comboUsuarios;
    
    // Estilo
    private final Color COLOR_FONDO = new Color(15, 23, 42);
    private final Color COLOR_PANEL = new Color(30, 41, 59);
    private final Color COLOR_PRIMARIO = new Color(59, 130, 246);
    private final Color COLOR_TEXTO = new Color(226, 232, 240);
    private final Font FUENTE_TITULO = new Font("Segoe UI", Font.BOLD, 20);
    private final Font FUENTE_BASE = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font FUENTE_BOTON = new Font("Segoe UI", Font.BOLD, 12);
    private final Color COLOR_SECUNDARIO = new Color(20, 184, 166);

    // Contadores para generar nombres automáticos sin pedir input al usuario
    private static int contadorAdmin = 1;
    private static int contadorUser = 1;

    public VentanaInicio(ControladorSistema controlador) {
        this.controlador = controlador;
        // Crear usuarios por defecto para arrancar la aplicación
        if (controlador.getUsuarios().obtenerTamano() == 0) {
            controlador.agregarUsuario(new Usuario("Admin_1", "", Config.ROL_ADMIN));
            controlador.agregarUsuario(new Usuario("Usuario_1", "", Config.ROL_USER));
        }
        configurarVentana();
        inicializarComponentes();
    }

    private void configurarVentana() {
        setTitle("Simulador OS - Selección de Perfil");
        setSize(420, 340);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centrar en la pantalla
        setLayout(new BorderLayout());
        getContentPane().setBackground(COLOR_FONDO);
    }

    private void inicializarComponentes() {
        // --- PANEL SUPERIOR: TÍTULO ---
        JLabel lblTitulo = new JLabel("Bienvenido al Simulador", SwingConstants.CENTER);
        lblTitulo.setFont(FUENTE_TITULO);
        lblTitulo.setForeground(COLOR_TEXTO);
        lblTitulo.setBorder(BorderFactory.createEmptyBorder(20, 0, 15, 0));
        add(lblTitulo, BorderLayout.NORTH);

        // --- PANEL CENTRAL: SELECCIÓN DE USUARIO ---
        JPanel panelCentral = new JPanel(new GridBagLayout());
        panelCentral.setBackground(COLOR_PANEL);
        panelCentral.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15),
                BorderFactory.createLineBorder(new Color(75, 85, 99), 1, true)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0; gbc.gridy = 0;

        JLabel lblSeleccion = new JLabel("Seleccione un perfil existente:");
        lblSeleccion.setForeground(COLOR_TEXTO);
        lblSeleccion.setFont(FUENTE_BASE);
        panelCentral.add(lblSeleccion, gbc);

        gbc.gridy = 1;
        comboUsuarios = new JComboBox<>();
        comboUsuarios.setFont(FUENTE_BASE);
        comboUsuarios.setBackground(COLOR_FONDO);
        comboUsuarios.setForeground(COLOR_TEXTO);
        comboUsuarios.setPrototypeDisplayValue("XXXXXXXXXXXXXXXXXXXX");
        DefaultComboBoxModel<String> comboModel = new DefaultComboBoxModel<>(getNombresUsuarios());
        comboUsuarios.setModel(comboModel);
        comboUsuarios.setPreferredSize(new Dimension(250, 30));
        panelCentral.add(comboUsuarios, gbc);

        gbc.gridy = 2;
        JButton btnIngresar = new JButton("Ingresar al Sistema");
        btnIngresar.setBackground(COLOR_PRIMARIO);
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setFont(FUENTE_BOTON);
        btnIngresar.setFocusPainted(false);
        btnIngresar.setPreferredSize(new Dimension(250, 35));
        panelCentral.add(btnIngresar, gbc);

        add(panelCentral, BorderLayout.CENTER);

        // --- PANEL INFERIOR: CREACIÓN DE USUARIOS ---
        JPanel panelInferior = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panelInferior.setBackground(COLOR_PANEL);
        panelInferior.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(75, 85, 99)), "Crear Nuevo Perfil", 0, 0, FUENTE_BASE, COLOR_TEXTO));

        JButton btnCrearAdmin = new JButton("Nuevo Administrador");
        JButton btnCrearUser = new JButton("Nuevo Usuario");
        btnCrearAdmin.setBackground(COLOR_PRIMARIO);
        btnCrearAdmin.setForeground(Color.WHITE);
        btnCrearAdmin.setFocusPainted(false);
        btnCrearAdmin.setFont(FUENTE_BOTON);
        btnCrearUser.setBackground(COLOR_SECUNDARIO);
        btnCrearUser.setForeground(Color.WHITE);
        btnCrearUser.setFocusPainted(false);
        btnCrearUser.setFont(FUENTE_BOTON);

        panelInferior.add(btnCrearAdmin);
        panelInferior.add(btnCrearUser);
        add(panelInferior, BorderLayout.SOUTH);

        // =====================================================================
        // EVENTOS DE LOS BOTONES
        // =====================================================================

        btnIngresar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                iniciarDashboard();
            }
        });

        btnCrearAdmin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                crearUsuarioAutomatico(Config.ROL_ADMIN);
                actualizarCombo();
                JOptionPane.showMessageDialog(VentanaInicio.this, "Administrador creado con éxito.");
            }
        });

        btnCrearUser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                crearUsuarioAutomatico(Config.ROL_USER);
                actualizarCombo();
                JOptionPane.showMessageDialog(VentanaInicio.this, "Usuario estándar creado con éxito.");
            }
        });
    }

    // =========================================================================
    // MÉTODOS LÓGICOS DE LA INTERFAZ
    // =========================================================================

    private void crearUsuarioAutomatico(String rol) {
        String nombreGenerado;
        if (rol.equals(Config.ROL_ADMIN)) {
            nombreGenerado = "Admin_" + contadorAdmin++;
        } else {
            nombreGenerado = "Usuario_" + contadorUser++;
        }
        
        // La contraseña queda vacía porque no se requiere validación estricta
        Usuario nuevoUsuario = new Usuario(nombreGenerado, "", rol);
        controlador.agregarUsuario(nuevoUsuario);
    }

    private void actualizarCombo() {
        DefaultComboBoxModel<String> comboModel = new DefaultComboBoxModel<>(getNombresUsuarios());
        comboUsuarios.setModel(comboModel);
        comboUsuarios.setSelectedIndex(controlador.getUsuarios().obtenerTamano() - 1); // Seleccionar el recién creado
    }

    private String[] getNombresUsuarios() {
        int tamano = controlador.getUsuarios().obtenerTamano();
        String[] nombres = new String[tamano];
        for (int i = 0; i < tamano; i++) {
            Usuario u = controlador.getUsuarios().obtener(i);
            nombres[i] = u.getNombreUsuario() + " (" + u.getRol() + ")";
        }
        return nombres;
    }

    private void iniciarDashboard() {
        int indexSeleccionado = comboUsuarios.getSelectedIndex();
        if (indexSeleccionado >= 0) {
            Usuario usuarioSeleccionado = controlador.getUsuarios().obtener(indexSeleccionado);
            
            // 1. Inyectar el usuario activo en el Controlador
            controlador.setUsuarioActivo(usuarioSeleccionado);
            
            // 2. Instanciar y mostrar el Dashboard Principal
            DashboardPrincipal dashboard = new DashboardPrincipal(controlador);
            dashboard.setLocationRelativeTo(null);
            dashboard.setVisible(true);
            
            // 3. Cerrar esta ventana de inicio
            this.dispose();
        }
    }
}