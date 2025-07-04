package com.example.myapplication.admin;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Device Admin Receiver for elevated permissions
 * This enables advanced system-level controls
 */
public class DeviceOwnerReceiver extends DeviceAdminReceiver {
    private static final String TAG = "DeviceOwnerReceiver";

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Log.d(TAG, "Device Admin enabled");
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Log.d(TAG, "Device Admin disabled");
    }

    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        super.onProfileProvisioningComplete(context, intent);
        Log.d(TAG, "Profile provisioning complete");
        
        // Device is now managed - can perform elevated operations
        DeviceOwnerManager.getInstance().onDeviceOwnerSet(context);
    }

    /**
     * Get the component name for this device admin receiver
     */
    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context, DeviceOwnerReceiver.class);
    }

    /**
     * Check if this app is the device owner
     */
    public static boolean isDeviceOwner(Context context) {
        try {
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            return dpm != null && dpm.isDeviceOwnerApp(context.getPackageName());
        } catch (Exception e) {
            Log.e(TAG, "Error checking device owner status", e);
            return false;
        }
    }

    /**
     * Check if app has device admin privileges
     */
    public static boolean isDeviceAdmin(Context context) {
        try {
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName adminComponent = getComponentName(context);
            return dpm != null && dpm.isAdminActive(adminComponent);
        } catch (Exception e) {
            Log.e(TAG, "Error checking device admin status", e);
            return false;
        }
    }
}
