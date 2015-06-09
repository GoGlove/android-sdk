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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class BLEDeviceInfoList {
	
	public List<BLEDeviceInfo> bleDevices;
	
	public BLEDeviceInfoList()
	{
		bleDevices = new ArrayList<BLEDeviceInfo>();
	}
	
	public void InsertOrUpdate(BLEDeviceInfo bleDevice)
	{
		Iterator<BLEDeviceInfo> deviceIter = bleDevices.iterator();
		boolean found = false;
		while(deviceIter.hasNext()){
			BLEDeviceInfo existingBleDevice = deviceIter.next();
			if (existingBleDevice.GetMAC().equalsIgnoreCase(bleDevice.GetMAC()))
			{
				found = true;
				existingBleDevice.UpdateRSSI(bleDevice.GetRSSI());
				break;
			}
		}
		if (!found)
		{
			bleDevices.add(bleDevice);
		}
	}
	public BLEDeviceInfo GetBLEDeviceInfo(int index)
	{
		return bleDevices.get(index);
	}
	public int GetCount()
	{
		return bleDevices.size();
	}
	
	public BLEDeviceInfoList MergeAndTakeUnique(BLEDeviceInfoList newDevices)
	{
		BLEDeviceInfoList mergedOverDevice = new BLEDeviceInfoList();
		
		Iterator<BLEDeviceInfo> deviceIter = newDevices.bleDevices.iterator();
		boolean found = false;
		while(deviceIter.hasNext()) {
			BLEDeviceInfo newBleDevice = deviceIter.next();
			if (!this.ContainsByAddress(newBleDevice))
			{
				mergedOverDevice.bleDevices.add(newBleDevice);
			}
		}
		
		return mergedOverDevice;
	}
	
	private boolean ContainsByAddress(BLEDeviceInfo bleDevice)
	{
		Iterator<BLEDeviceInfo> deviceIter = bleDevices.iterator();
		boolean found = false;
		while(deviceIter.hasNext()) {
			BLEDeviceInfo existingBleDevice = deviceIter.next();
			if (existingBleDevice.GetMAC().equalsIgnoreCase(bleDevice.GetMAC()))
			{
				found = true;
				break;
			}
		}
		return found;
	}
}
