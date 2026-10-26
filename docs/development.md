# 开发文档

## 工程结构

| 目录 | 说明 |
|---|---|
| `signaturepad` | Kuikly DSL 组件库 |
| `signaturepad-compose` | Compose DSL 组件库 |
| `shared` | Demo 页面 |
| `androidApp` / `iosApp` / `ohosApp` / `h5App` | 各平台宿主应用 |

Android、iOS 和 H5 使用 `settings.gradle.kts` 和 `build.gradle.kts`，
基于 Kuikly `2.29.2-2.1.21`。HarmonyOS 使用 `settings.ohos.gradle.kts` 和 `build.ohos.gradle.kts`，
基于 Kotlin `2.0.21-KBA-010` 和 Kuikly `2.23.2-2.0.21-ohos`。

## Demo 页面

默认入口是 `signature_pad_demo_home`，首页可跳转到：

- `signature_pad_demo`：Kuikly DSL；
- `signature_pad_compose_demo`：Compose DSL。

各平台使用相同页面名。H5 可通过 `?page_name=signature_pad_demo` 或
`?page_name=signature_pad_compose_demo` 直接打开对应示例页面。

## 构建

Android 和 H5：

```bash
./gradlew :androidApp:assembleDebug --no-daemon
./gradlew :h5App:jsBrowserProductionWebpack --no-daemon
```

iOS 需要在 macOS 上先生成共享 framework，再运行 CocoaPods：

```bash
./gradlew :shared:generateDummyFramework
cd iosApp
pod install
```

HarmonyOS 需要 DevEco Studio 提供的 OHOS SDK、`ohpm` 和 `hvigorw`：

```bash
cd ohosApp
ohpm install --all --strict_ssl true
hvigorw assembleApp --mode project -p product=default -p buildMode=debug --no-daemon
```

## 本地发布

Android、iOS 和 H5：

```bash
VERSION=0.2.0 ./gradlew :signaturepad:publishToMavenLocal \
  :signaturepad-compose:publishToMavenLocal --no-daemon
```

HarmonyOS：

```bash
VERSION=0.2.0 ./gradlew -c settings.ohos.gradle.kts \
  publishOhosToMavenLocal --no-daemon
```

省略 `VERSION` 时默认为 `0.2.0`。HarmonyOS 产物的版本号会自动追加 `-2.1.21-ohos`。
