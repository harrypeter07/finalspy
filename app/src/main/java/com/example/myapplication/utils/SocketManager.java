package com.example.myapplication.utils;

import android.util.Log;

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
            });
            
            socket.on(Socket.EVENT_DISCONNECT, args -> {
                Log.d(TAG, "Socket disconnected");
            });
            
            socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
                Log.e(TAG, "Socket connection error: " + args[0]);
            });
            
            // Connect to server
            socket.connect();
            
        } catch (URISyntaxException e) {
            Log.e(TAG, "Socket initialization error", e);
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
}
