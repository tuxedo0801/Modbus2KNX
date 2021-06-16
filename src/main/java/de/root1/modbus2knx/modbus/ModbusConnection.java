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
package de.root1.modbus2knx.modbus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import static java.lang.Thread.interrupted;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class ModbusConnection {

    private static final Logger log = LoggerFactory.getLogger(ModbusConnection.class);

    private Socket s;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isConnected = false;
    private final Timer timer = new Timer("ReconnectTimer", true);

    private long lastSend;

    private final String host;
    private final int port;
    /**
     * Expected properties: functioncode.digital.read-coils
     * functioncode.digital.read-discrete-inputs
     * functioncode.analog.read-holding-register
     * functioncode.analog.read-input-register
     * functioncode.digital.write-single-coil
     * functioncode.analog.write-single-register
     */
    private final Properties configdata;
    private final byte modbusSlaveAddress;
    private final int soTimeout;

    final List<ModbusResponse> responses = new ArrayList<>();

    public ModbusConnection(String host, int port, int soTimeout, byte modbusSlaveAddress, Properties configdata) throws IOException {
        this.host = host;
        this.port = port;
        this.soTimeout = soTimeout;
        this.modbusSlaveAddress = modbusSlaveAddress;
        this.configdata = configdata;

        connect();

    }

    public List<ModbusResponse> getResponses() {
        return responses;
    }
//        
//    private byte[] readCrc() throws IOException {
//        byte[] crcResult = new byte[2];
//
//        crcResult[0] = (byte) (inputStream.read() & 0xff);
//        crcResult[1] = (byte) (inputStream.read() & 0xff);
//        return crcResult;
//    }

//    private byte[] crc16(byte[] bytes) {
//        byte[] crcBytes = new byte[2];
//
//        int Poly16 = 0xA001;
//        int crc = 0xFFFF;
//
//        for (int i = 0; i < bytes.length; i++) {
//            byte data = bytes[i];
//            int j;
//            boolean LSB;
//            crc = ((crc ^ data) | 0xFF00) & (crc | 0x00FF);
//            for (j = 0; j < 8; j++) {
//                LSB = (crc & 0x0001) == 1;
//                crc = crc / 2;
//                if (LSB) {
//                    crc = crc ^ Poly16;
//                }
//            }
//        }
//
//        crcBytes[0] = (byte) (crc & 0x00FF);
//        crcBytes[1] = (byte) ((crc & 0xFF00) / 256);
//        return crcBytes;
//    }
    public synchronized void disconnect() {
        log.info("Closing connection ...");
        isConnected = false;
        try {
            inputStream.close();
            outputStream.close();
            s.close();
        } catch (Exception e) {
            // do nothing
            e.printStackTrace();
        }
    }

    private void connect() throws IOException {

        s = new Socket(host, port);
        s.setSoTimeout(soTimeout);
        log.info("New Timeout: {}", s.getSoTimeout());

        inputStream = s.getInputStream();
        outputStream = s.getOutputStream();
        log.info("Connected to {}:{}!", host, port);
        isConnected = true;

        Thread t = new Thread("Modbus Response Read Thread"){
            @Override
            public void run() {
                while (!interrupted()) {
                    try {
                        super.run(); //To change body of generated methods, choose Tools | Templates.

                        ModbusResponse modbusResponse = new ModbusResponse(inputStream);
                        synchronized (responses) {
                            if (modbusResponse.crcCheck()) {
                                responses.add(modbusResponse);
                                responses.notifyAll();
                            } else {
                                System.out.println("Failed response: " + modbusResponse);
                            }
                        }
                    } catch (IOException ex) {
                        java.util.logging.Logger.getLogger(ModbusConnection.class.getName()).log(Level.SEVERE, null, ex);
                        interrupt();
                    }
                }
            }

        };
        t.start();
    }

    public synchronized double readFloat16bit(int addr, int numberOfInputs) throws ModbusException {

        log.debug("Read ModBus float16 @ " + addr);

        byte function = (byte) Integer.parseInt(configdata.getProperty("functioncode.analog.read-holding-register", "3"));

        ModbusRequest req = new ModbusRequest(this, modbusSlaveAddress, function, addr, numberOfInputs);
        try {
            req.send(outputStream);
        } catch (IOException ex) {
            throw new ModbusException("Error while sending request", ex);
        }
        ModbusResponse resp = req.getResponse();
        if (resp.crcCheck()) {
            float v = resp.getFloat16();
            return v;
        } else {
            return -1;
        }

    }

    public synchronized int readUnsigned16bit(int address, int numberOfInputs) throws ModbusException {
        log.debug("Read ModBus uint16 @ " + address);
        byte function = (byte) Integer.parseInt(configdata.getProperty("functioncode.analog.read-holding-register", "3"));
        ModbusRequest req = new ModbusRequest(this, modbusSlaveAddress, function, address, numberOfInputs);
        try {
            req.send(outputStream);
        } catch (IOException ex) {
            throw new ModbusException("Error while sending request", ex);
        }
        ModbusResponse resp = req.getResponse();

        if (resp.crcCheck()) {
            return resp.getUint16();
        } else {
            return -1;
        }

    }

    public synchronized boolean readBoolean(int address, int numberOfInputs) throws ModbusException {

        log.debug("Read ModBus Boolean @ " + address);

        byte function = (byte) Integer.parseInt(configdata.getProperty("functioncode.digital.read-coils", "1"));
        ModbusRequest req = new ModbusRequest(this, modbusSlaveAddress, function, address, numberOfInputs);
        try {
            req.send(outputStream);
        } catch (IOException ex) {
            throw new ModbusException("Error while sending request", ex);
        }
        ModbusResponse resp = req.getResponse();
        if (resp.crcCheck()) {
            return resp.getBoolean();
        } else {
            return false;
        }

    }

}
