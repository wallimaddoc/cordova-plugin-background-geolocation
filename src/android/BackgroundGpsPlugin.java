package com.tenforwardconsulting.cordova.bgloc;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;

import android.os.Handler;
import android.os.Messenger;
import android.os.Message;
import android.widget.TextView;
import android.widget.Toast;


public class BackgroundGpsPlugin extends CordovaPlugin {
	
	
	/** Messenger for communicating with service. */
	Messenger mService = null;
	/** Flag indicating whether we have called bind on the service. */
	boolean mIsBound;
	/** Some text view we are using to show state information. */
	TextView mCallbackText;

	/**
	 * Handler of incoming messages from service.
	 */
	class IncomingHandler extends Handler {
	    @Override
	    public void handleMessage(Message msg) {
	        switch (msg.what) {
	            case MessengerService.MSG_SET_VALUE:
	                mCallbackText.setText("Received from service: " + msg.arg1);
	                break;
	            default:
	                super.handleMessage(msg);
	        }
	    }
	}

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler());

	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className,
	            IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  We are communicating with our
	        // service through an IDL interface, so get a client-side
	        // representation of that from the raw service object.
	        mService = new Messenger(service);
	        mCallbackText.setText("Attached.");

	        // We want to monitor the service for as long as we are
	        // connected to it.
	        try {
	            Message msg = Message.obtain(null,
	                    MessengerService.MSG_REGISTER_CLIENT);
	            msg.replyTo = mMessenger;
	            mService.send(msg);

	            // Give it some value as an example.
	            msg = Message.obtain(null,
	                    MessengerService.MSG_SET_VALUE, this.hashCode(), 0);
	            mService.send(msg);
	        } catch (RemoteException e) {
	            // In this case the service has crashed before we could even
	            // do anything with it; we can count on soon being
	            // disconnected (and then reconnected if it can be restarted)
	            // so there is no need to do anything here.
	        }

	        // As part of the sample, tell the user what happened.
	        Toast.makeText(Binding.this,"Connect"",
	                Toast.LENGTH_SHORT).show();
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        mService = null;
	        mCallbackText.setText("Disconnected.");

	        // As part of the sample, tell the user what happened.
	        Toast.makeText(Binding.this, "Disconnect",
	                Toast.LENGTH_SHORT).show();
	    }
	};
	
	/**
	 * 
	 * @author erik
	 *
	 */
	
	 // Event types for callbacks
	private enum Event {
		ACTIVATE, DEACTIVATE, FAILURE, RUNINBACKGROUND, RUNINFOREGROUND
	}
	
	
	 // Plugin namespace
	private static final String JS_NAMESPACE = "window.plugins.backgroundGeoLocation";
	
    private static final String TAG = "BackgroundGpsPlugin";
    
    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_CONFIGURE = "configure";
    public static final String ACTION_SET_CONFIG = "setConfig";
    
    
    private Intent updateServiceIntent;

    private Boolean isEnabled = false;

    private String url;
    private String params;
    private String headers;
    private String stationaryRadius = "30";
    private String desiredAccuracy = "100";
    private String distanceFilter = "30";
    private String locationTimeout = "60";
    private String isDebugging = "false";
    private String notificationTitle = "Background tracking";
    private String notificationText = "ENABLED";
    private String stopOnTerminate = "false";

    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {
        Activity activity = this.cordova.getActivity();
        Boolean result = false;
        updateServiceIntent = new Intent(activity, LocationUpdateService.class);

        if (ACTION_START.equalsIgnoreCase(action) && !isEnabled) {
            result = true;
            if (params == null || headers == null || url == null) {
                callbackContext.error("Call configure before calling start");
            } else {
                callbackContext.success();
                updateServiceIntent.putExtra("url", url);
                updateServiceIntent.putExtra("params", params);
                updateServiceIntent.putExtra("headers", headers);
                updateServiceIntent.putExtra("stationaryRadius", stationaryRadius);
                updateServiceIntent.putExtra("desiredAccuracy", desiredAccuracy);
                updateServiceIntent.putExtra("distanceFilter", distanceFilter);
                updateServiceIntent.putExtra("locationTimeout", locationTimeout);
                updateServiceIntent.putExtra("desiredAccuracy", desiredAccuracy);
                updateServiceIntent.putExtra("isDebugging", isDebugging);
                updateServiceIntent.putExtra("notificationTitle", notificationTitle);
                updateServiceIntent.putExtra("notificationText", notificationText);
                updateServiceIntent.putExtra("stopOnTerminate", stopOnTerminate);

                activity.startService(updateServiceIntent);
                fireEvent(Event.ACTIVATE, null);
                isEnabled = true;
            }
        } else if (ACTION_STOP.equalsIgnoreCase(action)) {
            isEnabled = false;
            result = true;
            activity.stopService(updateServiceIntent);
            fireEvent(Event.DEACTIVATE, null);
            callbackContext.success();
        } else if (ACTION_CONFIGURE.equalsIgnoreCase(action)) {
            result = true;
            try {
                // Params.
                //    0       1       2           3               4                5               6            7           8                9               10              11
                //[params, headers, url, stationaryRadius, distanceFilter, locationTimeout, desiredAccuracy, debug, notificationTitle, notificationText, activityType, stopOnTerminate]
                this.params = data.getString(0);
                this.headers = data.getString(1);
                this.url = data.getString(2);
                this.stationaryRadius = data.getString(3);
                this.distanceFilter = data.getString(4);
                this.locationTimeout = data.getString(5);
                this.desiredAccuracy = data.getString(6);
                this.isDebugging = data.getString(7);
                this.notificationTitle = data.getString(8);
                this.notificationText = data.getString(9);
                this.stopOnTerminate = data.getString(11);
            } catch (JSONException e) {
                callbackContext.error("authToken/url required as parameters: " + e.getMessage());
            }
        } else if (ACTION_SET_CONFIG.equalsIgnoreCase(action)) {
            result = true;
            // TODO reconfigure Service
            callbackContext.success();
        }

        return result;
    }

    /**
    * Called when the system is about to start resuming a previous activity.
    *
    * @param multitasking
    * Flag indicating if multitasking is turned on for app
    */
    @Override
    public void onPause(boolean multitasking) {
    	super.onPause(multitasking);
    	fireEvent(Event.RUNINBACKGROUND, null);
    }
    /**
    * Called when the activity will start interacting with the user.
    *
    * @param multitasking
    * Flag indicating if multitasking is turned on for app
    */
    @Override
    public void onResume(boolean multitasking) {
    	super.onResume(multitasking);
    	fireEvent(Event.RUNINFOREGROUND, null);
    }
    
    /**
     * Override method in CordovaPlugin.
     * Checks to see if it should turn off
     */
    public void onDestroy() {
        Activity activity = this.cordova.getActivity();
    	fireEvent(Event.DEACTIVATE, null);

        if(isEnabled && stopOnTerminate.equalsIgnoreCase("true")) {
            activity.stopService(updateServiceIntent);
        }
    }
    
    /**
    * Fire vent with some parameters inside the web view.
    *
    * @param event
    * The name of the event
    * @param params
    * Optional arguments for the event
    */
    private void fireEvent (Event event, String params) {
    	String eventName;
    	if (event != Event.FAILURE)
    		return;
    	switch (event) {
    	case ACTIVATE:
    		eventName = "activate"; break;
    	case DEACTIVATE:
    		eventName = "deactivate"; break;
    	case RUNINFOREGROUND:
    		eventName = "runinforeground"; break;
    	case RUNINBACKGROUND:
    		eventName = "rundinbackground"; break;
    	default:
    		eventName = "failure";
    	}
    	String active = event == Event.ACTIVATE ? "true" : "false";
    	String flag = String.format("%s._isActive=%s;",
    			JS_NAMESPACE, active);
    	String fn = String.format("setTimeout('%s.on%s(%s)',0);",
    			JS_NAMESPACE, eventName, params);
    	final String js = flag + fn;
    	cordova.getActivity().runOnUiThread(new Runnable() {
    		@Override
    		public void run() {
    			webView.loadUrl("javascript:" + js);
    		}
    	});
    }
    

    /**
     * The settings for the new/updated notification.
     *
     * @return
     * updateSettings if set or default settings
     */
    private void geolocationfound(JSONObject params) {
     	String fn = String.format("setTimeout('%s.callbackFn(%s)',0);",
    			JS_NAMESPACE, '"'+params.toString()+'"');
    	final String js = fn;
    	cordova.getActivity().runOnUiThread(new Runnable() {
    		@Override
    		public void run() {
    			webView.loadUrl("javascript:" + js);
    		}
    	});
  
    }
}
