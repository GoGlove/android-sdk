/**
 ******************************************************************************
 * @author  Eric Ely
 *
 * @brief   Main program body.
 ******************************************************************************
  Copyright (c) 2015 Easier to Use, LLC.  All rights reserved.
  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation, either
  version 3 of the License, or (at your option) any later version.
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.
  You should have received a copy of the GNU Lesser General Public
  License along with this program; if not, see <http://www.gnu.org/licenses/>.
  ******************************************************************************
 */

package com.etu.GoGloveSDK;

import android.bluetooth.BluetoothDevice;

public class BLEDeviceInfo {
	private BluetoothDevice bleDevice;
	private int rssi;
	
	public BLEDeviceInfo(BluetoothDevice device, final int rssi)
	{
		this.bleDevice = device;
		this.rssi = rssi;
	}
	
	public void UpdateRSSI(int newRSSI)
	{
		rssi = newRSSI;
	}
	
	public int GetRSSI()
	{
		return rssi;
	}
	public String GetName()
	{
		return bleDevice.getName();
	}
	public String GetMAC()
	{
		return bleDevice.getAddress();
	}
}
