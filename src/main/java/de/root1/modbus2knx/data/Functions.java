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
package de.root1.modbus2knx.data;

/**
 *
 * @author root
 */
public class Functions {
    
    /*
    Digital 	R 	01 (0x01) 	Read Coils
    Digital 	R 	02 (0x02) 	Read Discrete Inputs
    Analog 	R 	03 (0x03) 	Read Holding Register
    Analog 	R 	04 (0x04) 	Read Input Register
    Digital     W 	05 (0x05) 	Write Single Coil
    Analog 	W 	06 (0x06) 	Write Single Register 
    */
    public static final byte FUNC_CODE_Digital_ReadCoils = 0x01;
    public static final byte FUNC_CODE_Digital_ReadDiscreteInputs = 0x02;
    public static final byte FUNC_CODE_Analog_ReadHoldingRegister = 0x03;
    public static final byte FUNC_CODE_Analog_ReadInputRegister = 0x04;
    public static final byte FUNC_CODE_Digital_WriteSinglecoil = 0x05;
    public static final byte FUNC_CODE_Analog_WriteSingleRegister = 0x06;
    
    
}
