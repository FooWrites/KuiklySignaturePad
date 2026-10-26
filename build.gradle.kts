plugins {
    // 统一声明各子模块使用的插件版本
    id("com.android.application").version("8.2.2").apply(false)
    id("com.android.library").version("8.2.2").apply(false)
    kotlin("android").version("2.1.21").apply(false)
    kotlin("multiplatform").version("2.1.21").apply(false)
    id("com.google.devtools.ksp").version("2.1.21-2.0.2").apply(false)
    id("org.jetbrains.compose").version("1.7.3").apply(false)
    kotlin("plugin.compose").version("2.1.21").apply(false)
}

buildscript {
    repositories {
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-public/") }
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.tencent.kuikly-open:core-gradle-plugin:2.29.2-2.1.21")
    }
}
