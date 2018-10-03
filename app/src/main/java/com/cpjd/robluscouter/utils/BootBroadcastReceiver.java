package com.cpjd.robluscouter.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.cpjd.robluscouter.sync.cloud.Service;

/**
 * Starts this app's service when the Android device boots up
 *
 * @since 1.0.1
 * @author Will Davies
 */

public class BootBroadcastReceiver extends BroadcastReceiver {
    static final String ACTION = "android.intent.action.BOOT_COMPLETED";
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction() == null) {
            Log.d("RSBS", "Unable to start Roblu Scouter background service.");
            return;
        }

        // BOOT_COMPLETED” start Service
        if (intent.getAction().equals(ACTION)) {
            //Service
            Log.d("RSBS", "Auto-starting RobluScouter service.");
            try {
                Intent serviceIntent = new Intent(context, Service.class);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent);
                else context.startService(serviceIntent);
            } catch(Exception e) {
                Log.d("RSBS", "Failed to auto-start RobluScouter service.");
            }
        }
    }
}
