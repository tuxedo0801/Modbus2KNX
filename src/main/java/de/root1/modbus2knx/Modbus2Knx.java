/*
 * Copyright (C) 2015 Alexander Christian <alex(at)root1.de>. All rights reserved.
 * 
 * This file is part of Modbus2KNX.
 *
 *   Modbus2KNX is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Modbus2KNX is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with KarduinoConfig.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.root1.modbus2knx;

import de.root1.modbus2knx.modbus.ModbusConnection;
import de.root1.modbus2knx.modbus.ModbusException;
import de.root1.slicknx.GroupAddressEvent;
import de.root1.slicknx.GroupAddressListener;
import de.root1.slicknx.Knx;
import de.root1.slicknx.KnxException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author root
 */
public final class Modbus2Knx {

    private static final Logger log = LoggerFactory.getLogger(Modbus2Knx.class);

    private Knx knx;

    ModbusConnection modbus;
    Properties configdataProperties;

//    byte modbusSlaveAddress = 0x01;

    private final String host;
    private final int port;

    private String knxpa;

    private static int parseInt(String s) {
        if (s.startsWith("0x")) {
            return Integer.parseInt(s.substring(2), 16);
        } else {
            return Integer.parseInt(s);
        }
    }

    /**
     * gets index of the n's occurence of c in str
     * @param str
     * @param c
     * @param n
     * @return 
     */
    public static int nthOccurrence(String str, String c, int n) {
        n--;
        int pos = str.indexOf(c, 0);
        while (n-- > 0 && pos != -1) {
            pos = str.indexOf(c, pos + 1);
        }
        return pos;
    }
    private final int soTimeout;

    public Modbus2Knx(String[] filenames) throws IOException, InterruptedException, KnxException {

        List<Datapoint> dpts = new ArrayList<>();
        
        configdataProperties = new Properties();

        for (String file : filenames) {
            log.info("Reading file into properties: {}", file);
            File f = new File(file);
            configdataProperties.load(new FileInputStream(f));
        }
        

        host = configdataProperties.getProperty("modbus.tcp.host", "localhost");
        port = Integer.parseInt(configdataProperties.getProperty("modbus.tcp.port", "8899"));
//        modbusSlaveAddress = (byte) (parseInt(configdataProperties.getProperty("modbus.slaveaddress", "1")) & 0xFF);
        knxpa = configdataProperties.getProperty("knx.individualadress", "1.1.1");
        soTimeout = Integer.parseInt(configdataProperties.getProperty("modbus.tcp.sockettimeout", "0"));

        log.info("Settings: knx.ia={} host={} port={}", knxpa, host, port);

        Iterator<Object> iterator = configdataProperties.keySet().iterator();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            String value = (String) configdataProperties.get(key);
            key = key.trim();
            value = value.trim();

            if (key.startsWith("functioncode")) {
                // skip here
            } else if (key.startsWith("knx")) {
                // skip here
            } else if (key.startsWith("modbus")) {
                // skip here
            } else if (isReg(key)){

                log.info("Found reg: {}", key);
                Datapoint datapoint = new Datapoint();

                // default if non is set afterwards
                datapoint.setFunction(parseInt(configdataProperties.getProperty("functioncode.analog.read-holding-register", "0x03")));
                datapoint.setModbusSlaveAddress(getModbusSlaveAddress(key));

                String[] split = key.split("\\.");
                
                String group = split[0];
                String name = split[2];
                String split3 = split[3];
                
                datapoint.setGroup(group);
                datapoint.setName(name);
                boolean alreadyThere = false;

                if (dpts.contains(datapoint)) {
                    log.info("DPT already known: {}", datapoint);
                    int i = dpts.indexOf(datapoint);
                    datapoint = dpts.get(i);
                    alreadyThere = true;
                } 

                if (split.length >= 3) {

                    switch (split3) {
                        case "address":
                            try {
                            datapoint.setAddress(parseInt(value));
                        } catch (NumberFormatException ex) {
                            log.warn("Not able to parse address: '{}' -> '{}'", key, value);
                        }
                        break;
                        case "function":
                            try {
                                datapoint.setFunction(parseInt(configdataProperties.getProperty("functioncode."+value)));
                            } catch (NumberFormatException ex) {
                                log.warn("Not able to parse function: '{}' -> '{}'", key, value);
                            }
                            break;
                        case "type":
                            datapoint.setType(Type.valueOf(value));
                            log.info("Setting type: " + datapoint);
                            break;
                        case "numberofpoints":
                            datapoint.setNumberOfPoints(parseInt(value));
                            log.info("Setting number of points: " + datapoint);
                            break;
                        case "knx":
                            datapoint.parseKnxDataFromProperties(configdataProperties);
                            break;
                        default:
                            key = key.substring(nthOccurrence(key, ".", 2) + 1);
                            log.info("Adding property: " + key + "=" + value);
                            datapoint.addProperty(key, value);
                            break;

                    }
                    if (!alreadyThere) {
                        dpts.add(datapoint);
                        log.info("Finished reading: {}", datapoint);
                    }

                } else {
                    log.warn("Not supported: '" + key + "'");
                }
            }
        }

