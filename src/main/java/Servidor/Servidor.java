package Servidor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Servidor {

    private static Set<String> usuariosConectados = ConcurrentHashMap.newKeySet();
    private static Map<String, List<String>> mensajesPendientes = new ConcurrentHashMap<>();
    private static Set<String> usuariosRegistrados = ConcurrentHashMap.newKeySet();
    private static Map<String, Set<String>> usuariosBloqueados = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
 try (ServerSocket serverSocket = new ServerSocket(8080)) 
 {
    System.out.println("Servidor escuchando en puerto 8080...");

    while (true) {
        Socket cliente = serverSocket.accept();
        new Thread(new ManejadorCliente(cliente)).start();
    }
 }
    }

    static class ManejadorCliente implements Runnable {
        private Socket socket;
        private String usuario;
        private PrintWriter escritor;
        private BufferedReader lector;

        public ManejadorCliente(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                escritor = new PrintWriter(socket.getOutputStream(), true);
                lector = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                escritor.println("Ingrese su usuario:");
                usuario = lector.readLine();

               Files.createDirectories(Paths.get("carpeta_" + usuario));

               usuariosRegistrados.add(usuario);

                usuariosConectados.add(usuario);
                guardarRegistro(usuario, "Conectado");

                escritor.println("Usuarios conectados: " + usuariosConectados);

                if (mensajesPendientes.containsKey(usuario)) {
                    escritor.println("Tienes mensajes:");
                    for (String msg : mensajesPendientes.get(usuario)) {
                        escritor.println("📩 " + msg);
                    }
                    mensajesPendientes.remove(usuario);
                } else {
                    escritor.println("No tienes mensajes nuevos.");
                }
                    mostrarComandos();
                 
                String linea;
                while ((linea = lector.readLine()) != null) {
                    if (linea.equalsIgnoreCase("SALIR")) {
                        break;
                      }
                    if (linea.equalsIgnoreCase("AYUDA")) {
                        mostrarComandos();
                        continue;
                    }
                     if (linea.equalsIgnoreCase("USUARIOS")) {
                        escritor.println("👥 Usuarios conectados: " + usuariosConectados);
                        java.util.Set<String> noConectados = new java.util.HashSet<>(usuariosRegistrados);
                        noConectados.removeAll(usuariosConectados);
                        escritor.println("🔒 Usuarios no conectados (registrados): " + noConectados);
                        continue;
                    }
                   if (linea.equalsIgnoreCase("BORRAR")) {
                     if (mensajesPendientes.containsKey(usuario)) {
                       mensajesPendientes.remove(usuario);
                       escritor.println("🗑️ Todos tus mensajes fueron borrados.");
                       guardarRegistro(usuario, "Borró sus mensajes");
                        } else {
                       escritor.println("No tienes mensajes para borrar.");
                   }
                     continue; 
                   }
                   if (linea.equalsIgnoreCase("LISTAR") || linea.equalsIgnoreCase("LISTA")) {
                        listarArchivosDe(usuario);
                        continue;
                    }
                    if (linea.toUpperCase().startsWith("LISTAR:") || linea.toUpperCase().startsWith("LISTA:")) {
                        String[] p = linea.split(":", 2);
                        String objetivo = (p.length > 1) ? p[1].trim() : "";
                    if (objetivo.isEmpty()) {
                            escritor.println("❌ Uso: LISTAR:usuario  (o 'LISTAR' para ver tus archivos)");
                        } else {
                            listarArchivosDe(objetivo);
                        }
                        continue;
                    }
                   if (linea.startsWith("DESCARGAR")) {
                        String[] partes = linea.split(":", 3);
                   if (partes.length < 3) {
                       escritor.println("❌ Uso: DESCARGAR:usuario:archivo.txt");
                     continue;
                   }
                        String objetivo = partes[1];
                        String archivo = partes[2];
                      java.io.File file = new java.io.File("carpeta_" + objetivo + "/" + archivo);
                  if (!file.exists()) {
                        escritor.println("❌ El archivo no existe.");
                      } else {
                       escritor.println("📥 INICIO_ARCHIVO " + archivo);
                  try (BufferedReader fr = new BufferedReader(new java.io.FileReader(file))) {
                      String lineaArchivo;
                  while ((lineaArchivo = fr.readLine()) != null) {
                      escritor.println(lineaArchivo);
                  }
                   }
                       escritor.println("📥 FIN_ARCHIVO " + archivo);
                  }
                     continue;
                }
                    if (linea.startsWith("BLOQUEAR:")) {
                        String objetivo = linea.split(":", 2)[1].trim();
                      if (objetivo.equals(usuario)) {
                            escritor.println("❌ No puedes bloquearte a ti mismo.");
                        } else {
                            usuariosBloqueados.putIfAbsent(usuario, ConcurrentHashMap.newKeySet());
                            usuariosBloqueados.get(usuario).add(objetivo);
                            escritor.println("🚫 Has bloqueado a " + objetivo);
                            guardarRegistro(usuario, "Bloqueó a " + objetivo);
                        }
                        continue;
                    }
                       if (linea.startsWith("DESBLOQUEAR:")) {
                        String objetivo = linea.split(":", 2)[1].trim();
                    if (usuariosBloqueados.containsKey(usuario) && usuariosBloqueados.get(usuario).remove(objetivo)) {
                        escritor.println("✅ Has desbloqueado a " + objetivo);
                        guardarRegistro(usuario, "Desbloqueó a " + objetivo);
                  } else {
                        escritor.println("❌ " + objetivo + " no estaba bloqueado.");
                  }
                     continue;
                }
                    if (linea.contains(":")) {
                        String[] partes = linea.split(":", 2);
                        String destinatario = partes[0];
                        String mensaje = partes.length > 1 ? partes[1].trim() : "";
                        String completo = usuario + " dice: " + mensaje;

                    if (usuariosBloqueados.containsKey(destinatario) &&
                        usuariosBloqueados.get(destinatario).contains(usuario)) {
                        escritor.println("❌ No puedes enviar mensajes a " + destinatario + " (te ha bloqueado).");
                        guardarRegistro(usuario, "Intentó enviar mensaje a " + destinatario + " pero estaba bloqueado.");
                        continue;
                    }
                        mensajesPendientes.putIfAbsent(destinatario, new ArrayList<>());
                        mensajesPendientes.get(destinatario).add(completo);

                        escritor.println("✅ Mensaje enviado a " + destinatario);
                        guardarRegistro(usuario, "Mensaje a " + destinatario + ": " + mensaje);
                    } else {
                        escritor.println("Formato invalido. Usa 'destinatario: mensaje' o escribe AYUDA para ver comandos.");
                    }                    
                }
                usuariosConectados.remove(usuario);
                guardarRegistro(usuario, "Desconectado");
                escritor.println("Sesión cerrada. Adiós " + usuario);

                socket.close();
            } catch (IOException e) {
                System.out.println("Error con el cliente: " + e.getMessage());
            }
        }

         private void listarArchivosDe(String objetivo) {
            File carpeta = new File("carpeta_" + objetivo);
            if (!carpeta.exists()) {
                escritor.println("❌ No existe directorio para " + objetivo);
                return;
            }
            String[] archivos = carpeta.list((dir, name) -> name.toLowerCase().endsWith(".txt"));
            if (archivos != null && archivos.length > 0) {
                escritor.println("📂 Archivos de " + objetivo + ":");
                for (String archivo : archivos) {
                    escritor.println("   - " + archivo);
                }
            } else {
                escritor.println("📂 " + objetivo + " no tiene archivos .txt");
            }
        }

         private void guardarRegistro(String usuario, String accion) {
            try (FileWriter fw = new FileWriter("registros.txt", true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw)) {

                out.println("Usuario: " + usuario +
                        " | Acción: " + accion +
                        " | Fecha/Hora: " + java.time.LocalDateTime.now());
            } catch (IOException e) {
                System.out.println("Error al guardar registro: " + e.getMessage());
            }
        }
        private void mostrarComandos() {
              escritor.println("===== COMANDOS DISPONIBLES =====");
            escritor.println("1) Enviar mensaje: destinatario: mensaje");
            escritor.println("   Ejemplo: ana: hola, ¿cómo estás?");
            escritor.println("2) Borrar todos tus mensajes pendientes: BORRAR");
            escritor.println("3) Mostrar esta ayuda en cualquier momento: AYUDA");
            escritor.println("4) Listar tus archivos .txt (en servidor): LISTAR  (o LISTA)");
            escritor.println(". Listar archivos de otro usuario: LISTAR:usuario");
            escritor.println("   Ejemplo: LISTAR:ana");
            escritor.println("5) Descargar archivo de otro usuario: DESCARGAR:usuario:archivo.txt");
            escritor.println("   También aceptado: DESCARGAR usuario archivo.txt");
            escritor.println("6) Bloquear mensajes de un usuario: BLOQUEAR:usuario");
            escritor.println("7) Desbloquear usuario: DESBLOQUEAR:usuario");
            escritor.println("8) Ver usuarios conectados y no conectados: USUARIOS");
            escritor.println("9) Cerrar sesión: SALIR");
            escritor.println("====================================");
    }
  }
}
