plugins {
    kotlin("multiplatform")
}

kotlin {
    js(IR) {
        browser {
            webpackTask {
                outputFileName = "h5App.js"
            }
            commonWebpackConfig {
                output?.library = null
            }
        }
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation("com.tencent.kuikly-open.core-render-web:base:2.29.2-2.1.21")
                implementation("com.tencent.kuikly-open.core-render-web:h5:2.29.2-2.1.21")
            }
        }
    }
}
