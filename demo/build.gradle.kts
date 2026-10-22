import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
    kotlin("plugin.compose")
    id("com.android.library")
    id("com.google.devtools.ksp")
}

val kuiklyVersion: String by project

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    js(IR) {
        moduleName = "nativevue2"
        browser {
            webpackTask {
                outputFileName = "nativevue2.js"
            }
            commonWebpackConfig {
                output?.library = null
                devtool = "source-map"
            }
        }
        binaries.executable()
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    cocoapods {
        name = "demo"
        summary = "KuiklySignaturePad demo pages"
        homepage = "https://github.com/FooWrites/KuiklySignaturePad"
        version = "1.0"
        ios.deploymentTarget = "14.1"
        podfile = project.file("../iosApp/Podfile")
        framework {
            baseName = "demo"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("com.tencent.kuikly-open:core-annotations:$kuiklyVersion")
            implementation(project(":signaturepad"))
            implementation(project(":signaturepad-compose"))
        }
    }
}

ksp {
    arg("pageName", (findProperty("pageName") as? String) ?: "")
}

dependencies {
    // JS source sets fall through to the Android entry, which implements
    // IKuiklyCoreEntry. That type is not in the published core-js klib, so
    // KSP is not applied to JS. iOS entry does not use that type.
    val kspProcessor = "com.tencent.kuikly-open:core-ksp-jvm:$kuiklyVersion"
    add("kspAndroid", kspProcessor)
    add("kspIosArm64", kspProcessor)
    add("kspIosX64", kspProcessor)
    add("kspIosSimulatorArm64", kspProcessor)
}

android {
    namespace = "com.kuikly.signature.demo"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}
