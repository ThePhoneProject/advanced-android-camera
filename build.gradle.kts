// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Versions plugin, etc. as needed
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // e.g., id("com.android.application") version "8.1.0" apply false
    // e.g., id("com.android.library") version "8.1.0" apply false
    // ...
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
