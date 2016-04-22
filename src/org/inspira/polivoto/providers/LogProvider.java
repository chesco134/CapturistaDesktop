/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.inspira.polivoto.providers;

/**
 *
 * @author jcapiz
 */
public class LogProvider {
    
    public static void logMessage(String label, String message){
        System.out.println(label + ": " + message);
    }
}
