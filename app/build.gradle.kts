import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun configuredValue(name: String): String? =
    (project.findProperty(name) as String?)
        ?: System.getenv(name)
        ?: localProperties.getProperty(name)

val roadsenseApiBaseUrl = configuredValue("ROADSENSE_API_BASE_URL")
    ?: "https://server.tailda589d.ts.net/roadsense/api/v1"
val roadsenseIngestApiKey = configuredValue("ROADSENSE_INGEST_API_KEY") ?: ""

if (roadsenseIngestApiKey.isBlank()) {
    logger.warn(
        "ROADSENSE_INGEST_API_KEY is not configured. Observation sync will stay off. " +
            "Set it in local.properties, a Gradle property, or the environment."
    )
}

android {
    namespace = "com.roadsensevision"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.roadsensevision"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0.0-phase2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"$roadsenseApiBaseUrl\"")
        buildConfigField("String", "INGEST_API_KEY", "\"$roadsenseIngestApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources {
            excludes += listOf("/META-INF/*", "META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/LICENSE.txt", "META-INF/license.txt", "META-INF/NOTICE", "META-INF/NOTICE.txt", "META-INF/notice.txt", "META-INF/ASL2.0")
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.navigation:navigation-compose:2.8.4")

    // CameraX
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")

    // ONNX Runtime (full Android build, CPU execution provider).
    // NOTE: onnxruntime-mobile ships a *minimal* build that refuses ONNX-format
    // models ("Loading anything other than ORT format models is not enabled in
    // this build."), and this project packages best.onnx — so the full build is
    // required for the packaged model to load.
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.18.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // OSMDroid (offline-capable, key-free map library)
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Background offline upload queue
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("androidx.test:core:1.6.1")
    // Full ONNX Runtime for JVM so unit tests execute the real model end-to-end.
    testImplementation("com.microsoft.onnxruntime:onnxruntime:1.18.0")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
