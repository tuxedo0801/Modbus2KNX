/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.root1.modbus2knx.modbus;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author alexander
 */
public class ModbusResponse {
    
    private static final Logger log = LoggerFactory.getLogger(ModbusResponse.class);

    byte slaveAddress;
    byte function;
    int count;
    byte[] data; // multiple of 2
    byte errorCheckLow;
    byte errorCheckHigh;

    /**
     * cretae instance by reading from stream
     *
     * @param inputStream
     */
    public ModbusResponse(InputStream inputStream) throws IOException {
        log.debug("Reading/waiting for response from stream...");
        slaveAddress = (byte) inputStream.read();
        log.debug(" SlaveAddress = "+String.format("0x%02X", slaveAddress));
        function = (byte) inputStream.read();
        log.debug(" Function = "+String.format("0x%02X", function));
        count = inputStream.read();
        log.debug(" Count = "+count);
        data = inputStream.readNBytes(count);
        for (byte b : data) {
            log.debug("  Data: "+String.format("0x%02X", b));
        }
        errorCheckLow = (byte) inputStream.read();
        log.debug(" errorCheckLow = "+String.format("0x%02X", errorCheckLow));
        errorCheckHigh = (byte) inputStream.read();
        log.debug(" errorCheckHigh = "+String.format("0x%02X", errorCheckHigh));
        log.debug("*done* crc={}", crcCheck());
    }

    public byte getSlaveAddress() {
        return slaveAddress;
    }

    public byte getFunction() {
        return function;
    }

    public int getCount() {
        return count;
    }

    public byte[] getData() {
        return data;
    }

    public byte getErrorCheckLow() {
        return errorCheckLow;
    }

    public byte getErrorCheckHigh() {
        return errorCheckHigh;
    }

    /**
     *
     * @param msg
     * @return
     */
    public boolean crcCheck() {

        byte[] msg = new byte[5 + count];
        msg[0] = slaveAddress;
        msg[1] = function;
        msg[2] = (byte) count;
        System.arraycopy(data, 0, msg, 3, data.length);
        msg[2 + count + 1] = errorCheckLow;
        msg[2 + count + 2] = errorCheckHigh;

        CRC16Modbus crc = new CRC16Modbus();
        crc.update(msg, 0, msg.length - 2);

        //System.out.println(Integer.toHexString((int) crc.getValue()));
        byte[] byteStr = new byte[2];
        byteStr[0] = (byte) ((crc.getValue() & 0x000000ff));
        byteStr[1] = (byte) ((crc.getValue() & 0x0000ff00) >>> 8);

        //System.out.printf("%02X%02X\n", byteStr[0], byteStr[1]);
        if (msg[msg.length - 2] != byteStr[0] || msg[msg.length - 1] != byteStr[1]) {
            return false;
        }
        return true;
    }

    public float getFloat16() {
        int ch0 = data[0] & 0xff;
        int ch1 = data[1] & 0xff;

        int i = (int) ((ch0 << 8) + (ch1 << 0));
        return i / 10f;
    }
    
    public float getFloat32() {

        byte[] floatBytes = {
            data[0],
            data[1],
            data[2],
            data[3]
        };
        return ByteBuffer.wrap(floatBytes).getFloat();

    }
    
    public int getUint16() {
        int ch0 = data[0] & 0xff;
        int ch1 = data[1] & 0xff;
        int i = (int) ((ch0 << 8) + (ch1 << 0));
        return i;
    }
    
    public boolean getBoolean() {
        return data[0] == 1;
    }

    @Override
    public String toString() {
        return "ModbusResponse{" + "slaveAddress=" + String.format("0x%02X", slaveAddress) + ", function=" + String.format("0x%02X", function) + ", count=" + count + ", crcOkay=" + crcCheck() + '}';
    }
    
    

}
