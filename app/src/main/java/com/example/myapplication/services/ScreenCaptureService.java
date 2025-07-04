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
        createNotificationChannel();
        Log.d(TAG, "Screen Capture Service created");
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
    
    // Create invisible notification channel for Android O and above
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "",
                    NotificationManager.IMPORTANCE_MIN);
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
    
    // Create invisible notification for foreground service
    private Notification createNotification() {
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
}
