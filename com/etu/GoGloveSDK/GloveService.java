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

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Observable;
import java.util.Observer;

import com.etu.GoGloveSDK.GloveSDK.GoGloveMessageType;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;
import android.view.KeyEvent;

public class GloveService extends AbstractService implements Observer {
	private BLEManager bleManager;	
	ParticleCloudInterface particleSocket;
     
    AudioManager audio;  
  	//Tag for debug messages
  	private static final String TAG = "GoGlove Service";

	@Override
	public void onStartService() {
		//init the BLEManager
		bleManager = new BLEManager(this.GetContext());
		bleManager.addObserver(this);		

		//init the audio service for tone generation
		audio = (AudioManager)getSystemService(GetContext().AUDIO_SERVICE); 
		
		//init the Particle service for cloud access. this also runs as a thread to get data from the cloud
		particleSocket =  new ParticleCloudInterface();		
	}

	@Override
	public void onStopService() {
		particleSocket.stop();
		bleManager.close();
		bleManager.deleteObserver(this);
	}

	@Override
	public void onReceiveMessage(Message msg) {
		// TODO Auto-generated method stub
		try {
			Log.d(TAG, "Get message of type: " + msg.getData().getInt("info"));
			GoGloveMessageType eventType = GoGloveMessageType.values()[msg.getData().getInt("info")];			
			Log.d(TAG, "Received message: " + eventType);
			switch (eventType)
			{			
				case CONNECT:
					String address = msg.getData().getString("address");
					bleManager.connect(address);
					while (!bleManager.isInitialized()) {
						Thread.sleep(100);
					}
					Log.d(TAG, "We are now connected and initialized!");				
					Thread.sleep(2000);
					
					particleSocket.addObserver(this);
					Thread scannerThread = new Thread(particleSocket);
					scannerThread.start();
					particleSocket.Connect();
					break;
				case DISCONNECT:
					Log.d(TAG, "Disconnecting BLE");
					bleManager.disconnect();
					break;
				case GET_BLE_LIST:
					Log.d(TAG, "Starting Discovery");
					Message response = Message.obtain(null, 2);						
					response.obj = BLELister.getList(this.GetContext());
					send(response);
					break;
			}
		}
		catch (Exception ex)
		{
			Log.d(TAG, "Error processing message: " + ex.getMessage());
		}
	}
	
	@Override
	public void update(Observable observable, Object data) {
		// TODO Auto-generated method stub
		Log.d(TAG, "Received event from BLEManager");
		if (observable == particleSocket) {
			//send data from cloud down to the device
			byte[] buffer = (byte[])data;
			bleManager.sendData(buffer, BLEManager.UUID_PARTICLE_SEND);
			//send the EOS bytes
			byte[] eosBuffer = {0x03, 0x04};
			bleManager.sendData(eosBuffer, BLEManager.UUID_PARTICLE_SEND);
		} else if (observable == bleManager) {			
			BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic)data;
			if (characteristic.getUuid().equals(BLEManager.UUID_GG_RECEIVE)) {
				processData(characteristic.getValue());
			} else if (characteristic.getUuid().equals(BLEManager.UUID_PARTICLE_RECEIVE)) {
				particleSocket.HandleData(characteristic.getValue());
			}		
		}
	}
	
	ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);;
    private void processData(byte[] data) {
    	String ascii = HexAsciiHelper.bytesToAsciiMaybe(data);
    	Log.d(TAG, "Got data: " + ascii);
    	Log.d(TAG, "Data Length: " + data.length);
    	if (data.length == 1) {
    		Log.d(TAG, "Data: " + data[0]);
	    	switch (data[0]) {
	    		case 0x25:
	    			break;
	    		case 0x26:	
	    			//Activation Switch. Play a tone
	    		    toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200); //200 is duration in ms
	    			break;
	    		case 0x27:	
	    			
	    			break;
	    		case 0x28:	
	    			
	    			try {
	    			    Thread.sleep(1500);                 //1000 milliseconds is one second.
	    			} catch(InterruptedException ex) {
	    			    Thread.currentThread().interrupt();
	    			}
	        		
	    			break;
	    	
	    	}
    	}    	      
    }   
}