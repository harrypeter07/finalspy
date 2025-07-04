package com.example.myapplication.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.ArrayList;
import java.util.List;

public class PermissionManager {
    
    // Required permissions for camera and microphone
    private static String[] CAMERA_AND_MIC_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };
    
    // Required permissions for location
    private static String[] LOCATION_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    
    // Required permissions for storage (for saving media)
    private static String[] STORAGE_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    
    // Callback interface for permission results
    public interface PermissionCallback {
        void onPermissionsGranted();
        void onPermissionsDenied(List<String> deniedPermissions);
    }
    
    // Request all permissions needed for the app
    public static void requestAllPermissions(Activity activity, PermissionCallback callback) {
        List<String> permissions = new ArrayList<>();
        
        // Add camera and mic permissions
        for (String permission : CAMERA_AND_MIC_PERMISSIONS) {
            permissions.add(permission);
        }
        
        // Add location permissions
        for (String permission : LOCATION_PERMISSIONS) {
            permissions.add(permission);
        }
        
        // Add storage permissions (for Android < 11)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            for (String permission : STORAGE_PERMISSIONS) {
                permissions.add(permission);
            }
        }
        
        // Add notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        
        Dexter.withContext(activity)
                .withPermissions(permissions)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            callback.onPermissionsGranted();
                        } else {
                            callback.onPermissionsDenied(report.getDeniedPermissionResponses()
                                    .stream()
                                    .map(response -> response.getPermissionName())
                                    .toList());
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .check();
    }
    
    // Check if camera and microphone permissions are granted
    public static boolean hasCameraAndMicPermissions(Context context) {
        for (String permission : CAMERA_AND_MIC_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    // Check if location permissions are granted
    public static boolean hasLocationPermissions(Context context) {
        for (String permission : LOCATION_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
