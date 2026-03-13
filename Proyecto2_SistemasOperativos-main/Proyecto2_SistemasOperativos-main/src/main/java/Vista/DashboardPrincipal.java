/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Vista;

import Controlador.ControladorSistema;
import Controlador.Config;
import EstructurasDeDatos.Bloque;
import EstructurasDeDatos.EntradaFAT;
import Modelo.Proceso;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

public class DashboardPrincipal extends JFrame {
    
    private ControladorSistema controlador;
    
    // Componentes visuales clave
    private JTree arbolDirectorios;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode nodoRaiz;

    // Persistencia de la vista de árbol entre sesiones (para mantener los cambios)
    private static DefaultTreeModel sharedTreeModel;
    private static DefaultMutableTreeNode sharedRoot;

    private JPanel panelCentro;
    private CardLayout cardLayout;
    private JPanel panelDisco;
    private JPanel panelCarpeta;
    private JTable tablaCarpeta;
    private DefaultTableModel modeloTablaCarpeta;

    private JTable tablaFAT;
    private DefaultTableModel modeloTablaFAT;
    private JTextArea areaLog;
    private JTextArea areaJournal;
    private DefaultListModel<String> modeloColaProcesos;
    private JList<String> listaColaProcesos;

    // Barra de estado / ciclo
    private JLabel lblCiclo;
    private boolean cicloActivo = false;
    private int cicloContador = 0;

    // Configuración de vista
    private boolean mostrarNumerosBloques = true;

    // Estados de la vista
    private String carpetaActual = Config.NOMBRE_DIRECTORIO_RAIZ;

    // Botones CRUD
    private JToggleButton btnToggleVista;
    private JButton btnCrear;
    private JButton btnCrearCarpeta;
    private JButton btnLeer;
    private JButton btnModificar;
    private JButton btnEliminar;

    // --- COLORES / ESTILO ---
    private final Color COLOR_FONDO_APP = new Color(15, 23, 42);
    private final Color COLOR_PANEL = new Color(30, 41, 59);
    private final Color COLOR_PRIMARIO = new Color(59, 130, 246);
    private final Color COLOR_SECUNDARIO = new Color(20, 184, 166);
    private final Color COLOR_ALERTA = new Color(239, 68, 68);
    private final Color COLOR_TEXTO = new Color(226, 232, 240);
    private final Font FUENTE_BASE = new Font("Segoe UI", Font.PLAIN, 14);
    private final Font FUENTE_BOTON = new Font("Segoe UI", Font.BOLD, 12);
    private final Font FUENTE_TITULO = new Font("Segoe UI", Font.BOLD, 18); 

    // Clase auxiliar para manejar elementos del explorador de archivos
    private static class ElementoFS {
        private final String nombre;
        private final boolean carpeta;

        public ElementoFS(String nombre, boolean carpeta) {
            this.nombre = nombre;
            this.carpeta = carpeta;
        }

        public String getNombre() {
            return nombre;
        }

        public boolean esCarpeta() {
            return carpeta;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }
    
    public DashboardPrincipal(ControladorSistema controlador) {
        this.controlador = controlador;
        configurarVentana();
        inicializarPanelOperaciones(); // Nuevo panel CRUD (debe inicializarse antes de usar btnToggleVista)
        inicializarComponentes();
        aplicarPermisosRol(); // Bloquear/Ocultar botones según rol
        actualizarVistaGlobal();
    }
    
    private void configurarVentana() {
        setTitle("Simulador de Sistema de Archivos - SO 2425-2 - Usuario: " + 
                 controlador.getUsuarioActivo().getNombreUsuario());
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(COLOR_FONDO_APP);

    }

    private JMenuBar crearMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(COLOR_PANEL);
        menuBar.setBorder(new EmptyBorder(5, 10, 5, 10));

        JMenu menuArchivo = new JMenu("Archivo");
        JMenuItem miCerrarSesion = new JMenuItem("Cerrar Sesión");
        miCerrarSesion.addActionListener(e -> cerrarSesion());
        menuArchivo.add(miCerrarSesion);

        menuBar.add(menuArchivo);
        return menuBar;
    }
    
