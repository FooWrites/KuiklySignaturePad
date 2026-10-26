pluginManagement {
    repositories {
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/") }
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-public/") }
        google()
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/") }
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-public/") }
        google()
        mavenCentral()
        mavenLocal()
    }
}

val ohosBuildfFileName = "build.ohos.gradle.kts"
rootProject.buildFileName = ohosBuildfFileName
rootProject.name = "KuiklySignaturePadShell"

include(":shared")
include(":signaturepad")
include(":signaturepad-compose")
project(":shared").buildFileName = ohosBuildfFileName
project(":signaturepad").buildFileName = ohosBuildfFileName
project(":signaturepad-compose").buildFileName = ohosBuildfFileName
