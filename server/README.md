# Android Sharing App Server

This is the server for the Android Sharing App that provides real-time communication features using Socket.IO.

## Features
- Handles real-time screen sharing data
- Manages camera video streaming
- Manages voice/audio streaming
- Handles location sharing data

## Deployment Instructions

### Local Development
1. Install dependencies:
   ```
   npm install
   ```
2. Start the development server:
   ```
   npm run dev
   ```

### Deployment to Render
1. Create a new Web Service on Render
2. Connect your GitHub repository
3. Configure the service:
   - Build Command: `npm install`
   - Start Command: `npm start`
4. Deploy the service
5. Use the provided URL in your Android app

## Important
After deploying to Render, update the `SERVER_URL` constant in the Android app with the URL provided by Render.
