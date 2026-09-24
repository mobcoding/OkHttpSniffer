# OkHttpSniffer

在 Android Studio 中查看 Android 应用的 OkHttp 请求与响应，支持 JSON 查看、复制 cURL 和生成 Java/Kotlin 模型。

由 [mobcoding](https://github.com/mobcoding) 维护，基于 Eugene Tkachenko 和 Hanna Tkachenko 的 OkHttp Profiler 项目。插件通过 ADB 读取配套拦截器输出的 Logcat 数据；仅显示已接入的 OkHttp 客户端请求，不是全设备抓包代理。

## 功能

- 查看请求方法、URL、状态码、耗时、请求头和响应头。
- 查看原始请求/响应体、格式化 JSON 和 JSON 树。
- 复制 URL、响应内容和 cURL 命令。
- 从 JSON 节点生成带 Gson 注解的 Java/Kotlin 模型。
- 选择设备和应用进程；工具栏使用适配 IDE 主题与高 DPI 的原生图标。

## 版本与发布状态

IDE 插件与 Android 运行时库分别构建和管理版本，二者的版本号不需要一致。

| 组件 | 当前版本 | 获取方式 |
| --- | --- | --- |
| Android Studio 插件 | `1.2.0` | 从源码构建 ZIP，再从磁盘安装 |
| Android 运行时库 | `1.1.4` | [JitPack](https://jitpack.io/#mobcoding/OkHttpSniffer/1.1.4) |

本项目尚未发布为独立的 JetBrains Marketplace 条目。当前上架候选包使用 ID `com.mobcoding.okhttpsniffer`、名称 `OkHttpSniffer` 和 Vendor `mobcoding`，IDE 内显示为 **OkHttpSniffer**。发布准备和待办见 [Marketplace 提交指南](marketplace/提交指南.md)。

## 环境要求

- IDE 插件：声明最低平台版本为 `261`，本机构建环境为 Android Studio `2026.1.3`，构建使用 JDK 21。其他版本及未来版本的兼容性仍需 Plugin Verifier 和实际运行验证。
- Android 应用：Android API 21 或以上，并提供自己的 OkHttp 依赖。运行时库以 OkHttp `4.12.0` 编译，未将 OkHttp 作为传递依赖打包。
- 运行时库使用 Kotlin `2.2.0` 和 JVM 17 字节码；宿主的 Kotlin 编译器及 Android 构建工具需要兼容。JDK 21 是 IDE 插件的构建要求，不是 Android 应用的运行环境要求。
- 一台已授权 USB 调试的 Android 设备，或 Android 模拟器。

## 安装 IDE 插件

1. 按下方“从源码构建”生成插件 ZIP。
2. 在 Android Studio 中打开 **Settings → Plugins → 齿轮菜单 → Install Plugin from Disk…**，选择 `build/distributions` 中本次生成的 ZIP。
3. 按 IDE 提示重启，打开 Android 项目，在 **View → Tool Windows → OkHttpSniffer** 中打开工具窗口。

上架候选包使用独立插件 ID；如果同时安装上游 OkHttp Profiler，两个工具窗口可以同时出现，测试时建议只启用一个。Android 运行时 AAR 不能作为 IDE 插件安装。

## Android 应用接入

### 1. 添加仓库和依赖

在项目的 `settings.gradle.kts` 中加入 JitPack：

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}
```

在应用模块的 `build.gradle.kts` 中添加：

```kotlin
dependencies {
    debugImplementation("com.github.mobcoding:OkHttpSniffer:1.1.4")
}
```

应用应已有 OkHttp 依赖；如果尚未引入，可添加 `implementation("com.squareup.okhttp3:okhttp:4.12.0")`。

迁移时移除旧的 `io.nerdythings:okhttp-profiler` 依赖。新库为兼容现有接入保留了 `io.nerdythings.okhttp.profiler` 包名，同时引入两份库会产生重复类。

### 2. 将拦截器引用隔离到 debug 源码

使用 `debugImplementation` 时，不能在公共源码中直接引用拦截器，即使包在 `if (BuildConfig.DEBUG)` 中，release 编译时仍会找不到该类。请使用以下两套同名入口，将示例包名替换为自己的包名。

新建 `app/src/debug/java/com/example/app/network/HttpDebug.kt`：

```kotlin
package com.example.app.network

import io.nerdythings.okhttp.profiler.OkHttpProfilerInterceptor
import okhttp3.OkHttpClient

internal fun OkHttpClient.Builder.configureHttpDebug(): OkHttpClient.Builder =
    addInterceptor(OkHttpProfilerInterceptor())
```

新建 `app/src/release/java/com/example/app/network/HttpDebug.kt`：

```kotlin
package com.example.app.network

import okhttp3.OkHttpClient

internal fun OkHttpClient.Builder.configureHttpDebug(): OkHttpClient.Builder = this
```

在同一应用模块的公共源码中配置真正用于请求的客户端：

```kotlin
import com.example.app.network.configureHttpDebug
import okhttp3.OkHttpClient

val client = OkHttpClient.Builder()
    .configureHttpDebug()
    .build()
```

如果使用 Retrofit，将此客户端传给 `Retrofit.Builder().client(client)`。若有其他自定义构建类型，也需要为其提供对应的入口；不抓包的构建类型使用 release 示例中的空实现。

### 3. 查看请求

1. 连接设备或启动模拟器，运行应用的 debug 版本。
2. 打开当前名为 **OkHttpSniffer** 的工具窗口，选择目标设备和应用进程。
3. 在应用中发起请求，选中列表项查看请求头、响应体和 JSON。
4. 右键请求使用复制功能；右键 JSON 节点生成 Java/Kotlin 模型。生成的模型使用 Gson 注解，请在使用前检查字段类型和项目依赖。

## 常见问题

**没有看到请求？** 检查设备调试授权、所选进程，以及应用实际使用的 OkHttpClient 是否安装了拦截器。可以在 Logcat 中搜索 `OKPRFL_`。WebView、其他网络客户端和未接入拦截器的应用不会被自动抓取。

**release 编译找不到拦截器？** 检查 `src/main` 中是否仍直接导入 `OkHttpProfilerInterceptor`，按上方示例将引用移到 `src/debug`，并提供 release 空实现。

**出现重复类？** 移除原 `io.nerdythings:okhttp-profiler` 依赖，排查其他模块或本地 AAR 是否仍包含相同的运行时类。

**响应内容不完整？** 请同时安装重新构建的插件并升级运行时至 `1.1.4`，然后重新发起请求。本版移除了插件 30 万字符和运行时 10 MB 的正文上限，并按日志字节预算切分中文/Emoji。Logcat 仍可能因设备缓冲区覆盖、断连而丢失记录；无限流不适合此全量采集方式，大响应会增加调试应用和 IDE 的内存占用。

## 调试数据

拦截器将 HTTP 数据写入 Android Logcat，插件通过 ADB 读取并在本地展示。请求头与正文不会自动脱敏，可能包含 Token、Cookie 或个人信息。仅在调试构建中启用，分享日志、截图或导出文件前请脱敏。

清空插件列表不会清除设备 Logcat、系统剪贴板或已导出的文件。当前源码的数据处理说明见 [隐私说明草稿](marketplace/privacy.md)，正式发布前仍需复核。

## 从源码构建

### Android Studio 插件

在仓库根目录使用 JDK 21，指定本机 Android Studio 安装路径：

```powershell
.\gradlew.bat clean test buildPlugin verifyPluginStructure -PStudioCompilePath="C:\Program Files\Android\Android Studio"
```

也可以通过 `ANDROID_STUDIO_HOME` 环境变量提供安装路径。当前输出为 `build/distributions/OkHttpSniffer-1.2.0.zip`；不要误用仓库中遗留的其他版本 ZIP。

`verifyPluginStructure` 仅检查包结构，不能替代 Plugin Verifier 的 API 兼容性检查或实际设备抓包验证。

### Android 运行时库

`runtime` 是独立的 Android Gradle 项目，不依赖本机 Android Studio 插件平台。准备 JDK 17 和 Android SDK 34，通过 `ANDROID_HOME` 或 `runtime/local.properties` 配置 SDK 路径，然后运行：

```powershell
cd runtime
.\gradlew.bat clean :library:assembleRelease :library:publishReleasePublicationToMavenLocal
```

AAR 输出目录为 `runtime/library/build/outputs/aar`（相对仓库根目录）。发布到本地 Maven 不等于发布到 JitPack；远程构建由仓库根目录的 `jitpack.yml` 配置。

## Marketplace 发布材料

- [中文提交指南与发布前待办](marketplace/提交指南.md)
- [英文插件介绍](marketplace/description.html)
- [英文接入说明](marketplace/getting-started.html)
- [版本更新文案](marketplace/change-notes.html)
- [隐私说明草稿](marketplace/privacy.md)
- [浅色图标](marketplace/assets/pluginIcon.svg) / [深色图标](marketplace/assets/pluginIcon_dark.svg)

这些文件是发布准备材料，图标尚未放入插件资源。正式提交前还需完成独立插件身份与链接调整、联系信息、真实界面截图、兼容性检查和运行验证。英文上架文案中的新名称应在完成更名后使用。

## 反馈与许可证

问题和建议请提交到 [GitHub Issues](https://github.com/mobcoding/OkHttpSniffer/issues)，附上 Android Studio 版本、插件版本、运行时库版本和复现步骤；请勿上传未脱敏的请求数据。

本项目采用 [Apache License 2.0](LICENSE)，上游归属见 [NOTICE](NOTICE)。感谢 [OkHttp Profiler](https://github.com/itkacher/OkHttpProfiler) 与 [原 Android Studio 插件](https://github.com/gektor650/OkHttpProfiler-AndroidStudio-Plugin) 的作者和贡献者。
