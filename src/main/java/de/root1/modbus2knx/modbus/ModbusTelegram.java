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

import java.util.Arrays;

/**
 *
 * @author achristian
 */
public class ModbusTelegram {
    
    private int address;
    private int function;
    private byte[] data;

    public ModbusTelegram(int address, int function, byte[] data) {
        this.address = address;
        this.function = function;
        this.data = data;
    }

    /**
     * Creates a copy
     * @param tg 
     */
    ModbusTelegram(ModbusTelegram tg) {
        this.address= tg.address;
        this.function = tg.function;
        data = new byte[tg.data.length];
        System.arraycopy(tg.data, 0, data, 0, tg.data.length);
    }
    
    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public int getFunction() {
        return function;
    }

    public void setFunction(int function) {
        this.function = function;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
    
    public boolean isValid() {
        return address!=-1 && function!=-1 && data.length>0;
    }

    @Override
    public String toString() {
        return "ModbusTelegram{" + "address=" + address + ", function=" + function + ", data=" + Arrays.toString(data) + '}';
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + this.address;
        hash = 59 * hash + this.function;
        hash = 59 * hash + Arrays.hashCode(this.data);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ModbusTelegram other = (ModbusTelegram) obj;
        if (this.address != other.address) {
            return false;
        }
        if (this.function != other.function) {
            return false;
        }
        if (!Arrays.equals(this.data, other.data)) {
            return false;
        }
        return true;
    }
    
    

    
}
