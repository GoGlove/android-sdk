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
private class GoGloveHandlerExtension extends Handler {

    @Override
    public void handleMessage(Message msg) {
        int type = msg.getData().getInt("BLEEventType", -1);
        switch (type) {
            case GoGloveMessageType.CONNECTED:
                //Handle when a GoGlove is connected to the device
                break;
            case GoGloveMessageType.DISCONNECTED:
                //Handle when a GoGlove is disconnected from the device
                break;
            case GoGloveMessageType.BUTTON_PRESS_EVENT:
                //Handle when a a button in the NOTIFY state is pressed
                int button = msg.getData().getInt("button");
                break;
        }
    }
}
```

Next, you must instantiate and start a GoGlove service instance:
```Java
this.sManager = new ServiceManager(this, GoGloveService.class, new HandlerExtension());
```

The service is now started.

###Actions
The following actions are allowed for each button of GoGlove
```Java
public enum GoGloveActions {
    PLAY_PAUSE, //Will send the HID Play/Paude media key
    NEXT_TRACK, //Will send the Next Track media key
    PREVIOUS_TRACK, //Will send the Previous Track media key
    VOLUME_UP, //Will send the Volume Up media key
    VOLUME_DOWN, //Will send the Volume Down media key
    ACTIVATE, //Will activate the glove so commands can be entered
    NOTIFY //Will only send the event to the registered Handler, will not send any media key
}
```

Any action can be applied to any button, either on the remote or on the glove itself. The list of buttons available are:
```Java
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
```

###Messages
The following is the structure for event types that go to/from the GoGlove service
```Java
public enum GoGloveMessageType {
    CONNECTED, //Event sent when a GoGlove is connected
    DISCONNECTED, //Event sent when a GoGlove is disconnected
    BUTTON_PRESS_EVENT, //Event sent when a button is pressed on a connected GoGlove
    BUTTON_CONFIGURATION, //Event that can be sent to GoGlove to change the button configuration
    ACTIVATION_CONFIGURATION //Event that can be sent to GoGlove to change the activation timeout
}
```

Messages will be received from GoGlove in the specified Handler class

To send a message, you can call the following:
```Java
Message msg = new Message();
Bundle b = new Bundle();
b.putInt("type", GoGloveMessageType.BUTTON_CONFIGURATION);
b.putInt("button", GoGloveButtons.GLOVE_INDEX_FINGER_TIP);
b.putInt("action", GoGloveActions.PLAY_PAUSE);
msg.setData(b);
try {
    sManager.send(msg);
} catch (RemoteException e) {
    // Handle error appropriately
    e.printStackTrace();
}
```

This will send a message to GoGlove to set the button configuration for the first button to send the Play/Pause command when it is pressed

###Events
Events will be sent to the specified Event Handler class when one is received.

The following information will be sent with each Event from GoGlove:

<p><b>CONNECTED</b>: NONE</p>
<p><b>DISCONNECTED</b>: NONE</p>
<p><b>BUTTON_PRESS_EVENT</b>: "button" wil hold an integer value specifying the button that was pressed</p>
<p></p>
*NOTE: You will only receive the BUTTON_PRESS_EVENT if a button is configured as NOTIFY

When sending en event to GoGlove, the "type" must always be specified. The following information must be also be provided for each Event sent to GoGlove:

<p><b>BUTTON_CONFIGURATION</b>:
 <ul>
 <li>"button": one of the values from GoGloveButtons</li>
 <li>"action": one of the actions from GoGloveActions</li>
 </ul>
</p>
<p><b>ACTIVATION_CONFIGURATION</b>:
 <ul>
 <li>"time": the time, in seconds, the activation will wait for inputs before deactivating</li>
 </ul>
</p>
<p></p>

