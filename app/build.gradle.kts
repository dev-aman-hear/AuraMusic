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
        versionCode = 19
        versionName = "1.0.6"
        // Use the deployed AuraMusic Party backend by default; allow local/CI overrides with -PpartyApiBaseUrl=...
        val partyApiBaseUrl = providers.gradleProperty("partyApiBaseUrl").orNull
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "https://auramusic-party-server.onrender.com"
        buildConfigField("String", "PARTY_API_BASE_URL", "\"$partyApiBaseUrl\"")

        manifestPlaceholders["appName"] = "Aura Music"
    }

    flavorDimensions += listOf("brand")
    productFlavors {
        create("aura") {
            dimension = "brand"
            applicationId = "com.aman.auramusic"
            manifestPlaceholders["appName"] = "Aura Music"
        }
        create("heytap") {
            dimension = "brand"
            applicationId = "com.heytap.music"
            manifestPlaceholders["appName"] = "Aura Music"
        }
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
        }

        release {
            isMinifyEnabled = false
            signingConfig = if (signingPropsFile.exists() && signingProps["STORE_FILE"] != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a") //"x86", "x86_64" ,"armeabi-v7a"
            isUniversalApk = true
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
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
    implementation("androidx.media3:media3-common:1.2.1")
    implementation("androidx.media3:media3-datasource:1.2.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.nanohttpd:nanohttpd-websocket:2.3.1")
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:13.0.0")
    implementation("com.google.code.gson:gson:2.10.1")
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation & Storage
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.media:media:1.7.0")

    // Coil & ViewModel
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")

    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Retrofit & Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    testImplementation(libs.junit)
    testImplementation("org.json:json:20231013")
}
