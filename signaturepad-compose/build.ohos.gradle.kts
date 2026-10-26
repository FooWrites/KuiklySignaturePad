plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    `maven-publish`
}

val kuiklyOhosVersion = "2.23.2-2.0.21-ohos"
val baseVersion = System.getenv("VERSION")
    ?.removePrefix("v")
    ?.takeIf { it.isNotBlank() }
    ?: "0.2.0"

group = "com.github.FooWrites.KuiklySignaturePad"
version = if (baseVersion.endsWith("-2.1.21-ohos")) {
    baseVersion
} else {
    "$baseVersion-2.1.21-ohos"
}

kotlin {
    ohosArm64()

    sourceSets {
        commonMain.dependencies {
            implementation("com.tencent.kuikly-open:core:$kuiklyOhosVersion")
            implementation("com.tencent.kuikly-open:compose:$kuiklyOhosVersion")
        }
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