    private void inicializarComponentes() {
        // 1. Explorador de Archivos (JTree)
        if (sharedRoot == null) {
            sharedRoot = new DefaultMutableTreeNode(new ElementoFS(Config.NOMBRE_DIRECTORIO_RAIZ, true));
            sharedTreeModel = new DefaultTreeModel(sharedRoot);
        }
        nodoRaiz = sharedRoot;
        treeModel = sharedTreeModel;
        arbolDirectorios = new JTree(treeModel);
        arbolDirectorios.setRootVisible(true);
        arbolDirectorios.setShowsRootHandles(true);
        arbolDirectorios.setFont(FUENTE_BASE);

        // Estilizar árbol
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
            private final Icon iconoCarpeta = UIManager.getIcon("FileView.directoryIcon");
            private final Icon iconoArchivo = UIManager.getIcon("FileView.fileIcon");

            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                          boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
                if (value instanceof DefaultMutableTreeNode) {
                    Object usuario = ((DefaultMutableTreeNode) value).getUserObject();
                    if (usuario instanceof ElementoFS) {
                        ElementoFS elemento = (ElementoFS) usuario;
                        setIcon(elemento.esCarpeta() ? iconoCarpeta : iconoArchivo);
                    }
                }
                setBackgroundNonSelectionColor(COLOR_PANEL);
                setTextNonSelectionColor(COLOR_TEXTO);
                setBackgroundSelectionColor(COLOR_PRIMARIO);
                setTextSelectionColor(Color.WHITE);
                return this;
            }
        };
        arbolDirectorios.setCellRenderer(renderer);

        // Doble clic para navegar carpetas
        arbolDirectorios.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = arbolDirectorios.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode seleccionado = (DefaultMutableTreeNode) path.getLastPathComponent();
                        Object usuario = seleccionado.getUserObject();
                        if (usuario instanceof ElementoFS) {
                            ElementoFS element = (ElementoFS) usuario;
                            if (element.esCarpeta()) {
                                carpetaActual = obtenerRutaDesdeNodo(seleccionado);
                                btnToggleVista.setSelected(true);
                                btnToggleVista.setText("Ver Disco");
                                mostrarVistaCarpeta();
                                actualizarVistaGlobal();
                            }
                        }
                    }
                }
            }
        });

        JScrollPane scrollArbol = new JScrollPane(arbolDirectorios);
        scrollArbol.setPreferredSize(new Dimension(250, 0));
        scrollArbol.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(null, "Sistema de Archivos", 0, 0, FUENTE_BOTON, COLOR_TEXTO),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        scrollArbol.getViewport().setBackground(COLOR_PANEL);

        JPanel panelIzquierdo = new JPanel(new BorderLayout());
        panelIzquierdo.setBackground(COLOR_FONDO_APP);
        panelIzquierdo.add(scrollArbol, BorderLayout.CENTER);
        add(panelIzquierdo, BorderLayout.WEST);

        // Barra superior (simula toolbar)
        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panelSuperior.setBackground(COLOR_FONDO_APP);
        panelSuperior.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(100, 116, 139)));

        // Los botones del toolbar reutilizan las acciones ya definidas en el panel de operaciones
        JButton btnCrearArchivo = new JButton("Crear Archivo");
        JButton btnCrearCarpeta = new JButton("Crear Directorio");
        JButton btnLeerArchivo = new JButton("Leer");
        JButton btnRenombrar = new JButton("Renombrar");
        JButton btnEliminarArchivo = new JButton("Eliminar");
        JButton btnToggleVistaToolbar = new JButton("Vista Disco/Carpeta");
        JToggleButton btnToggleNumerosBloques = new JToggleButton("Ocultar Números");
        JButton btnCerrarSesionToolbar = new JButton("Cerrar Sesión");

        btnCerrarSesionToolbar.setBackground(Color.WHITE);
        btnCerrarSesionToolbar.setForeground(Color.BLACK);
        btnCerrarSesionToolbar.setFont(FUENTE_BOTON);
        btnCerrarSesionToolbar.setFocusPainted(false);
        btnCerrarSesionToolbar.setBorderPainted(false);
        btnCerrarSesionToolbar.setOpaque(true);
        btnCerrarSesionToolbar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCerrarSesionToolbar.setBorder(new EmptyBorder(8, 15, 8, 15));

        JComponent[] toolbarButtons = {btnCrearArchivo, btnCrearCarpeta, btnLeerArchivo, btnRenombrar, btnEliminarArchivo, btnToggleVistaToolbar, btnToggleNumerosBloques, btnCerrarSesionToolbar};
        for (JComponent b : toolbarButtons) {
            if (b != btnCerrarSesionToolbar) {
                estilizarBoton((AbstractButton) b, COLOR_SECUNDARIO);
            }
            b.setPreferredSize(new Dimension(120, 30));
            panelSuperior.add(b);
        }

        // Conectar las acciones del toolbar con los botones existentes
        btnCrearArchivo.addActionListener(e -> crearArchivoEnCarpetaActual());
        btnCrearCarpeta.addActionListener(e -> crearDirectorioEnCarpetaActual());
        btnLeerArchivo.addActionListener(e -> btnLeer.doClick());
        btnRenombrar.addActionListener(e -> btnModificar.doClick());
        btnEliminarArchivo.addActionListener(e -> btnEliminar.doClick());
        btnToggleVistaToolbar.addActionListener(e -> btnToggleVista.doClick());
        btnToggleNumerosBloques.addActionListener(e -> {
            mostrarNumerosBloques = !mostrarNumerosBloques;
            btnToggleNumerosBloques.setText(mostrarNumerosBloques ? "Ocultar Números" : "Mostrar Números");
            actualizarMapaDisco();
        });
        btnCerrarSesionToolbar.addActionListener(e -> cerrarSesion());

        add(panelSuperior, BorderLayout.NORTH);

        // 2. Panel central con alternancia Disco / Carpeta
        panelCentro = new JPanel();
        cardLayout = new CardLayout();
        panelCentro.setLayout(cardLayout);
        panelCentro.setBackground(COLOR_FONDO_APP);

        // 2a. Mapa del Disco (SD)
        panelDisco = new JPanel();
        int capacidad = controlador.getDisco().getCapacidadTotal();
        int columnas = 32;
        int filas = (int) Math.ceil((double) capacidad / columnas);
        panelDisco.setLayout(new GridLayout(filas, columnas, 2, 2));
        panelDisco.setBackground(COLOR_PANEL);
        panelDisco.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(null, "Disk Visualization", 0, 0, FUENTE_BOTON, COLOR_TEXTO),
                new EmptyBorder(10, 10, 10, 10)));
        JScrollPane scrollDisco = new JScrollPane(panelDisco);
        scrollDisco.setBorder(null);
        scrollDisco.getViewport().setBackground(COLOR_FONDO_APP);
        panelCentro.add(scrollDisco, "DISCO");

        // 2b. Vista de carpeta
        String[] columnasCarpeta = {"Nombre", "Tipo", "Tamaño (bloques)"};
        modeloTablaCarpeta = new DefaultTableModel(columnasCarpeta, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaCarpeta = new JTable(modeloTablaCarpeta);
        estilizarTabla(tablaCarpeta);
        tablaCarpeta.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int fila = tablaCarpeta.rowAtPoint(e.getPoint());
                    if (fila != -1) {
                        String tipo = (String) modeloTablaCarpeta.getValueAt(fila, 1);
                        if ("Carpeta".equals(tipo)) {
                            String nombre = (String) modeloTablaCarpeta.getValueAt(fila, 0);
                            carpetaActual = carpetaActual + "/" + nombre;
                            btnToggleVista.setSelected(true);
                            btnToggleVista.setText("Ver Disco");
                            seleccionarNodoPorRuta(carpetaActual);
                            mostrarVistaCarpeta();
                            actualizarVistaGlobal();
                        }
                    }
                }
            }
        });
        JScrollPane scrollCarpeta = new JScrollPane(tablaCarpeta);
        scrollCarpeta.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelCarpeta = new JPanel(new BorderLayout());
        panelCarpeta.setBackground(COLOR_PANEL);
        panelCarpeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(null, "Vista de Carpeta", 0, 0, FUENTE_BOTON, COLOR_TEXTO),
                new EmptyBorder(10, 10, 10, 10)));
        panelCarpeta.add(scrollCarpeta, BorderLayout.CENTER);
        panelCentro.add(panelCarpeta, "CARPETA");

        add(panelCentro, BorderLayout.CENTER);

        // 3. Panel inferior con pestañas: FAT + Log de Eventos
        String[] columnasFAT = {"Nombre Archivo", "Cantidad Bloques", "Dir. Primer Bloque"};
        modeloTablaFAT = new DefaultTableModel(columnasFAT, 0);
        tablaFAT = new JTable(modeloTablaFAT);
        estilizarTabla(tablaFAT);

        JScrollPane scrollFAT = new JScrollPane(tablaFAT);
        scrollFAT.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        scrollFAT.getViewport().setBackground(COLOR_PANEL);

        areaLog = new JTextArea();
        areaLog.setEditable(false);
        areaLog.setFont(FUENTE_BASE);
        areaLog.setBackground(new Color(15, 23, 42));
        areaLog.setForeground(COLOR_TEXTO);
        areaLog.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane scrollLog = new JScrollPane(areaLog);
        scrollLog.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(COLOR_PANEL);
        tabs.addTab("Tabla de Asignación", scrollFAT);
        tabs.addTab("Log de Eventos", scrollLog);
        tabs.setFont(FUENTE_BOTON);
        tabs.setForeground(COLOR_TEXTO);
        tabs.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(null, "Información", 0, 0, FUENTE_BOTON, COLOR_TEXTO),
                new EmptyBorder(5, 5, 5, 5)));

        // Limitar altura para dar más espacio al área superior (disco y árbol de directorios)
        tabs.setPreferredSize(new Dimension(0, 180));

        add(tabs, BorderLayout.SOUTH);

        // Inicialmente mostrar disco
        mostrarVistaDisco();
    }

    // =========================================================================
    // NUEVO: PANEL DE OPERACIONES CRUD
    // =========================================================================
    private void inicializarPanelOperaciones() {
        JPanel panelOperaciones = new JPanel(new BorderLayout());
        panelOperaciones.setPreferredSize(new Dimension(260, 0));
        panelOperaciones.setBackground(COLOR_FONDO_APP);
        panelOperaciones.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(null, "Controles", 0, 0, FUENTE_TITULO, COLOR_TEXTO),
                new EmptyBorder(12, 12, 12, 12)));

        // Información de usuario y planificador
        JPanel panelInfo = new JPanel();
        panelInfo.setLayout(new BoxLayout(panelInfo, BoxLayout.Y_AXIS));
        panelInfo.setBackground(COLOR_PANEL);
        panelInfo.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel lblUsuario = new JLabel("Usuario: " + controlador.getUsuarioActivo().getNombreUsuario());
        lblUsuario.setForeground(COLOR_TEXTO);
        lblUsuario.setFont(FUENTE_BASE);
        panelInfo.add(lblUsuario);

        JLabel lblRol = new JLabel("Rol: " + controlador.getUsuarioActivo().getRol());
        lblRol.setForeground(COLOR_TEXTO);
        lblRol.setFont(FUENTE_BASE);
        panelInfo.add(lblRol);

        // (Se eliminó el botón para cambiar entre administrador y usuario)

        panelInfo.add(Box.createRigidArea(new Dimension(0, 10)));

        JLabel lblPlanificador = new JLabel("Planificador:");
        lblPlanificador.setForeground(COLOR_TEXTO);
        lblPlanificador.setFont(FUENTE_BASE);
        panelInfo.add(lblPlanificador);

        JComboBox<String> comboPlanificador = new JComboBox<>(new String[] {"SCAN", "FCFS", "SSTF"});
        comboPlanificador.setBackground(COLOR_FONDO_APP);
        comboPlanificador.setForeground(COLOR_TEXTO);
        comboPlanificador.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        comboPlanificador.setFocusable(false);
        comboPlanificador.setSelectedItem(controlador.getPlanificador().getPoliticaActiva());
        comboPlanificador.addActionListener(e -> {
            String seleccion = (String) comboPlanificador.getSelectedItem();
            controlador.getPlanificador().setPoliticaActiva(seleccion);
        });
        panelInfo.add(comboPlanificador);

        panelOperaciones.add(panelInfo, BorderLayout.NORTH);

        // Botones de acción
        JPanel panelBotones = new JPanel();
        panelBotones.setBackground(COLOR_PANEL);
        panelBotones.setLayout(new BoxLayout(panelBotones, BoxLayout.Y_AXIS));
        panelBotones.setBorder(new EmptyBorder(10, 10, 10, 10));

        btnToggleVista = new JToggleButton("Ver Carpetas");
        btnCrear = new JButton("Crear Archivo");
        btnCrearCarpeta = new JButton("Crear Carpeta");
        btnLeer = new JButton("Leer");
        btnModificar = new JButton("Renombrar");
        btnEliminar = new JButton("Eliminar");
        JComponent[] botones = {btnToggleVista, btnCrear, btnCrearCarpeta, btnLeer, btnModificar, btnEliminar};
        for (JComponent btn : botones) {
            Color bg = COLOR_PRIMARIO;
            estilizarBoton((AbstractButton) btn, bg);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setMaximumSize(new Dimension(220, 40));
            panelBotones.add(Box.createRigidArea(new Dimension(0, 10)));
            panelBotones.add(btn);
        }

        // Toggle de vista Disco / Carpeta
        btnToggleVista.addActionListener(e -> {
            if (btnToggleVista.isSelected()) {
                btnToggleVista.setText("Ver Disco");
                mostrarVistaCarpeta();
            } else {
                btnToggleVista.setText("Ver Carpetas");
                mostrarVistaDisco();
            }
            actualizarVistaGlobal();
        });

        panelOperaciones.add(panelBotones, BorderLayout.CENTER);

        // Journal (estilo)
        JPanel panelJournal = new JPanel(new BorderLayout());
        panelJournal.setBackground(COLOR_PANEL);
        panelJournal.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(null, "Journal", 0, 0, FUENTE_BOTON, COLOR_TEXTO),
                new EmptyBorder(10, 10, 10, 10)));

        areaJournal = new JTextArea();
        areaJournal.setEditable(false);
        areaJournal.setBackground(COLOR_FONDO_APP);
        areaJournal.setForeground(COLOR_TEXTO);
        areaJournal.setFont(FUENTE_BASE);
        areaJournal.setText("Crear Archivo 'doc1': PENDIENTE\nCrear Directorio 'docs': CONFIRMADA\nEliminar 'dosS2': PENDIENTE\n");
        JScrollPane scrollJournal = new JScrollPane(areaJournal);
        scrollJournal.setBorder(null);
        panelJournal.add(scrollJournal, BorderLayout.CENTER);

        // Estado sistema
        JLabel lblEstado = new JLabel("Estado del Sistema: Normal");
        lblEstado.setForeground(COLOR_TEXTO);
        lblEstado.setFont(FUENTE_BASE);

        JPanel panelJournalFooter = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panelJournalFooter.setBackground(COLOR_PANEL);
        panelJournalFooter.add(lblEstado);
        panelJournal.add(panelJournalFooter, BorderLayout.SOUTH);

        // Cola de Procesos
        JPanel panelCola = new JPanel(new BorderLayout());
        panelCola.setBackground(COLOR_PANEL);
        panelCola.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(null, "Cola de Procesos", 0, 0, FUENTE_BOTON, COLOR_TEXTO),
                new EmptyBorder(10, 10, 10, 10)));

        modeloColaProcesos = new DefaultListModel<>();
        listaColaProcesos = new JList<>(modeloColaProcesos);
        listaColaProcesos.setBackground(COLOR_FONDO_APP);
        listaColaProcesos.setForeground(COLOR_TEXTO);
        listaColaProcesos.setFont(FUENTE_BASE);
        JScrollPane scrollCola = new JScrollPane(listaColaProcesos);
        scrollCola.setBorder(null);
        panelCola.add(scrollCola, BorderLayout.CENTER);

        panelOperaciones.add(panelJournal, BorderLayout.CENTER);
        panelOperaciones.add(panelCola, BorderLayout.SOUTH);

        add(panelOperaciones, BorderLayout.EAST);

        // --- EVENTOS CRUD ---

        // CREATE Archivo
        btnCrear.addActionListener(e -> crearArchivoEnCarpetaActual());

        // CREATE Carpeta
        btnCrearCarpeta.addActionListener(e -> crearDirectorioEnCarpetaActual());

        // READ
        btnLeer.addActionListener(e -> {
            String nombre = obtenerNombreArchivoSeleccionado("leer");
            if (nombre != null) {
                String resultado = controlador.leerArchivo(nombre);
                mostrarMensaje(resultado);
                // Leer no modifica el disco, no es estrictamente necesario actualizar la vista
            }
        });

        // UPDATE
        btnModificar.addActionListener(e -> {
            String nombre = obtenerNombreArchivoSeleccionado("modificar");
            if (nombre != null) {
                String strVariacion = JOptionPane.showInputDialog(this, 
                        "Ingrese variación de bloques (Ej: '2' para agregar, '-1' para quitar):");
                if (strVariacion != null) {
                    try {
                        int modificacion = Integer.parseInt(strVariacion);
                        String resultado = controlador.modificarArchivo(nombre, modificacion);
                        mostrarMensaje(resultado);
                        actualizarVistaGlobal();
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Cantidad inválida.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        // DELETE
        btnEliminar.addActionListener(e -> {
            if (isVistaCarpeta()) {
                int filaSeleccionada = tablaCarpeta.getSelectedRow();
                if (filaSeleccionada != -1) {
                    String nombre = (String) modeloTablaCarpeta.getValueAt(filaSeleccionada, 0);
                    String tipo = (String) modeloTablaCarpeta.getValueAt(filaSeleccionada, 1);
                    int confirmacion = JOptionPane.showConfirmDialog(this,
                            "¿Está seguro de eliminar el " + ("Carpeta".equals(tipo) ? "carpeta" : "archivo") + " '" + nombre + "'?",
                            "Confirmar Eliminación", JOptionPane.YES_NO_OPTION);
                    if (confirmacion == JOptionPane.YES_OPTION) {
                        if ("Carpeta".equals(tipo)) {
                            eliminarRamaCarpeta(nombre);
                        } else {
                            String ruta = carpetaActual + "/" + nombre;
                            String resultado = controlador.eliminarArchivo(ruta);
                            mostrarMensaje(resultado);
                            if (!resultado.startsWith("Error")) {
                                eliminarElementoEnCarpeta(nombre, false);
                            }
                        }
                        actualizarVistaGlobal();
                    }
                } else {
                    mostrarMensaje("Seleccione un elemento en la vista de carpeta para eliminar.");
                }
                return;
            }

            String nombre = obtenerNombreArchivoSeleccionado("eliminar");
            if (nombre != null) {
                int confirmacion = JOptionPane.showConfirmDialog(this, 
                        "¿Está seguro de eliminar el archivo '" + nombre + "'?", 
                        "Confirmar Eliminación", JOptionPane.YES_NO_OPTION);
                
                if (confirmacion == JOptionPane.YES_OPTION) {
                    String resultado = controlador.eliminarArchivo(nombre);
                    mostrarMensaje(resultado);
                    if (!resultado.startsWith("Error")) {
                        eliminarArchivoDelArbol(nombre);
                    }
                    actualizarVistaGlobal();
                }
            }
        });

        // CERRAR SESIÓN
    }
    
    // Método de utilidad para mostrar popups dependiendo de si es Error o Éxito
    private void mostrarMensaje(String mensaje) {
        if (mensaje.startsWith("Error")) {
            JOptionPane.showMessageDialog(this, mensaje, "Operación Fallida", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, mensaje, "Operación Exitosa", JOptionPane.INFORMATION_MESSAGE);
        }

        String linea = java.time.LocalTime.now().withNano(0) + " - " + mensaje + "\n";
        if (areaLog != null) {
            areaLog.append(linea);
        }
        if (areaJournal != null) {
            areaJournal.append(linea);
        }
    }

    private void estilizarBoton(AbstractButton btn, Color colorFondo) {
        btn.setBackground(colorFondo);
        btn.setForeground(Color.WHITE);
        btn.setFont(FUENTE_BOTON);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 15, 8, 15));
    }

    private void estilizarTabla(JTable tabla) {
        tabla.setFont(FUENTE_BASE);
        tabla.setRowHeight(30);
        tabla.setShowGrid(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(new Color(71, 85, 105));
        tabla.setBackground(COLOR_PANEL);
        tabla.setForeground(COLOR_TEXTO);
        tabla.setSelectionBackground(COLOR_PRIMARIO);
        tabla.setSelectionForeground(Color.WHITE);

        JTableHeader header = tabla.getTableHeader();
        header.setBackground(COLOR_PANEL);
        header.setForeground(COLOR_TEXTO);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setPreferredSize(new Dimension(100, 35));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setBorder(new EmptyBorder(0, 5, 0, 5));
    }

    // Método auxiliar para obtener el nombre del archivo (ya sea por input o seleccionando en la tabla)
    private String obtenerNombreArchivoSeleccionado(String accion) {
        if (isVistaCarpeta()) {
            int filaSeleccionada = tablaCarpeta.getSelectedRow();
            if (filaSeleccionada != -1) {
                String nombre = (String) modeloTablaCarpeta.getValueAt(filaSeleccionada, 0);
                String tipo = (String) modeloTablaCarpeta.getValueAt(filaSeleccionada, 1);
                if ("Carpeta".equals(tipo)) {
                    mostrarMensaje("Error: Selecciona un archivo para " + accion + " (no una carpeta). ");
                    return null;
                }
                return carpetaActual + "/" + nombre;
            }
            String nombre = JOptionPane.showInputDialog(this, "Ingrese el nombre del archivo a " + accion + ":");
            if (nombre != null && !nombre.trim().isEmpty()) {
                return carpetaActual + "/" + nombre;
            }
            return null;
        }

        int filaSeleccionada = tablaFAT.getSelectedRow();
        if (filaSeleccionada != -1) {
            return (String) modeloTablaFAT.getValueAt(filaSeleccionada, 0);
        } else {
            return JOptionPane.showInputDialog(this, "Ingrese el nombre del archivo a " + accion + ":");
        }
    }

    // =========================================================================
    // GESTIÓN DE PERMISOS
    // =========================================================================
    private void aplicarPermisosRol() {
        // Ejemplo de cómo puedes bloquear botones según el rol
        if (controlador.getUsuarioActivo() != null) {
            if (controlador.getUsuarioActivo().getRol().equals(Config.ROL_USER)) {
                // Para usuarios estándar: ocultar modificar, ya que no pueden modificar archivos
                btnModificar.setVisible(false);
            }
        }
    }

    // =========================================================================
    // MÉTODOS DE ACTUALIZACIÓN EN TIEMPO REAL
    // =========================================================================

    public void actualizarVistaGlobal() {
        actualizarMapaDisco();
        actualizarTablaFAT();
        actualizarColaProcesos();
        actualizarCiclo();
        // Siempre actualizamos la vista de carpeta para que refleje cambios aun cuando
        // estemos en la vista de disco o FAT.
        actualizarTablaCarpeta();
        // actualizarArbol();
    }

    private void actualizarCiclo() {
        if (lblCiclo == null) return;
        if (cicloActivo) {
            cicloContador++;
        }
        lblCiclo.setText("Ciclo: " + cicloContador);
    }

    private void actualizarColaProcesos() {
        if (modeloColaProcesos == null) return;
        modeloColaProcesos.clear();
        if (controlador == null || controlador.getPlanificador() == null) return;
        for (int i = 0; i < controlador.getPlanificador().getColaListos().obtenerTamano(); i++) {
            Proceso p = controlador.getPlanificador().getColaListos().obtener(i);
            if (p != null) {
                modeloColaProcesos.addElement(p.getIdProceso() + " - " + p.getOperacion());
            }
        }
    }

    private void actualizarMapaDisco() {
        panelDisco.removeAll();
        int capacidad = controlador.getDisco().getCapacidadTotal();
        
        for (int i = 0; i < capacidad; i++) {
            Bloque bloque = controlador.getDisco().getBloque(i);
            JPanel vistaBloque = new JPanel(new BorderLayout());
            vistaBloque.setBorder(BorderFactory.createLineBorder(new Color(75, 85, 99)));

            if (mostrarNumerosBloques) {
                JLabel lblNumero = new JLabel(String.format("%02d", i), SwingConstants.CENTER);
                lblNumero.setForeground(COLOR_TEXTO);
                lblNumero.setFont(new Font("Segoe UI", Font.BOLD, 11));
                vistaBloque.add(lblNumero, BorderLayout.CENTER);
            }
            
            if (bloque.estaOcupado()) {
                String nombreArchivo = bloque.getNombreArchivo();
                EntradaFAT entrada = controlador.getFat().buscarArchivo(nombreArchivo);
                if (entrada != null) {
                    String propietario = entrada.getPropietario();
                    Color colorUsuario = controlador.getColorUsuario(propietario);
                    vistaBloque.setBackground(colorUsuario);
                    vistaBloque.setToolTipText("Bloque " + i + " - " + nombreArchivo + " (Propietario: " + propietario + ")");
                } else {
                    vistaBloque.setBackground(Color.GRAY);
                    vistaBloque.setToolTipText("Bloque " + i + " - " + nombreArchivo + " (Propietario desconocido)");
                }
            } else {
                vistaBloque.setBackground(COLOR_PANEL);
                vistaBloque.setToolTipText("Bloque " + i + " - Libre");
            }
            panelDisco.add(vistaBloque);
        }
        panelDisco.revalidate();
        panelDisco.repaint();
    }

    private void actualizarTablaFAT() {
        modeloTablaFAT.setRowCount(0); 
        
        int tamanoFAT = controlador.getFat().getEntradas().obtenerTamano();
        for (int i = 0; i < tamanoFAT; i++) {
            EntradaFAT entrada = controlador.getFat().getEntradas().obtener(i);
            if (entrada != null) {
                modeloTablaFAT.addRow(new Object[]{
                    entrada.getNombreArchivo(),
                    entrada.getCantidadBloques(),
                    entrada.getDireccionPrimerBloque()
                });
            }
        }
    }

    private boolean isVistaCarpeta() {
        return btnToggleVista != null && btnToggleVista.isSelected();
    }

    private void mostrarVistaDisco() {
        if (cardLayout != null && panelCentro != null) {
            cardLayout.show(panelCentro, "DISCO");
        }
    }

    private void mostrarVistaCarpeta() {
        if (cardLayout != null && panelCentro != null) {
            cardLayout.show(panelCentro, "CARPETA");
        }
        actualizarTablaCarpeta();
    }

    private void crearArchivoEnCarpetaActual() {
        String nombre = JOptionPane.showInputDialog(this, "Ingrese el nombre del nuevo archivo:");
        if (nombre == null || nombre.trim().isEmpty()) {
            return;
        }

        String strTamano = JOptionPane.showInputDialog(this, "Ingrese el tamaño en bloques:");
        if (strTamano == null) {
            return;
        }

        int tamano;
        try {
            tamano = Integer.parseInt(strTamano);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Tamaño inválido. Ingrese un número entero.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String carpetaDestino = carpetaActual;
        String rutaCompleta = carpetaDestino + "/" + nombre;
        if (existeElementoEnCarpeta(nombre, carpetaDestino)) {
            mostrarMensaje("Error: Ya existe un elemento con ese nombre en la carpeta actual.");
            return;
        }

        String resultado = controlador.crearArchivo(rutaCompleta, tamano);
        if (!resultado.startsWith("Error")) {
            agregarElementoEnCarpeta(nombre, false);
        }
        mostrarMensaje(resultado);
        actualizarVistaGlobal();
    }

    private void crearDirectorioEnCarpetaActual() {
        String nombre = JOptionPane.showInputDialog(this, "Ingrese el nombre de la nueva carpeta:");
        if (nombre == null || nombre.trim().isEmpty()) {
            return;
        }

        if (existeElementoEnCarpeta(nombre, carpetaActual)) {
            mostrarMensaje("Error: Ya existe un elemento con ese nombre en la carpeta actual.");
            return;
        }

        agregarElementoEnCarpeta(nombre, true);
        mostrarMensaje("Éxito: Carpeta creada.");
        actualizarVistaGlobal();
    }

    private void actualizarTablaCarpeta() {
        modeloTablaCarpeta.setRowCount(0);

        DefaultMutableTreeNode nodoActual = encontrarNodoPorRuta(carpetaActual);
        if (nodoActual == null) {
            return;
        }

        for (int i = 0; i < nodoActual.getChildCount(); i++) {
            DefaultMutableTreeNode hijo = (DefaultMutableTreeNode) nodoActual.getChildAt(i);
            Object usuario = hijo.getUserObject();
            if (usuario instanceof ElementoFS) {
                ElementoFS elemento = (ElementoFS) usuario;
                String tipo = elemento.esCarpeta() ? "Carpeta" : "Archivo";
                String nombre = elemento.getNombre();
                String tamano = "";

                if (!elemento.esCarpeta()) {
                    String rutaCompleta = carpetaActual + "/" + nombre;
                    EntradaFAT entrada = controlador.getFat().buscarArchivo(rutaCompleta);
                    if (entrada != null) {
                        tamano = String.valueOf(entrada.getCantidadBloques());
                    }
                } else {
                    tamano = String.valueOf(hijo.getChildCount());
                }

                modeloTablaCarpeta.addRow(new Object[]{nombre, tipo, tamano});
            }
        }
    }

    private DefaultMutableTreeNode encontrarNodoPorRuta(String ruta) {
        if (ruta == null || ruta.isEmpty()) {
            return null;
        }
        String[] partes = ruta.split("/");
        DefaultMutableTreeNode actual = nodoRaiz;
        if (partes.length == 0) {
            return actual;
        }
        // Verificar que la raíz coincide
        Object raizObj = actual.getUserObject();
        if (raizObj instanceof ElementoFS) {
            ElementoFS raizElem = (ElementoFS) raizObj;
            if (!raizElem.getNombre().equals(partes[0])) {
                return null;
            }
        }
        for (int i = 1; i < partes.length; i++) {
            String nombreBuscado = partes[i];
            boolean encontrado = false;
            for (int j = 0; j < actual.getChildCount(); j++) {
                DefaultMutableTreeNode hijo = (DefaultMutableTreeNode) actual.getChildAt(j);
                Object usuario = hijo.getUserObject();
                if (usuario instanceof ElementoFS) {
                    ElementoFS elem = (ElementoFS) usuario;
                    if (elem.getNombre().equals(nombreBuscado) && elem.esCarpeta()) {
                        actual = hijo;
                        encontrado = true;
                        break;
                    }
                }
            }
            if (!encontrado) {
                return null;
            }
        }
        return actual;
    }

    private String obtenerRutaDesdeNodo(DefaultMutableTreeNode nodo) {
        if (nodo == null) return "";
        Object usuario = nodo.getUserObject();
        String nombre = (usuario instanceof ElementoFS) ? ((ElementoFS) usuario).getNombre() : nodo.toString();
        DefaultMutableTreeNode padre = (DefaultMutableTreeNode) nodo.getParent();
        if (padre == null) {
            return nombre;
        }
        String rutaPadre = obtenerRutaDesdeNodo(padre);
        return rutaPadre + "/" + nombre;
    }

    private void seleccionarNodoPorRuta(String ruta) {
        DefaultMutableTreeNode nodo = encontrarNodoPorRuta(ruta);
        if (nodo == null) return;
        TreePath path = new TreePath(nodo.getPath());
        arbolDirectorios.setSelectionPath(path);
        arbolDirectorios.scrollPathToVisible(path);
    }

    private boolean existeElementoEnCarpeta(String nombre, String rutaCarpeta) {
        DefaultMutableTreeNode nodo = encontrarNodoPorRuta(rutaCarpeta);
        if (nodo == null) return false;
        for (int i = 0; i < nodo.getChildCount(); i++) {
            DefaultMutableTreeNode hijo = (DefaultMutableTreeNode) nodo.getChildAt(i);
            Object usuario = hijo.getUserObject();
            if (usuario instanceof ElementoFS) {
                ElementoFS elem = (ElementoFS) usuario;
                if (elem.getNombre().equalsIgnoreCase(nombre)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void agregarElementoEnCarpeta(String nombre, boolean esCarpeta) {
        DefaultMutableTreeNode nodo = encontrarNodoPorRuta(carpetaActual);
        if (nodo == null) {
            return;
        }
        DefaultMutableTreeNode nuevo = new DefaultMutableTreeNode(new ElementoFS(nombre, esCarpeta));
        treeModel.insertNodeInto(nuevo, nodo, nodo.getChildCount());
        actualizarTablaCarpeta();
    }

    private void eliminarRamaCarpeta(String nombre) {
        DefaultMutableTreeNode nodoPadre = encontrarNodoPorRuta(carpetaActual);
        if (nodoPadre == null) {
            return;
        }
        DefaultMutableTreeNode nodoEliminar = null;
        for (int i = 0; i < nodoPadre.getChildCount(); i++) {
            DefaultMutableTreeNode hijo = (DefaultMutableTreeNode) nodoPadre.getChildAt(i);
            Object usuario = hijo.getUserObject();
            if (usuario instanceof ElementoFS) {
                ElementoFS elem = (ElementoFS) usuario;
                if (elem.getNombre().equals(nombre) && elem.esCarpeta()) {
                    nodoEliminar = hijo;
                    break;
                }
            }
        }
        if (nodoEliminar == null) {
            return;
        }

        // Eliminar recursivamente archivos en disco
        eliminarRama(nodoEliminar, carpetaActual);
        treeModel.removeNodeFromParent(nodoEliminar);
        actualizarTablaCarpeta();
    }

    private void eliminarRama(DefaultMutableTreeNode nodo, String rutaBase) {
        Object usuario = nodo.getUserObject();
        if (!(usuario instanceof ElementoFS)) {
            return;
        }
        ElementoFS elem = (ElementoFS) usuario;
        String rutaCompleta = rutaBase + "/" + elem.getNombre();

        if (elem.esCarpeta()) {
            for (int i = 0; i < nodo.getChildCount(); i++) {
                DefaultMutableTreeNode hijo = (DefaultMutableTreeNode) nodo.getChildAt(i);
                eliminarRama(hijo, rutaCompleta);
            }
        } else {
            controlador.eliminarArchivo(rutaCompleta);
        }
    }

    private void eliminarElementoEnCarpeta(String nombre, boolean esCarpeta) {
        DefaultMutableTreeNode nodoPadre = encontrarNodoPorRuta(carpetaActual);
        if (nodoPadre == null) {
            return;
        }
        for (int i = 0; i < nodoPadre.getChildCount(); i++) {
            DefaultMutableTreeNode hijo = (DefaultMutableTreeNode) nodoPadre.getChildAt(i);
            Object usuario = hijo.getUserObject();
            if (usuario instanceof ElementoFS) {
                ElementoFS elem = (ElementoFS) usuario;
                if (elem.getNombre().equals(nombre) && elem.esCarpeta() == esCarpeta) {
                    treeModel.removeNodeFromParent(hijo);
                    return;
                }
            }
        }
    }

    private void eliminarArchivoDelArbol(String rutaArchivo) {
        // La ruta viene en formato 'Raiz/.../archivo'. Tenemos que encontrar la carpeta padre y eliminar el nodo del archivo.
        if (rutaArchivo == null || !rutaArchivo.contains("/")) {
            return;
        }

        int idx = rutaArchivo.lastIndexOf('/');
        String rutaCarpeta = rutaArchivo.substring(0, idx);
        String nombreArchivo = rutaArchivo.substring(idx + 1);

        DefaultMutableTreeNode nodoCarpeta = encontrarNodoPorRuta(rutaCarpeta);
        if (nodoCarpeta == null) {
            return;
        }

        for (int i = 0; i < nodoCarpeta.getChildCount(); i++) {
            DefaultMutableTreeNode hijo = (DefaultMutableTreeNode) nodoCarpeta.getChildAt(i);
            Object usuario = hijo.getUserObject();
            if (usuario instanceof ElementoFS) {
                ElementoFS elem = (ElementoFS) usuario;
                if (!elem.esCarpeta() && elem.getNombre().equals(nombreArchivo)) {
                    treeModel.removeNodeFromParent(hijo);
                    return;
                }
            }
        }
    }

    private void cerrarSesion() {
        // Volver a la pantalla de inicio de sesión / selección de perfil
        this.dispose();
        VentanaInicio ventanaInicio = new VentanaInicio(controlador);
        ventanaInicio.setVisible(true);
    }
}

