package org.inspira.polivoto.networking;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.inspira.polivoto.providers.LogProvider;
import org.inspira.polivoto.security.Hasher;

import shared.DatosDeAccionCliente;

public class AccionesDeCliente {

    // Al conectar con el servidor, cuenta el envío del primer byte.
    // Al enviar el byte -1, el servidor entiende que lo que sigue es
    // un intercambio de llave simétrica a través de un equema de llave
    // pública.
    // Posterior a que el sistema acepta un usuario,
    // le entrega un id de inicio de sesión que el cliente deberá
    // usar para hacer peticiones al servidor.
    // Con ello el servidor verifica que se trate de un inicio de
    // sesión válido y utilizará la llave simétrica acordada en dicho
    // establecimiento de sesión con el cliente.
    private Socket socket;
    private IOHandler ioHandler;
    private byte[] chunk;
    private SecretKey secretKey;
    private Cipher cipher;
    private SecretKey partKey;
    private DatosDeAccionCliente data;

    public AccionesDeCliente(DatosDeAccionCliente data) {
        this.data = data;
    }

    public void probarConexion(String host) throws IOException {
        socket = new Socket(host, 23543);
        data.setrHost(host);
        socket.close();
    }

    public void signIn() throws IOException,
            NoSuchAlgorithmException, InvalidKeySpecException, JSONException,
            InvalidKeyException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException {
        socket = new Socket(data.getrHost(), 23543);
        ioHandler = new IOHandler(new DataInputStream(socket.getInputStream()),
                new DataOutputStream(socket.getOutputStream()));
        ioHandler.writeInt(-1);
        System.out.println("We're waiting for the public key...");
        secretKey = null;
        cipher = null;
        chunk = ioHandler.handleIncommingMessage();
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(chunk);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey usrPubKey = keyFactory.generatePublic(pubKeySpec);
        cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, usrPubKey);
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128); // por ejemplo
        secretKey = keyGen.generateKey();
        data.setEncodedKey(secretKey.getEncoded());
        byte[] cipB = cipher.doFinal(data.getEncodedKey());
        ioHandler.sendMessage(cipB);
        System.out.println("We've just sent " + cipB.length + " bytes.");
        JSONObject json = new JSONObject();
        json.put("uName", data.getUsrName());
        json.put("psswd", data.getPsswd());
        cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        chunk = cipher.doFinal(json.toString().getBytes());
        ioHandler.sendMessage(chunk);
        data.setLID(ioHandler.readInt());
        ioHandler.close();
        System.out.println("Me llegó " + data.getLID());
        socket.close();
        if (data.getLID() == -1) {
            throw new IOException("Error en las credenciales");
        }
    }

    public void grabParticipant() throws IOException,
            JSONException, InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException {
        Socket socket = new Socket(data.getrHost(), 23543);
        System.out.println("Sending: " + data.getLID());
        ioHandler = new IOHandler(new DataInputStream(socket.getInputStream()),
                new DataOutputStream(socket.getOutputStream()));
        ioHandler.writeInt(data.getLID());
        JSONObject json = new JSONObject();
        json.put("action", 1);
        secretKey = new SecretKeySpec(data.getEncodedKey(), "AES");
        cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        chunk = cipher.doFinal(json.toString().getBytes());
        ioHandler.sendMessage(chunk);
        // The Participante breaks!!
        chunk = ioHandler.handleIncommingMessage();
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        JSONObject participanteInfo = new JSONObject(new String(
                cipher.doFinal(chunk)));
        data.setHostParticipante(participanteInfo.getString("nHost"));
        partKey = new SecretKeySpec(
                Hasher.hexStringToByteArray(participanteInfo.getString("key")),
                "AES");
        data.setEncodedPartKey(partKey.getEncoded());
        System.out.println(participanteInfo);
        ioHandler.close();
        socket.close();
    }

    public void postMe() throws IOException,
            JSONException, InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException {
        Socket socket = new Socket(data.getrHost(), 23543);
        ioHandler = new IOHandler(new DataInputStream(socket.getInputStream()),
                new DataOutputStream(socket.getOutputStream()));
        System.out.println("Sending: " + data.getLID());
        ioHandler.writeInt(data.getLID());
        JSONObject json = new JSONObject();
        json.put("action", 1);
        secretKey = new SecretKeySpec(data.getEncodedKey(), "AES");
        cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        chunk = cipher.doFinal(json.toString().getBytes());
        ioHandler.sendMessage(chunk);
        ioHandler.close();
        socket.close();
    }

    public Map<String, String> consultaBoleta(String boleta) throws NegativeArraySizeException, NoSuchPaddingException, NoSuchAlgorithmException {
        Map<String, String> result = new TreeMap<>();
        try {
            socket = new Socket(data.getrHost(), 23543);
            ioHandler = new IOHandler(new DataInputStream(
                    socket.getInputStream()), new DataOutputStream(
                            socket.getOutputStream()));
            ioHandler.writeInt(data.getLID());
            JSONObject json = new JSONObject();
            json.put("action", 2);
            json.put("boleta", boleta);
            secretKey = new SecretKeySpec(data.getEncodedKey(), "AES");
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            chunk = cipher.doFinal(json.toString().getBytes());
            ioHandler.sendMessage(chunk);
            chunk = ioHandler.handleIncommingMessage();
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            String recv = new String(cipher.doFinal(chunk));
            json = new JSONObject(recv);
            int veredicto = json.getInt("veredicto");
            System.out.println("Comprobando existencia de " + boleta + ": "
                    + (veredicto != 0 ? "existe" : "no existe"));
            ioHandler.close();
            socket.close();
            result.put("veredicto", String.valueOf(veredicto));
            result.put("perfil", json.getString("perfil"));
        } catch (IOException | JSONException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return result;
    }

    public int conectaParticipante(String boleta, String perfil)
            throws IOException {
        int tul = -1;
        try {
            JSONObject json = new JSONObject();
            json.put("content", 1);
            json.put("boleta", boleta);
            json.put("perfil", perfil);
            do {
                socket = new Socket(data.getHostParticipante(), 5003);
                ioHandler = new IOHandler(new DataInputStream(
                        socket.getInputStream()), new DataOutputStream(
                                socket.getOutputStream()));
                LogProvider.logMessage("ConnParticipante", Arrays.toString(data.getEncodedPartKey()));
                partKey = new SecretKeySpec(data.getEncodedPartKey(), "AES");
                cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, partKey);
                chunk = cipher.doFinal(json.toString().getBytes());
                ioHandler.sendMessage(chunk);
                tul = ioHandler.readInt();
                ioHandler.close();
                socket.close();
                switch (tul) {
                    case 74:
                        json.put("content", 3);
                        LogProvider.logMessage("JEYSON", "Reconectando... =D");
                        synchronized (this) {
                            try {
                                wait(3000);
                            } catch (InterruptedException i) {
                                i.printStackTrace();
                            }
                        }
                        break;
                    case 24:
                        System.out.println("El participante ya ha votado.");
                        break;
                    case 48:
                        System.out.println("El participante no está registrado.");
                        break;
                    default:
                        throw new IOException();
                }
            } while (tul == 74);
        } catch (JSONException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        return tul;
    }

    public int conectaParticipante()
            throws IOException {
        int tul = -1;
        try {
            socket = new Socket(data.getHostParticipante(), 5003);
            JSONObject json = new JSONObject();
            json.put("content", 2);
            ioHandler = new IOHandler(new DataInputStream(
                    socket.getInputStream()), new DataOutputStream(
                            socket.getOutputStream()));
            LogProvider.logMessage("ConectaConParticipante", Arrays.toString(data.getEncodedPartKey()));
            partKey = new SecretKeySpec(data.getEncodedPartKey(), "AES");
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, partKey);
            chunk = cipher.doFinal(json.toString().getBytes());
            ioHandler.sendMessage(chunk);
            ioHandler.close();
            socket.close();
        } catch (JSONException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        return tul;
    }

    public String consultaParametrosIniciales() {
        String resp = null;
        try {
            socket = new Socket(data.getrHost(), 23543);
            ioHandler = new IOHandler(new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
            ioHandler.writeInt(data.getLID());
            JSONObject json = new JSONObject();
            json.put("action", 15);
            secretKey = new SecretKeySpec(data.getEncodedKey(), "AES");
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            chunk = cipher.doFinal(json.toString().getBytes());
            ioHandler.sendMessage(chunk);
            chunk = ioHandler.handleIncommingMessage();
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            resp = new String(cipher.doFinal(chunk));
            LogProvider.logMessage("Chainal", "resp: " + resp);
            ioHandler.close();
            socket.close();
        } catch (IOException | JSONException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        return resp;
    }

    public void consultaPerfiles() {
        try {
            socket = new Socket(data.getrHost(), 23543);
            ioHandler = new IOHandler(new DataInputStream(
                    socket.getInputStream()), new DataOutputStream(
                            socket.getOutputStream()));
            ioHandler.writeInt(data.getLID());
            JSONObject json = new JSONObject();
            json.put("action", 10);
            secretKey = new SecretKeySpec(data.getEncodedKey(), "AES");
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            chunk = cipher.doFinal(json.toString().getBytes());
            ioHandler.sendMessage(chunk);
            chunk = ioHandler.handleIncommingMessage();
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            JSONArray jsonArray = new JSONArray(new String(cipher.doFinal(chunk)));
            String[] perfsArr = new String[jsonArray.length()];
            for (int i = 0; i < perfsArr.length; i++) {
                perfsArr[i] = jsonArray.getString(i);
            }
            System.out.println(jsonArray.toString());
            data.setPerfiles(perfsArr);
            ioHandler.close();
        } catch (IOException | JSONException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    public JSONArray consultaPreguntasParticipante(String boleta) {
        JSONArray response = null;
        try {
            socket = new Socket(data.getrHost(), 23543);
            ioHandler = new IOHandler(new DataInputStream(
                    socket.getInputStream()), new DataOutputStream(
                            socket.getOutputStream()));
            ioHandler.writeInt(data.getLID());
            JSONObject json = new JSONObject();
            json.put("action", 11);
            json.put("boleta", boleta);
            secretKey = new SecretKeySpec(data.getEncodedKey(), "AES");
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            chunk = cipher.doFinal(json.toString().getBytes());
            ioHandler.sendMessage(chunk);
            chunk = ioHandler.handleIncommingMessage();
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            JSONArray preguntasParticipante = new JSONArray(new String(
                    cipher.doFinal(chunk)));
            System.out.println("Preguntas para participante: "
                    + preguntasParticipante);
            ioHandler.close();
            socket.close();
            response = preguntasParticipante;
        } catch (IOException | JSONException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        return response;
    }

    public JSONArray consultaOpcionesPreguntaParticipante(String pregunta) {
        JSONArray resp = null;
        try {
            socket = new Socket(data.getrHost(), 23543);
            ioHandler = new IOHandler(new DataInputStream(
                    socket.getInputStream()), new DataOutputStream(
                            socket.getOutputStream()));
            ioHandler.writeInt(data.getLID());
            JSONObject json = new JSONObject();
            json.put("action", 13);
            json.put("pregunta", pregunta);
            secretKey = new SecretKeySpec(data.getEncodedKey(), "AES");
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            chunk = cipher.doFinal(json.toString().getBytes());
            ioHandler.sendMessage(chunk);
            chunk = ioHandler.handleIncommingMessage();
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            resp = new JSONArray(new String(cipher.doFinal(chunk)));
            ioHandler.close();
            socket.close();
        } catch (IOException | JSONException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        return resp;
    }

    public int enviaVotoParticipante(String pregunta, String opcion,
            String perfil) {
        int veredicto = 0;
        try {
            socket = new Socket(data.getrHost(), 23543);
            ioHandler = new IOHandler(new DataInputStream(
                    socket.getInputStream()), new DataOutputStream(
                            socket.getOutputStream()));
            ioHandler.writeInt(data.getLID());
            JSONObject json = new JSONObject();
            json.put("action", 3);
            Random r = new Random();
            json.put(
                    "idVoto",
                    new Hasher().makeHashString(new Date().toString() + "..."
                            + r.nextInt() + opcion)); // La cajita trabaja con
            // sal
            json.put("pregunta", pregunta);
            json.put("perfil", perfil);
            json.put("voto", opcion);
            secretKey = new SecretKeySpec(data.getEncodedKey(), "AES");
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            chunk = cipher.doFinal(json.toString().getBytes());
            ioHandler.sendMessage(chunk);
            chunk = ioHandler.handleIncommingMessage();
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            System.out.println("Lo que mandamos fue:\n" + json.toString());
            veredicto = Integer.parseInt(new String(cipher.doFinal(chunk)));
            System.out.println("El veredicto es: " + veredicto);
            ioHandler.close();
            socket.close();
        } catch (IOException | JSONException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        return veredicto;
    }

    public void enviaParticipanteContestoPregunta(String boleta, String pregunta) {
        try {
            socket = new Socket(data.getrHost(), 23543);
            ioHandler = new IOHandler(new DataInputStream(
                    socket.getInputStream()), new DataOutputStream(
                            socket.getOutputStream()));
            ioHandler.writeInt(data.getLID());
            JSONObject json = new JSONObject();
            json.put("action", 12);
            json.put("boleta", boleta);
            json.put("pregunta", pregunta);
            secretKey = new SecretKeySpec(data.getEncodedKey(), "AES");
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            chunk = cipher.doFinal(json.toString().getBytes());
            ioHandler.sendMessage(chunk);
            ioHandler.close();
            socket.close();
        } catch (IOException | JSONException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    public String pideTituloDeVotacion() {
        String titulo = "";
        try {
            socket = new Socket(data.getrHost(), 23543);
            ioHandler = new IOHandler(new DataInputStream(
                    socket.getInputStream()), new DataOutputStream(
                            socket.getOutputStream()));
            ioHandler.writeInt(data.getLID());
            JSONObject json = new JSONObject();
            json.put("action", 5);
            secretKey = new SecretKeySpec(data.getEncodedKey(), "AES");
            cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            chunk = cipher.doFinal(json.toString().getBytes());
            ioHandler.sendMessage(chunk);
            chunk = ioHandler.handleIncommingMessage();
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            titulo = new String(cipher.doFinal(chunk));
            ioHandler.close();
            socket.close();
        } catch (IOException | JSONException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        return titulo;
    }

    public byte[] decrypt(byte[] bytes) throws InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException {
        secretKey = new SecretKeySpec(data.getEncodedKey(), "AES");
        LogProvider.logMessage("Decrypt", Arrays.toString(secretKey.getEncoded()));
        cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(bytes);
    }

    public String getHost() {
        return data.getrHost();
    }

    public void setHost(String host) {
        data.setrHost(host);
    }

    public void setPsswd(String psswd) {
        data.setPsswd(psswd);
    }

    public void setUsrName(String usrName) {
        data.setUsrName(usrName);
    }

    public DatosDeAccionCliente getData() {
        return data;
    }

    public void setData(DatosDeAccionCliente data) {
        this.data = data;
    }

    public String[] getPerfilesArray() throws JSONException {
        return data.getPerfiles();
    }
    
    public void close(){
        try{
            socket.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
