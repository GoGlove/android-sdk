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

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Observable;
import java.util.Observer;

import com.etu.GoGloveSDK.GloveSDK.GoGloveButtonPressType;
import com.etu.GoGloveSDK.GloveSDK.GoGloveAction;
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
		// Messages that come down from the UI or Service upper layer
		try {
			Log.d(TAG, "Get message of type: " + msg.getData().getInt("info"));
			GoGloveMessageType eventType = GoGloveMessageType.values()[msg.getData().getInt("info")];			
			Log.d(TAG, "Received message: " + eventType);
			int button , buttonType;
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

					byte[] startConnectionBuffer = {0x07, 0x06, 0x05, 0x04, 0x03};
					bleManager.sendData(startConnectionBuffer, BLEManager.UUID_PARTICLE_SEND);
					byte[] eosBuffer = {0x03, 0x04};
					bleManager.sendData(eosBuffer, BLEManager.UUID_PARTICLE_SEND);
					Thread.sleep(100);

					particleSocket.addObserver(this);
					Thread scannerThread = new Thread(particleSocket);
					scannerThread.start();
					particleSocket.Connect();
					break;
				case BUTTON_CONFIGURATION:
					button = msg.getData().getInt("button");
					buttonType = msg.getData().getInt("buttonType");
					int action = msg.getData().getInt("action");
					boolean activation = msg.getData().getBoolean("activation");
					//data packet for button config is 5 bytes
					byte[] buttonData = { new Byte(Integer.toString(msg.getData().getInt("info"))),
							new Byte(Integer.toString(button)),
							new Byte(Integer.toString(buttonType)),
							new Byte(Integer.toString(action)),
							new Byte(Integer.toString(activation ? 1 : 0))};
					bleManager.sendData(buttonData, BLEManager.UUID_GG_SEND);
					break;
				case BUTTON_QUERY:
					button = msg.getData().getInt("button");
					buttonType = msg.getData().getInt("buttonType");
					//data packet for button config is 4 bytes
					Log.d("GoGlove Service", "Button = " + Integer.toString(button) + " and type = " + Integer.toString(buttonType));
					byte[] buttonQuery = { new Byte(Integer.toString(msg.getData().getInt("info"))),
							new Byte(Integer.toString(button)),
							new Byte(Integer.toString(buttonType))};
					bleManager.sendData(buttonQuery, BLEManager.UUID_GG_SEND);
					break;
				case ACTIVATION_CONFIGURATION:
					int time = msg.getData().getInt("time");					
					//data packet for activation config is 2 bytes
					byte[] activationData = { new Byte(Integer.toString(msg.getData().getInt("info"))),
							new Byte(Integer.toString(time))};
					bleManager.sendData(activationData, BLEManager.UUID_GG_SEND);
					break;
				case DISCONNECT:
					Log.d(TAG, "Disconnecting BLE");
					bleManager.disconnect();
					break;
				case GET_BLE_LIST:
					Log.d(TAG, "Starting Discovery");
					Message response = new Message();
					response.obj = BLELister.getList(this.GetContext());
					send(response);
					break;
			}
		}
		catch (Exception ex)
		{
			Log.d(TAG, "Error processing message: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	@Override
	public void update(Observable observable, Object data) {
		// Events received from the BLE Manager, which is the lower layer
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
    	//Got en event from GoGlove. Check the type first
    	String ascii = HexAsciiHelper.bytesToAsciiMaybe(data);
    	Log.d(TAG, "Event Type: " + data[0]);
    	GoGloveMessageType eventType = GoGloveMessageType.values()[data[0]];	
    	//toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200); //200 is duration in ms
		Message msg = new Message();
    	switch (eventType) {
			case BUTTON_QUERY_RESPONSE:
				Bundle b = new Bundle();
				b.putInt("BLEEventType", data[0]);
				b.putInt("BUTTON", data[1]);
				b.putInt("BUTTON_TYPE", data[2]);
				b.putInt("ACTION", data[3]);
				b.putBoolean("ACTIVATION", data[4] != 0x00);
				msg.setData(b);
				Log.d("GloveService", "Activation for the query is " + String.valueOf(data[4]!=0x00));
				try {
					send(msg);
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case ID_QUERY_RESPONSE:
				StringBuilder sb = new StringBuilder();
				for (byte by : data) {
					sb.append(String.format("%02X ", by));
				}
				Log.d("GloveService", "The ID is: " + sb.toString());
				break;
			case BUTTON_PRESS_EVENT:
				int action = data[1];
				if (GloveSDK.GoGloveAction.values()[action] == GloveSDK.GoGloveAction.ACTIVATE) {
					toneG.startTone(0x00000056, 300);
				} else if (GloveSDK.GoGloveAction.values()[action] == GloveSDK.GoGloveAction.DEACTIVATE) {
					toneG.startTone(0x00000014, 300);
				}
				break;
    	}      
    }   
}