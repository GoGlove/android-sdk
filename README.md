<p align="center" >
<img src="http://goglove.io/static/img/Logo-horizontal.png" alt="GoGlove" title="GoGlove">
</p>

GoGlove Android SDK
==========
GoGlove is a wearable, wireless remote to control your smartphone or other mobilde devices or cameras. It comes with a standalone remote that can be used without the glove in warmer weather.

The Android SDK is meant to allow developers to interact with GoGlove through their own apps. This library will allow you to detect or be alerted if GoGlove is currently present, configure which buttons perform which action, and be alerted when a specific button is pressed.

These libraries should be added to your app "as-is", then they can be used as below.

###Service
To use the GoGlove SDK, the first step is to start the service. To do this, you must first define a ServiceManager instance.
```Java
import com.goglove.sdk;

private ServiceManager sManager;
```

You must also define a handler class, this class will receive messages and events from the GoGlove service such as when a GoGlove attaches or when the user presses a specific key.
```Java
private class HandlerExtension extends Handler {

		@Override
		public void handleMessage(Message msg) {
			int type = msg.getData().getInt("BLEEventType", -1);
			//handle event
		}
	}
```

Next, you must instantiate and start a GoGlove service instance:
```Java
this.sManager = new ServiceManager(this, GoGloveSDK.class, new HandlerExtension());
```

The service is now started.



