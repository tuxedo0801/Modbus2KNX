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
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author achristian
 */
public class WatchContainer {
    
    private static final Logger log = LoggerFactory.getLogger(WatchContainer.class);
    
    private Datapoint datapoint;
    private final ModbusConnection modbus;
    private Object value;


    WatchContainer(ModbusConnection modbus, Datapoint dpt) {
        this.datapoint = dpt;
        this.modbus = modbus;
    }
    
    public boolean hasChanged() throws IOException {
        
        int address = datapoint.getAddress();
        int numberOfInputs = datapoint.getNumberOfInputs();
        Object x = null;
        log.debug("Checking if {} has changed", datapoint.getName());
        switch(datapoint.getType()) {
            case bool:
                x = modbus.readBoolean(address, numberOfInputs);
                break;
            case float16bit:
                x = modbus.readFloat16bit(address, numberOfInputs);
                break;
            case unsigned16bit:
                x = modbus.readUnsigned16bit(address, numberOfInputs);
                break;
        }
        if (x!=null && !x.equals(value)) {
            log.info("{} has changed from {} to {}", datapoint.getName(), value, x);
            value = x;
            
            return true;
        }
        return false;
    }
    
    public Object getValue() {
        return value;
    }
    

    public Datapoint getDatapoint() {
        return datapoint;
    }

    public void setDatapoint(Datapoint datapoint) {
        this.datapoint = datapoint;
    }

}
