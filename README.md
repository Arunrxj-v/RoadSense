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
- **Offline-First Sync** — Saves detections in Room, then uploads metadata to the RoadSense API when a network is available.
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
| Room | Local observation persistence |
| WorkManager | Offline upload queue and retries |
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
                │
                ▼
        Room (local-first)
                │
                ▼
      WorkManager upload queue
                │
                ▼
   POST /api/v1/internal/reports
```

## 📱 Project Roadmap

### Phase 1 — Android Application

- [x] Native Android application foundation
- [x] Camera preview integration
- [x] GPS integration
- [x] Monitoring interface
- [x] Reliable real-time pothole detection
- [x] Detection visualization and confidence scores
- [x] Associate detections with GPS coordinates
- [ ] Validate performance on physical Android devices

### Phase 2 — Server Sync and Public Map

- [x] Public website and read-only API
- [x] Interactive canonical pothole map and ledger
- [x] Backend storage, deduplication, and retrieval
- [x] Android metadata ingestion with offline queue and retries
- [ ] Validate the full ingestion path on physical devices

## 🚀 Getting Started

### Prerequisites

- Android Studio
- Android SDK Platform 35 and Build Tools 35
- JDK 17 (Android Gradle Plugin 8.7.3; the Gradle wrapper rejects an absent JDK)
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

**4. Configure server ingestion**

The API base and ingest key are build values and are not stored in source.
Set them through Gradle properties or environment variables:

```bash
export ROADSENSE_API_BASE_URL=http://100.104.179.69:5001/api/v1
export ROADSENSE_INGEST_API_KEY='<same key as the server .env>'
```

`ROADSENSE_API_BASE_URL` may be the server root (`http://100.104.179.69:5001`)
or the full API base (`http://100.104.179.69:5001/api/v1`); the client normalizes
it to `/api/v1`. The phone must be connected to the same Tailscale tailnet as the
server because the API binds only to the Tailscale address.

Without a key, detection remains local and Settings reports that sync is not
configured. Camera frames and evidence images are never uploaded; only report
ID, coordinates, timestamp, confidence, and GPS accuracy are sent. Rebuild and
reinstall the APK after setting the values; the old Phase 1 build has no upload
implementation.

**5. Build the application**

On macOS or Linux:

```bash
chmod +x gradlew
./gradlew assembleDebug
```

To pass the values explicitly:

```bash
./gradlew assembleDebug \
  -PROADSENSE_API_BASE_URL=http://100.104.179.69:5001/api/v1 \
  -PROADSENSE_INGEST_API_KEY='<ingest key>'
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

**6. Install the APK**

Connect an Android device with USB debugging enabled, then run:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 🔐 Permissions

The application may require the following permissions, depending on the implemented features:

- **Camera:** Capture road footage for analysis.
- **Location:** Retrieve geographical coordinates for detections.
- **Internet / network state:** Upload queued observation metadata and retry when connected.

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
- Validate sync retries, duplicate handling, and GPS gaps on physical devices.
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
