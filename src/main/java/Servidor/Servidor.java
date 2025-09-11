package Servidor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Servidor {

    private static Set<String> usuariosConectados = ConcurrentHashMap.newKeySet();
    private static Map<String, List<String>> mensajesPendientes = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
 ServerSocket serverSocket = new ServerSocket(8080);
        System.out.println("Servidor escuchando en puerto 8080...");

        while (true) {
            Socket cliente = serverSocket.accept();
            new Thread(new ManejadorCliente(cliente)).start();
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

                String linea;
                while ((linea = lector.readLine()) != null) {
                    if (linea.equalsIgnoreCase("SALIR")) {
                        break;
                    }
                    if (linea.contains(":")) {
                        String[] partes = linea.split(":", 2);
                        String destinatario = partes[0];
                        String mensaje = partes[1];

                        String completo = usuario + " dice: " + mensaje;

                        mensajesPendientes.putIfAbsent(destinatario, new ArrayList<>());
                        mensajesPendientes.get(destinatario).add(completo);

                        escritor.println("✅ Mensaje enviado a " + destinatario);
                        guardarRegistro(usuario, "Mensaje a " + destinatario + ": " + mensaje);
                    } else {
                        escritor.println("Formato inválido. Usa destinatario:mensaje");
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
    }
}
