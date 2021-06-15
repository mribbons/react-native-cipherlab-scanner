/*
The MIT License (MIT)
Copyright (c) 2017 Michael Ribbons
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

// MDR 13/02/2017 - Use gradlew build -x lint --stacktrace to build this without deploying

package au.com.micropacific.react.cipherlab;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.CatalystInstance;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.IllegalViewOperationException;

import android.widget.Toast;
import android.app.Activity;

import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import javax.annotation.Nullable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;

import com.cipherlab.barcode.*;
import com.cipherlab.barcode.decoder.*;
import com.cipherlab.barcode.decoderparams.*;
import com.cipherlab.barcodebase.*;

import android.util.Log;
import java.io.*;
import org.json.JSONArray;

import au.com.micropacific.react.cipherlab.IModuleCounter;

public class CipherLabScannerModule extends ReactContextBaseJavaModule {

    private ReactApplicationContext _reactContext;

    public static final String DEBUG_TAG = "CipherLabScanner";
    public static final String EVENT_TAG = "CIPHERLAB";

    private static final String DURATION_SHORT_KEY = "SHORT";
    private static final String DURATION_LONG_KEY = "LONG";

    private com.cipherlab.barcode.ReaderManager mReaderManager;
    private DataReceiver mDataReceiver;

    public static IModuleCounter MainActivity;

    public CipherLabScannerModule(ReactApplicationContext reactContext) {
        super(reactContext);

        this._reactContext = reactContext;
    }

@Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();
    constants.put(DURATION_SHORT_KEY, Toast.LENGTH_SHORT);
    constants.put(DURATION_LONG_KEY, Toast.LENGTH_LONG);
    return constants;
  }    

    @Override
    public String getName() {
        return "CipherLabScannerModule";
    }

@ReactMethod
  public void show(String message, int duration) {
    Toast.makeText(getReactApplicationContext(), "" + message, duration).show();
  }

  private void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
        CatalystInstance instance = reactContext.getCatalystInstance();

        

        if (instance.isDestroyed())
        {
            Log.v(DEBUG_TAG, "skipping call, isDestroyed");

            if (this.mDataReceiver != null)
            {
                try {            
                    //((Context)this._reactContext.getBaseContext()).unregisterReceiver(mDataReceiver);
                    Log.v(DEBUG_TAG, "sendEvent: unregisterReceiver()");
                    this.activity.unregisterReceiver(mDataReceiver);

                    mReaderManager.Release();
                    this.mReaderManager = null;
                    this.mDataReceiver = null;
                    this.activity = null;
                } catch (Exception e)
                {
                    Log.v(DEBUG_TAG, "Error in unregister: " + e.toString());
                }
            } else {
                Log.v(DEBUG_TAG, "mDataReceiver already null.");
            }

            return;
        }

        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(EVENT_TAG + "." + eventName, params);
  }

    private IntentFilter filter = null;
    private Activity activity;
    private static BcReaderType mReaderType;

    @ReactMethod
    public void initialise() {
        this.initialise(true);
    }

	public void initialise(boolean checkRegisteredModules)
	{
        Log.v(CipherLabScannerModule.DEBUG_TAG, "CipherLabScanner.initialise()");
        
        if (MainActivity != null)
            Log.v(CipherLabScannerModule.DEBUG_TAG, "module size: " + MainActivity.getModuleSize() + ", " + checkRegisteredModules);

        if (checkRegisteredModules && MainActivity != null && MainActivity.getModuleSize() > 0) {
            // MDR 27/04/2018 - Previous copy already exists, js code will catch RS30.init event when that versions init is called
            Log.v(CipherLabScannerModule.DEBUG_TAG, "Module already registered, deferring");            
			Log.v(CipherLabScannerModule.DEBUG_TAG, "RS30.init 2");
            this.initCallback();
            return;
        } else {
        }

        sendEvent(_reactContext, "setupStarted", null);

		try
		{
			//Log.v(CipherLabScannerModule.DEBUG_TAG, "com.cipherlab.barcode.GeneralString.Intent_SOFTTRIGGER_DATA: " + com.cipherlab.barcode.GeneralString.Intent_SOFTTRIGGER_DATA);
            Activity activity = null;
			if ((activity = getCurrentActivity()) == null)
			{
				Log.v(CipherLabScannerModule.DEBUG_TAG, "getCurrentActivity() is null");
			} else {
				Log.v(CipherLabScannerModule.DEBUG_TAG, "getCurrentActivity() is something");
			}

			filter = new IntentFilter(); 
			filter.addAction(com.cipherlab.barcode.GeneralString.Intent_SOFTTRIGGER_DATA);
			filter.addAction(com.cipherlab.barcode.GeneralString.Intent_PASS_TO_APP);
			filter.addAction(com.cipherlab.barcode.GeneralString.Intent_READERSERVICE_CONNECTED);
			
			

            // MDR 26/04/2018 - Should be handled in Intent_READERSERVICE_CONNECTED handler, but we never get that intent
            // com.cipherlab.barcode.decoderparams.ReaderOutputConfiguration settings = new ReaderOutputConfiguration();
            // settings.enableKeyboardEmulation = KeyboardEmulationType.None;
            // mReaderManager.Set_ReaderOutputConfiguration(settings);

			mDataReceiver = new DataReceiver(this, null, _reactContext, checkRegisteredModules);
			activity.registerReceiver(mDataReceiver, filter);

            mReaderManager = ReaderManager.InitInstance(activity);
            //Log.v(DEBUG_TAG, "Service running: " + mReaderManager.GetReaderType());

            this.activity = activity;

            if (mReaderManager != null)
            {
                mDataReceiver.setReaderManager(mReaderManager);
                this.mReaderType = mReaderManager.GetReaderType();
                Log.v(CipherLabScannerModule.DEBUG_TAG, "Got reader manager: " + mReaderType);
                //Log.v(CipherLabScannerModule.DEBUG_TAG, "RS30.init 3");
                //this.initCallback();
            } else {
                Log.v(CipherLabScannerModule.DEBUG_TAG, "Null ReaderManager, no scanner?");
            }

			//Log.v(CipherLabScannerModule.DEBUG_TAG, "Got reader");
		} catch (Exception e)
		{
			StringWriter writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter( writer );
			e.printStackTrace( printWriter );
			printWriter.flush();

			String stackTrace = writer.toString();

			Log.v(CipherLabScannerModule.DEBUG_TAG, "Error starting reader manager: " + stackTrace);
		}

        if (MainActivity != null && MainActivity.getModuleSize() == 0)
            MainActivity.registerModule(this);

		Log.v(CipherLabScannerModule.DEBUG_TAG, "CipherLabScanner.initialise() Done");
	}

    public void initCallback()
    {
        Log.v(CipherLabScannerModule.DEBUG_TAG, "initCallback");
        sendEvent(_reactContext, "initEvent", null);
    }

    public void receiveData(String barcode, int barcodeTypeInt, byte[] binary)
    {
        BcDecodeType barcodeType = BcDecodeType.valueOf(barcodeTypeInt);

        barcode = barcode.replace("\n", "");

        WritableMap params = Arguments.createMap();
        params.putString("barcode", barcode);
        params.putString("type", barcodeType.name().toUpperCase()
            .replace("CODE_128", "CODE128")
            .replace("CODE_39", "CODE39")
        );

        if (this.getBinaryDataEnabled()) {
            // MDR 24/04/2018 - byte array doesn't get passed through properly, convert to integer array
			WritableArray integers = Arguments.createArray();

			for (int x = 0; x < binary.length; x++) {
				integers.pushInt((int)binary[x] & 0xFF);
            }
            
            params.putArray("binary", integers);
        }

        Log.v(CipherLabScannerModule.DEBUG_TAG, "barcodeReadEvent(" + barcode + ", " + barcodeType.name().toUpperCase() + ")");

        sendEvent(_reactContext, "barcodeReadEvent", params);
    }

    public void receiveIntent(Intent intent) {
        String action = intent.getAction();
        WritableMap params = Arguments.createMap();
        params.putString("action", action);

        // MDR 05/08/2017 - This may cause some kind of memory leak, had an instance where it crashed app repeatedly until redeploy
        Bundle bundle = intent.getExtras();
        if (bundle != null)
        {
            for (String key : bundle.keySet())
            {
                Object value = bundle.get(key);
                params.putString(key, value != null ? value.toString() : "null" );
            }
        }

        //Log.v(CipherLabScannerModule.DEBUG_TAG, "receiveIntent(" + action + ")");
        sendEvent(_reactContext, "receiveIntent", params);
    }

    @ReactMethod
    public void requestScan()
    {
        mReaderManager.SoftScanTrigger();
    }

    private boolean enableBinary = false;

    @ReactMethod
    public void enableBinaryData() {
        Log.v(DEBUG_TAG, "enableBinaryData()");
        enableBinary = true;
    }

    @ReactMethod
    public void disableBinaryData() {
        enableBinary = false;
    }

    // MDR 21/08/2018 - data can't be returned over RN bridge
    public boolean getBinaryDataEnabled() {
        return enableBinary;
    }

    @ReactMethod
    public void releaseResources(boolean releaseReaderManager) {
        if (this.mDataReceiver != null)
        {
            try {
                //getCurrentActivity().unregisterReceiver(mDataReceiver);
                if (mDataReceiver != null)
                    activity.unregisterReceiver(mDataReceiver);

                //mReaderManager.ResetReaderToDefault();
                if (releaseReaderManager)
                    mReaderManager.Release();

                this.mReaderManager = null;
                this.mDataReceiver = null;
                this.activity = null;
            } catch (Exception e)
            {
                Log.v(DEBUG_TAG, "Error in releaseResources: " + e.toString());
            }
        } else {
            Log.v(DEBUG_TAG, "mDataReceiver already null.");
        }

        Log.v(DEBUG_TAG, "releaseResources() done.");
    }

    private static final String READER_TYPE = "ReaderType";

    public void onSaveInstanceState(Bundle outState) {
        if (mReaderType != null) {
            int current = mReaderType.getValue();
            Log.v(DEBUG_TAG, String.format("onSaveInstanceState = %d", new Object[]{Integer.valueOf(current)}));
            outState.putInt(READER_TYPE, mReaderType.getValue());
        }
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            int current = savedInstanceState.getInt(READER_TYPE);
            Log.v(DEBUG_TAG, String.format("onRestoreInstanceState = %d", new Object[]{Integer.valueOf(current)}));
        }
    }
}