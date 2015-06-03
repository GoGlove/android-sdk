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

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class ServiceManager {
	private Class<? extends AbstractService> mServiceClass;
	private Context mActivity;
    private boolean mIsBound;
    private Messenger mService = null;
    private Handler mIncomingHandler = null;
    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    
    final public static int SERVICE_BOUND = 10;
    
    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	if (mIncomingHandler != null) {
        		//Log.i("ServiceHandler", "Incoming message. Passing to handler: "+msg);
        		mIncomingHandler.handleMessage(msg);
        	}
        }
    }
    
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
        	
            //textStatus.setText("Attached.");
            Log.i("ServiceHandler", "Attached.");
            try {
                Message msg = Message.obtain(null, AbstractService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
            //textStatus.setText("Disconnected.");
            Log.i("ServiceHandler", "Disconnected.");
        }
    };
    
    public ServiceManager(Context context, Class<? extends AbstractService> serviceClass, Handler incomingHandler) {
    	this.mActivity = context;
    	this.mServiceClass = serviceClass;
    	this.mIncomingHandler = incomingHandler;
    	if (isRunning()) {
    		doBindService();
    	}
    }

    public void start() {
    	doStartService();
    	doBindService();
    }
    
    public void stop() {
    	doUnbindService();
    	doStopService();    	
    }
    
    /**
     * Use with caution (only in Activity.onDestroy())! 
     */
    public void unbind() {
    	doUnbindService();
    }
    
    public void bind() {
    	doBindService();
    }
    
    public boolean isRunning() {
    	ActivityManager manager = (ActivityManager) mActivity.getSystemService(Context.ACTIVITY_SERVICE);
	    
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (mServiceClass.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    
	    return false;
    }
    
    public void send(Message msg) throws RemoteException {
    	if (mIsBound) {
            if (mService != null) {
            	mService.send(msg);
            }
    	}
    }
    
    private void doStartService() {
    	mActivity.startService(new Intent(mActivity, mServiceClass));
    }
    
    private void doStopService() {
    	mActivity.stopService(new Intent(mActivity, mServiceClass));
    }
    
    private void doBindService() {
    	mActivity.bindService(new Intent(mActivity, mServiceClass), mConnection, Context.BIND_AUTO_CREATE);
    	mIsBound = true;
    }
    
    private void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, AbstractService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            
            // Detach our existing connection.
            mActivity.unbindService(mConnection);
            mIsBound = false;
            //textStatus.setText("Unbinding.");
            Log.i("ServiceHandler", "Unbinding.");
        }
    }
}