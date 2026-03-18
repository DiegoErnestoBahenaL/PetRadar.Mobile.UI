plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

@Suppress("DEPRECATION")
android {
    namespace = "com.example.petradar"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.petradar"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // AppCompat for Material Components
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.androidx.recyclerview)

    // Retrofit for HTTP calls
    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.gson)

    // OkHttp for logging
    implementation(libs.squareup.okhttp.logging)

    // Coroutines for asynchronous calls
    implementation(libs.kotlinx.coroutines.android)

    // ViewModel and LiveData
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.ktx)

    // Compose Animation
    implementation(libs.androidx.compose.animation)

    // Compose Icons Extended
    implementation(libs.androidx.compose.icons.extended)

    // Compose LiveData integration
    implementation(libs.androidx.compose.runtime.livedata)

    // Coil — image loading for Compose
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}