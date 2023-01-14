buildscript {
    repositories {
        google()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.4.0")
        classpath(kotlin("gradle-plugin", version = "1.8.0"))
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.7.20")

        classpath("com.github.ben-manes:gradle-versions-plugin:0.44.0")
    }
}

tasks.create("clean", Delete::class) {
    delete(rootProject.buildDir)
}
