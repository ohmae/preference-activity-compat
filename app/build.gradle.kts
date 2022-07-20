import build.dependencyUpdatesSettings

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.github.ben-manes.versions")
}

android {
    compileSdk = 33

    namespace = "net.mm2d.preference.sample"
    defaultConfig {
        applicationId = "net.mm2d.preference.sample"
        minSdk = 14
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    api(project(":activity"))
    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("androidx.preference:preference-ktx:1.2.0")
}

dependencyUpdatesSettings()
