buildscript {
    repositories {
        google()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.0")
        classpath(kotlin("gradle-plugin", version = "1.9.0"))
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.8.20")

        classpath("com.github.ben-manes:gradle-versions-plugin:0.47.0")
    }
}

tasks.create("clean", Delete::class) {
    delete(rootProject.buildDir)
}
