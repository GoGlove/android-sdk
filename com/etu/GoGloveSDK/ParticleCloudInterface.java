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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Observable;

import android.os.AsyncTask;
import android.util.Log;

public class ParticleCloudInterface extends Observable implements Runnable {
	String cloudServer = "54.208.229.4";
	int cloudPort = 5683;
	
	Socket socket;
	InputStream inputStream;
	OutputStream outputStream;
	
	byte[] dlBuffer, ulBuffer;
    int ulBufferLength;
	
	public ParticleCloudInterface() {
		ulBuffer = new byte[512];
	}
	
	public void Connect() throws UnknownHostException, IOException {
		
		class Retrievedata extends AsyncTask<String, Void, String> {
			@Override
			    protected String doInBackground(String... params) {
			         try{
			        	 Log.d("SparkleCloudInterface", "Connecting to Spark Cloud");
			        	 InetAddress serveraddress=InetAddress.getByName(cloudServer);
			        	 Log.d("SparkCloudInterface", "Got Here!");
			        	 Log.d("SparkleCloudInterface", "We should have the IP Address " + serveraddress);
			        	 socket = new Socket(cloudServer, cloudPort);
			        	 Log.d("SparkleCloudInterface", "Did we connect?");
			        	 Log.d("SparkleCloudInterface", Boolean.toString(socket.isConnected()));
			        	 inputStream = socket.getInputStream();
			        	 outputStream = socket.getOutputStream();    
			         }
			         catch (Exception e)
			         {
			        	 e.printStackTrace();
			         }
			         return null;
			    }
			}
		String params = "";
		new Retrievedata().execute(params);
	}
	public void Disconnect() {
		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (outputStream != null) {
			try {
				outputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public void HandleData(byte[] data) 
	{
		StringBuilder sb = new StringBuilder();
	    for (byte b : data) {
	        sb.append(String.format("%02X ", b));
	    }
//	    Log.d("BLEService", "Processing Data: " + sb.toString());
        
	    if (data.length == 2 && data[0] == 0x03 && data[1] == 0x04) {
        	try {
//        		Log.d("BLEService", "Got a full buffer, attempting to send it up");
        		byte[] tmpBuffer = new byte[ulBufferLength];
        		System.arraycopy(ulBuffer, 0, tmpBuffer, 0, ulBufferLength);
//        		Log.d("BLEService", "About to write this many bytes " + tmpBuffer.length);
				Write(tmpBuffer);
				Log.d("BLEManager", "Received this many bytes from BLE: " + tmpBuffer.length);
			} catch (IOException e) {
				e.printStackTrace();
			}
        	ulBuffer = new byte[512];
        	ulBufferLength = 0;
        } else {
//        	Log.d("BLESerivce", "buffer: " + ulBuffer + " has length " + ulBuffer.length + " with current position set to " + ulBufferLength);
        	System.arraycopy(data, 0, ulBuffer, ulBufferLength, data.length);
        	ulBufferLength += data.length;
        }

	}
	public void Write(byte[] data) throws IOException {
		Log.d("SparkleCloudInterface", "When writing, are we connected: " + Boolean.toString(socket.isConnected()));
		outputStream.write(data);
		outputStream.flush();		
		Log.d("SparkleCloudInterface", "Sending data to Cloue of size: " + data.length);
	}
	public int Available() throws IOException {
		if (inputStream != null) {
			return inputStream.available();
		}
		return 0;
	}
	public boolean Connected() {
		if (inputStream != null) {
			return socket.isConnected();
		} else {
			return false;
		}
	}
	public byte[] Read() throws IOException {	
		byte[] data = new byte[inputStream.available()];
		inputStream.read(data, 0, inputStream.available());
	    StringBuilder sb = new StringBuilder();
	    
	    for (byte b : data) {
	        sb.append(String.format("%02X ", b));
	    }
	    Log.d("SparkleCloudInterface", "Got data from Cloud of size: " + data.length);
	    
		return data;
	}
	
	private void SendEvent(byte[] data)
    {
    	setChanged();
		notifyObservers(data);
    }

	public boolean shouldRun = false;
	@Override
	public void run() {
		shouldRun = true;
		while (shouldRun) {
			try {
				int bytesAvailable = Available();
				Log.d("BLEService", "Connected: " + Boolean.toString(Connected()) + "  Bytes Available: " + bytesAvailable);
				if (bytesAvailable > 0) {
					dlBuffer = Read();
//					Log.d("BLEService", "We read some bytes from SPark Cloud: " + dlBuffer.length);
					SendEvent(dlBuffer);
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public void stop() {
		shouldRun = false;		
		Disconnect();
	}
}
