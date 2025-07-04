package com.example.myapplication;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.myapplication.admin.DeviceOwnerManager;
import com.example.myapplication.admin.DeviceOwnerReceiver;
import com.example.myapplication.utils.PermissionManager;
import com.example.myapplication.utils.RemoteControlManager;

import java.util.List;

public class PermissionSetupActivity extends AppCompatActivity {
    private static final String TAG = "PermissionSetupActivity";
    
    // UI Components
    private LinearLayout permissionsList;
    private Button btnContinue;
    private TextView statusText;
    
    // Permission checkboxes
    private CheckBox cbBasicPermissions;
    private CheckBox cbDeviceAdmin;
    private CheckBox cbScreenCapture;
    private CheckBox cbBatteryOptimization;
    private CheckBox cbAccessibility;
    
    // Permission status
    private boolean basicPermissionsGranted = false;
    private boolean deviceAdminEnabled = false;
    private boolean screenCaptureGranted = false;
    private boolean batteryOptimizationDisabled = false;
    private boolean accessibilityEnabled = false;
    
    // Activity result launchers
    private ActivityResultLauncher<Intent> deviceAdminLauncher;
    private ActivityResultLauncher<Intent> screenCaptureLauncher;
    private ActivityResultLauncher<Intent> batteryOptimizationLauncher;
    private ActivityResultLauncher<Intent> accessibilityLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if setup has already been completed
        if (isSetupCompleted()) {
            // Skip setup and go directly to main app
            proceedToMainApp();
            return;
        }
        
        setContentView(R.layout.activity_permission_setup);
        
