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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author achristian
 */
public class KnxData {
    
    public static final int INTERVAL_OFF = 0;
    public static final int INTERVAL_SENDONUPDATE = -1;
    
    String dpt;
    List<String> ga = new ArrayList<>();
    int sendCyclic = INTERVAL_OFF;

    public void setGAs(String value) {
        String[] split = value.split(",");
        for (String s : split) {
            ga.add(s);
        }
    }

    public void setDpt(String dpt) {
        this.dpt = dpt;
    }

    public  void setSendCyclic(int sendCylic) {
        this.sendCyclic = sendCylic;
    }

    public String getDpt() {
        return dpt;
    }

    public int getSendCyclic() {
        return sendCyclic;
    }
    
    

    public List<String> getGaList() {
        return ga;
    }

    @Override
    public String toString() {
        return "KnxData{" + "dpt=" + dpt + ", ga=" + ga + ", sendCyclic=" + sendCyclic + '}';
    }
    
    
    
}
