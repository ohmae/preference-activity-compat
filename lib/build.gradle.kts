import build.*
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("com.android.library")
    id("kotlin-android")
    maven
    `maven-publish`
    id("org.jetbrains.dokka")
    id("com.jfrog.bintray")
    id("com.github.ben-manes.versions")
}

base.archivesBaseName = "preference"
group = ProjectProperties.groupId
version = ProjectProperties.versionName

android {
    compileSdkVersion(30)

    defaultConfig {
        minSdkVersion(14)
        targetSdkVersion(30)
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

tasks.named<DokkaTask>("dokkaHtml") {
    outputDirectory.set(File(projectDir, "../docs/dokka"))
}

tasks.create("sourcesJar", Jar::class) {
    archiveClassifier.set("sources")
    from(android.sourceSets["main"].java.srcDirs)
}

artifacts {
    archives(tasks.named<Jar>("sourcesJar"))
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.core:core-ktx:1.3.2")
}

uploadArchivesSettings()
publishingSettings()
bintraySettings()
dependencyUpdatesSettings()
