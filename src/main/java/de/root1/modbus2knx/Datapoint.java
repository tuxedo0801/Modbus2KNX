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

import java.util.Iterator;
import java.util.Objects;
import java.util.Properties;

/**
 * Modbus&Knx DataPoint
 * @author achristian
 */
public class Datapoint {
    
    private String group;
    private String name;
    private int address;
    private int numberOfInputs;
    private Type type;
    private KnxData knxData;
    private final Properties properties = new Properties();

    public int getNumberOfInputs() {
        return numberOfInputs;
    }

    public void setNumberOfInputs(int numberOfInputs) {
        this.numberOfInputs = numberOfInputs;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public KnxData getKnxData() {
        return knxData;
    }

    public void setKnxData(KnxData knxData) {
        this.knxData = knxData;
    }

    void parseKnxDataFromProperties(Properties configdata) {
        Iterator<Object> iterator = configdata.keySet().iterator();
        String prefix = group+"."+name+".knx.";
        knxData = new KnxData();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            String value = configdata.getProperty(key);
            key = key.trim();
            value = value.trim();
            
            if (key.startsWith(prefix)) {
                
                String[] split = key.split("\\.");
                switch(split[3]) {
                    case "dpt":
                        knxData.setDpt(value);
                        break;
                    case "ga":
                        if (value != null && value.length()>0) {
                            knxData.setGAs(value);
                        }
                        break;
                    case "sendcyclic":
                        knxData.setSendCyclic(Integer.parseInt(value));
                        break;
                    default:
                        break;
                }
                
            }
        }
    }
    
    public boolean hasUseableKnxData() {
        return getKnxData()!=null && getKnxData().dpt!=null && getKnxData().ga.size()>0;
    }

    void addProperty(String key, String value) {
        properties.put(key, value);
    }
    
    public String getProperty(String key) {
        return (String) properties.get(key);
    }
    
    public Properties getProperties() {
        return properties;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + Objects.hashCode(this.group);
        hash = 29 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Datapoint other = (Datapoint) obj;
        if (!Objects.equals(this.group, other.group)) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }

    

    @Override
    public String toString() {
        return "Datapoint{" + "group=" + group + ", name=" + name + ", address=" + address + ", numberOfInputs=" + numberOfInputs + ", type=" + type + ", knxData=" + knxData + ", properties=" + properties + '}';
    }

    
    
}
