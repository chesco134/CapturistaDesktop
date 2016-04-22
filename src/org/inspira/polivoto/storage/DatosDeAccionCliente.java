package shared;

import java.io.Serializable;

/**
 * Created by jcapiz on 13/01/16.
 */
public class DatosDeAccionCliente implements Serializable {

    private String[] perfiles;
    private String rHost;
    private String usrName;
    private String psswd;
    private byte[] encodedKey;
    private int LID;
    private byte[] encodedPartKey;
    private String hostParticipante;

    public byte[] getEncodedKey() {
        return encodedKey;
    }

    public void setEncodedKey(byte[] encodedKey) {
        this.encodedKey = encodedKey;
    }

    public int getLID() {
        return LID;
    }

    public void setLID(int LID) {
        this.LID = LID;
    }

    public String getrHost() {
        return rHost;
    }

    public void setrHost(String rHost) {
        this.rHost = rHost;
    }

    public String getUsrName() {
        return usrName;
    }

    public void setUsrName(String usrName) {
        this.usrName = usrName;
    }

    public String getPsswd() {
        return psswd;
    }

    public void setPsswd(String psswd) {
        this.psswd = psswd;
    }

    public String[] getPerfiles() {
        return perfiles;
    }

    public void setPerfiles(String[] perfiles) {
        this.perfiles = perfiles;
    }

    public void setEncodedPartKey(byte[] encodedPartKey) {
        this.encodedPartKey = encodedPartKey;
    }

    public byte[] getEncodedPartKey() {
        return encodedPartKey;
    }

    public void setHostParticipante(String hostParticipante) {
        this.hostParticipante = hostParticipante;
    }

    public String getHostParticipante() {
        return hostParticipante;
    }
}
