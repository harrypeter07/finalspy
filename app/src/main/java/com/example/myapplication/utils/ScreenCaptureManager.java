package com.example.myapplication.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;

public class ScreenCaptureManager {
    private static final String TAG = "ScreenCaptureManager";
    private static ScreenCaptureManager instance;
    
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaCodec encoder;
    private Surface encoderSurface;
    private HandlerThread handlerThread;
    private Handler handler;
    private boolean isCapturing = false;
    
    // Interface for screen capture callbacks
    public interface ScreenCaptureListener {
        void onScreenDataAvailable(byte[] data);
        void onScreenCaptureError(String error);
    }
    
    private ScreenCaptureManager() {
        // Private constructor for singleton
    }
    
    public static synchronized ScreenCaptureManager getInstance() {
        if (instance == null) {
            instance = new ScreenCaptureManager();
        }
        return instance;
    }
    
    public void initialize(Context context) {
        projectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }
    
    // Create intent for screen capture
    public Intent createScreenCaptureIntent() {
        return projectionManager.createScreenCaptureIntent();
    }
    
    // Start screen capture with result from permission dialog
    public void startCapture(Context context, Intent resultData, int resultCode, ScreenCaptureListener listener) {
        if (isCapturing) {
            stopCapture();
        }
        
        // Start foreground service for screen capture
        Intent serviceIntent = new Intent(context, com.example.myapplication.services.ScreenCaptureService.class);
        context.startForegroundService(serviceIntent);
        
        try {
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData);
            if (mediaProjection == null) {
                listener.onScreenCaptureError("Failed to create media projection");
                return;
            }
            
            // Create encoder
            prepareVideoEncoder();
            
            // Create a virtual display that captures the screen
            createVirtualDisplay();
            
            // Start a thread to process encoder output
            startEncoderThread(listener);
            
            isCapturing = true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting screen capture", e);
            listener.onScreenCaptureError("Error starting screen capture: " + e.getMessage());
        }
    }
    
    private void prepareVideoEncoder() throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 
                Constants.SCREEN_CAPTURE_WIDTH, Constants.SCREEN_CAPTURE_HEIGHT);
        
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, Constants.SCREEN_CAPTURE_BITRATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        
        encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        
        encoderSurface = encoder.createInputSurface();
        encoder.start();
    }
    
    private void createVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                Constants.SCREEN_CAPTURE_WIDTH, Constants.SCREEN_CAPTURE_HEIGHT, 
                1, 
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                encoderSurface, 
                null, 
                null);
    }
    
    private void startEncoderThread(ScreenCaptureListener listener) {
        handlerThread = new HandlerThread("EncoderThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        
        handler.post(new Runnable() {
            @Override
            public void run() {
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                
                while (isCapturing) {
                    try {
                        int outputBufferId = encoder.dequeueOutputBuffer(bufferInfo, 10000);
                        if (outputBufferId >= 0) {
                            ByteBuffer outputBuffer = encoder.getOutputBuffer(outputBufferId);
                            
                            byte[] data = new byte[bufferInfo.size];
                            outputBuffer.get(data);
                            
                            // Send screen data to listener
                            listener.onScreenDataAvailable(data);
                            
                            // Send data to server
                            sendScreenDataToServer(data);
                            
                            encoder.releaseOutputBuffer(outputBufferId, false);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing encoder output", e);
                        listener.onScreenCaptureError("Error processing encoder output: " + e.getMessage());
                        break;
                    }
                }
            }
        });
    }
    
    private void sendScreenDataToServer(byte[] data) {
        try {
            // Convert byte array to Base64 string to send over JSON
            String base64Data = android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT);
            
            JSONObject screenData = new JSONObject();
            screenData.put("data", base64Data);
            screenData.put("width", Constants.SCREEN_CAPTURE_WIDTH);
            screenData.put("height", Constants.SCREEN_CAPTURE_HEIGHT);
            screenData.put("timestamp", System.currentTimeMillis());
            
            // Send screen data via socket
            SocketManager.getInstance().emit(Constants.EVENT_SHARE_SCREEN, screenData);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating screen data JSON", e);
        }
    }
    
    public void stopCapture() {
        isCapturing = false;
        
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
        
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        
        if (encoderSurface != null) {
            encoderSurface.release();
            encoderSurface = null;
        }
        
        if (encoder != null) {
            encoder.stop();
            encoder.release();
            encoder = null;
        }
        
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        
        if (handlerThread != null) {
            handlerThread.quitSafely();
            handlerThread = null;
        }
    }
    
    public boolean isCapturing() {
        return isCapturing;
    }
}
