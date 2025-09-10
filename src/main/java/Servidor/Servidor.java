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

    public static void main(String[] args) throws Exception {
             ServerSocket socketServidor = new ServerSocket(8080);
        System.out.println("Servidor esperando conexión...");

        Socket cliente = socketServidor.accept();
        System.out.println("Cliente conectado.");

        PrintWriter escritor = new PrintWriter(cliente.getOutputStream(), true);
        BufferedReader lector = new BufferedReader(new InputStreamReader(cliente.getInputStream()));

        // Recibir usuario
        String usuario = lector.readLine();
        // Recibir contraseña
        String contrasena = lector.readLine();

        // Enviar confirmación al cliente
        escritor.println("✅ Datos recibidos. Usuario: " + usuario);

        // Guardar en archivo (registro)
        guardarRegistro(usuario, contrasena);

        // Cerrar conexiones
        escritor.close();
        lector.close();
        cliente.close();
        socketServidor.close();
    }

    // Método para guardar los datos en un archivo de texto
    private static void guardarRegistro(String usuario, String contrasena) {
        try (FileWriter fw = new FileWriter("registros.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            
            out.println("Usuario: " + usuario + 
                        " | Contraseña: " + contrasena + 
                        " | Fecha/Hora: " + java.time.LocalDateTime.now());
        } catch (IOException e) {
            System.out.println("Error al guardar el registro: " + e.getMessage());
        }
    }
}
