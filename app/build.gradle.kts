plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("jacoco")
}

android {
    namespace = "co.stonephone.stonecamera"
    compileSdk = 34

    defaultConfig {
        applicationId = "co.stonephone.stonecamera"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "none"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15" // Matches Compose BOM 2024.12.01
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Compose BOM
    implementation(libs.androidx.compiler)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.monitor)
    implementation(libs.androidx.junit.ktx)
    testImplementation(libs.junit.junit)

    debugImplementation(libs.androidx.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // CameraX
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.video)

    // Accompanist Permissions
    implementation(libs.accompanist.permissions)

    // Coil
    implementation(libs.coil.compose)

    // ML Kit for Barcode/QR scanning
    implementation(libs.barcode.scanning)
}


tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest") // Ensure unit tests are run

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*"
    )

    val debugTree =
        fileTree(mapOf("dir" to "$buildDir/tmp/kotlin-classes/debug", "excludes" to fileFilter))
    val mainSrc = "$projectDir/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(
        fileTree(
            mapOf(
                "dir" to "$buildDir",
                "includes" to listOf("jacoco/testDebugUnitTest.exec")
            )
        )
    )
}