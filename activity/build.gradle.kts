import build.ProjectProperties
import build.dependencyUpdatesSettings
import build.publishingSettings
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("com.android.library")
    id("kotlin-android")
    `maven-publish`
    signing
    id("org.jetbrains.dokka")
    id("com.github.ben-manes.versions")
}

base.archivesName.set("preference")
group = ProjectProperties.groupId
version = ProjectProperties.versionName

android {
    compileSdk = 31

    defaultConfig {
        minSdk = 14
        targetSdk = 31
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

tasks.named<DokkaTask>("dokkaJavadoc") {
    outputDirectory.set(File(buildDir, "docs/javadoc"))
}

tasks.create("javadocJar", Jar::class) {
    dependsOn("dokkaJavadoc")
    archiveClassifier.set("javadoc")
    from(File(buildDir, "docs/javadoc"))
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
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.core:core-ktx:1.7.0")
}

publishingSettings()
dependencyUpdatesSettings()
