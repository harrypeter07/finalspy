package com.example.myapplication.admin;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Device Owner Manager for advanced system permissions
 * Handles one-time setup for permanent elevated access
 */
public class DeviceOwnerManager {
    private static final String TAG = "DeviceOwnerManager";
    private static DeviceOwnerManager instance;
    
    private boolean isDeviceOwner = false;
    private boolean isDeviceAdmin = false;
    private DevicePolicyManager devicePolicyManager;
    
    private DeviceOwnerManager() {}
    
    public static synchronized DeviceOwnerManager getInstance() {
        if (instance == null) {
            instance = new DeviceOwnerManager();
        }
        return instance;
    }
    
    /**
     * Initialize device owner capabilities
     */
    public void initialize(Context context) {
        devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        isDeviceOwner = DeviceOwnerReceiver.isDeviceOwner(context);
        isDeviceAdmin = DeviceOwnerReceiver.isDeviceAdmin(context);
        
        Log.d(TAG, "Device Owner: " + isDeviceOwner + ", Device Admin: " + isDeviceAdmin);
        
        if (isDeviceOwner || isDeviceAdmin) {
            enableAdvancedPermissions(context);
        }
    }
    
    /**
     * Called when device owner is successfully set
     */
    public void onDeviceOwnerSet(Context context) {
        isDeviceOwner = true;
        enableAdvancedPermissions(context);
        Log.d(TAG, "Device owner set - enabling advanced permissions");
    }
    
    /**
     * Enable all advanced permissions using device owner capabilities
     */
    private void enableAdvancedPermissions(Context context) {
        if (!isDeviceOwner && !isDeviceAdmin) {
            Log.w(TAG, "No device admin privileges - cannot enable advanced permissions");
            return;
        }
        
        try {
            // Enable location services programmatically
            enableLocationServices(context);
            
            // Grant runtime permissions
            grantRuntimePermissions(context);
            
            // Set app as non-removable (if device owner)
            if (isDeviceOwner) {
                setAppAsNonRemovable(context);
            }
            
            Log.d(TAG, "Advanced permissions enabled successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error enabling advanced permissions", e);
        }
    }
    
    /**
     * Enable location services programmatically
     */
    public boolean enableLocationServices(Context context) {
        try {
            if (isDeviceOwner || isDeviceAdmin) {
                // Method 1: Using DevicePolicyManager (Device Owner)
                if (isDeviceOwner && devicePolicyManager != null) {
                    ComponentName adminComponent = DeviceOwnerReceiver.getComponentName(context);
                    
                    // Enable location mode
                    devicePolicyManager.setSecureSetting(adminComponent, 
                        Settings.Secure.LOCATION_MODE, 
                        String.valueOf(Settings.Secure.LOCATION_MODE_HIGH_ACCURACY));
                    
                    Log.d(TAG, "Location services enabled via Device Policy Manager");
                    return true;
                }
                
                // Method 2: Using Settings.System (requires WRITE_SECURE_SETTINGS)
                if (hasWriteSecureSettingsPermission(context)) {
                    Settings.Secure.putInt(context.getContentResolver(),
                        Settings.Secure.LOCATION_MODE,
                        Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);
                    
                    Log.d(TAG, "Location services enabled via Secure Settings");
                    return true;
                }
                
                // Method 3: Reflection method for older devices
                return enableLocationViaReflection(context);
                
            } else {
                Log.w(TAG, "Cannot enable location services - no elevated permissions");
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error enabling location services", e);
            return false;
        }
    }
    
    /**
     * Enable location using reflection (for older Android versions)
     */
    private boolean enableLocationViaReflection(Context context) {
        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            
            // Use reflection to enable GPS
            Method method = locationManager.getClass().getMethod("setTestProviderEnabled", String.class, boolean.class);
            method.invoke(locationManager, LocationManager.GPS_PROVIDER, true);
            
            Log.d(TAG, "Location enabled via reflection");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Reflection method failed", e);
            return false;
        }
    }
    
    /**
     * Grant runtime permissions programmatically
     */
    private void grantRuntimePermissions(Context context) {
        if (!isDeviceOwner || devicePolicyManager == null) return;
        
        try {
            ComponentName adminComponent = DeviceOwnerReceiver.getComponentName(context);
            String packageName = context.getPackageName();
            
            // Permissions to grant automatically
            String[] permissions = {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            
            for (String permission : permissions) {
                try {
                    devicePolicyManager.setPermissionGrantState(adminComponent, packageName, permission,
                        DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);
                    Log.d(TAG, "Granted permission: " + permission);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to grant permission: " + permission, e);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error granting runtime permissions", e);
        }
    }
    
    /**
     * Make app non-removable (Device Owner only)
     */
    private void setAppAsNonRemovable(Context context) {
        if (!isDeviceOwner || devicePolicyManager == null) return;
        
        try {
            ComponentName adminComponent = DeviceOwnerReceiver.getComponentName(context);
            String packageName = context.getPackageName();
            
            // Prevent app from being uninstalled
            devicePolicyManager.setUninstallBlocked(adminComponent, packageName, true);
            
            Log.d(TAG, "App set as non-removable");
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting app as non-removable", e);
        }
    }
    
    /**
     * Enable screen capture permission permanently
     */
    public Intent getScreenCapturePermissionIntent(Context context) {
        MediaProjectionManager projectionManager = 
            (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        return projectionManager.createScreenCaptureIntent();
    }
    
    /**
     * Store screen capture permission for permanent use
     */
    public void storeScreenCapturePermission(Context context, Intent data, int resultCode) {
        // Store in SharedPreferences for persistent access
        context.getSharedPreferences("device_permissions", Context.MODE_PRIVATE)
            .edit()
            .putString("screen_capture_data", data.toUri(0))
            .putInt("screen_capture_result", resultCode)
            .putBoolean("screen_capture_granted", true)
            .apply();
        
        Log.d(TAG, "Screen capture permission stored permanently");
    }
    
    /**
     * Get stored screen capture permission
     */
    public boolean hasStoredScreenCapturePermission(Context context) {
        return context.getSharedPreferences("device_permissions", Context.MODE_PRIVATE)
            .getBoolean("screen_capture_granted", false);
    }
    
    /**
     * Get stored screen capture intent
     */
    public Intent getStoredScreenCaptureIntent(Context context) {
        try {
            String uriString = context.getSharedPreferences("device_permissions", Context.MODE_PRIVATE)
                .getString("screen_capture_data", null);
            if (uriString != null) {
                return Intent.parseUri(uriString, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing stored screen capture intent", e);
        }
        return null;
    }
    
    /**
     * Get stored screen capture result code
     */
    public int getStoredScreenCaptureResultCode(Context context) {
        return context.getSharedPreferences("device_permissions", Context.MODE_PRIVATE)
            .getInt("screen_capture_result", -1);
    }
    
    /**
     * Check if app has WRITE_SECURE_SETTINGS permission
     */
    private boolean hasWriteSecureSettingsPermission(Context context) {
        return context.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") 
            == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Get the ADB command to grant WRITE_SECURE_SETTINGS
     */
    public static String getADBCommand(String packageName) {
        return "adb shell pm grant " + packageName + " android.permission.WRITE_SECURE_SETTINGS";
    }
    
    /**
     * Get the ADB command to set as device owner
     */
    public static String getDeviceOwnerADBCommand(String packageName) {
        return "adb shell dpm set-device-owner " + packageName + "/.admin.DeviceOwnerReceiver";
    }
    
    // Getters
    public boolean isDeviceOwner() { return isDeviceOwner; }
    public boolean isDeviceAdmin() { return isDeviceAdmin; }
}
