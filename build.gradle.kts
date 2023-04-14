buildscript {
    repositories {
        google()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.0.0")
        classpath(kotlin("gradle-plugin", version = "1.8.20"))
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.8.10")

        classpath("com.github.ben-manes:gradle-versions-plugin:0.46.0")
    }
}

tasks.create("clean", Delete::class) {
    delete(rootProject.buildDir)
}
