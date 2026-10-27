plugins {
    kotlin("multiplatform")
    `maven-publish`
}

val kuiklyOhosVersion = property("KUIKLY_OHOS_VERSION") as String

group = "com.github.FooWrites.KuiklySignaturePad"
// JitPack 以 tag 作为 VERSION，tag 需与 {MAVEN_VERSION}-{OHOS_KOTLIN_VERSION} 一致
version = System.getenv("VERSION")?.takeIf { it.isNotBlank() }
    ?: "${property("MAVEN_VERSION")}-${property("OHOS_KOTLIN_VERSION")}"

kotlin {
    ohosArm64()

    sourceSets {
        commonMain.dependencies {
            implementation("com.tencent.kuikly-open:core:$kuiklyOhosVersion")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("signaturepad")
            description.set("Kuikly signature pad — Kuikly DSL.")
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
