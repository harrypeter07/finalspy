package com.example.myapplication.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONException;
import org.json.JSONObject;

public class LocationManager {
    private static final String TAG = "LocationManager";
    private static LocationManager instance;
    
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isTracking = false;
    
    public interface LocationUpdateListener {
        void onLocationUpdate(Location location);
        void onLocationError(String error);
    }
    
    private LocationManager() {
        // Private constructor for singleton
    }
    
    public static synchronized LocationManager getInstance() {
        if (instance == null) {
            instance = new LocationManager();
        }
        return instance;
    }
    
    public void initialize(Context context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }
    
    @SuppressLint("MissingPermission")
    public void startLocationUpdates(Context context, LocationUpdateListener listener) {
        if (!PermissionManager.hasLocationPermissions(context)) {
            listener.onLocationError("Location permissions not granted");
            return;
        }
        
        if (fusedLocationClient == null) {
            initialize(context);
        }
        
        // Create location request
        LocationRequest locationRequest = new LocationRequest.Builder(Constants.LOCATION_UPDATE_INTERVAL)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateIntervalMillis(Constants.LOCATION_UPDATE_INTERVAL / 2)
                .build();
        
        // Create location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult == null) {
                    return;
                }
                
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        listener.onLocationUpdate(location);
                        sendLocationToServer(location);
                    }
                }
            }
        };
        
        // Start location updates
        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );
            isTracking = true;
        } catch (Exception e) {
            Log.e(TAG, "Error starting location updates", e);
            listener.onLocationError("Error starting location updates: " + e.getMessage());
        }
    }
    
    public void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            isTracking = false;
        }
    }
    
    public boolean isTracking() {
        return isTracking;
    }
    
    private void sendLocationToServer(Location location) {
        try {
            JSONObject locationData = new JSONObject();
            locationData.put("latitude", location.getLatitude());
            locationData.put("longitude", location.getLongitude());
            locationData.put("accuracy", location.getAccuracy());
            locationData.put("timestamp", location.getTime());
            
            // Send location data via socket
            SocketManager.getInstance().emit(Constants.EVENT_SHARE_LOCATION, locationData);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating location JSON", e);
        }
    }
}
