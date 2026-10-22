# KuiklySignaturePad

Kuikly 手写签名板，自研 DSL 与 Compose DSL 都能用。

作者：[@FooWrites](https://github.com/FooWrites)

## 效果

自研 DSL · Android

<table>
  <tr>
    <td><img src="docs/screenshots/signaturepad-empty.png" alt="空态" width="240" /></td>
    <td><img src="docs/screenshots/signaturepad-stroke.png" alt="书写后" width="240" /></td>
    <td><img src="docs/screenshots/signaturepad-exported.png" alt="导出预览" width="240" /></td>
  </tr>
  <tr>
    <td align="center">空态</td>
    <td align="center">书写后</td>
    <td align="center">导出预览</td>
  </tr>
</table>

空态显示占位「请在此签名」，状态为「已清除」。书写后笔迹留在画板上，状态仍是「已清除」。导出后状态为「已导出」，并出现预览图。

## 安装

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}
```

自研 DSL：

```kotlin
implementation("com.github.FooWrites.KuiklySignaturePad:signaturepad:v0.1.1")
```

Compose DSL：

```kotlin
implementation("com.github.FooWrites.KuiklySignaturePad:signaturepad-compose:v0.1.1")
```

`signaturepad-compose` 已传递依赖 `signaturepad`。本库基于 Kotlin 2.1.21 / Kuikly 2.4.2-2.0.21 构建。

## 自研 DSL

```kotlin
SignaturePad {
    ref { padRef = it }
    attr {
        height(240f)
        placeholder("请在此签名")
        placeholderFontSize(16f)
        penColor(Color.BLACK)
        penWidth(3f)
    }
}

padRef.view?.clear()
padRef.view?.toImage { dataUri ->
    // dataUri 可预览，也可直接上传服务端；失败为 null
}
```

颜色用 `com.tencent.kuikly.core.base.Color`。默认导出 data URI；要文件路径或改采样用 `exportImage(ImageType.FILE)` / `exportImage(..., sampleSize)`。

## Compose DSL

```kotlin
val controller = remember { SignaturePadController() }

SignaturePad(
    modifier = Modifier.fillMaxWidth().height(240.dp),
    placeholder = "请在此签名",
    placeholderFontSize = 16f,
    controller = controller,
)

controller.clear()
controller.toImage { dataUri ->
    // 同上，拿去上传或预览
}
```

进阶导出用 `controller.exportImage(...)`。

## 示例

`:demo` 两页，不发布：

- `signature_pad_demo`：自研 DSL，各端默认页
- `signature_pad_compose_demo`：Compose DSL

四个宿主壳也不发布。`jitpack.yml` 只发布 `:signaturepad` 和 `:signaturepad-compose`。

| 宿主 | 说明 |
| --- | --- |
| `:androidApp` | Android 宿主，默认打开自研 DSL 页。 |
| `iosApp/` | iOS 宿主，XcodeGen 与 CocoaPods。 |
| `ohosApp/` | HarmonyOS 宿主，`@kuikly-open/render` 为 `2.4.2`。 |
| `:h5App` | H5 宿主。JS 产物没有页面入口，浏览器里看不到签名页。 |

KSP（`com.tencent.kuikly-open:core-ksp-jvm`，KSP `2.1.21-2.0.2`）给 Android 和 iOS 生成 `KuiklyCoreEntry`。已发布的 `core-js` 没有 `IKuiklyCoreEntry`，JS 源集没有挂 KSP，也没有页面注册代码。

### Android

默认页是自研 DSL。Compose DSL 用第二个启动命令。

```bash
./gradlew :androidApp:assembleDebug
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell am start -n com.foowrites.kuikly.host/.KuiklyRenderActivity --es pageName signature_pad_demo
adb shell am start -n com.foowrites.kuikly.host/.KuiklyRenderActivity --es pageName signature_pad_compose_demo
```

不传 `pageName` 时打开 `signature_pad_demo`。

### iOS

`iosApp/` 用 XcodeGen 和 CocoaPods。`OpenKuiklyIOSRender` 固定 KuiklyUI tag `2.4.2`。在 macOS 上：

```bash
brew install xcodegen cocoapods
./gradlew :demo:generateDummyFramework
cd iosApp
xcodegen generate
pod install
open iosApp.xcworkspace
```

默认页 `signature_pad_demo`。Compose 页：`KUIKLY_PAGE=signature_pad_compose_demo`。`Podfile` 不要加 `use_frameworks!`。

### HarmonyOS

`ohosApp/` 是 DevEco 工程，默认页 `signature_pad_demo`。`@kuikly-open/render` 为 `2.4.2`，与 KMP 制品 `2.4.2-2.0.21` 对应。`:demo` 没有 OHOS 目标，库也不发布 OHOS klib。marketplace 条目不包含 Ohos。

```bash
cd ohosApp
ohpm install --all --strict_ssl true
hvigorw assembleApp --mode project -p product=default -p buildMode=debug --no-daemon
```

页面参数 `pageName` 可传 `signature_pad_compose_demo`。

### H5

`:h5App` 依赖 `com.tencent.kuikly-open.core-render-web:base` 和 `:h5`，版本 `2.4.0-2.0.21`。`:demo` 的开发服务器在 8083，提供 `nativevue2.js`。已发布的 `core-js` 没有 `IKuiklyCoreEntry`，JS 没有页面入口，浏览器里看不到签名页。

```bash
./gradlew :demo:jsBrowserDevelopmentRun
# 另一个终端
./gradlew :h5App:jsBrowserDevelopmentRun
```

开发服务器地址是 <http://127.0.0.1:8080/> ，Compose 页参数是 `?page_name=signature_pad_compose_demo`。

## 许可

Apache License 2.0

Copyright (c) 2026 FooWrites