        log.info("DPTs found: " + dpts.size());

        for (Datapoint dpt : dpts) {
            log.info("datapoint: " + dpt);
        }

        // -----------------------
        // Modbus Connection 
        // -----------------------
        modbus = new ModbusConnection(host, port, soTimeout, configdataProperties);

        // -----------------------
        // KNX Connection
        // -----------------------
        /**
         * mapping GA to Modbus datapoint
         */
        final Map<String, Datapoint> gaMap = new HashMap<>();

        /**
         * varmap for string concat
         */
        final Map<String, Datapoint> varMap = new HashMap<>();

        final List<WatchContainer> watchlist = new ArrayList<>();
        final List<WatchContainer> watchlistCyclic = new ArrayList<>();

        for (Datapoint dpt : dpts) {
            if (dpt.hasUseableKnxData()) {

                KnxData knxData = dpt.getKnxData();
                List<String> gaList = knxData.getGaList();
                for (String ga : gaList) {
                    log.info("Registering for GA: " + ga);
                    Datapoint put = gaMap.put(ga, dpt);
                    if (put != null) {
                        log.warn("Duplicate GA config: " + ga + "!!!");
                    }
                }

                if (knxData.getSendCyclic() == KnxData.INTERVAL_SENDONUPDATE) {
                    watchlist.add(new WatchContainer(modbus, dpt));
                } else if (knxData.getSendCyclic() > 0) {
                    WatchContainer wc = new WatchContainer(modbus, dpt);
                    wc.setCyclicUpdateTime(knxData.getSendCyclic());
                    watchlistCyclic.add(wc);
                }
            }
            varMap.put(dpt.getGroup() + "." + dpt.getName(), dpt);
        }

        knx = new Knx(knxpa);

        Thread sendOnUpdateThread = new Thread("WatchContainer-SendOnUpdate") {

            @Override
            public void run() {
                log.info("Watchcontainer size: {}", watchlist.size());
                if (watchlist.isEmpty()) return;
                
                while (true) {
                    for (WatchContainer c : watchlist) {

                        try {
                            if (c.hasChanged()) {
                                Object value = c.getValue();
                                List<String> gaList = c.getDatapoint().getKnxData().getGaList();
                                for (String ga : gaList) {
                                    log.info("{}.{}: Sending haschanged update value {} to {}", c.getDatapoint().getGroup(), c.getDatapoint().getName(), value, ga);
                                    
                                    String dpt = c.getDatapoint().getKnxData().getDpt();
                                    if (!dpt.contains(".")) {
                                        dpt += ".000";
                                    }
                                    
//                                    switch (c.getDatapoint().getType()) {
//                                        case bool:
//                                            knx.writeBoolean(false, ga, (Boolean) value);
//                                            break;
//                                        case float16bit:
//                                            knx.write2ByteFloat(false, ga, ((Double) value).floatValue());
//                                            break;
//                                        case float32bit:
//                                            knx.write4ByteFloat(false, ga, ((Double) value).floatValue());
//                                            break;                                            
//                                        case unsigned16bit:
//                                            knx.writeDpt7(false, ga, (int) value);
//                                            break;
//
//                                    }
                                    switch (dpt) {
                                        case "1.000":
                                        case "1.001":
                                             knx.writeBoolean(false, ga, (Boolean) value);
                                            break;
                                        case "9.000":
                                        case "9.001":
                                            knx.write2ByteFloat(false, ga, ((Double) value).floatValue());
                                            break;
                                        case "7.007":
                                            knx.writeDpt7(false, ga, (int) value);
                                            break;
                                        case "13.010":
                                            knx.writeDpt13(false, ga, ((Double)value).intValue());
                                        case "16.001":
                                            break;
                                    
                                }
                                    // sleep after each knx send
                                    try {
                                        sleep(150);
                                    } catch (InterruptedException ex) {
                                        /* do nothing */ }

                                } // end of for ga
                            } // end of if hasChanged 

                        } catch (KnxException ex) {
                            log.warn("Unable to call hasChanged()", ex);
                        } catch (ModbusException ex) {
                            log.warn("Unable to call hasChanged()", ex);
                            log.info("Reconnecting...");
                            modbus.disconnect();
                            try {
                                modbus.connect();
                                log.info("Reconnecting...*done*");
                            } catch (IOException ex1) {
                                log.error("Unable to reconnect modbus", ex1);
                            }
                        }
                        try {
                            sleep(5);
                        } catch (InterruptedException ex) {
                            /* do nothing */ }

                    } // end of for watch container
                    // sleep between loop
                    try {
                        log.debug("#### Sleep until next round ...");
                        sleep(1000);
                    } catch (InterruptedException ex) {
                        /* do nothing */ }
                }
            }

        };
        sendOnUpdateThread.setDaemon(true);
        sendOnUpdateThread.start();

