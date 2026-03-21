package Controlador;

import org.json.JSONArray;
import org.json.JSONObject;
import Modelo.Proceso;

import java.nio.file.Files;
import java.nio.file.Paths;

public class ParserJSON {
    
    private ControladorSistema controlador;
    
    public ParserJSON(ControladorSistema controlador) {
        this.controlador = controlador;
    }

    public String cargarCaso(String rutaArchivo) {
        try {
            String contenido = new String(Files.readAllBytes(Paths.get(rutaArchivo)), "UTF-8");
            JSONObject json = new JSONObject(contenido);
            
            // 1. initial_head (opcional pero util cargarlo antes)
            if (json.has("initial_head")) {
                int initialHead = json.getInt("initial_head");
                controlador.getPlanificador().setPosicionCabezal(initialHead);
            }

            // 2. system_files (Carga directa)
            if (json.has("system_files")) {
                JSONArray files = json.getJSONArray("system_files");
                for (int i = 0; i < files.length(); i++) {
                    JSONObject fileObj = files.getJSONObject(i);
                    String nombre = fileObj.getString("name");
                    int tamano = fileObj.getInt("size");
                    
                    // Ejecutamos sincrónicamente para pre-llenar el disco
                    Proceso dummy = new Proceso("SYS_LOAD", "admin", Config.OP_CREATE, nombre, controlador.getPlanificador().getPosicionCabezal(), tamano);
                    controlador.ejecutarCrearProceso(dummy);
                }
            }
            
            // 3. requests (Se encolan para HiloDisco)
            if (json.has("requests")) {
                JSONArray requests = json.getJSONArray("requests");
                for (int i = 0; i < requests.length(); i++) {
                    JSONObject req = requests.getJSONObject(i);
                    String type = req.getString("type").toUpperCase();
                    String user = req.has("user") ? req.getString("user") : "admin";
                    String name = req.has("name") ? req.getString("name") : "archivo_req" + i;
                    
                    if (type.equals("CREATE")) {
                        int size = req.getInt("size");
                        controlador.crearArchivo(name, size);
                    } else if (type.equals("DELETE")) {
                        controlador.eliminarArchivo(name);
                    } else if (type.equals("UPDATE")) {
                        int size = req.getInt("size"); // en UPDATE suele ser variacion, o nuevo tamano. Asumimos variacion aqui
                        controlador.modificarArchivo(name, size);
                    } else if (type.equals("READ")) {
                        controlador.leerArchivo(name);
                    }
                }
            }
            return "JSON cargado con éxito. Procesos encolados.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error al cargar JSON: " + e.getMessage();
        }
    }
}
