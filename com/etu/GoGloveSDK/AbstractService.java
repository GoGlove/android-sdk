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

import java.util.ArrayList;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public abstract class AbstractService extends Service {
    static final int MSG_REGISTER_CLIENT = 9991;
    static final int MSG_UNREGISTER_CLIENT = 9992;

    ArrayList<Messenger> mClients = new ArrayList<Messenger>(); // Keeps track of all current registered clients.
    final Messenger mMessenger = new Messenger(new IncomingHandler()); // Target we publish for clients to send messages to IncomingHandler.
    
    private class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_REGISTER_CLIENT:
            	Log.i("MyService", "Client registered: "+msg.replyTo);
                mClients.add(msg.replyTo);
                Log.i("ServiceHandler", "Sending Message to Activity that Service is bound.");
            	Message startedMessage = new Message();
    			Bundle b = new Bundle();
    			b.putInt("info", ServiceManager.SERVICE_BOUND);
    			startedMessage.setData(b);	            	
    			try {
					msg.replyTo.send(startedMessage);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}    			
                break;
            case MSG_UNREGISTER_CLIENT:
            	Log.i("MyService", "Client un-registered: "+msg.replyTo);
                mClients.remove(msg.replyTo);
                break;            
            default:
                //super.handleMessage(msg);
            	onReceiveMessage(msg);
            }
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        onStartService();
        
        Log.i("MyService", "Service Started.");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("MyService", "Received start id " + startId + ": " + intent);
        return START_STICKY; // run until explicitly stopped.
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();

        onStopService();
        
        Log.i("MyService", "Service Stopped.");
    }    
    
    protected void send(Message msg) {
   	 for (int i=mClients.size()-1; i>=0; i--) {
            try {
            	//Log.i("MyService", "Sending message to clients: "+msg);
               mClients.get(i).send(msg);
            }
            catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
            	Log.e("MyService", "Client is dead. Removing from list: "+i);
            	mClients.remove(i);
            }
        }    	
   }


    public abstract void onStartService();
    public abstract void onStopService();
    public abstract void onReceiveMessage(Message msg);
    
    public Context GetContext()
    {
    	return this.getApplicationContext();
    }
}