        Thread sendCyclicThread = new Thread("WatchContainer-SendCyclic") {

            @Override
            public void run() {

                log.info("Cyclic Watchcontainer size: {}", watchlistCyclic.size());
                if (watchlistCyclic.isEmpty()) return;
                
                while (true) {
                    for (WatchContainer c : watchlistCyclic) {

                        try {
                            if (c.checkCyclicUpdate()) {
                                Object value = c.getValue();
                                List<String> gaList = c.getDatapoint().getKnxData().getGaList();
                                for (String ga : gaList) {
                                    log.info("{}.{}: Sending cyclicupdate value {} to {}", c.getDatapoint().getGroup(), c.getDatapoint().getName(), value, ga);
                                    switch (c.getDatapoint().getType()) {
                                        case bool:
                                            knx.writeBoolean(false, ga, (Boolean) value);
                                            break;
                                        case float16bit:
                                            knx.write2ByteFloat(false, ga, ((Double) value).floatValue());
                                            break;
                                        case unsigned16bit:
                                            knx.writeDpt7(false, ga, (int) value);
                                            break;

                                    }
                                    // sleep after each knx send
                                    try {
                                        sleep(150);
                                    } catch (InterruptedException ex) {
                                        /* do nothing */ }

                                } // end of for ga
                            } // end of if hasChanged 

                        } catch (KnxException ex) {
                            log.warn("Unable to call hasChanged()", ex);
                        } catch (ModbusException ex) {
                            log.warn("Unable to call hasChanged()", ex);
                        }
                        try {
                            sleep(5);
                        } catch (InterruptedException ex) {
                            /* do nothing */ }

                    } // end of for watch container
                    // sleep between loop
                    try {
                        log.debug("#### Sleep until next round ...");
                        sleep(100);
                    } catch (InterruptedException ex) {
                        /* do nothing */ }
                }
            }

        };
        sendCyclicThread.setDaemon(true);
        sendCyclicThread.start();

        knx.setGlobalGroupAddressListener(new GroupAddressListener() {

            @Override
            public void readRequest(GroupAddressEvent event) {
                Datapoint datapoint = gaMap.get(event.getDestination());
                log.info("groupReadRequest: {} -> {}", event.getSource(), event.getDestination());

                if (datapoint != null) {

                    log.info("Answering KNX request for {} from {}", event.getDestination(), event.getSource());

                    try {

                        switch (datapoint.getType()) {

                            case float16bit:
                                log.info("Reading modbus for float16..");
                                double valueF = modbus.readFloat16bit(datapoint.getModbusSlaveAddress(), datapoint.getAddress(), datapoint.getFunction(), datapoint.getNumberOfPoints());
                                log.info("Reading modbus... *done*");
                                log.info("{}.{}: {}", datapoint.getGroup(), datapoint.getName(), valueF);
                                log.info("Writing float to knx ...");
                                knx.write2ByteFloat(true, event.getDestination(), (float) valueF);

                                log.info("KNX written float");
                                break;

                            case unsigned16bit:
                                int valueI = modbus.readUnsigned16bit(datapoint.getModbusSlaveAddress(), datapoint.getAddress(), datapoint.getFunction(), datapoint.getNumberOfPoints());
                                log.info("{}.{}: {}", datapoint.getGroup(), datapoint.getName(), valueI);

                                knx.writeDpt7(true, event.getDestination(), valueI);
                                log.info("KNX written uint16");
                                break;

                            case bool:
                                boolean valueB = modbus.readBoolean(datapoint.getModbusSlaveAddress(), datapoint.getAddress(), datapoint.getFunction(), datapoint.getNumberOfPoints());
                                log.info(datapoint.getGroup() + "." + datapoint.getName() + ": " + valueB);
                                knx.writeBoolean(true, event.getDestination(), valueB);
                                log.info("KNX written boolean");
                                break;

                        }

                    } catch (ModbusException ex) {
                        ex.printStackTrace();
                    } catch (KnxException ex) {
                        log.error("Error while answering knx request", ex);
                    }

                }
            }

            @Override
            public void readResponse(GroupAddressEvent event) {
            }

            @Override
            public void write(GroupAddressEvent event) {
            }
        });

        // forever-loop
        int i = 1;
        while (i != 0) {
            Thread.sleep(1000);
        }
        modbus.disconnect();

    }
    
    private static boolean isReg(String key) {
        
        int from = nthOccurrence(key, ".", 1);
        int to = nthOccurrence(key, ".", 2);
        System.out.println(key.substring(from+1, to));
        if (key.substring(from+1, to).equalsIgnoreCase("reg"))
            return true;
        
        return false;
    }
    
    private byte getModbusSlaveAddress(String key) {
        int to = nthOccurrence(key, ".", 1);
        String name = key.substring(0, to);
        System.out.println(name);
        String newKey = name+".modbus.slaveaddress";
        byte slaveAddr = Byte.parseByte(configdataProperties.getProperty(newKey, "0x01"));
        return slaveAddr;
    }



    public static void main(String[] args) throws IOException, InterruptedException, KnxException {

        new Modbus2Knx(args);

    }

}
