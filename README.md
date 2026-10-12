# RoadSense Vision 🚧

**AI-Powered Pothole Detection for Safer Roads**

RoadSense Vision is an Android application designed to detect potholes using real-time camera input and computer vision. It uses a YOLO-based ONNX model to analyze road conditions and GPS to associate detections with geographical locations.

The goal is to simplify road damage identification and help build safer roads through technology.

## ✨ Features

- **AI-Powered Detection** — Uses a YOLO-based model to identify potholes.
- **Real-Time Camera Analysis** — Captures road footage using Android CameraX.
- **GPS Integration** — Retrieves geographical coordinates for detected potholes.
- **Live Monitoring** — Provides an interface for monitoring road conditions.
- **On-Device Inference** — Designed to run machine learning inference locally on Android.
- **Native Android App** — Built with Kotlin and Jetpack Compose.

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| Kotlin | Android development |
| Jetpack Compose | User interface |
| CameraX | Camera access and image analysis |
| ONNX Runtime | On-device machine learning inference |
| YOLO | Pothole detection |
| Android Location APIs | GPS coordinates |
| Gradle | Build automation |

## 🏗️ Architecture

```text
Android Camera
      │
      ▼
CameraX ImageAnalysis
      │
      ▼
Image Preprocessing
      │
      ▼
YOLO ONNX Model
      │
      ▼
Detection Postprocessing
      │
      ▼
Pothole Detections
      │
      ├── Detection Information
      │
      └── GPS Coordinates
```

## 📱 Project Roadmap

### Phase 1 — Android Application

- [x] Native Android application foundation
- [x] Camera preview integration
- [x] GPS integration
- [x] Monitoring interface
- [ ] Reliable real-time pothole detection
- [ ] Detection visualization and confidence scores
- [ ] Associate detections with GPS coordinates
- [ ] Validate performance on physical Android devices

### Phase 2 — Public Pothole Map

- [ ] Develop a public website to display pothole detections
- [ ] Display detected potholes on an interactive map
- [ ] Show geographical locations and detection details
- [ ] Develop a backend for storing and retrieving detection data
- [ ] Allow the public to explore road conditions geographically

> **Note:** The public website and backend are planned for Phase 2 and are not part of the current Phase 1 implementation.

## 🚀 Getting Started

### Prerequisites

- Android Studio
- Android SDK
- JDK compatible with the project's Gradle configuration
- An Android device or emulator

### Installation

**1. Clone the repository**

```bash
git clone https://github.com/YOUR_USERNAME/PotHole.git
cd PotHole
```

Replace `YOUR_USERNAME` with your GitHub username.

**2. Open the project**

Open the project in Android Studio and allow Gradle to synchronize.

**3. Configure the model**

The project uses a YOLO-based model exported in ONNX format.

Development model location:

```text
Model/best.onnx
```

Ensure the model is available at the location expected by the application's model-loading implementation.

**4. Build the application**

On macOS or Linux:

```bash
chmod +x gradlew
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

**5. Install the APK**

Connect an Android device with USB debugging enabled, then run:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 🔐 Permissions

The application may require the following permissions, depending on the implemented features:

- **Camera:** Capture road footage for analysis.
- **Location:** Retrieve geographical coordinates for detections.

Grant the required permissions when prompted.

## 🤖 Machine Learning Model

RoadSense Vision uses a YOLO-based object detection model exported to ONNX format.

The model's input dimensions, image preprocessing, tensor layout, output decoding, confidence threshold, and postprocessing must match the actual exported model.

Detection accuracy and inference performance should be validated using representative road images and real-world camera footage.

## ⚠️ Project Status

RoadSense Vision is currently under development.

The primary focus is completing and validating the Android application's on-device pothole detection pipeline. Detection reliability and performance must be verified before the application can be considered production-ready.

This project is a prototype and should not be used as the sole basis for road safety decisions.

## 🔮 Future Improvements

- Improve detection accuracy in different lighting and weather conditions.
- Optimize inference performance on mobile devices.
- Reduce duplicate detections across consecutive frames.
- Improve GPS accuracy and location association.
- Introduce an interactive public pothole map.
- Add detection history and road-condition analytics.
- Evaluate performance across multiple Android devices.

## 🤝 Contributing

Contributions, suggestions, and bug reports are welcome.

1. Fork the repository.
2. Create a feature branch.
3. Commit your changes.
4. Push the branch to your fork.
5. Open a pull request.

## 📄 License

No license has been specified yet. Until a license is added, permission to reuse, modify, or redistribute this project should not be assumed.

---

**RoadSense Vision** — Using computer vision to help identify potholes and build safer roads.
