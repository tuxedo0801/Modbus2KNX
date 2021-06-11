/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.modbus2knx;

import de.root1.modbus2knx.modbus.ModbusConnection;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author achristian
 */
public class DumpRegister {
    
//    static {
//        String property = System.getProperty("java.util.logging.config.file");
//        File logconfig = new File("./logging.properties");
//        if (property == null && logconfig.isFile() && logconfig.exists()) {
//            System.out.println("Use automatic log config based on " + logconfig.getAbsolutePath());
//            System.setProperty("java.util.logging.config.file", logconfig.getAbsolutePath());
//            try {
//                FileInputStream is = new FileInputStream(logconfig);
//                LogManager.getLogManager().readConfiguration(is);
//            } catch (Exception ex) {
//                java.util.logging.Logger.getLogger(Modbus2Knx.class.getName()).log(Level.SEVERE, null, ex);
//            }
//
//        }
//    }

    public DumpRegister() throws IOException {
        
        ModbusConnection mc = new ModbusConnection("192.168.200.4", 4002, 0, (byte)0x01, new Properties());
        
        Map<Integer, Boolean> digitalMap = new HashMap<>();
        Map<Integer, Double> analogMap = new HashMap<>();
        Map<Integer, Integer> integerMap = new HashMap<>();
        
        mc.writeBoolean(161, false);
//        
        System.out.println("---------------------------------");
        System.out.println("Reading dump ....");
        System.out.println("---------------------------------");
        
        // digital --> boolean
        System.out.println("Digital Variables:");
        for (int i=0; i<200;i++) {
            boolean readBoolean = mc.readBoolean(i);
            digitalMap.put(i, readBoolean);
            System.out.print(i+"="+(readBoolean?"1":"0")+"\t");
            if ((i+1)%20==0) {
                System.out.println("");
            }
        }
        
        DecimalFormat dfAnalog = new DecimalFormat("0000.0");
        
        System.out.println("");
        System.out.println("Analog Variables:");
        // analog --> float 16 bit
        for (int i=0; i<255;i++) {
            double readFloat16bit = mc.readFloat16bit(i);
            analogMap.put(i, readFloat16bit);
            System.out.print(i+"="+dfAnalog.format(readFloat16bit)+"\t");
            if ((i+1)%20==0) {
                System.out.println("");
            }
        }
        System.out.println("");
        
        DecimalFormat dfInteger = new DecimalFormat("00000");
        System.out.println("");
        System.out.println("Integer Variables:");                
        // integer --> unsigned 16 bit
        for (int i=0; i<255;i++) {
            int readUnsigned16bit = mc.readUnsigned16bit(i);
            integerMap.put(i, readUnsigned16bit);
            System.out.print(i+"="+dfInteger.format(readUnsigned16bit)+"\t");
            if ((i+1)%20==0) {
                System.out.println("");
            }
        }
        System.out.println("");
        mc.disconnect();
        
        
    }
    
    
    
    public static void main(String[] args) throws IOException {
        new DumpRegister();
    }
    
}
