plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.ccard.tracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ccard.tracker"
        minSdk = 26
        targetSdk = 34
        // CI passes -PccardVersionCode/-PccardVersionName so every release build has a unique,
        // monotonically increasing version the in-app updater can compare against.
        versionCode = (findProperty("ccardVersionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (findProperty("ccardVersionName") as String?) ?: "0.1.0-dev"
    }

    // Fixed keystore committed at the repo root so every CI build (and local debug installs of the
    // release variant) share the same signing certificate. Android refuses to install an "update"
    // whose signature doesn't match the currently installed app, so this is what makes the in-app
    // self-update flow (see com.ccard.tracker.update) work without ever needing to uninstall first.
    // This is a throwaway dev key for sideloading only — not meant for Play Store distribution.
    signingConfigs {
        create("release") {
            storeFile = file("../ccard-release.keystore")
            storePassword = "ccard-dev-2026"
            keyAlias = "ccard"
            keyPassword = "ccard-dev-2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
