package com.example.myapplication.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

public class StealthManager {
    private static final String TAG = "StealthManager";
    
    // Define multiple launcher aliases for stealth mode
    private static final String[] LAUNCHER_ALIASES = {
        "com.example.myapplication.Calculator",
        "com.example.myapplication.Settings", 
        "com.example.myapplication.Gallery",
        "com.example.myapplication.Music",
        "com.example.myapplication.Notes"
    };
    
    private static final String MAIN_LAUNCHER = "com.example.myapplication.SharingActivity";
    
    public static void enableStealthMode(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            
            // Disable main launcher
            ComponentName mainComponent = new ComponentName(context, MAIN_LAUNCHER);
            packageManager.setComponentEnabledSetting(
                mainComponent,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            );
            
            // Enable a random stealth launcher
            int randomIndex = (int) (Math.random() * LAUNCHER_ALIASES.length);
            String stealthLauncher = LAUNCHER_ALIASES[randomIndex];
            
            ComponentName stealthComponent = new ComponentName(context, stealthLauncher);
            packageManager.setComponentEnabledSetting(
                stealthComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            );
            
            Log.d(TAG, "Stealth mode enabled with launcher: " + stealthLauncher);
            
        } catch (Exception e) {
            Log.e(TAG, "Error enabling stealth mode", e);
        }
    }
    
    public static void disableStealthMode(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            
            // Enable main launcher
            ComponentName mainComponent = new ComponentName(context, MAIN_LAUNCHER);
            packageManager.setComponentEnabledSetting(
                mainComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            );
            
            // Disable all stealth launchers
            for (String alias : LAUNCHER_ALIASES) {
                ComponentName stealthComponent = new ComponentName(context, alias);
                packageManager.setComponentEnabledSetting(
                    stealthComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                );
            }
            
            Log.d(TAG, "Stealth mode disabled");
            
        } catch (Exception e) {
            Log.e(TAG, "Error disabling stealth mode", e);
        }
    }
    
    public static void hideAppFromLauncher(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            
            // Disable main launcher to hide from app drawer
            ComponentName mainComponent = new ComponentName(context, MAIN_LAUNCHER);
            packageManager.setComponentEnabledSetting(
                mainComponent,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            );
            
            Log.d(TAG, "App hidden from launcher");
            
        } catch (Exception e) {
            Log.e(TAG, "Error hiding app from launcher", e);
        }
    }
    
    public static boolean isStealthModeEnabled(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ComponentName mainComponent = new ComponentName(context, MAIN_LAUNCHER);
            
            int state = packageManager.getComponentEnabledSetting(mainComponent);
            return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking stealth mode status", e);
            return false;
        }
    }
}
