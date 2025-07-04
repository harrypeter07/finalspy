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
    private Context context;
    
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
        this.context = context.getApplicationContext();
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
        // Use LocationHelper to get address and send location with readable address
        LocationHelper.getAddressFromLocation(context, location.getLatitude(), location.getLongitude(), 
            new LocationHelper.LocationAddressCallback() {
                @Override
                public void onAddressFound(String address, String city, String country) {
                    // Send location with resolved address
                    LocationHelper.sendLocationWithAddress(
                        location.getLatitude(), 
                        location.getLongitude(), 
                        address, 
                        city, 
                        country
                    );
                }
                
                @Override
                public void onAddressError(String error) {
                    Log.w(TAG, "Address resolution failed: " + error);
                    // Send location without address (fallback to coordinates only)
                    LocationHelper.sendLocationWithAddress(
                        location.getLatitude(), 
                        location.getLongitude(), 
                        null, 
                        null, 
                        null
                    );
                }
            }
        );
    }
}
