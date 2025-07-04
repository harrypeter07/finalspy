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

// Event listeners for socket.io
io.on('connection', (socket) => {
  console.log('a user connected');

  // Handle screen sharing event
  socket.on('share-screen', (data) => {
    io.emit('share-screen', data);
  });

  // Handle voice sharing event
  socket.on('share-voice', (data) => {
    io.emit('share-voice', data);
  });

  // Handle location sharing event
  socket.on('share-location', (data) => {
    io.emit('share-location', data);
  });

  // Handle disconnection
  socket.on('disconnect', () => {
    console.log('user disconnected');
  });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
  console.log(`Server listening on port ${PORT}`);
});
