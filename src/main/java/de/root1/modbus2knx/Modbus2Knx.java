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
import de.root1.slicknx.GroupAddressEvent;
import de.root1.slicknx.GroupAddressListener;
import de.root1.slicknx.Knx;
import de.root1.slicknx.KnxException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
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

    byte modbusSlaveAddress = 0x01;
    
    private final String host;
    private final int port;

    
    private String knxpa;

    public static int nthOccurrence(String str, String c, int n) {
        n--;
        int pos = str.indexOf(c, 0);
        while (n-- > 0 && pos != -1) {
            pos = str.indexOf(c, pos + 1);
        }
        return pos;
    }
    private final int soTimeout;

    public Modbus2Knx(String file) throws IOException, InterruptedException, KnxException {

        List<Datapoint> dpts = new ArrayList<>();

        File f = new File(file);
        configdataProperties = new Properties();
        configdataProperties.load(new FileInputStream(f));
        
        host = configdataProperties.getProperty("modbus.tcp.host", "localhost");
        port = Integer.parseInt(configdataProperties.getProperty("modbus.tcp.port", "8899"));
        modbusSlaveAddress = (byte)(Integer.parseInt(configdataProperties.getProperty("modbus.slaveaddress", "1"))&0xFF);
        knxpa = configdataProperties.getProperty("knx.individualadress", "1.1.1");
        soTimeout = Integer.parseInt(configdataProperties.getProperty("modbus.tcp.sockettimeout", "0"));
        
        log.info("Settings: knx.ia={} host={} port={} modbusSlaveAddr={}", knxpa, host, port, modbusSlaveAddress);

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
            }else {

                Datapoint dpt = new Datapoint();

                String[] split = key.split("\\.");
                dpt.setGroup(split[0]);
                dpt.setName(split[1]);
                boolean alreadyThere = false;

                if (dpts.contains(dpt)) {
                    log.debug("DPT already known: {}", dpt);
                    int i = dpts.indexOf(dpt);
                    dpt = dpts.get(i);
                    alreadyThere = true;
                } else {
                    log.debug("DPT unknown till now: {}", dpt);
                }

                if (split.length >= 3) {

                    switch (split[2]) {
                        case "address":
                            try {
                                dpt.setAddress(Integer.parseInt(value));
                            } catch (NumberFormatException ex) {
                                log.warn("Not able to parse address: '{}' -> '{}'", key, value);
                            }
                            break;
                        case "type":
                            dpt.setType(Type.valueOf(value));
                            log.debug("Setting type: " + dpt);
                            break;
                        case "readwrite":
                            dpt.setReadWrite(ReadWrite.valueOf(value));
                            break;
                        case "knx":
                            dpt.parseKnxDataFromProperties(configdataProperties);
                            break;
                        default:
                            key = key.substring(nthOccurrence(key, ".", 2) + 1);
                            log.debug("Adding property: " + key + "=" + value);
                            dpt.addProperty(key, value);
                            break;

                    }
                    if (!alreadyThere) {
                        dpts.add(dpt);
                    }

                } else {
                    log.warn("Not supported: '" + key + "'");
                }

            }
        }

        log.debug("DPTs found: " + dpts.size());

        for (Datapoint dpt : dpts) {
            log.debug("dpt: " + dpt);
        }

        // -----------------------
        // Modbus Connection 
        // -----------------------
        modbus = new ModbusConnection(host, port, soTimeout, modbusSlaveAddress, configdataProperties);

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

        for (Datapoint dpt : dpts) {
            if (dpt.hasUseableKnxData()) {

                KnxData knxData = dpt.getKnxData();
                List<String> gaList = knxData.getGaList();
                for (String ga : gaList) {
                    log.debug("Registering for GA: " + ga);
                    Datapoint put = gaMap.put(ga, dpt);
                    if (put != null) {
                        log.warn("Duplicate GA config: " + ga + "!!!");
                    }
                }

                if (knxData.getSendCyclic() == KnxData.INTERVAL_SENDONUPDATE) {
                    watchlist.add(new WatchContainer(modbus, dpt));
                }
            }
            varMap.put(dpt.getGroup() + "." + dpt.getName(), dpt);
        }

        knx = new Knx(knxpa);

        Thread t = new Thread("WatchContainer-Work") {

            @Override
            public void run() {

                while (true) {
                    for (WatchContainer c : watchlist) {

                        try {
                            if (c.hasChanged()) {
                                Object value = c.getValue();
                                List<String> gaList = c.getDatapoint().getKnxData().getGaList();
                                for (String ga : gaList) {
                                    log.debug("{}: Sending value {} to {}", c.getDatapoint().getName(), value, ga);
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
                                        sleep(300);
                                    } catch (InterruptedException ex) { /* do nothing */ }

                                } // end of for ga
                            } // end of if hasChanged 

                        } catch (KnxException ex) {
                            log.warn("Unable to call hasChanged()", ex);
                        } catch (IOException ex) {
                            log.warn("Unable to call hasChanged()", ex);
                        }
                        try {
                            sleep(150);
                        } catch (InterruptedException ex) { /* do nothing */ }

                    } // end of for watch container
                    // sleep between loop
                    try {
                        sleep(1000);
                    } catch (InterruptedException ex) { /* do nothing */ }
                }
            }

        };
        t.setDaemon(true);
        t.start();

        knx.setGlobalGroupAddressListener(new GroupAddressListener() {

            @Override
            public void readRequest(GroupAddressEvent event) {
                Datapoint dpt = gaMap.get(event.getDestination());
                log.debug("groupReadRequest: {} -> {}", event.getSource(), event.getDestination());

                if (dpt != null) {

                    log.info("Answering KNX request for {} from {}", event.getDestination(), event.getSource());

                    try {

                        switch (dpt.getType()) {

                            case float16bit:
                                log.debug("Reading modbus for float16..");
                                double valueF = modbus.readFloat16bit(dpt.getAddress());
                                log.debug("Reading modbus... *done*");
                                log.info("{}.{}: {}", dpt.getGroup(), dpt.getName(), valueF);
                                log.debug("Writing float to knx ...");
                                knx.write2ByteFloat(true, event.getDestination(), (float) valueF);

                                log.debug("KNX written float");
                                break;

                            case unsigned16bit:
                                int valueI = modbus.readUnsigned16bit(dpt.getAddress());
                                log.info("{}.{}: {}", dpt.getGroup(), dpt.getName(), valueI);

                                knx.writeDpt7(true, event.getDestination(), valueI);
                                log.debug("KNX written uint16");
                                break;

                            case bool:
                                boolean valueB = modbus.readBoolean(dpt.getAddress());
                                log.info(dpt.getGroup() + "." + dpt.getName() + ": " + valueB);
                                knx.writeBoolean(true, event.getDestination(), valueB);
                                log.debug("KNX written boolean");
                                break;

                        }

                    } catch (IOException ex) {
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

    static {
        String property = System.getProperty("java.util.logging.config.file");
        File logconfig = new File("logging.properties");
        if (property == null && logconfig.isFile() && logconfig.exists()) {
            System.out.println("Use automatic log config based on " + logconfig.getAbsolutePath());
            System.setProperty("java.util.logging.config.file", logconfig.getAbsolutePath());
            try {
                FileInputStream is = new FileInputStream(logconfig);
                LogManager.getLogManager().readConfiguration(is);
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(Modbus2Knx.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, KnxException {

        new Modbus2Knx(args[0]);

    }

}
