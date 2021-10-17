buildscript {
    repositories {
        google()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.3")
        classpath(kotlin("gradle-plugin", version = "1.5.31"))
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.5.31")

        classpath("com.github.ben-manes:gradle-versions-plugin:0.39.0")
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