        initializeViews();
        setupActivityLaunchers();
        setupClickListeners();
        checkCurrentPermissions();
        updateUI();
    }
    
    private void initializeViews() {
        permissionsList = findViewById(R.id.permissions_list);
        btnContinue = findViewById(R.id.btn_continue);
        statusText = findViewById(R.id.status_text);
        
        cbBasicPermissions = findViewById(R.id.cb_basic_permissions);
        cbDeviceAdmin = findViewById(R.id.cb_device_admin);
        cbScreenCapture = findViewById(R.id.cb_screen_capture);
        cbBatteryOptimization = findViewById(R.id.cb_battery_optimization);
        cbAccessibility = findViewById(R.id.cb_accessibility);
    }
    
    private void setupActivityLaunchers() {
        // Device Admin launcher
        deviceAdminLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                deviceAdminEnabled = DeviceOwnerReceiver.isDeviceAdmin(this);
                updateUI();
                if (deviceAdminEnabled) {
                    Toast.makeText(this, "Device Admin enabled successfully!", Toast.LENGTH_SHORT).show();
                    // Initialize device owner manager
                    DeviceOwnerManager.getInstance().initialize(this);
                } else {
                    Toast.makeText(this, "Device Admin setup failed", Toast.LENGTH_SHORT).show();
                }
            }
        );
        
        // Screen Capture launcher
        screenCaptureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    screenCaptureGranted = true;
                    // Store the permission for permanent use
                    RemoteControlManager.getInstance().setScreenCapturePermission(
                        result.getData(), result.getResultCode());
                    Toast.makeText(this, "Screen capture permission granted!", Toast.LENGTH_SHORT).show();
                } else {
                    screenCaptureGranted = false;
                    Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show();
                }
                updateUI();
            }
        );
        
        // Battery Optimization launcher
        batteryOptimizationLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                checkBatteryOptimization();
                updateUI();
            }
        );
        
        // Accessibility launcher
        accessibilityLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                checkAccessibilityService();
                updateUI();
            }
        );
    }
    
    private void setupClickListeners() {
        // Basic Permissions
        findViewById(R.id.btn_basic_permissions).setOnClickListener(v -> requestBasicPermissions());
        
        // Device Admin
        findViewById(R.id.btn_device_admin).setOnClickListener(v -> requestDeviceAdmin());
        
        // Screen Capture
        findViewById(R.id.btn_screen_capture).setOnClickListener(v -> requestScreenCapture());
        
        // Battery Optimization
        findViewById(R.id.btn_battery_optimization).setOnClickListener(v -> requestBatteryOptimization());
        
        // Accessibility
        findViewById(R.id.btn_accessibility).setOnClickListener(v -> requestAccessibilityService());
        
        // Continue button
        btnContinue.setOnClickListener(v -> {
            if (allCriticalPermissionsGranted()) {
                proceedToMainApp();
            } else {
                showMissingPermissionsDialog();
            }
        });
        
        // ADB Instructions button
        findViewById(R.id.btn_adb_instructions).setOnClickListener(v -> showADBInstructions());
    }
    
    private void requestBasicPermissions() {
        PermissionManager.requestAllPermissions(this, new PermissionManager.PermissionCallback() {
            @Override
            public void onPermissionsGranted() {
                basicPermissionsGranted = true;
                Toast.makeText(PermissionSetupActivity.this, "Basic permissions granted!", Toast.LENGTH_SHORT).show();
                updateUI();
            }
            
            @Override
            public void onPermissionsDenied(List<String> deniedPermissions) {
                basicPermissionsGranted = false;
                Toast.makeText(PermissionSetupActivity.this, 
                    "Some basic permissions were denied. Please grant all permissions.", Toast.LENGTH_LONG).show();
                updateUI();
            }
        });
    }
    
    private void requestDeviceAdmin() {
        try {
            ComponentName adminComponent = DeviceOwnerReceiver.getComponentName(this);
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, 
                "Enable Device Admin to allow advanced system control and remote management capabilities.");
            
            deviceAdminLauncher.launch(intent);
            
        } catch (Exception e) {
            Log.e(TAG, "Error requesting device admin", e);
            Toast.makeText(this, "Error requesting device admin: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void requestScreenCapture() {
        try {
            MediaProjectionManager projectionManager = 
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            Intent intent = projectionManager.createScreenCaptureIntent();
            screenCaptureLauncher.launch(intent);
            
        } catch (Exception e) {
            Log.e(TAG, "Error requesting screen capture", e);
            Toast.makeText(this, "Error requesting screen capture: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void requestBatteryOptimization() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    batteryOptimizationLauncher.launch(intent);
                } else {
                    batteryOptimizationDisabled = true;
                    updateUI();
                    Toast.makeText(this, "Battery optimization already disabled", Toast.LENGTH_SHORT).show();
                }
            } else {
                batteryOptimizationDisabled = true;
                updateUI();
                Toast.makeText(this, "Battery optimization not available on this Android version", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error requesting battery optimization", e);
            // Try fallback to general settings
            try {
                Intent fallbackIntent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                batteryOptimizationLauncher.launch(fallbackIntent);
            } catch (Exception e2) {
                Toast.makeText(this, "Unable to open battery optimization settings", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void requestAccessibilityService() {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            accessibilityLauncher.launch(intent);
            
            Toast.makeText(this, "Please enable the accessibility service for this app", Toast.LENGTH_LONG).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening accessibility settings", e);
            Toast.makeText(this, "Unable to open accessibility settings", Toast.LENGTH_LONG).show();
        }
    }
    
    private void checkCurrentPermissions() {
        // Check basic permissions
        basicPermissionsGranted = PermissionManager.hasAllPermissions(this);
        
        // Check device admin
        deviceAdminEnabled = DeviceOwnerReceiver.isDeviceAdmin(this);
        
        // Check screen capture (check if we have stored permission)
        screenCaptureGranted = DeviceOwnerManager.getInstance().hasStoredScreenCapturePermission(this);
        
        // Check battery optimization
        checkBatteryOptimization();
        
        // Check accessibility service
        checkAccessibilityService();
    }
    
    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            batteryOptimizationDisabled = powerManager != null && 
                powerManager.isIgnoringBatteryOptimizations(getPackageName());
        } else {
            batteryOptimizationDisabled = true; // Not applicable for older versions
        }
    }
    
    private void checkAccessibilityService() {
        // For now, mark as optional - implement if you have an accessibility service
        accessibilityEnabled = true; // Mark as completed since it's optional
    }
    
    private void updateUI() {
        cbBasicPermissions.setChecked(basicPermissionsGranted);
        cbDeviceAdmin.setChecked(deviceAdminEnabled);
        cbScreenCapture.setChecked(screenCaptureGranted);
        cbBatteryOptimization.setChecked(batteryOptimizationDisabled);
        cbAccessibility.setChecked(accessibilityEnabled);
        
        // Update status text
        int completedCount = 0;
        if (basicPermissionsGranted) completedCount++;
        if (deviceAdminEnabled) completedCount++;
        if (screenCaptureGranted) completedCount++;
        if (batteryOptimizationDisabled) completedCount++;
        if (accessibilityEnabled) completedCount++;
        
        statusText.setText(String.format("Setup Progress: %d/5 completed", completedCount));
        
        // Enable/disable continue button
        btnContinue.setEnabled(allCriticalPermissionsGranted());
        btnContinue.setText(allCriticalPermissionsGranted() ? "Continue to App" : "Complete Setup First");
    }
    
    private boolean allCriticalPermissionsGranted() {
        // Basic permissions and screen capture are critical
        // Device admin is highly recommended but not strictly required
        return basicPermissionsGranted && screenCaptureGranted;
    }
    
    private boolean isSetupCompleted() {
        return getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getBoolean("setup_completed", false);
    }
    
    private void showMissingPermissionsDialog() {
        StringBuilder missing = new StringBuilder("Please complete the following setup steps:\n\n");
        
        if (!basicPermissionsGranted) {
            missing.append("• Basic Permissions (Camera, Location, etc.)\n");
        }
        if (!screenCaptureGranted) {
            missing.append("• Screen Capture Permission\n");
        }
        if (!deviceAdminEnabled) {
            missing.append("• Device Admin (Recommended for full control)\n");
        }
        if (!batteryOptimizationDisabled) {
            missing.append("• Battery Optimization (Recommended)\n");
        }
        
        new AlertDialog.Builder(this)
            .setTitle("Setup Incomplete")
            .setMessage(missing.toString())
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void showADBInstructions() {
        String instructions = "For ADVANCED CONTROL, run these ADB commands:\n\n" +
            "1. Enable USB Debugging on your device\n" +
            "2. Connect via USB\n" +
            "3. Run these commands:\n\n" +
            "adb shell pm grant " + getPackageName() + " android.permission.WRITE_SECURE_SETTINGS\n\n" +
            "For DEVICE OWNER mode (maximum control):\n" +
            "adb shell dpm set-device-owner " + getPackageName() + "/.admin.DeviceOwnerReceiver\n\n" +
            "Note: Device owner requires factory reset and no accounts on device.";
        
        new AlertDialog.Builder(this)
            .setTitle("ADB Setup Instructions")
            .setMessage(instructions)
            .setPositiveButton("Copy Package Name", (dialog, which) -> {
                android.content.ClipboardManager clipboard = 
                    (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Package Name", getPackageName());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Package name copied to clipboard", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Close", null)
            .show();
    }
    
    private void proceedToMainApp() {
        // Mark setup as completed
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("setup_completed", true)
            .apply();
        
        // Initialize all managers with elevated permissions
        DeviceOwnerManager.getInstance().initialize(this);
        
        // Start the main app
        Intent intent = new Intent(this, SharingActivity.class);
        startActivity(intent);
        finish();
        
        Toast.makeText(this, "Setup completed! Your app now has advanced control capabilities.", 
            Toast.LENGTH_LONG).show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Recheck permissions when returning from settings
        checkCurrentPermissions();
        updateUI();
    }
}
