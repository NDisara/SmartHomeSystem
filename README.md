# Smart Home System

A comprehensive Smart Home System consisting of an Android application built with Jetpack Compose and a web-based Hardware Simulator. Both components communicate in real-time via Firebase Realtime Database to monitor and control various smart devices across different floors of a home.

## Features

*   **Real-time Monitoring & Control:** Instantly view and change the state of home devices (lights, cameras, irons, multi-switches) with live updates synced through Firebase.
*   **Android App (Jetpack Compose):**
    *   Modern, reactive UI built entirely with Jetpack Compose and Material 3.
    *   Authentication and device management (Login, Main Dashboard, Floor views, Device detail views).
    *   Background Safety Service to monitor critical conditions and automate device actions.
    *   Detailed reporting interface to view device usage logs.
*   **Web-based Hardware Simulator:**
    *   An HTML/JS/CSS frontend to mock physical IoT devices.
    *   Syncs bidirectionally with the Android app via Firebase.
    *   Visual representation of floor layouts (Ground Floor, First Floor, etc.) and device states.

## Tech Stack

*   **Android Application:**
    *   Kotlin
    *   Jetpack Compose (UI)
    *   Firebase Realtime Database (Backend / State Sync)
    *   Coil (Image Loading)
    *   Android Navigation Compose
*   **Hardware Simulator:**
    *   HTML5 / CSS3 (Outfit Font, Gradient Styling)
    *   Vanilla JavaScript
    *   Firebase Web SDK (via CDN)

## Project Structure

*   `app/`: The Android application module containing all Kotlin/Compose source code.
    *   `src/main/java/com/example/smarthomesystem/`: Contains all the UI screens, cards, models, and background services.
*   `Simulator/`: The web-based hardware simulator.
    *   `index.html`: The main entry point for the simulator UI and logic.
    *   `ground_devices.json`: Mock data representing the initial state of devices.

## Setup Instructions

### 1. Firebase Configuration
1.  Create a project in the [Firebase Console](https://console.firebase.google.com/).
2.  Enable **Realtime Database**.
3.  Add an Android app to the Firebase project with the package name `com.example.smarthomesystem`.
4.  Download the `google-services.json` file and place it in the `app/` directory of this project.
5.  Set up Firebase Authentication (if login requires actual auth provider setup in your implementation).
6.  Ensure your Realtime Database rules allow read/write access for your authenticated users (or public for testing).
7.  Update the Firebase config within `Simulator/index.html` with your project's web credentials to allow the simulator to connect to the same database.

### 2. Running the Android App
1.  Open the project in **Android Studio**.
2.  Sync the project with Gradle files.
3.  Run the app on an Android emulator or a physical device (API 24+).

### 3. Running the Simulator
1.  Navigate to the `Simulator/` directory.
2.  Open `index.html` in any modern web browser.
3.  The simulator will connect to Firebase and display the current state of all virtual hardware. You can toggle switches here and see them reflect in the Android app immediately, and vice-versa.

## License
MIT License
