import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.library")
    `maven-publish`
}

val kuiklyVersion: String by project

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    js(IR) {
        browser()
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    sourceSets {
        commonMain.dependencies {
            api("com.tencent.kuikly-open:core:$kuiklyVersion")
            api("com.tencent.kuikly-open:compose:$kuiklyVersion")
            api(project(":signaturepad"))
        }
    }
}

android {
    namespace = "com.kuikly.signature.compose"
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

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("signaturepad-compose")
            description.set("Kuikly signature pad — Compose DSL.")
            url.set("https://github.com/FooWrites/KuiklySignaturePad")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("FooWrites")
                    name.set("FooWrites")
                }
            }
            scm {
                url.set("https://github.com/FooWrites/KuiklySignaturePad")
                connection.set("scm:git:https://github.com/FooWrites/KuiklySignaturePad.git")
                developerConnection.set("scm:git:ssh://git@github.com/FooWrites/KuiklySignaturePad.git")
            }
        }
    }
}
