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

package au.com.micropacific.react.cipherlab;

import com.cipherlab.barcode.*;
import com.cipherlab.barcode.decoder.*;
import com.cipherlab.barcode.decoderparams.*;
import com.cipherlab.barcodebase.*;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.*;
import java.util.Iterator;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;

/// Create a broadcast object to receive the intent coming from service.
public class DataReceiver extends BroadcastReceiver {

	private ReaderManager mReaderManager;
	private boolean readerServiceConnected = false;  
	private CipherLabScannerModule plugin;
	private Context mContext;

	public DataReceiver(CipherLabScannerModule _plugin, ReaderManager _ReaderManager, Context context, boolean checkRegisteredModules)
	{
		this.mReaderManager = _ReaderManager;
		this.plugin = _plugin;
		this.mContext = context;

		if (!checkRegisteredModules)
			readerServiceConnected = true;
	}

	public boolean isReaderServiceRunning() {
        ActivityManager manager = (ActivityManager)mContext.getSystemService("activity");
        Iterator var3 = manager.getRunningServices(2147483647).iterator();

        while(var3.hasNext()) {
            RunningServiceInfo service = (RunningServiceInfo)var3.next();
            if("com.cipherlab.clbarcodeservice.MainService".equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }

	public void setReaderManager(ReaderManager rm) {
		this.mReaderManager = rm;
		
		int sleep_count = 1;

		while (!readerServiceConnected && sleep_count < 4) {
			try {
				if (sleep_count < 16)
					// MDR 27/04/2018 - warning: mReaderManager.GetActive() can throw null reference exceptions (mReaderManagerAPI == null)
					//Log.v(CipherLabScannerModule.DEBUG_TAG, "Waiting for " + GeneralString.Intent_READERSERVICE_CONNECTED + ", " + sleep_count + ", service running: " + this.isReaderServiceRunning() + ", active: " + mReaderManager.GetActive());
					Log.v(CipherLabScannerModule.DEBUG_TAG, "Waiting for " + GeneralString.Intent_READERSERVICE_CONNECTED + ", " + sleep_count + ", service running: " + this.isReaderServiceRunning());

				Thread.sleep(250 * sleep_count);
				sleep_count = sleep_count + 1;
			} catch (InterruptedException e) {
				Log.v(CipherLabScannerModule.DEBUG_TAG, "setReaderManager() InterruptionException");
			}
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(GeneralString.Intent_SOFTTRIGGER_DATA) || intent.getAction().equals(GeneralString.Intent_PASS_TO_APP)) {
				
			byte [] binary = null;
			
			if (this.plugin.getBinaryDataEnabled())
				binary = intent.getByteArrayExtra(GeneralString.BcReaderDataArray);
			
			String data = intent.getStringExtra(GeneralString.BcReaderData);
            int iCodeType = intent.getIntExtra(GeneralString.BcReaderCodeType, 0);
			this.plugin.receiveData(data, iCodeType, binary);				
		} else if(intent.getAction().equals(GeneralString.Intent_READERSERVICE_CONNECTED)){				

			try {
				while (mReaderManager == null) {
					Log.v(CipherLabScannerModule.DEBUG_TAG, "Warning, reader manager is null in " + GeneralString.Intent_READERSERVICE_CONNECTED);
					Thread.sleep(500);
				}
			} catch (InterruptedException e) {
					Log.v(CipherLabScannerModule.DEBUG_TAG, GeneralString.Intent_READERSERVICE_CONNECTED + ": InterruptionException");
			}

			Log.v(CipherLabScannerModule.DEBUG_TAG, "Got data, 3");

			BcReaderType myReaderType =  mReaderManager.GetReaderType();

			Log.v(CipherLabScannerModule.DEBUG_TAG, "DataReceiver got reader: " + mReaderManager.GetReaderType());			

            ReaderOutputConfiguration settings = new ReaderOutputConfiguration();
            mReaderManager.Get_ReaderOutputConfiguration(settings);
            settings.enableKeyboardEmulation = KeyboardEmulationType.None;
            mReaderManager.Set_ReaderOutputConfiguration(settings);

			readerServiceConnected = true;

			Log.v(CipherLabScannerModule.DEBUG_TAG, "scanner init 1");
			this.plugin.initCallback();
		} else {
			this.plugin.receiveIntent(intent);
		}

	}
};