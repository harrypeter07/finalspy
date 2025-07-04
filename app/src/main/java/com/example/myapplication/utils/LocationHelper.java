package com.example.myapplication.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationHelper {
    private static final String TAG = "LocationHelper";
    
    public interface LocationAddressCallback {
        void onAddressFound(String address, String city, String country);
        void onAddressError(String error);
    }
    
    public static void getAddressFromLocation(Context context, double latitude, double longitude, LocationAddressCallback callback) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            
            // Run geocoding in background thread to avoid blocking UI
            new Thread(() -> {
                try {
                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        
                        String fullAddress = "";
                        String city = "";
                        String country = "";
                        
                        // Build full address
                        if (address.getAddressLine(0) != null) {
                            fullAddress = address.getAddressLine(0);
                        }
                        
                        if (address.getLocality() != null) {
                            city = address.getLocality();
                        }
                        
                        if (address.getCountryName() != null) {
                            country = address.getCountryName();
                        }
                        
                        // If no address line, build from components
                        if (fullAddress.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            if (address.getFeatureName() != null) {
                                sb.append(address.getFeatureName()).append(", ");
                            }
                            if (address.getThoroughfare() != null) {
                                sb.append(address.getThoroughfare()).append(", ");
                            }
                            if (address.getSubLocality() != null) {
                                sb.append(address.getSubLocality()).append(", ");
                            }
                            if (city != null && !city.isEmpty()) {
                                sb.append(city).append(", ");
                            }
                            if (address.getAdminArea() != null) {
                                sb.append(address.getAdminArea()).append(", ");
                            }
                            if (country != null && !country.isEmpty()) {
                                sb.append(country);
                            }
                            
                            fullAddress = sb.toString().replaceAll(", $", "");
                        }
                        
                        Log.d(TAG, "Address found: " + fullAddress);
                        callback.onAddressFound(fullAddress, city, country);
                        
                    } else {
                        Log.w(TAG, "No address found for coordinates");
                        callback.onAddressError("No address found for these coordinates");
                    }
                    
                } catch (IOException e) {
                    Log.e(TAG, "Geocoding failed", e);
                    callback.onAddressError("Geocoding service unavailable: " + e.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "Error in geocoding", e);
                    callback.onAddressError("Error getting address: " + e.getMessage());
                }
            }).start();
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up geocoder", e);
            callback.onAddressError("Error setting up location service: " + e.getMessage());
        }
    }
    
    public static void sendLocationWithAddress(double latitude, double longitude, String address, String city, String country) {
        try {
            JSONObject locationData = new JSONObject();
            locationData.put("latitude", latitude);
            locationData.put("longitude", longitude);
            locationData.put("address", address != null ? address : "Unknown location");
            locationData.put("city", city != null ? city : "Unknown city");
            locationData.put("country", country != null ? country : "Unknown country");
            locationData.put("timestamp", System.currentTimeMillis());
            
            // Send location data via socket
            SocketManager.getInstance().emit(Constants.EVENT_SHARE_LOCATION, locationData);
            
            Log.d(TAG, "Location sent with address: " + address);
            
        } catch (JSONException e) {
            Log.e(TAG, "Error creating location data JSON", e);
        }
    }
    
    public static String formatCoordinates(double latitude, double longitude) {
        return String.format(Locale.getDefault(), "%.6f, %.6f", latitude, longitude);
    }
    
    public static String getLocationSummary(double latitude, double longitude, String address) {
        String coords = formatCoordinates(latitude, longitude);
        if (address != null && !address.isEmpty() && !address.equals("Unknown location")) {
            return address + " (" + coords + ")";
        } else {
            return coords;
        }
    }
}
