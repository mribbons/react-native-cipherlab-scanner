package au.com.micropacific.react.cipherlab;

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

import com.cipherlab.rfid.GeneralString;
import com.cipherlab.rfid.RfidOutputConfiguration;
import com.cipherlab.rfidapi.RfidManager;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;


import au.com.micropacific.react.cipherlab.CipherLabScannerModule;

public class RfidDataReceiver extends BroadcastReceiver {
    private RfidManager mReaderManager;
    private boolean readerServiceConnected = false;
    private CipherLabScannerModule plugin;
    private Context mContext;

    public RfidDataReceiver(CipherLabScannerModule _plugin, RfidManager _ReaderManager, Context context, boolean checkRegisteredModules)
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
            if("com.cipherlab.clrfidservice.MainService".equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }

    public void setReaderManager(RfidManager rm) {
        this.mReaderManager = rm;

        int sleep_count = 1;

        while (!readerServiceConnected && sleep_count < 4) {
            try {
                if (sleep_count < 16)
                    Log.v(CipherLabScannerModule.DEBUG_TAG, "Waiting for " + GeneralString.Intent_RFIDSERVICE_CONNECTED + ", " + sleep_count + ", service running: " + this.isReaderServiceRunning());

                Thread.sleep(250 * sleep_count);
                sleep_count = sleep_count + 1;
            } catch (InterruptedException e) {
                Log.v(CipherLabScannerModule.DEBUG_TAG, "setReaderManager() InterruptionException");
            }
        }
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(GeneralString.Intent_RFIDSERVICE_TAG_DATA)) {
            //sending required rfid data
            int type = intent.getIntExtra(GeneralString.EXTRA_DATA_TYPE, -1);
            int response = intent.getIntExtra(GeneralString.EXTRA_RESPONSE, -1);
            double data_rssi = intent.getDoubleExtra(GeneralString.EXTRA_DATA_RSSI, 0);
            String PC = intent.getStringExtra(GeneralString.EXTRA_PC);
            String EPC = intent.getStringExtra(GeneralString.EXTRA_EPC);
            String TID = intent.getStringExtra(GeneralString.EXTRA_TID);
            String ReadData = intent.getStringExtra(GeneralString.EXTRA_ReadData);
            int EPC_length = intent.getIntExtra(GeneralString.EXTRA_EPC_LENGTH, 0);
            int TID_length = intent.getIntExtra(GeneralString.EXTRA_TID_LENGTH, 0);
            int ReadData_length = intent.getIntExtra(GeneralString.EXTRA_ReadData_LENGTH, 0);

            WritableMap params = Arguments.createMap();
            params.putInt("type", type);
            params.putInt("response", response);
            params.putInt("EPC_length", EPC_length);
            params.putInt("TID_length", TID_length);
            params.putInt("ReadData_length", ReadData_length);
            params.putString("PC", PC);
            params.putString("EPC", EPC);
            params.putString("TID", TID);
            params.putString("ReadData", ReadData);
            params.putDouble("data_rssi", data_rssi);
            this.plugin.receiveRfidData(params);
        } else if(intent.getAction().equals(GeneralString.Intent_RFIDSERVICE_CONNECTED)){

            try {
                while (mReaderManager == null) {
                    Log.v(CipherLabScannerModule.DEBUG_TAG, "Warning, reader manager is null in " + com.cipherlab.rfid.GeneralString.Intent_RFIDSERVICE_CONNECTED);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Log.v(CipherLabScannerModule.DEBUG_TAG, com.cipherlab.rfid.GeneralString.Intent_RFIDSERVICE_CONNECTED + ": InterruptionException");
            }

            Log.v(CipherLabScannerModule.DEBUG_TAG, "Got data, 3");

            Log.v(CipherLabScannerModule.DEBUG_TAG, "DataReceiver got reader: ");

            RfidOutputConfiguration settings = new RfidOutputConfiguration();
            mReaderManager.GetDataOutputSettings(settings);
            settings.KeyEventOutput = false;
            settings.InterCharDelay = 100;
            mReaderManager.SetDataOutputSettings(settings);

            readerServiceConnected = true;

            Log.v(CipherLabScannerModule.DEBUG_TAG, "scanner init 1");
            this.plugin.initCallback();
        } else {
            this.plugin.receiveIntent(intent);
        }
    }
}
