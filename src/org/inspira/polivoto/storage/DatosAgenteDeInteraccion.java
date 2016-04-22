package org.inspira.polivoto.storage;

import org.json.JSONArray;

import java.io.Serializable;

/**
 * Created by jcapiz on 25/02/16.
 */
public class DatosAgenteDeInteraccion implements Serializable{

    private String mainHost;
    private String secondaryHost;
    private JSONArray members; // Array of hosts participating
    private int hbPeriod; // Periodo de heartbit
    private int idVotacion;

    public int getIdVotacion() {
        return idVotacion;
    }

    public void setIdVotacion(int idVotacion) {
        this.idVotacion = idVotacion;
    }

    public String getMainHost() {
        return mainHost;
    }

    public void setMainHost(String mainHost) {
        this.mainHost = mainHost;
    }

    public String getSecondaryHost() {
        return secondaryHost;
    }

    public void setSecondaryHost(String secondaryHost) {
        this.secondaryHost = secondaryHost;
    }

    public JSONArray getMembers() {
        return members;
    }

    public void setMembers(JSONArray members) {
        this.members = members;
    }

    public int getHbPeriod() {
        return hbPeriod;
    }

    public void setHbPeriod(int hbPeriod) {
        this.hbPeriod = hbPeriod;
    }
}
