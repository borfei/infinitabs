plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "id.ryenyuku.infinitabs"
    compileSdk = 35

    defaultConfig {
        applicationId = "id.ryenyuku.infinitabs"
        minSdk = 24
        targetSdk = 35
        versionCode = 20
        versionName = "2.0"

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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.gson)
    implementation(libs.swiperefreshlayout)
    implementation(libs.okhttp)
    implementation(libs.exifinterface)
    implementation(libs.webkit)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}