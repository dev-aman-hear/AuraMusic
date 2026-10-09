import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val signingPropsFile = rootProject.file("signing.properties")
val signingProps = Properties()
if (signingPropsFile.exists()) {
    signingProps.load(signingPropsFile.inputStream())
}

android {
    namespace = "com.aman.auramusic"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aman.auramusic"
        minSdk = 26
        targetSdk = 36
        versionCode = 11
        versionName = "3.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appName"] = "Aura Music"
    }

    signingConfigs {
        create("release") {
            storeFile = signingProps["STORE_FILE"]?.let { file(it) }
            storePassword = signingProps["STORE_PASSWORD"] as String?
            keyAlias = signingProps["KEY_ALIAS"] as String?
            keyPassword = signingProps["KEY_PASSWORD"] as String?
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["appName"] = "Aura Music (Debug)"
        }

        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["appName"] = "Aura Music"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

android {

    splits {
        abi {
            isEnable = true
            reset()

            include(
                "arm64-v8a",
                "armeabi-v7a",
                "x86",
                "x86_64"
            )

            isUniversalApk = true
        }
    }
}

dependencies {

    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.videolan.android:libvlc-all:3.6.0-eap14")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.palette)
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-session:1.2.1")
    implementation("androidx.media3:media3-common:1.2.1")
    implementation("androidx.media3:media3-datasource:1.2.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.2.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:13.0.0")
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.media:media:1.7.0")

// Album Art
    implementation("io.coil-kt:coil-compose:2.7.0")

// ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")

// Runtime Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}
