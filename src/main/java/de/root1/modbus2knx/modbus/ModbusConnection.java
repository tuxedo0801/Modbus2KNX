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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
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

    /**
     * Eigene ModBus Adresse als "Absender". Die Response muss die gleiche
     * Adresse aufweisen.
     */
    private byte ownAddress = 1;
    private long lastSend;

    private final String host;
    private final int port;
    /**
     * Expected properties:
     * functioncode.digital.read-coils
     * functioncode.digital.read-discrete-inputs
     * functioncode.analog.read-holding-register
     * functioncode.analog.read-input-register
     * functioncode.digital.write-single-coil
     * functioncode.analog.write-single-register
     */
    private final Properties configdata;

    public ModbusConnection(String host, int port, byte ownownAddress, Properties configdata) throws IOException {
        this.host = host;
        this.port = port;
        this.ownAddress = ownownAddress;
        this.configdata = configdata;
        
        connect();

    }

    /**
     *
     * @param t telegram to send
     * @return return null if response telegram was errornous, response telegram
     * otherwise
     * @throws IOException
     */
    public synchronized ModbusTelegram sendTelegram(ModbusTelegram t) throws IOException {

        byte[] crc;

        byte[] telegram = new byte[1 /* address */ + 1 /* function */ + t.getData().length];

        telegram[0] = (byte) (t.getAddress() & 0xFF);
        telegram[1] = (byte) (t.getFunction() & 0xFF);
        System.arraycopy(t.getData(), 0, telegram, 2, t.getData().length);

        crc = crc16(telegram);
        long waitTimeout=30000;
        long start = System.currentTimeMillis();
        while (!isConnected && ((System.currentTimeMillis()-start)<waitTimeout)) {
            try {
                log.debug("Waiting for connection ...");
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                // do nothing
            }
        }
        if (isConnected) {
            log.debug("Connection is available.");
        } else {
            log.error("Waited too long for connection. Shutdown.");
            System.exit(1);
        }

        log.debug("Sending: {} CRC: {}", Arrays.toString(telegram), Arrays.toString(crc));

        
        if (System.currentTimeMillis() - lastSend < 2) {
            log.debug("Sleep to discharge modbus connection");
            try {
                Thread.sleep(2);
            } catch (InterruptedException ex) {
            }
            log.debug("Sleep done");
        }
        
        outputStream.write(telegram);
        outputStream.write(crc);
        outputStream.flush();
        lastSend = System.currentTimeMillis();

        int soTimeout = s.getSoTimeout();
        s.setSoTimeout(10000);
        
        ModbusTelegram result=null;
        int loop=0;
            while (result==null && loop<5) { // try to get result max. 5 times
                try {
                    log.debug("Waiting for telegram ...");
                    int address = inputStream.read();
                    if (address == -1) {
                        log.debug("Got -1 while reading address. disconnected.");
                        throw new IOException("End of stream reached while reading address");
                    }
                    int function = inputStream.read();
                    if (function == -1) {
                        log.debug("Got -1 while reading function. disconnected.");
                        throw new IOException("End of stream reached while reading function");
                    }
                    int datasize = inputStream.read();
                    if (datasize == -1) {
                        log.debug("Got -1 while reading datasize. disconnected.");
                        throw new IOException("End of stream reached while reading datasize.");
                    }

                    log.debug("Response: ");
                    log.debug("Address: {}", Integer.toHexString(address));
                    log.debug("Function: {}", Integer.toHexString(function));
                    log.debug("datasize: {}", datasize);

                    if (datasize > 0 && datasize < 256) {
                        byte[] data = new byte[datasize];

                        int n = 0;
                        while (n < datasize) {
                            int count = inputStream.read(data, 0 + n, datasize - n);
                            if (count == -1) {
                                throw new IOException("End of stream reached while reading " + datasize + " databytes.");
                            }
                            n += count;
                        }

                        for (int i = 0; i < data.length; i++) {
                            log.debug(" data[{}]: {}", i, String.format("0x%02x", data[i]));
                        }

                        byte[] crcResult = new byte[2];

                        crcResult[0] = (byte) (inputStream.read() & 0xff);
                        crcResult[1] = (byte) (inputStream.read() & 0xff);

                        // crc check
                        byte[] tg = new byte[1 + 1 + 1 + datasize];
                        tg[0] = (byte) (address & 0xFF);
                        tg[1] = (byte) (function & 0xFF);
                        tg[2] = (byte) (datasize & 0xFF);
                        System.arraycopy(data, 0, tg, 3, datasize);

                        byte[] crcCheck = crc16(tg);
                        log.debug("End of telegram. crc ={}", Arrays.toString(crc));

                        boolean crcOkay = Arrays.equals(crcResult, crcCheck);

                        if (address == ownAddress && crcOkay) {
                            log.debug("Finished reading response...CRC okay? -> {}", crcOkay);
                            result = new ModbusTelegram(address, function, data);
                        } else if (!crcOkay) {
                            log.warn("CRC Error: crc read: {} crc calculated: {}", Arrays.toString(crcResult), Arrays.toString(crcCheck));
                            result = new ModbusTelegram(-1, -1, new byte[]{});
                            throw new IOException("CRC error happened.");
                        }
                    }

                } catch (IOException ex) {
                    ex.printStackTrace();
                    log.warn("Error occured. Triggering disconnect+connect");
                    disconnect();
                    TimerTask tt = new TimerTask() {

                        @Override
                        public void run() {
                            try {
                                connect();
                            } catch (IOException ex1) {
                                ex1.printStackTrace();
                            }
                        }
                    };
                    log.warn("scheduled reconnect ...");
                    timer.schedule(tt, 5000);
                }
                loop++;
            }
            
            s.setSoTimeout(soTimeout);

            if (result!=null && !result.isValid()) {
                result = null;
            }
            
        log.debug("returning: " + result);
        return result;

    }

    private byte[] crc16(byte[] bytes) {
        byte[] crcBytes = new byte[2];

        int Poly16 = 0xA001;
        int crc = 0xFFFF;

        for (int i = 0; i < bytes.length; i++) {
            byte data = bytes[i];
            int j;
            boolean LSB;
            crc = ((crc ^ data) | 0xFF00) & (crc | 0x00FF);
            for (j = 0; j < 8; j++) {
                LSB = (crc & 0x0001) == 1;
                crc = crc / 2;
                if (LSB) {
                    crc = crc ^ Poly16;
                }
            }
        }

        crcBytes[0] = (byte) (crc & 0x00FF);
        crcBytes[1] = (byte) ((crc & 0xFF00) / 256);
        return crcBytes;
    }

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
        s.setTcpNoDelay(true);
        inputStream = new BufferedInputStream(s.getInputStream());
        outputStream = new BufferedOutputStream(s.getOutputStream());
        log.info("Connected!");
        isConnected = true;
    }

    public double readFloat16bit(int addr) throws IOException {

        log.debug("Read ModBus float16: " + addr);

        byte addrHigh = (byte) ((addr >>> 8) & 0xFF);
        byte addrLow = (byte) ((addr >>> 0) & 0xFF);

        ModbusTelegram response = sendTelegram(
                new ModbusTelegram(ownAddress, Integer.parseInt(configdata.getProperty("functioncode.analog.read-holding-register", "3")),
                        new byte[]{addrHigh, addrLow, 0x00, 0x01}));

        if (response == null) {
            throw new IOException("ModBus CRC failed.");
        }
        byte[] data = response.getData();

        int ch0 = data[0] & 0xff;
        int ch1 = data[1] & 0xff;

        int i = (int) ((ch0 << 8) + (ch1 << 0));

        log.debug("Read ModBus float16: " + addr + " -> " + (i / 10d));
        return i / 10d;
    }

    public int readUnsigned16bit(int address) throws IOException {
        log.debug("Read ModBus uint16: " + address);
        byte addrHigh = (byte) ((address >>> 8) & 0xFF);
        byte addrLow = (byte) ((address >>> 0) & 0xFF);

        ModbusTelegram response = sendTelegram(
                new ModbusTelegram(ownAddress, Integer.parseInt(configdata.getProperty("functioncode.analog.read-holding-register", "3")),
                        new byte[]{addrHigh, addrLow, 0x00, 0x01}));

        if (response == null) {
            throw new IOException("ModBus CRC failed.");
        }

        byte[] data = response.getData();

        int ch0 = data[0] & 0xff;
        int ch1 = data[1] & 0xff;

        int i = (int) ((ch0 << 8) + (ch1 << 0));
        log.debug("Read ModBus uint16: " + address + " -> " + i);
        return i;
    }

    public boolean readBoolean(int address) throws IOException {
        log.debug("Read ModBus Boolean: " + address);
        byte addrHigh = (byte) ((address >>> 8) & 0xFF);
        byte addrLow = (byte) ((address >>> 0) & 0xFF);

        ModbusTelegram response = sendTelegram(
                new ModbusTelegram(ownAddress, Integer.parseInt(configdata.getProperty("functioncode.digital.read-coils", "1")),
                        new byte[]{addrHigh, addrLow, 0x00, 0x01}));

        if (response == null) {
            throw new IOException("ModBus CRC failed.");
        }

        byte[] data = response.getData();

        log.debug("Read ModBus Boolean: " + address + " -> " + (data[0] == 1));
        return data[0] == 1;
    }

}
