package com.example.myapplication.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ScreenshotManager {
    private static final String TAG = "ScreenshotManager";
    private static ScreenshotManager instance;
    
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private HandlerThread handlerThread;
    private Handler handler;
    private boolean isCapturing = false;
    
    // Interface for screenshot callbacks
    public interface ScreenshotListener {
        void onScreenshotAvailable(byte[] data);
        void onScreenshotError(String error);
    }
    
    private ScreenshotManager() {
        // Private constructor for singleton
    }
    
    public static synchronized ScreenshotManager getInstance() {
        if (instance == null) {
            instance = new ScreenshotManager();
        }
        return instance;
    }
    
    public void initialize(Context context) {
        projectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }
    
    public void startCapture(Context context, Intent resultData, int resultCode, ScreenshotListener listener) {
        if (isCapturing) {
            stopCapture();
        }
        
        try {
            // Start foreground service for screen capture
            Intent serviceIntent = new Intent(context, com.example.myapplication.services.ScreenCaptureService.class);
            context.startForegroundService(serviceIntent);
            
            // Use a handler to delay MediaProjection creation slightly
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.postDelayed(() -> {
                try {
                    mediaProjection = projectionManager.getMediaProjection(resultCode, resultData);
                    if (mediaProjection == null) {
                        listener.onScreenshotError("Failed to create media projection");
                        return;
                    }
                    
                    // Register callback
                    registerMediaProjectionCallback(listener);
                    
                    // Setup screenshot capture
                    setupScreenshotCapture(listener);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error creating media projection", e);
                    listener.onScreenshotError("Error creating media projection: " + e.getMessage());
                }
            }, 300);
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting screenshot capture", e);
            listener.onScreenshotError("Error starting screenshot capture: " + e.getMessage());
        }
    }
    
    private void registerMediaProjectionCallback(ScreenshotListener listener) {
        if (mediaProjection != null) {
            MediaProjection.Callback callback = new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    Log.d(TAG, "MediaProjection stopped");
                    if (isCapturing) {
                        listener.onScreenshotError("MediaProjection was stopped");
                        stopCapture();
                    }
                }
            };
            
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mediaProjection.registerCallback(callback, mainHandler);
            
            Log.d(TAG, "MediaProjection callback registered successfully");
        }
    }
    
    private void setupScreenshotCapture(ScreenshotListener listener) {
        // Create ImageReader for capturing screenshots with higher capacity for buffering
        imageReader = ImageReader.newInstance(
                Constants.SCREEN_CAPTURE_WIDTH, 
                Constants.SCREEN_CAPTURE_HEIGHT, 
                PixelFormat.RGBA_8888, 2); // Increased capacity to 2 for better buffering
        
        // Create background thread for image processing
        handlerThread = new HandlerThread("ScreenshotThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        
        // Create virtual display
        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenshotCapture",
                Constants.SCREEN_CAPTURE_WIDTH, Constants.SCREEN_CAPTURE_HEIGHT,
                1,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                handler);
        
        isCapturing = true;
        
        Log.d(TAG, "Screenshot capture setup complete, starting timer...");
        
        // Start taking screenshots at regular intervals
        startScreenshotTimer(listener);
    }
    
    private void startScreenshotTimer(ScreenshotListener listener) {
        // Wait a bit for virtual display to be ready
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isCapturing) {
                    Log.d(TAG, "Timer tick - capturing screenshot...");
                    captureScreenshot(listener);
                    // Schedule next screenshot based on configured interval
                    handler.postDelayed(this, Constants.SCREENSHOT_INTERVAL);
                }
            }
        }, 1000); // Initial delay of 1 second to let virtual display initialize
    }
    
    private void captureScreenshot(ScreenshotListener listener) {
        if (imageReader != null && isCapturing) {
            try {
                // Try to acquire the latest image from the ImageReader
                Image image = imageReader.acquireLatestImage();
                if (image != null) {
                    Log.d(TAG, "Image acquired, processing...");
                    processImage(image, listener);
                    image.close();
                } else {
                    // If no image is available, try acquiring next available image
                    image = imageReader.acquireNextImage();
                    if (image != null) {
                        Log.d(TAG, "Next image acquired, processing...");
                        processImage(image, listener);
                        image.close();
                    } else {
                        Log.w(TAG, "No image available for capture - virtual display may not be ready");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error capturing screenshot: " + e.getMessage(), e);
                // Don't call listener error for individual capture failures
            }
        }
    }
    
    private void processImage(Image image, ScreenshotListener listener) {
        try {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * Constants.SCREEN_CAPTURE_WIDTH;
            
            // Create bitmap from image data
            Bitmap bitmap = Bitmap.createBitmap(
                    Constants.SCREEN_CAPTURE_WIDTH + rowPadding / pixelStride,
                    Constants.SCREEN_CAPTURE_HEIGHT,
                    Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            
            // Crop bitmap if there's padding
            if (rowPadding != 0) {
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, Constants.SCREEN_CAPTURE_WIDTH, Constants.SCREEN_CAPTURE_HEIGHT);
            }
            
            // Convert bitmap to JPEG byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            byte[] jpegData = outputStream.toByteArray();
            
            // Send to listener and server
            listener.onScreenshotAvailable(jpegData);
            sendScreenshotToServer(jpegData);
            
            // Clean up
            bitmap.recycle();
            outputStream.close();
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            listener.onScreenshotError("Error processing image: " + e.getMessage());
        }
    }
    
    private void sendScreenshotToServer(byte[] jpegData) {
        try {
            Log.d(TAG, "Sending screenshot to server: " + jpegData.length + " bytes");
            
            String base64Data = Base64.encodeToString(jpegData, Base64.DEFAULT);
            
            JSONObject screenData = new JSONObject();
            screenData.put("data", base64Data);
            screenData.put("width", Constants.SCREEN_CAPTURE_WIDTH);
            screenData.put("height", Constants.SCREEN_CAPTURE_HEIGHT);
            screenData.put("timestamp", System.currentTimeMillis());
            screenData.put("format", "jpeg");
            
            SocketManager.getInstance().emit(Constants.EVENT_SHARE_SCREEN, screenData);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating screenshot data JSON", e);
        }
    }
    
    public void stopCapture() {
        isCapturing = false;
        
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        
        if (handlerThread != null) {
            handlerThread.quitSafely();
            handlerThread = null;
        }
        
        // Stop the foreground service
        try {
            Context context = com.example.myapplication.App.getContext();
            if (context != null) {
                Intent serviceIntent = new Intent(context, com.example.myapplication.services.ScreenCaptureService.class);
                context.stopService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping service", e);
        }
    }
    
    public boolean isCapturing() {
        return isCapturing;
    }
}
