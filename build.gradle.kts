buildscript {
    repositories {
        google()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.2.1")
        classpath(kotlin("gradle-plugin", version = "1.7.10"))
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.7.0")

        classpath("com.github.ben-manes:gradle-versions-plugin:0.42.0")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.create("clean", Delete::class) {
    delete(rootProject.buildDir)
}
