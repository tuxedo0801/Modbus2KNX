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
    
    private long lastUpdate = 0;
    private int cyclicUpdateTime = 0;


    WatchContainer(ModbusConnection modbus, Datapoint dpt) {
        this.datapoint = dpt;
        this.modbus = modbus;
    }

    public int getCyclicUpdateTime() {
        return cyclicUpdateTime;
    }

    public void setCyclicUpdateTime(int cyclicUpdateTime) {
        this.cyclicUpdateTime = cyclicUpdateTime;
    }
    
    /**
     * triggers update if required
     * @return true, if value has changed, false if not
     */
    public boolean checkCyclicUpdate() throws ModbusException {
        if (System.currentTimeMillis()-lastUpdate>cyclicUpdateTime) {
            log.info("@@@ Time for cyclic update for {}", datapoint.getName());
            hasChanged();
            return true;
        }
        return false;
    }
    
    
    
    public boolean hasChanged() throws ModbusException {
        
        int address = datapoint.getAddress();
        int numberOfInputs = datapoint.getNumberOfPoints();
        int function = datapoint.getFunction();
        Object x = null;
        log.debug("Checking if {} has changed", datapoint.getName());
        switch(datapoint.getType()) {
            case bool:
                x = modbus.readBoolean(address, function, numberOfInputs);
                break;
            case float16bit:
                x = modbus.readFloat16bit(address, function, numberOfInputs);
                break;
            case float32bit:
                x = modbus.readFloat32bit(address, function, numberOfInputs);
                break;
            case unsigned16bit:
                x = modbus.readUnsigned16bit(address, function, numberOfInputs);
                break;
        }
        lastUpdate = System.currentTimeMillis();
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
