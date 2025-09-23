
package Cliente;

import java.io.BufferedReader;
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
                while ((linea = lector.readLine()) != null) {
                    System.out.println(linea);
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
