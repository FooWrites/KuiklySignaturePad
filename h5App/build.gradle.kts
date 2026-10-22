plugins {
    kotlin("multiplatform")
}

val webRenderVersion = "2.4.0-2.0.21"

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
                // Published h5 metadata does not pull base, but the host sources use both.
                api("com.tencent.kuikly-open.core-render-web:base:$webRenderVersion")
                api("com.tencent.kuikly-open.core-render-web:h5:$webRenderVersion")
            }
        }
    }
}
