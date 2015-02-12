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
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;
import android.os.RemoteException;

public class BackgroundGpsPlugin extends CordovaPlugin {
	
	
	/** Messenger for communicating with service. */
	Messenger mService = null;

	/** Some text view we are using to show state information. */
	TextView mCallbackText;

	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler());

	 // Event types for callbacks
	private enum Event {
		ACTIVATE, DEACTIVATE, FAILURE, RUNINBACKGROUND, RUNINFOREGROUND, MESSAGE, ENABLE, DISABLE
	}
	
	 // Plugin namespace
	private static final String JS_NAMESPACE = "window.plugins.backgroundGeoLocation";
	
    private static final String TAG = "BackgroundGpsPlugin";
    
    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_ENABLE = "enable";
    public static final String ACTION_DISABLE = "disable";
    public static final String ACTION_CONFIGURE = "configure";
    public static final String ACTION_SET_CONFIG = "setConfig";
    
    
    private Intent updateServiceIntent;

	/** Flag indicating whether we have called bind on the service. */
	boolean isActivated = false;
    private Boolean isEnabled = false;
    private Boolean isInBackGround = false;
    
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
    private String TRUE = "true";
    private String ERROR_1 = "Can't start service before it is enabled";

    
	/**
	 * Handler of incoming messages from service.
	 */
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			try {
				String msg_text;
				switch (msg.what) {
				case LocationUpdateService.MSG_SET_VALUE:
					fireEvent(Event.MESSAGE, "4"+msg.what);
					//  mCallbackText.setText("Received from service: " + msg.arg1);
					break;
				case LocationUpdateService.MSG_UPDATE_LOCATION:
					fireEvent(Event.MESSAGE, "5"+msg.what);
					msg_text = msg.obj.toString();
					geolocationfound(msg_text);
					break;
				default:
					super.handleMessage(msg);
				}
			} catch (Exception e) {
				fireEvent(Event.MESSAGE, "2"+msg.what);
			}
		}
	}

	/**
	 * Class for interacting with the main interface of the service.
	 */
	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className,
	            IBinder service) {
	    	try {
	    		// This is called when the connection with the service has been
	    		// established, giving us the service object we can use to
	    		// interact with the service.  We are communicating with our
	    		// service through an IDL interface, so get a client-side
	    		// representation of that from the raw service object.
	    		mService = new Messenger(service);
	    		//	mCallbackText.setText("Attached.");

	    		// We want to monitor the service for as long as we are
	    		// connected to it.
	    		Message msg = Message.obtain(null,LocationUpdateService.MSG_REGISTER_CLIENT);
	    		msg.replyTo = mMessenger;
	    		mService.send(msg);

	    		// Give it some value as an example.
	    		msg = Message.obtain(null,LocationUpdateService.MSG_SET_VALUE, this.hashCode(), 0);
	    		mService.send(msg);
				fireEvent(Event.MESSAGE, "1");

	        } catch (RemoteException e) {
	            // In this case the service has crashed before we could even
	            // do anything with it; we can count on soon being
	            // disconnected (and then reconnected if it can be restarted)
	            // so there is no need to do anything here.
	        } catch (Exception e) {
				fireEvent(Event.MESSAGE, "2");
	        	// do nothing
	        }

	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        mService = null;
	        //  mCallbackText.setText("Disconnected.");
			fireEvent(Event.MESSAGE, "3");
	    }
	};
	
	/**
	 * 
	 * @author erik
	 * from http://developer.android.com/reference/android/app/Service.html
	 */

    
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {
        String result = stopOnTerminate;

        if (ACTION_START.equalsIgnoreCase(action)) {
        	
        	// start location service
        	result = startServiceIfNotRunning();

        } else if (ACTION_STOP.equalsIgnoreCase(action)) {
        	
        	// stop location service
        	result = stopServiceIfRunning();
        	
        } else if (ACTION_ENABLE.equalsIgnoreCase(action)) {
        	
        	// enable service (service is allowed to start if the app comes running in background)
        	result = enableService();
        	
        } else if (ACTION_DISABLE.equalsIgnoreCase(action)) {
        	
        	// disable service (service is not allowed to start if service is running it will be stopped)
        	result = disableService();
        	
        } else if (ACTION_CONFIGURE.equalsIgnoreCase(action)) {
            result = TRUE;
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
            	result = "authToken/url required as parameters: " + e.getMessage();
            }
        } else if (ACTION_SET_CONFIG.equalsIgnoreCase(action)) {
            result = TRUE;
            // TODO reconfigure Service
        }
        
        if (TRUE.equalsIgnoreCase(result)) {
        	callbackContext.success();
        	return true;
        } else {
        	callbackContext.error(result);
        	return false;
        }
    }

    /**
     * Start the Location service if not running
     */
    public String startServiceIfNotRunning() {

        Activity activity = this.cordova.getActivity();
        updateServiceIntent = new Intent(activity, LocationUpdateService.class);

    	// check if service is enabled
    	if (isEnabled != true) {
    		// check if the service is activated
    		if ( isActivated == true) {
    			// stop service
    			stopServiceIfRunning();
    		}
    		return ERROR_1;
    	}

    	// check if the service is activated
    	if ( isActivated == true) {
    		return TRUE;
    	}
    	
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
        
        activity.bindService(updateServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
        isActivated = true;
        fireEvent(Event.ACTIVATE, null);
        return TRUE;
    }
    
    /**
     * Stop the Location service if running
     */
    public String stopServiceIfRunning() {

        Activity activity = this.cordova.getActivity();

    	// Check if the service is running
    	if (isActivated == true) {
    		activity.unbindService(mConnection);
    		isActivated = false;
    		fireEvent(Event.DEACTIVATE, null);
    		return TRUE;
    	} else {
    		return TRUE;
    	}
    }

    /**
     * Enable service (service is allowed to start if the app comes running in background)
     */
    public String enableService() {
    	isEnabled = true;
    	fireEvent(Event.ENABLE, null);

    	if (isInBackGround == true) {
    		// start service
    		startServiceIfNotRunning();
    	} else {
    		// stop service
    		stopServiceIfRunning();
    	}
    	return TRUE;
    }
    
    /**
     * Disable service (service is not allowed to start if service is running it will be stopped)
     */
    public String disableService() {
    	isEnabled = false;
    	fireEvent(Event.DISABLE, null);

    	// stop service
    	stopServiceIfRunning();
    	return TRUE;
    }
    
    /**
    * Called when app switch to background
    *
    * @param multitasking
    * Flag indicating if multitasking is turned on for app
    */
    @Override
    public void onPause(boolean multitasking) {
    	isInBackGround = true;
    	fireEvent(Event.RUNINBACKGROUND, null);
    	if (isEnabled == true) {
    		startServiceIfNotRunning();
    	} else {
    		stopServiceIfRunning();
    	}
    	super.onPause(multitasking);
    }
    
    /**
    * Called when app starts again
    *
    * @param multitasking
    * Flag indicating if multitasking is turned on for app
    */
    @Override
    public void onResume(boolean multitasking) {
    	isInBackGround = false;
    	fireEvent(Event.RUNINFOREGROUND, null);
		stopServiceIfRunning();
    	super.onResume(multitasking);
    }
    
    /**
     * Override method in CordovaPlugin.
     * Checks to see if it should turn off
     */
    @Override
    public void onDestroy() {
        stopServiceIfRunning();
        super.onDestroy();
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
    	if (event == Event.FAILURE)
    		return;
    	switch (event) {
    	case ACTIVATE:
    		eventName = "activate"; break;
    	case DEACTIVATE:
    		eventName = "deactivate"; break;
    	case RUNINFOREGROUND:
    		eventName = "runinforeground"; break;
    	case RUNINBACKGROUND:
    		eventName = "runinbackground"; break;
    	case ENABLE:
    		eventName = "enable"; break;
    	case DISABLE:
    		eventName = "disable"; break;
    	case MESSAGE:
    		eventName = "message"; break;
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
    private void geolocationfound(String params) {
     	String fn = String.format("setTimeout('%s.callbackFn(%s)',0);",
    			JS_NAMESPACE, '"'+params+'"');
    	final String js = fn;
    	cordova.getActivity().runOnUiThread(new Runnable() {
    		@Override
    		public void run() {
    			webView.loadUrl("javascript:" + js);
    		}
    	});
  
    }
}
