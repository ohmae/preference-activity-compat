import build.dependencyUpdates

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.github.ben-manes.versions")
}

android {
    compileSdkVersion(30)

    defaultConfig {
        applicationId = "net.mm2d.preference.sample"
        minSdkVersion(14)
        targetSdkVersion(30)
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
    api(project(":lib"))
    implementation(kotlin("stdlib"))
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.preference:preference-ktx:1.1.1")
}

dependencyUpdates()
