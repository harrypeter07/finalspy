package com.example.myapplication.utils;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioManager {
    private static final String TAG = "AudioManager";
    private static AudioManager instance;
    
    private AudioRecord audioRecord;
    private ExecutorService executor;
    private boolean isRecording = false;
    
    // Audio configuration
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE_FACTOR = 2;
    
    // Interface for audio stream callbacks
    public interface AudioStreamListener {
        void onAudioDataAvailable(byte[] data, long timestamp);
        void onAudioError(String error);
    }
    
    private AudioManager() {
        // Private constructor for singleton
    }
    
    public static synchronized AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }
    
    public void startRecording(Context context, AudioStreamListener listener) {
        if (!PermissionManager.hasCameraAndMicPermissions(context)) {
            listener.onAudioError("Microphone permission not granted");
            return;
        }
        
        if (isRecording) {
            stopRecording();
        }
        
        try {
            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                listener.onAudioError("Invalid audio buffer size");
                return;
            }
            
            int bufferSize = minBufferSize * BUFFER_SIZE_FACTOR;
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);
            
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                listener.onAudioError("Failed to initialize AudioRecord");
                return;
            }
            
            audioRecord.startRecording();
            isRecording = true;
            
            // Start reading audio data on a background thread
            executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                byte[] buffer = new byte[bufferSize];
                
                while (isRecording) {
                    int readResult = audioRecord.read(buffer, 0, buffer.length);
                    
                    if (readResult > 0) {
                        // Create a copy of the buffer to avoid modifications
                        byte[] audioData = new byte[readResult];
                        System.arraycopy(buffer, 0, audioData, 0, readResult);
                        
                        // Send audio data to listener
                        listener.onAudioDataAvailable(audioData, System.currentTimeMillis());
                        
                        // Send data to server
                        sendAudioDataToServer(audioData);
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting audio recording", e);
            isRecording = false;
            listener.onAudioError("Error starting audio recording: " + e.getMessage());
        }
    }
    
    public void stopRecording() {
        isRecording = false;
        
        if (audioRecord != null) {
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.stop();
            }
            audioRecord.release();
            audioRecord = null;
        }
        
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }
    
    private void sendAudioDataToServer(byte[] data) {
        try {
            // Convert byte array to Base64 string to send over JSON
            String base64Data = android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT);
            
            JSONObject audioData = new JSONObject();
            audioData.put("data", base64Data);
            audioData.put("sampleRate", SAMPLE_RATE);
            audioData.put("timestamp", System.currentTimeMillis());
            
            // Send audio data via socket
            SocketManager.getInstance().emit(Constants.EVENT_SHARE_VOICE, audioData);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating audio data JSON", e);
        }
    }
    
    public boolean isRecording() {
        return isRecording;
    }
}
