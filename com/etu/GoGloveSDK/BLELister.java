/**
 ******************************************************************************
 * @author  Eric Ely
 * @version V1.0.0
 * @date    15-May-2015
 * 
 * 
 * @brief   Main program body.
 ******************************************************************************
  Copyright (c) 2013 Spark Labs, Inc.  All rights reserved.
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

import java.util.List;
import java.util.Observable;
import android.util.Log;

import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

//!  BLELister
/*!
	One function class which lists all the BLE devices currently connected to this device.
*/
public class BLELister extends Observable {	
	private static final String TAG = "GoGlove BLE Lister";
	
	public static BLEDeviceInfoList getList(Context context) {			
		BluetoothManager mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		BLEDeviceInfoList devices = new BLEDeviceInfoList();
		
		List<BluetoothDevice> connectedDevices = mBluetoothManager.getDevicesMatchingConnectionStates(BluetoothProfile.GATT,
				new int[] {BluetoothProfile.STATE_CONNECTED, BluetoothProfile.STATE_CONNECTING, BluetoothProfile.STATE_DISCONNECTED, BluetoothProfile.STATE_DISCONNECTING});
		Log.d(TAG, "Found this many connected devices: " + connectedDevices.size());
		for (BluetoothDevice connectedDevice : connectedDevices) 
		{
			Log.d(TAG, "Found device with this address: " + connectedDevice.getAddress());
//			BLEDeviceInfo bleDevice = new BLEDeviceInfo(connectedDevice, Integer.parseInt(connectedDevice.EXTRA_RSSI));
			BLEDeviceInfo bleDevice = new BLEDeviceInfo(connectedDevice, -80);
			devices.InsertOrUpdate(bleDevice);			
		}
		return devices;
	}
}
