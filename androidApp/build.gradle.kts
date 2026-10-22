plugins {
    id("com.android.application")
    kotlin("android")
}

val kuiklyVersion: String by project

android {
    namespace = "com.foowrites.kuikly.host"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.foowrites.kuikly.host"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.1"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":demo"))
    implementation("com.tencent.kuikly-open:core-render-android:$kuiklyVersion")
    implementation("com.tencent.kuikly-open:core:$kuiklyVersion")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
