package cliente;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Cliente {

    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("localhost", 8080);
        PrintWriter escritor = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader lector = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));

        Thread receptor = new Thread(() -> {
            try {
                String linea;
                boolean recibiendoArchivo = false;
                PrintWriter archivoActual = null;

                while ((linea = lector.readLine()) != null) {
                    if (linea.startsWith("📥 INICIO_ARCHIVO")) {
                        String nombreArchivo = linea.split(" ", 2)[1];
                        archivoActual = new PrintWriter(new FileWriter(nombreArchivo));
                        recibiendoArchivo = true;
                        System.out.println("Descargando archivo: " + nombreArchivo);
                        continue;
                    }
                    if (linea.startsWith("📥 FIN_ARCHIVO")) {
                        if (archivoActual != null) {
                            archivoActual.close();
                            archivoActual = null;
                        }
                        recibiendoArchivo = false;
                        System.out.println("✅ Archivo descargado con éxito.");
                        continue;
                    }
                    if (recibiendoArchivo && archivoActual != null) {
                        archivoActual.println(linea);
                    } else {
                        System.out.println(linea);
                    }
                }
            } catch (IOException e) {
                System.out.println("Conexión cerrada.");
            }
        });
        receptor.start();

        String texto;
        while ((texto = teclado.readLine()) != null) {
            escritor.println(texto);
            if (texto.equalsIgnoreCase("SALIR")) {
                break;
            }
        }
        socket.close();
    }
}
