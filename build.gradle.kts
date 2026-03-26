// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
}

/**
 * Android has no JVM-style `run` task. IDEs or scripts that invoke `gradlew run` map to installing **devDebug**
 * on a connected device/emulator (same as `:app:installDevDebug`). Use **Run ▶** in Android Studio for full launch.
 */
tasks.register("run") {
    group = "application"
    description = "Installs the devDebug APK on a connected device or emulator."
    dependsOn(":app:installDevDebug")
}