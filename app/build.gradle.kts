plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.roborazzi)
}

// Screenshot tests (Robolectric + Roborazzi) only run when explicitly requested, e.g.
// `./gradlew recordRoborazziDebug` or `-PrecordScreenshots=true`; a plain `testDebugUnitTest`
// skips them so the regular unit-test run stays fast.
val screenshotRun = project.hasProperty("recordScreenshots") ||
    gradle.startParameter.taskNames.any { it.contains("Roborazzi", ignoreCase = true) }

android {
    namespace = "io.github.ieswar23.strikezone"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.ieswar23.strikezone"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
        unitTests.all { test ->
            if (screenshotRun) {
                test.maxHeapSize = "2g"
                if (project.hasProperty("recordScreenshots")) {
                    test.systemProperty("roborazzi.test.record", "true")
                }
            } else {
                test.exclude("**/screenshots/**")
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.gson)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.arch.core.testing) {
        // InstantTaskExecutorRule does not need Mockito; keep the test classpath lean.
        exclude(group = "org.mockito")
    }

    // Screenshot tests: Robolectric native graphics + Roborazzi, launching the real Hilt activity.
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.hilt.android.testing)
    testAnnotationProcessor(libs.hilt.compiler)
}
