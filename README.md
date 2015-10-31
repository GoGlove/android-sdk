<p align="center" >
<img src="http://goglove.io/static/img/Logo-horizontal.png" alt="GoGlove" title="GoGlove">
</p>

GoGlove Android SDK
==========
GoGlove is a wearable, wireless remote to control your smartphone or other mobilde devices or cameras. It comes with a standalone remote that can be used without the glove in warmer weather.

The Android SDK is meant to allow developers to interact with GoGlove through their own apps. This library will allow you to detect or be alerted if GoGlove is currently present, configure which buttons perform which action, and be alerted when a specific button is pressed.

These libraries should be added to your app "as-is", then they can be used as below.

###Instantiation
To use the GoGlove SDK, the first step is to start the service. To do this, you must first define a GoGloveSDK instance.
```Java
import com.etu.GoGloveSDK.BLEDeviceInfoList;
import com.etu.GoGloveSDK.GloveSDK;

private GloveSDK goglove;
```

If you wish to receive events from GoGlove, you must make your main class implement java.util.Observer by declaring it as
```Java
public class MyMainActivity extends Activity implements Observer {
```

Next, you must implement the necessary class to handle getting updates from GoGloveSDK
```Java
@Override
public void update(Observable observable, Object data) {

}
```

You can now declare a new GoGloveSDK when you wish to start using the functions:
```Java
goglove = new GloveSDK(this);
```

When you are done using the SDK, and your Activity or instance will be closed, first unbind from the SDK:
```Java
goglove.unbind();
```

###Actions
The following actions are allowed for each button of GoGlove
```Java
public enum GoGloveAction {
    PLAY_PAUSE, //Will send the HID Play/Paude media key
    NEXT_TRACK, //Will send the Next Track media key
    PREVIOUS_TRACK, //Will send the Previous Track media key
    VOLUME_UP, //Will send the Volume Up media key
    VOLUME_DOWN, //Will send the Volume Down media key
    ACTIVATE, //Will activate the glove so commands can be entered
    DEACTIVATE, //Notice that the glove is no longer active
    NOTIFY, //Will only send the event to the registered Handler, will not send any media key
    PUBLISH //Will publish an event to the Spark cloud with the button number and action type
}
```
###Buttons
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

Each button can have different types of actions as well, they are:
```Java
public enum GoGloveButtonPressType {
    SINGLE_TAP,
    DOUBLE_TAP
}
```

###Messages
The following is the structure for event types that come from GoGlove
```Java
public enum GoGloveMessageType {
    CONNECT, //Event to tell the service to connect to a specific GoGLove
    DISCONNECT, //Event to tell the service to disconnect from a specific GoGlove
    GET_BLE_LIST, //Get the list of all BLE devices connected
    CONNECTED, //Event sent when a GoGlove is connected
    DISCONNECTED, //Event sent when a GoGlove is disconnected
    BUTTON_PRESS_EVENT, //Event sent when a button is pressed on a connected GoGlove
    BUTTON_CONFIGURATION, //Event that can be sent to GoGlove to change the button configuration
    BUTTON_QUERY, //Event that can be sent to GoGlove to poll the command of a button
    BUTTON_QUERY_RESPONSE, //Event that comes back from GoGlove in repsonse to a BUTTON_QUERY
    ACTIVATION_CONFIGURATION, //Event that can be sent to GoGlove to change the activation timeout
} 
```

###Functions
The following functions are available to connect to and control a GoGlove:
```Java
    //! GloveSDK Constructor
    /*!
      Starts a background service that allows you to control and list attached GoGloves
    */
	public GloveSDK(Context context);

	//! Unbind
    /*!
      This function unbinds the activity from the underlying service. Must be called when you leave any Activity that as called the constructor
    */
	public void unbind();

	//! Stop
    /*!
      This function stops the underlying service. Should be called when you wich the application to exit and GoGLove service to end
    */
	public void stop();

	//! BLE Device Info List
    /*!
      Returns a list of all BLE Devices attached
    */
	public BLEDeviceInfoList getConnectedDevices;

	//! Connect
    /*!
      Connects to a specific BLE device by the DeviceInfo class
    */
	public void connect(BLEDeviceInfo device);

	//! Connect
    /*!
      Connects to a specific BLE device by MAC address
    */
	public void connect(String address);

	//! Disconnect
    /*!
      Disconnects from any connected GoGlove
    */
	public void disconnect();

	//! Configure Button
    /*!
      Allows you to specify the action for one button
    */
	public void configureButton(GoGloveButtons button, GoGloveButtonPressType buttonPressType, GoGloveAction action);

	//! Configure Activation Timeout
    /*!
      Configures how long the activation timer will last
    */
	public void configureActivationTimeout(int timeoutSeconds);
```

