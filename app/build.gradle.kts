plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}
android {
    namespace = "com.example.testdialer"
    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    defaultConfig {
        applicationId = "com.example.testdialer"
        minSdk = 26
        targetSdk = 36
        versionCode = System.getenv("TEST_DIALER_VERSION_CODE")?.let { value ->
            requireNotNull(value.toIntOrNull()?.takeIf { it in 1..2_100_000_000 }) { "Invalid TEST_DIALER_VERSION_CODE" }
        } ?: 1
        versionName = System.getenv("TEST_DIALER_VERSION_NAME") ?: "1.0-dev"
    }
    signingConfigs {
        create("stable") {
            System.getenv("ANDROID_KEYSTORE_PATH")?.let { storeFile = file(it) }
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_KEY_ALIAS")
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
        }
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "Test Dialer Dev")
        }
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("stable")
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")
    implementation("androidx.room:room-runtime:2.7.2")
    kapt("androidx.room:room-compiler:2.7.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.room:room-testing:2.7.2")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.robolectric:robolectric:4.14.1")
}

val validateReleaseSigning = tasks.register("validateReleaseSigning") {
    doLast {
        listOf("ANDROID_KEYSTORE_PATH", "ANDROID_KEYSTORE_PASSWORD", "ANDROID_KEY_ALIAS", "ANDROID_KEY_PASSWORD",
            "TEST_DIALER_VERSION_CODE", "TEST_DIALER_VERSION_NAME").forEach { name ->
            require(!System.getenv(name).isNullOrBlank()) { "Missing release configuration: $name" }
        }
        require(System.getenv("ANDROID_KEY_ALIAS") == "test-dialer") { "Unexpected signing alias" }
        require(file(System.getenv("ANDROID_KEYSTORE_PATH")).isFile) { "Signing keystore unavailable" }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(validateReleaseSigning) }
