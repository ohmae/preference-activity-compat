buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.1.2")
        classpath(kotlin("gradle-plugin", version = "1.4.30"))
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.4.20")

        classpath("com.github.ben-manes:gradle-versions-plugin:0.36.0")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

tasks.create("clean", Delete::class) {
    delete(rootProject.buildDir)
}
