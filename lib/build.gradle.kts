import build.*

plugins {
    id("com.android.library")
    id("kotlin-android")
    maven
    `maven-publish`
    id("com.jfrog.bintray")
    id("com.github.ben-manes.versions")
}

base.archivesBaseName = "preference"
group = ProjectProperties.groupId
version = ProjectProperties.versionName

android {
    compileSdkVersion(29)

    defaultConfig {
        minSdkVersion(14)
        targetSdkVersion(29)
        versionCode = ProjectProperties.versionCode
        versionName = ProjectProperties.versionName
        consumerProguardFile("proguard-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.preference:preference:1.1.1")
    implementation("androidx.core:core-ktx:1.3.1")
}

commonSettings()
