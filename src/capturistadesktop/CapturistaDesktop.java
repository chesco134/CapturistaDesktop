/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package capturistadesktop;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.inspira.jcapiz.polivoto.seguridad.MD5Hash;
import org.inspira.polivoto.networking.AccionesDeCliente;
import org.inspira.polivoto.providers.LogProvider;
import org.json.JSONException;
import org.json.JSONObject;
import shared.DatosDeAccionCliente;

/**
 *
 * @author jcapiz
 */
public class CapturistaDesktop {

    private static final String USAGE
            = "Debe proporcionar el host del servidor, el nombre "
            + "de usuario y la dirección de un archivo"
            + " que contenga la contraseña.";
    private static boolean isRunning;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        if (args.length < 3) {
            LogProvider
                    .logMessage("Main Thread", USAGE);
        } else {
            DatosDeAccionCliente datosDeAccionCliente
                    = new DatosDeAccionCliente();
            datosDeAccionCliente.setUsrName(args[1]);   // Colocamos usrName
            try {
                BufferedReader entradaArchivo
                        = new BufferedReader(
                                new FileReader(new File(args[2])));
                String psswd = new MD5Hash().makeHash(entradaArchivo.readLine());
                datosDeAccionCliente.setPsswd(psswd);   // Colocamos Psswd
                entradaArchivo.close();
                try {
                    AccionesDeCliente accionesDeCliente
                            = new AccionesDeCliente(datosDeAccionCliente);
                    accionesDeCliente.probarConexion(args[0]);
                    accionesDeCliente.signIn();
                    accionesDeCliente.grabParticipant();
                    long tiempoFinal = new JSONObject(accionesDeCliente.consultaParametrosIniciales())
                            .getLong("tiempo_final");
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            LogProvider.logMessage("Capturista", "Votación terminada");
                            isRunning = false;
                            accionesDeCliente.close();
                        }
                    }, tiempoFinal);
                    Scanner scanner = new Scanner(System.in);
                    String boleta;
                    Map<String, String> resultado;
                    int resultadoConexionParticipante;
                    isRunning = true;
                    while (isRunning) {
                        try {
                            System.out.print("Escriba la boleta: ");
                            boleta = scanner.nextLine();
                            resultado = accionesDeCliente.consultaBoleta(boleta);
                            if (Integer.parseInt(resultado.get("veredicto")) != 0) {
                                LogProvider.logMessage("Main Thread",
                                        "Perfil: " + resultado.get("perfil"));
                                resultadoConexionParticipante
                                        = accionesDeCliente.conectaParticipante(boleta,
                                                resultado.get("perfil"));
                                LogProvider.logMessage("Main Thread", "Atención completa (código " + resultadoConexionParticipante + ")");
                            } else {
                                LogProvider.logMessage("Main Thread", "No pudimos validar la boleta esta vez, intente de nuevo por favor.");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }catch (NoSuchAlgorithmException | InvalidKeySpecException | JSONException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException ex) {
                    Logger.getLogger(CapturistaDesktop.class.getName()).log(Level.SEVERE, null, ex);
                }
                } catch (IOException e) {
                    LogProvider.logMessage("Main Thread",
                            "Tuvimos problemas al abrir el archivo de contraseña.");
                    System.exit(0);
                }
            }
            LogProvider.logMessage("Main Thread", "Terminamos.");
        }

    }
