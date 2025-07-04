package com.example.myapplication.utils;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class CameraManager {
    private static final String TAG = "CameraManager";
    private static CameraManager instance;
    
    private PeerConnectionFactory peerConnectionFactory;
    private CameraVideoCapturer videoCapturer;
    private VideoSource videoSource;
    private VideoTrack videoTrack;
    private AudioSource audioSource;
    private AudioTrack audioTrack;
    private MediaStream mediaStream;
    private EglBase eglBase;
    private boolean isStreaming = false;
    
    // Store current state for camera switching
    private Context currentContext;
    private SurfaceViewRenderer currentTextureView;
    private CameraStreamListener currentListener;
    private boolean isUsingFrontCamera = true;
    
    // WebRTC constants
    private static final String VIDEO_TRACK_ID = "ARDAMSv0";
    private static final String AUDIO_TRACK_ID = "ARDAMSa0";
    private static final String STREAM_ID = "ARDAMS";
    
    // Interface for camera stream callbacks
    public interface CameraStreamListener {
        void onFrameAvailable(ByteBuffer data, int width, int height, long timestamp);
        void onCameraError(String error);
    }
    
    private CameraManager() {
        // Private constructor for singleton
    }
    
    public static synchronized CameraManager getInstance() {
        if (instance == null) {
            instance = new CameraManager();
        }
        return instance;
    }
    
    public void initialize(Context context) {
        // Create EGL context for rendering
        eglBase = EglBase.create();
        
        // Initialize PeerConnectionFactory
        PeerConnectionFactory.InitializationOptions initOptions = PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions();
        
        PeerConnectionFactory.initialize(initOptions);
        
        // Create PeerConnectionFactory
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory videoEncoderFactory = new DefaultVideoEncoderFactory(
                eglBase.getEglBaseContext(), true, true);
        DefaultVideoDecoderFactory videoDecoderFactory = new DefaultVideoDecoderFactory(
                eglBase.getEglBaseContext());
        
        peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(videoEncoderFactory)
                .setVideoDecoderFactory(videoDecoderFactory)
                .createPeerConnectionFactory();
    }
    
    public void startCamera(Context context, SurfaceViewRenderer localView, CameraStreamListener listener) {
        // Store current state
        this.currentContext = context;
        this.currentTextureView = localView;
        this.currentListener = listener;
        if (!PermissionManager.hasCameraAndMicPermissions(context)) {
            listener.onCameraError("Camera and microphone permissions not granted");
            return;
        }
        
        if (isStreaming) {
            stopCamera();
        }
        
        try {
            // Create video capturer
            videoCapturer = createCameraCapturer(context);
            if (videoCapturer == null) {
                listener.onCameraError("Failed to create camera capturer");
                return;
            }
            
            // Setup local preview if view is provided
            if (localView != null) {
                localView.init(eglBase.getEglBaseContext(), null);
                localView.setEnableHardwareScaler(true);
                localView.setMirror(true);
            }
            
            // Create video source
            SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create(
                    "CaptureThread", eglBase.getEglBaseContext());
            videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
            videoCapturer.initialize(surfaceTextureHelper, context, videoSource.getCapturerObserver());
            
            // Create video track
            videoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
            if (localView != null) {
                videoTrack.addSink(localView);
            }
            
            // Create audio source and track
            audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
            audioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
            
            // Create media stream
            mediaStream = peerConnectionFactory.createLocalMediaStream(STREAM_ID);
            mediaStream.addTrack(videoTrack);
            mediaStream.addTrack(audioTrack);
            
            // Start camera
            videoCapturer.startCapture(640, 480, 30);
            
            isStreaming = true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting camera", e);
            listener.onCameraError("Error starting camera: " + e.getMessage());
        }
    }
    
    private CameraVideoCapturer createCameraCapturer(Context context) {
        CameraEnumerator enumerator;
        
        // Try to use Camera2 if supported
        if (Camera2Enumerator.isSupported(context)) {
            enumerator = new Camera2Enumerator(context);
        } else {
            enumerator = new Camera1Enumerator(false);
        }
        
        // Try to find camera based on current preference
        for (String deviceName : enumerator.getDeviceNames()) {
            if (enumerator.isFrontFacing(deviceName) == isUsingFrontCamera) {
                return enumerator.createCapturer(deviceName, null);
            }
        }
        
        // If preferred camera not found, try the other one
        for (String deviceName : enumerator.getDeviceNames()) {
            if (enumerator.isFrontFacing(deviceName) != isUsingFrontCamera) {
                return enumerator.createCapturer(deviceName, null);
            }
        }
        
        return null;
    }
    
    public void stopCamera() {
        isStreaming = false;
        
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping camera capturer", e);
            }
            videoCapturer.dispose();
            videoCapturer = null;
        }
        
        if (videoSource != null) {
            videoSource.dispose();
            videoSource = null;
        }
        
        if (audioSource != null) {
            audioSource.dispose();
            audioSource = null;
        }
        
        if (mediaStream != null) {
            mediaStream = null;
        }
    }
    
    public void sendCameraDataToServer(byte[] data, int width, int height) {
        try {
            Log.d(TAG, "Sending camera data to server: " + (data != null ? data.length + " bytes" : "null") + ", " + width + "x" + height);
            
            // Convert image to Base64
            String base64Data = Base64.encodeToString(data, Base64.DEFAULT);
            
            JSONObject cameraData = new JSONObject();
            cameraData.put("data", base64Data);
            cameraData.put("width", width);
            cameraData.put("height", height);
            cameraData.put("timestamp", System.currentTimeMillis());
            
            // Send camera data via socket
            SocketManager.getInstance().emit(Constants.EVENT_SHARE_VOICE, cameraData);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating camera data JSON", e);
        }
    }
    
    public boolean isStreaming() {
        return isStreaming;
    }
    
    // Remote camera support - start camera without UI
    public void startCameraRemote(Context context, String cameraType, CameraStreamListener listener) {
        try {
            Log.d(TAG, "Starting remote camera: " + cameraType);
            // For simplicity, use existing camera start method without TextureView
            startCamera(context, null, listener);
        } catch (Exception e) {
            Log.e(TAG, "Error starting remote camera", e);
            if (listener != null) {
                listener.onCameraError("Remote camera error: " + e.getMessage());
            }
        }
    }
    
    // Switch between front and back camera
    public void switchCamera() {
        try {
            if (videoCapturer != null) {
                Log.d(TAG, "Switching camera");
                // Stop current camera
                stopCamera();
                
                // Create new capturer with opposite camera
                isUsingFrontCamera = !isUsingFrontCamera;
                CameraVideoCapturer newCapturer = createCameraCapturer(currentContext);
                if (newCapturer != null) {
                    videoCapturer = newCapturer;
                    // Restart camera with new capturer
                    if (currentContext != null && currentListener != null) {
                        startCamera(currentContext, currentTextureView, currentListener);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error switching camera", e);
        }
    }
    
    public void release() {
        stopCamera();
        
        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
            peerConnectionFactory = null;
        }
        
        if (eglBase != null) {
            eglBase.release();
            eglBase = null;
        }
    }
}
