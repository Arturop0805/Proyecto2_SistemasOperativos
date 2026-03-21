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
import Modelo.Estado;

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
import javax.swing.JList;
import javax.swing.DefaultListModel;

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
    private JList<String> listaJournal;
    private DefaultListModel<String> modeloJournal;
    private DefaultListModel<String> modeloColaProcesos;
    private JList<String> listaColaProcesos;
    private JTextArea areaMovimientos; // Log de movimientos del cabezal por política

    // Barra de estado / ciclo
    private JLabel lblCiclo;
    private boolean cicloActivo = false;
    private int cicloContador = 0;
    private Controlador.HiloDisco hiloDisco;
    private JButton btnPausaReanudar;

    // Configuración de vista
    private boolean mostrarNumerosBloques = true;

    // Cache LRU (últimos 8 archivos accedidos)
    private java.util.LinkedList<String> historialCache = new java.util.LinkedList<>();
    private DefaultListModel<String> modeloCache;
    private JList<String> listaCache;

    // Labels de estado dinámico
    private JLabel lblCabeza;
    private JLabel lblBloquesLibres;

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
        menuArchivo.setForeground(COLOR_TEXTO);
        JMenuItem miCerrarSesion = new JMenuItem("Cerrar Sesión");
        miCerrarSesion.addActionListener(e -> cerrarSesion());
        menuArchivo.add(miCerrarSesion);

        JMenu menuReportes = new JMenu("Reportes");
        menuReportes.setForeground(COLOR_TEXTO);
        JMenuItem miVerReporte = new JMenuItem("Ver Reporte del Sistema");
        miVerReporte.addActionListener(e -> mostrarReporte());
        menuReportes.add(miVerReporte);

        menuBar.add(menuArchivo);
        menuBar.add(menuReportes);
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
                    Object userObj = ((DefaultMutableTreeNode) value).getUserObject();
                    if (userObj instanceof ElementoFS) {
                        ElementoFS elemento = (ElementoFS) userObj;
                        setIcon(elemento.esCarpeta() ? iconoCarpeta : iconoArchivo);
                        
                        // Agregar conteo de bloques si es un archivo
                        if (!elemento.esCarpeta()) {
                            EntradaFAT entrada = controlador.getFat().buscarArchivo(elemento.getNombre());
                            if (entrada != null) {
                                setText(elemento.getNombre() + " [" + entrada.getCantidadBloques() + " bloques]");
                            }
                        }
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
                                mostrarVistaCarpeta();
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


        // Barra superior (toolbar)
        JPanel panelSuperior = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 5));
        panelSuperior.setBackground(COLOR_FONDO_APP);
        panelSuperior.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(100, 116, 139)));

        // Modo radio buttons
        JLabel lblModo = new JLabel("Modo:");
        lblModo.setForeground(COLOR_TEXTO); lblModo.setFont(FUENTE_BOTON);
        JRadioButton rbAdmin = new JRadioButton("Administrador");
        JRadioButton rbUser  = new JRadioButton("Usuario");
        for (JRadioButton rb : new JRadioButton[]{rbAdmin, rbUser}) {
            rb.setForeground(COLOR_TEXTO);
            rb.setBackground(COLOR_FONDO_APP);
            rb.setFont(FUENTE_BOTON);
        }
        ButtonGroup grupoModo = new ButtonGroup();
        grupoModo.add(rbAdmin); grupoModo.add(rbUser);
        boolean esAdminInicio = controlador.getUsuarioActivo().getRol().equals(Config.ROL_ADMIN);
        rbAdmin.setSelected(esAdminInicio);
        rbUser.setSelected(!esAdminInicio);
        rbAdmin.addActionListener(e -> {
            for (int i = 0; i < controlador.getUsuarios().obtenerTamano(); i++) {
                Modelo.Usuario u = controlador.getUsuarios().obtener(i);
                if (u.getRol().equals(Config.ROL_ADMIN)) { controlador.setUsuarioActivo(u); aplicarPermisosRol(); break; }
            }
        });
        rbUser.addActionListener(e -> {
            for (int i = 0; i < controlador.getUsuarios().obtenerTamano(); i++) {
                Modelo.Usuario u = controlador.getUsuarios().obtener(i);
                if (u.getRol().equals(Config.ROL_USER)) { controlador.setUsuarioActivo(u); aplicarPermisosRol(); break; }
            }
        });

        // Planificador
        JComboBox<String> comboPlanToolbar = new JComboBox<>(new String[]{"FIFO","SSTF","SCAN","C-SCAN"});
        comboPlanToolbar.setBackground(COLOR_FONDO_APP);
        comboPlanToolbar.setForeground(COLOR_TEXTO);
        comboPlanToolbar.setFont(FUENTE_BOTON);
        comboPlanToolbar.setSelectedItem(controlador.getPlanificador().getPoliticaActiva());
        comboPlanToolbar.addActionListener(e ->
            controlador.getPlanificador().setPoliticaActiva((String) comboPlanToolbar.getSelectedItem()));

        // Botones de operación
        JButton btnCrearArchivo    = new JButton("Crear Archivo");
        JButton btnCrearDir        = new JButton("Crear Directorio");
        JButton btnLeerArchivo     = new JButton("Leer");
        JButton btnRenombrar       = new JButton("Renombrar");
        JButton btnEliminarArchivo = new JButton("Eliminar");
        JButton btnEstadisticas    = new JButton("Estadísticas");
        for (JButton b : new JButton[]{btnCrearArchivo,btnCrearDir,btnLeerArchivo,btnRenombrar,btnEliminarArchivo})
            estilizarBoton(b, COLOR_SECUNDARIO);
        estilizarBoton(btnEstadisticas, new Color(124, 58, 237));
        btnCrearArchivo.addActionListener(e    -> crearArchivoEnCarpetaActual());
        btnCrearDir.addActionListener(e        -> crearDirectorioEnCarpetaActual());
        btnLeerArchivo.addActionListener(e     -> btnLeer.doClick());
        btnRenombrar.addActionListener(e       -> btnModificar.doClick());
        btnEliminarArchivo.addActionListener(e -> btnEliminar.doClick());
        btnEstadisticas.addActionListener(e    -> mostrarEstadisticas());

        // Pausa / Reanudar
        btnPausaReanudar = new JButton("Reanudar");
        estilizarBoton(btnPausaReanudar, COLOR_ALERTA);
        btnPausaReanudar.addActionListener(e -> toggleCiclo());

        // Botones de velocidad
        JLabel lblVelocidad = new JLabel("Velocidad:");
        lblVelocidad.setForeground(COLOR_TEXTO); lblVelocidad.setFont(FUENTE_BOTON);
        final Color CV_OFF = new Color(51, 65, 85), CV_ON = COLOR_PRIMARIO;
        JToggleButton btnX4  = new JToggleButton("x4");
        JToggleButton btnX2  = new JToggleButton("x2");
        JToggleButton btnX1  = new JToggleButton("x1");
        JToggleButton btnX05 = new JToggleButton("x0.5");
        ButtonGroup grupoVel = new ButtonGroup();
        for (JToggleButton tb : new JToggleButton[]{btnX4, btnX2, btnX1, btnX05}) {
            grupoVel.add(tb);
            tb.setFont(FUENTE_BOTON); tb.setForeground(Color.WHITE); tb.setBackground(CV_OFF);
            tb.setFocusPainted(false); tb.setBorderPainted(false); tb.setOpaque(true);
            tb.setBorder(new EmptyBorder(5, 10, 5, 10)); tb.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        btnX1.setSelected(true); btnX1.setBackground(CV_ON);
        btnX4.addActionListener(e  -> { hiloDisco.setVelocidadMs(Config.VELOCIDAD_X4);  btnX4.setBackground(CV_ON);  btnX2.setBackground(CV_OFF); btnX1.setBackground(CV_OFF); btnX05.setBackground(CV_OFF); });
        btnX2.addActionListener(e  -> { hiloDisco.setVelocidadMs(Config.VELOCIDAD_X2);  btnX2.setBackground(CV_ON);  btnX4.setBackground(CV_OFF); btnX1.setBackground(CV_OFF); btnX05.setBackground(CV_OFF); });
        btnX1.addActionListener(e  -> { hiloDisco.setVelocidadMs(Config.VELOCIDAD_X1);  btnX1.setBackground(CV_ON);  btnX4.setBackground(CV_OFF); btnX2.setBackground(CV_OFF); btnX05.setBackground(CV_OFF); });
        btnX05.addActionListener(e -> { hiloDisco.setVelocidadMs(Config.VELOCIDAD_X05); btnX05.setBackground(CV_ON); btnX4.setBackground(CV_OFF); btnX2.setBackground(CV_OFF); btnX1.setBackground(CV_OFF); });

        // Ciclo y Cabeza labels
        lblCiclo = new JLabel("Ciclo: 0");
        lblCiclo.setForeground(COLOR_TEXTO); lblCiclo.setFont(FUENTE_BOTON);
        lblCabeza = new JLabel("Cabeza: 0");
        lblCabeza.setForeground(COLOR_SECUNDARIO); lblCabeza.setFont(FUENTE_BOTON);

        // Cerrar sesión
        JButton btnCerrarSesionToolbar = new JButton("Cerrar Sesión");
        btnCerrarSesionToolbar.setBackground(new Color(100,116,139)); btnCerrarSesionToolbar.setForeground(Color.WHITE);
        btnCerrarSesionToolbar.setFont(FUENTE_BOTON); btnCerrarSesionToolbar.setFocusPainted(false);
        btnCerrarSesionToolbar.setBorderPainted(false); btnCerrarSesionToolbar.setOpaque(true);
        btnCerrarSesionToolbar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCerrarSesionToolbar.setBorder(new EmptyBorder(5,12,5,12));
        btnCerrarSesionToolbar.addActionListener(e -> cerrarSesion());

        // Cargar JSON
        JButton btnCargarJSON = new JButton("Cargar JSON");
        estilizarBoton(btnCargarJSON, new Color(16, 185, 129)); // Verde Esmeralda
        btnCargarJSON.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos JSON", "json"));
            int seleccion = fileChooser.showOpenDialog(this);
            if (seleccion == JFileChooser.APPROVE_OPTION) {
                String ruta = fileChooser.getSelectedFile().getAbsolutePath();
                Controlador.ParserJSON parser = new Controlador.ParserJSON(controlador);
                String msg = parser.cargarCaso(ruta);
                mostrarMensaje(msg);
                actualizarVistaGlobal();
            }
        });

        panelSuperior.add(lblModo);
        panelSuperior.add(rbAdmin);
        panelSuperior.add(rbUser);
        panelSuperior.add(Box.createHorizontalStrut(10));
        panelSuperior.add(comboPlanToolbar);
        panelSuperior.add(Box.createHorizontalStrut(10));
        panelSuperior.add(btnCargarJSON);
        panelSuperior.add(btnCrearArchivo);
        panelSuperior.add(btnCrearDir);
        panelSuperior.add(btnLeerArchivo);
        panelSuperior.add(btnRenombrar);
        panelSuperior.add(btnEliminarArchivo);
        panelSuperior.add(btnEstadisticas);
        panelSuperior.add(Box.createHorizontalStrut(10));
        panelSuperior.add(btnPausaReanudar);
        panelSuperior.add(lblVelocidad); panelSuperior.add(btnX4); panelSuperior.add(btnX2);
        panelSuperior.add(btnX1); panelSuperior.add(btnX05);
        panelSuperior.add(lblCiclo); panelSuperior.add(lblCabeza);
        panelSuperior.add(btnCerrarSesionToolbar);

        setJMenuBar(crearMenuBar());
        add(panelSuperior, BorderLayout.NORTH);

        // Hilo en background para procesar E/S
        hiloDisco = new Controlador.HiloDisco(
            controlador,
            Config.VELOCIDAD_X1,
            (resultado) -> {
                mostrarMensaje(resultado);
                cicloContador++;
                actualizarCiclo();
            },
            () -> actualizarVistaGlobal()
        );
        hiloDisco.start();

        // 2. PANEL CENTRAL: TABS (Disco / FAT / Cache)
        JTabbedPane tabsCentro = new JTabbedPane();
        tabsCentro.setBackground(COLOR_PANEL);
        tabsCentro.setForeground(COLOR_TEXTO);
        tabsCentro.setFont(FUENTE_BOTON);

        // 2a. Simulación de Disco
        panelCentro = new JPanel(new CardLayout());
        cardLayout = (CardLayout) panelCentro.getLayout();
        
        panelDisco = new JPanel();
        int capacidad = controlador.getDisco().getCapacidadTotal();
        int columnas = 32;
        int filas = (int) Math.ceil((double) capacidad / columnas);
        panelDisco.setLayout(new GridLayout(filas, columnas, 2, 2));
        panelDisco.setBackground(COLOR_PANEL);
        JScrollPane scrollDisco = new JScrollPane(panelDisco);
        scrollDisco.setBorder(null);
        scrollDisco.getViewport().setBackground(COLOR_FONDO_APP);
        
        lblBloquesLibres = new JLabel("Bloques libres: 0/0");
        lblBloquesLibres.setForeground(COLOR_TEXTO);
        lblBloquesLibres.setFont(FUENTE_BOTON);
        
        JPanel panelDiscoContenedor = new JPanel(new BorderLayout());
        panelDiscoContenedor.add(scrollDisco, BorderLayout.CENTER);
        panelDiscoContenedor.add(lblBloquesLibres, BorderLayout.SOUTH);
        panelCentro.add(panelDiscoContenedor, "DISCO");

        // 2b. Vista de carpeta (dentro de la misma pestaña DISCO via CardLayout)
        String[] columnasCarpeta = {"Nombre", "Tipo", "Tamaño (bloques)"};
        modeloTablaCarpeta = new DefaultTableModel(columnasCarpeta, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaCarpeta = new JTable(modeloTablaCarpeta);
        estilizarTabla(tablaCarpeta);
        tablaCarpeta.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int fila = tablaCarpeta.rowAtPoint(e.getPoint());
                    if (fila != -1) {
                        String tipo = (String) modeloTablaCarpeta.getValueAt(fila, 1);
                        if ("Carpeta".equals(tipo)) {
                            carpetaActual += "/" + (String) modeloTablaCarpeta.getValueAt(fila, 0);
                            seleccionarNodoPorRuta(carpetaActual);
                            mostrarVistaCarpeta();
                        }
                    }
                }
            }
        });
        panelCarpeta = new JPanel(new BorderLayout());
        panelCarpeta.add(new JScrollPane(tablaCarpeta), BorderLayout.CENTER);
        panelCentro.add(panelCarpeta, "CARPETA");

        tabsCentro.addTab("Simulación de Disco", panelCentro);

        // 2c. Tabla de Asignación (FAT)
        String[] columnasFAT = {"Nombre Archivo", "Cantidad Bloques", "Dir. Primer Bloque"};
        modeloTablaFAT = new DefaultTableModel(columnasFAT, 0);
        tablaFAT = new JTable(modeloTablaFAT);
        estilizarTabla(tablaFAT);
        tabsCentro.addTab("Tabla de Asignación", new JScrollPane(tablaFAT));

        // 2d. Cache
        modeloCache = new DefaultListModel<>();
        listaCache = new JList<>(modeloCache);
        listaCache.setBackground(COLOR_PANEL);
        listaCache.setForeground(COLOR_TEXTO);
        listaCache.setFont(FUENTE_BASE);
        tabsCentro.addTab("Cache", new JScrollPane(listaCache));

        // 2e. Journal
        modeloJournal = new DefaultListModel<>();
        listaJournal = new JList<>(modeloJournal);
        listaJournal.setBackground(COLOR_PANEL);
        listaJournal.setForeground(COLOR_TEXTO);
        listaJournal.setFont(FUENTE_BASE);
        JPanel pJournal = new JPanel(new BorderLayout());
        pJournal.add(new JScrollPane(listaJournal), BorderLayout.CENTER);
        
        JPanel pJournalBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pJournalBotones.setBackground(COLOR_PANEL);
        JButton btnFallo = new JButton("Simular Fallo");
        estilizarBoton(btnFallo, new Color(220, 38, 38)); // Red
        btnFallo.addActionListener(e -> {
            controlador.setForzarFallo(true);
            mostrarMensaje("Atención: El próximo CREATE/UPDATE/DELETE fallará a medias.");
        });
        JButton btnRecuperar = new JButton("Recuperar Sistema");
        estilizarBoton(btnRecuperar, new Color(34, 197, 94)); // Green
        btnRecuperar.addActionListener(e -> {
            String res = controlador.recuperarFallos();
            mostrarMensaje(res);
            actualizarVistaGlobal();
        });
        pJournalBotones.add(btnFallo);
        pJournalBotones.add(btnRecuperar);
        pJournal.add(pJournalBotones, BorderLayout.SOUTH);
        
        tabsCentro.addTab("Journal (Fallos)", pJournal);

        // 2f. Movimientos del Cabezal
        areaMovimientos = new JTextArea();
        areaMovimientos.setEditable(false);
        areaMovimientos.setBackground(new Color(15, 23, 42));
        areaMovimientos.setForeground(new Color(134, 239, 172)); // Verde claro para resaltar
        areaMovimientos.setFont(new Font("Monospaced", Font.PLAIN, 13));
        areaMovimientos.setText("Ejecuta operaciones y la secuencia de movimientos del cabezal aparecera aqui.\n"
                + "Cambia la politica (FIFO/SSTF/SCAN/C-SCAN) para comparar distancias.");

        JPanel pMovimientos = new JPanel(new BorderLayout());
        pMovimientos.add(new JScrollPane(areaMovimientos), BorderLayout.CENTER);

        JPanel pMovBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pMovBotones.setBackground(COLOR_PANEL);
        JButton btnResetMovimientos = new JButton("Resetear Historial");
        estilizarBoton(btnResetMovimientos, new Color(99, 102, 241)); // Violeta
        btnResetMovimientos.addActionListener(e -> {
            controlador.getPlanificador().resetearHistorial();
            actualizarMovimientos();
        });
        pMovBotones.add(btnResetMovimientos);
        pMovimientos.add(pMovBotones, BorderLayout.SOUTH);

        tabsCentro.addTab("Movimientos Cabezal", pMovimientos);

        add(tabsCentro, BorderLayout.CENTER);

        // 3. PANEL INFERIOR: LOG (IZQ) + COLA (DER)
        JPanel panelInferior = new JPanel(new GridLayout(1, 2, 10, 0));
        panelInferior.setBackground(COLOR_FONDO_APP);
        panelInferior.setPreferredSize(new Dimension(0, 200));
        panelInferior.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Log
        areaLog = new JTextArea();
        areaLog.setEditable(false);
        areaLog.setFont(FUENTE_BASE);
        areaLog.setBackground(new Color(15, 23, 42));
        areaLog.setForeground(COLOR_TEXTO);
        JButton btnLimpiarLog = new JButton("Limpiar");
        estilizarBoton(btnLimpiarLog, COLOR_PANEL);
        btnLimpiarLog.addActionListener(e -> areaLog.setText(""));
        JPanel pLog = new JPanel(new BorderLayout());
        pLog.setBorder(BorderFactory.createTitledBorder(null, "Log de Eventos", 0, 0, FUENTE_BOTON, COLOR_TEXTO));
        pLog.add(new JScrollPane(areaLog), BorderLayout.CENTER);
        pLog.add(btnLimpiarLog, BorderLayout.SOUTH);

        // Cola
        modeloColaProcesos = new DefaultListModel<>();
        listaColaProcesos = new JList<>(modeloColaProcesos);
        listaColaProcesos.setBackground(new Color(15, 23, 42));
        listaColaProcesos.setForeground(COLOR_TEXTO);
        listaColaProcesos.setFont(FUENTE_BASE);
        JPanel pCola = new JPanel(new BorderLayout());
        pCola.setBorder(BorderFactory.createTitledBorder(null, "Cola de Procesos", 0, 0, FUENTE_BOTON, COLOR_TEXTO));
        pCola.add(new JScrollPane(listaColaProcesos), BorderLayout.CENTER);

        panelInferior.add(pLog);
        panelInferior.add(pCola);
        add(panelInferior, BorderLayout.SOUTH);

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

        JComboBox<String> comboPlanificador = new JComboBox<>(new String[] {"FIFO", "SSTF", "SCAN", "C-SCAN"});
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
        JButton btnProcesar = new JButton("Procesar Siguiente");

        JComponent[] botones = {btnToggleVista, btnCrear, btnCrearCarpeta, btnLeer, btnModificar, btnEliminar, btnProcesar};
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

        JTextArea areaJournalLocal = new JTextArea();
        areaJournalLocal.setEditable(false);
        areaJournalLocal.setBackground(COLOR_FONDO_APP);
        areaJournalLocal.setForeground(COLOR_TEXTO);
        areaJournalLocal.setFont(FUENTE_BASE);
        areaJournalLocal.setText("Journal y Fallos se encuentran en la pestaña activa superior.");
        JScrollPane scrollJournal = new JScrollPane(areaJournalLocal);
        scrollJournal.setBorder(null);
        panelJournal.add(scrollJournal, BorderLayout.CENTER);

        // Estado sistema
        JButton btnSimularFallo = new JButton("Simular Fallo");
        estilizarBoton(btnSimularFallo, COLOR_ALERTA);
        btnSimularFallo.addActionListener(e -> {
            mostrarMensaje("Fallo simulado. Revise el Journal para deshacer operaciones PENDIENTES.");
        });

        JLabel lblEstado = new JLabel("Estado del Sistema: Normal");
        lblEstado.setForeground(COLOR_TEXTO);
        lblEstado.setFont(FUENTE_BASE);

        JPanel panelJournalFooter = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panelJournalFooter.setBackground(COLOR_PANEL);
        panelJournalFooter.add(btnSimularFallo);
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
                // Intentar primero con el nombre tal cual, luego con prefijo de carpeta actual si falla
                String resultado = controlador.leerArchivo(nombre);
                if (resultado.startsWith("Error") && !nombre.contains("/")) {
                    // El usuario tecleó sólo el nombre corto; intentamos con la ruta completa
                    resultado = controlador.leerArchivo(carpetaActual + "/" + nombre);
                }
                mostrarMensaje(resultado);
                actualizarVistaGlobal();
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

        // PROCESAR SIGUIENTE PROCESO (FIFO)
        btnProcesar.addActionListener(e -> {
            String resultado = controlador.procesarSiguienteOperacion();
            mostrarMensaje(resultado);
            actualizarVistaGlobal();
        });
    }

    private void mostrarEstadisticas() {
        int totalBloques = controlador.getDisco().getCapacidadTotal();
        int ocupados = totalBloques - controlador.getDisco().getBloquesLibres();
        int archs = controlador.getFat().getEntradas().obtenerTamano();
        String msg = "=== ESTADÍSTICAS DEL SISTEMA ===\n\n" +
                     "Archivos Creados: " + archs + "\n" +
                     "Bloques Ocupados: " + ocupados + "\n" +
                     "Bloques Libres: " + (totalBloques - ocupados) + "\n" +
                     "Capacidad Total: " + totalBloques + " bloques\n" +
                     "Ciclos de E/S: " + cicloContador + "\n";
        JOptionPane.showMessageDialog(this, msg, "Estadísticas", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarReporte() {
        StringBuilder sb = new StringBuilder("=== REPORTE DE SISTEMA DE ARCHIVOS ===\n\n");
        sb.append(String.format("%-25s | %-10s | %-12s\n", "Nombre", "Bloques", "Inicio"));
        sb.append("----------------------------------------------------------\n");
        for (int i = 0; i < controlador.getFat().getEntradas().obtenerTamano(); i++) {
            EntradaFAT e = controlador.getFat().getEntradas().obtener(i);
            sb.append(String.format("%-25s | %-10d | %-12d\n", e.getNombreArchivo(), e.getCantidadBloques(), e.getDireccionPrimerBloque()));
        }
        JTextArea area = new JTextArea(sb.toString());
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Reporte FAT", JOptionPane.PLAIN_MESSAGE);
    }
    
    // Método de utilidad para mostrar popups dependiendo de si es Error o Éxito
    private void mostrarMensaje(String mensaje) {
        if (mensaje.startsWith("Error")) {
            JOptionPane.showMessageDialog(this, mensaje, "Operación Fallida", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, mensaje, "Operación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            // Si la operación fue lectura (o creación/modificación), podemos actualizar el cache
            // Para simplificar, buscamos si el mensaje tiene comillas simples que indiquen el archivo
            if (mensaje.contains("'")) {
                int first = mensaje.indexOf("'");
                int last = mensaje.lastIndexOf("'");
                if (first != last) {
                    actualizarCacheReciente(mensaje.substring(first + 1, last));
                }
            }
        }

        String linea = java.time.LocalTime.now().withNano(0) + " - " + mensaje + "\n";
        if (areaLog != null) {
            areaLog.append(linea);
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
        actualizarJournal();
        actualizarMovimientos();
        // Siempre actualizamos la vista de carpeta para que refleje cambios aun cuando
        // estemos en la vista de disco o FAT.
        actualizarTablaCarpeta();
        treeModel.reload(); // Sync JTree with FAT after every operation
        // actualizarArbol();
    }

    private void actualizarJournal() {
        if (modeloJournal == null || controlador == null) return;
        modeloJournal.clear();
        EstructurasDeDatos.Nodo<EstructurasDeDatos.Transaccion> actual = controlador.getJournal().getInicio();
        while (actual != null) {
            modeloJournal.addElement(actual.getDato().toString());
            actual = actual.getSiguiente();
        }
    }

    private void actualizarMovimientos() {
        if (areaMovimientos == null || controlador == null) return;
        areaMovimientos.setText(controlador.getPlanificador().getResumenMovimientos());
    }

    private void actualizarCiclo() {
        if (lblCiclo == null) return;
        lblCiclo.setText("Ciclo: " + cicloContador);
        if (lblCabeza != null) lblCabeza.setText("Cabeza: " + controlador.getPlanificador().getPosicionCabezal());
    }

    // Método para pausar/reanudar el ciclo
    private void toggleCiclo() {
        cicloActivo = !cicloActivo;
        if (cicloActivo) {
            hiloDisco.reanudar();
            btnPausaReanudar.setText("Pausar");
        } else {
            hiloDisco.pausar();
            btnPausaReanudar.setText("Reanudar");
        }
    }
    

    private void actualizarColaProcesos() {
        if (modeloColaProcesos == null) return;
        modeloColaProcesos.clear();
        if (controlador == null || controlador.getPlanificador() == null) return;
        
        modeloColaProcesos.addElement("=== LISTOS ===");
        for (int i=0; i < controlador.getPlanificador().getColaListos().obtenerTamano(); i++) {
            Proceso p = controlador.getPlanificador().getColaListos().obtener(i);
            if (p.getEstado() == Estado.LISTO) modeloColaProcesos.addElement("  " + p.getIdProceso() + " - " + p.getOperacion() + " (" + p.getArchivoObjetivo() + ")");
        }
        
        modeloColaProcesos.addElement("=== EN CPU ===");
        // Nota: En este simulador FIFO simple, no hay un "Proceso en CPU" persistente más allá de la ejecución instantánea.
        
        modeloColaProcesos.addElement("=== BLOQUEADOS ===");
        
        modeloColaProcesos.addElement("=== I/O EN EJECUCION ===");
        
        modeloColaProcesos.addElement("=== COLA I/O ===");
    }

    private void actualizarCacheReciente(String nombreArchivo) {
        historialCache.remove(nombreArchivo);
        historialCache.addFirst(nombreArchivo);
        if (historialCache.size() > 8) historialCache.removeLast();
        
        modeloCache.clear();
        for (String s : historialCache) modeloCache.addElement(s);
    }

    private void actualizarMapaDisco() {
        try {
            panelDisco.removeAll();
            int capacidad = controlador.getDisco().getCapacidadTotal();
            int ocupados = 0;
            
            for (int i = 0; i < capacidad; i++) {
                Bloque bloque = controlador.getDisco().getBloque(i);
                JPanel vistaBloque = new JPanel(new BorderLayout());
                vistaBloque.setPreferredSize(new Dimension(25, 25)); // FORZAR tamaño para evitar que desaparezca en el Layout
                vistaBloque.setBorder(BorderFactory.createLineBorder(new Color(75, 85, 99)));

            if (mostrarNumerosBloques) {
                JLabel lblNumero = new JLabel(String.format("%02d", i), SwingConstants.CENTER);
                lblNumero.setForeground(COLOR_TEXTO);
                lblNumero.setFont(new Font("Segoe UI", Font.BOLD, 11));
                vistaBloque.add(lblNumero, BorderLayout.CENTER);
            }
            
            if (bloque.estaOcupado()) {
                ocupados++;
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
            
            lblBloquesLibres.setText("Bloques libres: " + (capacidad - ocupados) + "/" + capacidad);
        } catch (Exception e) {
            System.err.println("Error al actualizar mapa: " + e.getMessage());
        } finally {
            panelDisco.revalidate();
            panelDisco.repaint();
        }
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
        // Guardar la fila seleccionada antes de actualizar
        int filaSeleccionada = tablaCarpeta.getSelectedRow();
        String nombreSeleccionado = null;
        if (filaSeleccionada != -1) {
            nombreSeleccionado = (String) modeloTablaCarpeta.getValueAt(filaSeleccionada, 0);
        }

        modeloTablaCarpeta.setRowCount(0);

        DefaultMutableTreeNode nodoActual = encontrarNodoPorRuta(carpetaActual);
        if (nodoActual == null) {
            return;
        }

        int filaRestaurar = -1;
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
                // Si el nombre coincide con el que estaba seleccionado, recordar la fila
                if (nombreSeleccionado != null && nombreSeleccionado.equals(nombre)) {
                    filaRestaurar = i;
                }
            }
        }
        // Restaurar la selección si corresponde
        if (filaRestaurar != -1 && filaRestaurar < modeloTablaCarpeta.getRowCount()) {
            tablaCarpeta.setRowSelectionInterval(filaRestaurar, filaRestaurar);
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
        if (hiloDisco != null) {
            hiloDisco.detener();
        }
        // Volver a la pantalla de inicio de sesión / selección de perfil
        this.dispose();
        VentanaInicio ventanaInicio = new VentanaInicio(controlador);
        ventanaInicio.setVisible(true);
    }
}

