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
