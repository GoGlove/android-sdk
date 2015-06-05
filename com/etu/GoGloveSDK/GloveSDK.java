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

import java.util.Observable;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

public class GloveSDK extends Observable {
	
	ServiceManager sManager;
	
	public enum GoGloveButtons {
	    GLOVE_INDEX_FINGER_TIP,
	    GLOVE_MIDDLE_FINGER_TIP,
	    GLOVE_RING_FINGER_TIP,
	    GLOVE_PINKY_FINGER_TIP,
	    GLOVE_INDEX_FINGER_BASE,
	    REMOTE_ONE,
	    REMOTE_TWO,
	    REMOTE_THREE,
	    REMOTE_FOUR,
	    REMOTE_FIVE,
	}
	
	public enum GoGloveButtonPressType {
	    SINGLE_TAP,
	    DOUBLE_TAP
	}
	
	public enum GoGloveAction {
	    PLAY_PAUSE, //Will send the HID Play/Paude media key
	    NEXT_TRACK, //Will send the Next Track media key
	    PREVIOUS_TRACK, //Will send the Previous Track media key
	    VOLUME_UP, //Will send the Volume Up media key
	    VOLUME_DOWN, //Will send the Volume Down media key
	    ACTIVATE, //Will activate the glove so commands can be entered
	    NOTIFY //Will only send the event to the registered Handler, will not send any media key
	}
	
    public enum GoGloveMessageType {
    	CONNECT, //Event to tell the service to connect to a specific GoGLove
    	DISCONNECT, //Event to tell the service to disconnect from a specific GoGlove
    	GET_BLE_LIST, //Get the list of all BLE devices connected
        CONNECTED, //Event sent when a GoGlove is connected
        DISCONNECTED, //Event sent when a GoGlove is disconnected
        BUTTON_PRESS_EVENT, //Event sent when a button is pressed on a connected GoGlove
        BUTTON_CONFIGURATION, //Event that can be sent to GoGlove to change the button configuration
        ACTIVATION_CONFIGURATION //Event that can be sent to GoGlove to change the activation timeout
    } 
    
    private Context context;
    
    private static final String TAG = "GoGlove SDK";
	
    //! GloveSDK Constructor
    /*!
      Starts a background service that allows you to control and list attached GoGloves
    */
	public GloveSDK(Context context) {		
		this.context = context;
		this.sManager = new ServiceManager(this.context, GloveService.class, new GoGloveHandlerExtension());
		if (!sManager.isRunning()) {
			Log.d(TAG, "Service is not running. Starting!");
			sManager.start();
		}
		sManager.bind();
	}
	
	//! Unbind
    /*!
      This function unbinds the activity from the underlying service. Must be called when you leave any Activity that as called the constructor
    */
	public void unbind() {				
        sManager.unbind();
	}
	//! Stop
    /*!
      This function stops the underlying service. Should be called when you wich the application to exit and GoGLove service to end
    */
	public void stop() {
		sManager.stop();
	}
	//! BLE Device Info List
    /*!
      Returns a list of all BLE Devices attached
    */
	public BLEDeviceInfoList getConnectedDevices() 
	{
		return BLELister.getList(this.context);
	}
	//! Connect
    /*!
      Connects to a specific BLE device by the DeviceInfo class
    */
	public void connect(BLEDeviceInfo device) 
	{		
		Log.d(TAG, "Sending message of type " + GoGloveMessageType.CONNECT.ordinal());
		Message msg = new Message();
		Bundle b = new Bundle();	
		b.putInt("info", GoGloveMessageType.CONNECT.ordinal());
		b.putString("address", device.GetMAC());
		msg.setData(b);
		sendMessage(msg);
	}
	//! Connect
    /*!
      Connects to a specific BLE device by MAC address
    */
	public void connect(String address) 
	{		
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putInt("info", GoGloveMessageType.CONNECT.ordinal());
		b.putString("address", address);
		msg.setData(b);
		sendMessage(msg);
	}
	//! Disconnect
    /*!
      Disconnects from any connected GoGlove
    */
	public void disconnect() 
	{
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putInt("info", GoGloveMessageType.DISCONNECT.ordinal());
		msg.setData(b);
		sendMessage(msg);
	}
	//! Configure Button
    /*!
      Allows you to specify the action for one button
    */
	public void configureButton(GoGloveButtons button, GoGloveButtonPressType buttonPressType, GoGloveAction action) 
	{
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putInt("info", GoGloveMessageType.BUTTON_CONFIGURATION.ordinal());
		b.putInt("button", button.ordinal());
		b.putInt("action", action.ordinal());
		msg.setData(b);
		sendMessage(msg);	
	}
	//! Configure Activation Timeout
    /*!
      Configures how long the activation timer will last
    */
	public void configureActivationTimeout(int timeoutSeconds) 
	{
		Message msg = new Message();
		Bundle b = new Bundle();
		b.putInt("info", GoGloveMessageType.ACTIVATION_CONFIGURATION.ordinal());
		b.putInt("time", timeoutSeconds);
		msg.setData(b);
		sendMessage(msg);
	}
	
	private void sendMessage(Message msg)
	{
		try {
			sManager.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	private class GoGloveHandlerExtension extends Handler {

	    @Override
	    public void handleMessage(Message msg) {
	    	Log.d(TAG, "Received event type: " + msg.getData().getInt("BLEEventType", -1));
	    	int msgType = msg.getData().getInt("BLEEventType", -1);
	    	if (msgType != -1)
			{
		    	GoGloveMessageType type = GoGloveMessageType.values()[msgType];
		    	BLEEvent event = new BLEEvent();
		    	switch (type) {
		            case CONNECTED:
		            	//Handle when a GoGlove is connected to the device
		            	event.type = GoGloveMessageType.CONNECTED;
		            	event.contents = null;	                
		                break;
		            case DISCONNECTED:
		                //Handle when a GoGlove is disconnected from the device
		            	event.type = GoGloveMessageType.DISCONNECTED;
		            	event.contents = null;	
		                break;
		            case BUTTON_PRESS_EVENT:
		                //Handle when a a button in the NOTIFY state is pressed
		            	event.type = GoGloveMessageType.BUTTON_PRESS_EVENT;
		            	event.contents = msg.getData();	           
		                break;	                
		        }
		    	sendEvent(event);
			} else {
				//otherwise, it is a message from the ServiceManager
				Log.d("GloveSelection", "Received ServiceManager Message in UI");
				msgType = msg.getData().getInt("info", -1);
				if (msgType == ServiceManager.SERVICE_BOUND)
				{
					
				}
			}
	    }
	}
	private void sendEvent(BLEEvent e)
    {
    	setChanged();
		notifyObservers(e);
    }
}
