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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Observable;
import java.util.UUID;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class BLEManager extends Observable {
	final private static int STATE_BLUETOOTH_OFF = 1;
    final private static int STATE_DISCONNECTED = 2;
    final private static int STATE_CONNECTING = 3;
    final private static int STATE_CONNECTED = 4;
    private int state;
	
	private Context context;
	private BLELister lister;
	
	private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattService mParticleBluetoothGattService;
    private BluetoothGattService mGGBluetoothGattService;
    
    public enum GATTEvent {
        DISCONNECTED, 
        CONNECTED,
        DATA_AVAILABLE,
        DATA_READ
    }
	
	public BLEDeviceInfoList bleDevices;	
	
	//UUID's for GoGlove service, the local service to talk to the remote
	public final static UUID UUID_GG_SERVICE = BLEHelper.sixteenBitUuid(0x1623);
	public final static UUID UUID_GG_RECEIVE = BLEHelper.sixteenBitUuid(0x1624);
	public final static UUID UUID_GG_SEND = BLEHelper.sixteenBitUuid(0x1625);
	  
	//UUID's for Particle service, the the service that passes data to the cloud
    public final static UUID UUID_PARTICLE_SERVICE = BLEHelper.sixteenBitUuid(0x1523);
    public final static UUID UUID_PARTICLE_RECEIVE = BLEHelper.sixteenBitUuid(0x1524);
    public final static UUID UUID_PARTICLE_SEND = BLEHelper.sixteenBitUuid(0x1525);
	  
    //UUID for the client configuration
	public final static UUID UUID_CLIENT_CONFIGURATION = BLEHelper.sixteenBitUuidClient(0x2902);
	
	//Tag for debug messages
	private static final String TAG = "GoGlove BLE Manager";
	
	public BLEManager(Context context) {
		this.context = context;
		
		bleDevices = new BLEDeviceInfoList();
		
		initialize();	
		
		//set up the bluetooth registers and start scanning
        context.registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));            
	}
	
	private void upgradeState(int newState) {
        if (newState > state) {
            updateState(newState);
        }
    }
	private void downgradeState(int newState) {
        if (newState < state) {
            updateState(newState);
        }
    }

    private void updateState(int newState) {
        state = newState;
        //TO DO: Send notification up that the state has changed
    }     

	private boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }
		 
	// Handles GATT events
	volatile boolean transmissionDone = false;
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GoGlove.");
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from Sparkle.");
                GattEventReceived(GATTEvent.DISCONNECTED, null);
            }
        }
        
        private void enableNotifications(UUID UUID_SERVICE, UUID UUID_RECEIVE, BluetoothGatt gatt) 
        {
        	BluetoothGattService mBluetoothGattService = null;  
        	if (UUID_SERVICE.equals(UUID_PARTICLE_SERVICE)) {
        		mParticleBluetoothGattService = gatt.getService(UUID_SERVICE);
        		mBluetoothGattService = mParticleBluetoothGattService;
        	} else if (UUID_SERVICE.equals(UUID_GG_SERVICE)) {
        		mGGBluetoothGattService = gatt.getService(UUID_SERVICE);
        		mBluetoothGattService = mGGBluetoothGattService;
        	}
            Log.d(TAG, "Looking for service " + UUID_SERVICE);
            
            ArrayList<BluetoothGattService> services = (ArrayList<BluetoothGattService>) gatt.getServices();
            Iterator<BluetoothGattService> it = services.iterator();
            while(it.hasNext())
            {
            	BluetoothGattService obj = it.next();
                Log.d(TAG, "Found GATT service " + obj.getUuid());
            }
            
            if (mBluetoothGattService == null) {
                Log.e(TAG, "GATT service " + UUID_SERVICE + " not found!");
                return;
            }

            BluetoothGattCharacteristic receiveCharacteristic =
                    mBluetoothGattService.getCharacteristic(UUID_RECEIVE);
            
            ArrayList<BluetoothGattCharacteristic> characteristics = (ArrayList<BluetoothGattCharacteristic>) mBluetoothGattService.getCharacteristics();
            Iterator<BluetoothGattCharacteristic> iter = characteristics.iterator();
            while(iter.hasNext())
            {
            	BluetoothGattCharacteristic obj = iter.next();
                Log.d(TAG, "Found GATT characteristic " + obj.getUuid());
                BluetoothGattDescriptor descr =
                		obj.getDescriptor(obj.getUuid());
                if (descr != null) {
                	Log.d(TAG, "GATT characteristic has descriptor" + descr.toString());
                } else {
                	Log.d(TAG, "GATT characteristic has no descriptor");
                }
            }
            
            
            if (receiveCharacteristic != null) {
            	ArrayList<BluetoothGattDescriptor> descriptors = (ArrayList<BluetoothGattDescriptor>) receiveCharacteristic.getDescriptors();
                Iterator<BluetoothGattDescriptor> dIter = descriptors.iterator();
                while(dIter.hasNext())
                {
                	BluetoothGattDescriptor obj = dIter.next();
                	Log.d(TAG, "Found GATT descriptor " + obj.getUuid());
                }
                BluetoothGattDescriptor receiveConfigDescriptor =
                        receiveCharacteristic.getDescriptor(UUID_CLIENT_CONFIGURATION);
                if (receiveConfigDescriptor != null) {
                  gatt.setCharacteristicNotification(receiveCharacteristic, true);                    	
                  receiveConfigDescriptor.setValue(
                          BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                  gatt.writeDescriptor(receiveConfigDescriptor);
                    Log.d(TAG, "Descriptr Notified");
                } else {
                    Log.e(TAG, "Receive config descriptor not found!");
                }

            } else {
                Log.e(TAG, "Receive characteristic not found!");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        	Log.d(TAG, "Found status " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {

                enableNotifications(UUID_PARTICLE_SERVICE, UUID_PARTICLE_RECEIVE, gatt);
                enableNotifications(UUID_GG_SERVICE, UUID_GG_RECEIVE, gatt);


                GattEventReceived(GATTEvent.CONNECTED, null);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	Log.d(TAG, "Characteristic Read!");
            	GattEventReceived(GATTEvent.DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        	Log.d(TAG, "Characteristic Changed!");
        	GattEventReceived(GATTEvent.DATA_AVAILABLE, characteristic);
        }
        
        @Override
        public void onCharacteristicWrite (BluetoothGatt gatt, 
        		BluetoothGattCharacteristic characteristic, 
        		int status) {
            Log.d("BLEManager", "Transmission Done!");
            transmissionDone = true;
        }
	};
	
	private void SendEvent(BluetoothGattCharacteristic e)
    {
    	setChanged();
		notifyObservers(e);
    }
	//Handles Intents right from the Bluetooth Adapter
	private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            if (state == BluetoothAdapter.STATE_ON) {
                upgradeState(STATE_DISCONNECTED);
            } else if (state == BluetoothAdapter.STATE_OFF) {
                downgradeState(STATE_BLUETOOTH_OFF);
            }
        }
    };
    //Handles Events from GATT     
    public void GattEventReceived(GATTEvent eventType, BluetoothGattCharacteristic characteristic) {
    	Log.d(TAG, "Sending event of type " + eventType);
    	switch (eventType) {
    		case DISCONNECTED:
    			downgradeState(STATE_DISCONNECTED);
    			break;
    		case CONNECTED:
    			upgradeState(STATE_CONNECTED);
    			break;
    		case DATA_AVAILABLE:
    		case DATA_READ:
    			BLEEvent event = new BLEEvent();    			
            	event.contents = characteristic;
            	SendEvent(characteristic);
    			break;
    	}        
    }	
	
	public boolean connect(final String address) {    	
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            return mBluetoothGatt.connect();
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        return true;
    }
   
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }
    public boolean sendData(byte[] data, UUID UUID_SEND) {
        if (mBluetoothGatt == null || mParticleBluetoothGattService == null || mGGBluetoothGattService == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return false;
        }     
        
        BluetoothGattCharacteristic characteristic = null;
        if (UUID_SEND.equals(UUID_PARTICLE_SEND)) {
        	Log.d(TAG, "Comparing against Particle");
        	characteristic = mParticleBluetoothGattService.getCharacteristic(UUID_PARTICLE_SEND);
    	} else if (UUID_SEND.equals(UUID_GG_SEND)) {
    		Log.d(TAG, "Comparing against GG");
    		characteristic = mGGBluetoothGattService.getCharacteristic(UUID_GG_SEND);
    	} else {
    		Log.d(TAG, "Cannot find characteristic " + UUID_SEND);
    	}


        if (characteristic == null) {
            Log.w(TAG, "Send characteristic not found");
            return false;
        }

        boolean success = false;
        byte[] buffer = new byte[20];
        for (int i = 0; i < data.length; i+=20) {
        	int size = (data.length-i > 20 ? 20 : data.length-i);
        	byte[] tmpBuffer = new byte[size];
        	int originalIndex = 0;
        	for (int j = i; j < i+size; j++) {
        		tmpBuffer[originalIndex] = data[j];
        		originalIndex++;
        	}
        	Log.d(TAG, "Sending down this many bytes on BLE: " + tmpBuffer.length);
        	
        	StringBuilder sb = new StringBuilder();    
    	    for (byte b : tmpBuffer) {
    	        sb.append(String.format("%02X ", b));
    	    }    	    
	        
    	    characteristic.setValue(tmpBuffer);
	        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
	        transmissionDone = false;

	        android.util.Log.d(TAG, "  Millisecinds before send " + System.currentTimeMillis());
	        success = mBluetoothGatt.writeCharacteristic(characteristic);        	
        	while (!transmissionDone) { }
        	Log.d(TAG, "Success of send: " + success);
        }
        return success;
    }
    public boolean isInitialized() {
    	if (mBluetoothGatt == null || mParticleBluetoothGattService == null || mGGBluetoothGattService == null) {
    		return false;
    	} else {
    		return true;
    	}
    }
    
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
}
