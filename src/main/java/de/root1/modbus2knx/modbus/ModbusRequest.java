/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.modbus2knx.modbus;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author alexander
 */
public class ModbusRequest {
    
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(ModbusRequest.class);

    private final byte slaveAddress;
    private final byte function;
    private final int startAddress;
    private final int numberOfPoints;
    private ModbusResponse response;
    private final ModbusConnection app;

    /**
     * 
     * @param slaveAddress
     * @param function
     * @param startAddress Modbus protocol start address (hex)
     * @param numberOfPoints
     * @throws IOException 
     */
    public ModbusRequest(ModbusConnection app, byte slaveAddress, byte function, int startAddress, int numberOfPoints) throws IOException {
        this.app = app;
        this.slaveAddress = slaveAddress;
        this.function = function;
        this.startAddress = startAddress;
        this.numberOfPoints = numberOfPoints;
    }
    
    public void send(OutputStream outputstream) throws IOException {
        byte[] msg = new byte[8];
        msg[0] = slaveAddress;
        msg[1] = function;
        msg[2] = (byte) ((startAddress >> 8) & 0xFF);
        msg[3] = (byte) (startAddress & 0xFF);
        msg[4] = (byte) ((numberOfPoints >> 8) & 0xFF);
        msg[5] = (byte) (numberOfPoints & 0xFF);
        insertCRC(msg);
        for (byte b : msg) {
            log.debug(" => {}",String.format("%02X", b));
        }
        log.debug(" *DONE*");
        // modbus safety wait before
        try {
            Thread.sleep(5);
        } catch (InterruptedException ex) {
        }
        outputstream.write(msg);
        outputstream.flush();
        // modbus safety wait after
        try {
            Thread.sleep(5);
        } catch (InterruptedException ex) {
        }
        List<ModbusResponse> responses = app.getResponses();
        synchronized(responses) {
            while (responses.isEmpty()) {
                try {
                    responses.wait(10);
                } catch (InterruptedException ex) {
                }
            }
            if (!responses.isEmpty()) {
                response = responses.remove(0);
            }
            
        }
    }

    public byte getSlaveAddress() {
        return slaveAddress;
    }

    public byte getFunction() {
        return function;
    }

    /**
     * insert's CRC into last two bytes of given message
     *
     * @param msg
     */
    private void insertCRC(byte[] msg) {

        CRC16Modbus crc = new CRC16Modbus();
        crc.update(msg, 0, msg.length - 2);

        //System.out.println(Integer.toHexString((int) crc.getValue()));
        byte[] byteStr = new byte[2];
        byteStr[0] = (byte) ((crc.getValue() & 0x000000ff));
        byteStr[1] = (byte) ((crc.getValue() & 0x0000ff00) >>> 8);

        //System.out.printf("%02X%02X\n", byteStr[0], byteStr[1]);
        msg[msg.length - 2] = byteStr[0];
        msg[msg.length - 1] = byteStr[1];
    }

    /**
     * Blocks until response is present!
     * @return 
     */
    public ModbusResponse getResponse() {
        while (response==null) {
            try {
                Thread.currentThread().sleep(100);
            } catch (InterruptedException ex) {
            }
        }
        return response;
    }

    public void setResponse(ModbusResponse response) {
        this.response = response;
    }
    
    

}
