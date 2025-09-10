
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

        // Pedir usuario
        System.out.print("Ingrese usuario: ");
        String usuario = teclado.readLine();
        escritor.println(usuario);

        // Pedir contraseña
        System.out.print("Ingrese contraseña: ");
        String contrasena = teclado.readLine();
        escritor.println(contrasena);

        // Leer respuesta del servidor
        String respuesta = lector.readLine();
        System.out.println(respuesta);

        // Cerrar conexiones
        escritor.close();
        lector.close();
        socket.close();
    }
}
