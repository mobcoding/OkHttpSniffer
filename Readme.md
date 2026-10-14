# OkHttpSniffer

在 Android Studio 中查看 OkHttp 请求、响应和 JSON，支持复制响应体、cURL 和生成模型。

- IDE 插件：`1.2.4`，适用于 Android Studio 261 平台；已验证本机 2026.1.3。
- Android 依赖：`1.1.4`，支持 Android 5.0 及以上，应用需已有 OkHttp 依赖，构建工具需兼容 Kotlin 2.2.0 和 JVM 17。

## 安装插件

在 Android Studio 的“设置 → 插件 → 齿轮菜单 → 从磁盘安装插件”中选择 `OkHttpSniffer-1.2.4.zip`，按提示重启。通过“视图 → 工具窗口 → OkHttpSniffer”打开插件。

## 1. 添加依赖

在 `settings.gradle.kts` 中添加 [JitPack](https://jitpack.io/#mobcoding/OkHttpSniffer/1.1.4)：

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

迁移时移除旧的 `io.nerdythings:okhttp-profiler` 依赖，避免重复类。新库保留原拦截器包名。

## 2. 配置拦截器

以下文件放在应用模块中，示例包名请替换为自己的包名。

`src/debug/kotlin/com/example/app/network/HttpDebug.kt`：

```kotlin
package com.example.app.network

import io.nerdythings.okhttp.profiler.OkHttpProfilerInterceptor
import okhttp3.OkHttpClient

internal fun OkHttpClient.Builder.configureHttpDebug(): OkHttpClient.Builder =
    addInterceptor(OkHttpProfilerInterceptor())
```

`src/release/kotlin/com/example/app/network/HttpDebug.kt`：

```kotlin
package com.example.app.network

import okhttp3.OkHttpClient

internal fun OkHttpClient.Builder.configureHttpDebug(): OkHttpClient.Builder = this
```

在公共源码中配置实际使用的客户端：

```kotlin
import com.example.app.network.configureHttpDebug
import okhttp3.OkHttpClient

val client = OkHttpClient.Builder()
    .configureHttpDebug()
    .build()
```

使用 Retrofit 时，将此客户端传入 `Retrofit.Builder().client(client)`。

不要在公共源码中直接引用拦截器，即使使用 `BuildConfig.DEBUG` 判断也会导致 release 编译失败。自定义构建类型需提供对应实现。

## 3. 查看请求

1. 连接已授权调试的设备或模拟器，运行应用的 debug 版本。
2. 在插件中选择设备和应用进程，再发起请求。
3. 点击请求查看响应和 JSON，右键请求复制完整响应体或 cURL。

长文本通过 `Previous / Next` 翻页，内容不会因分页丢失。`Copy all` 复制当前视图的全部文本（Raw 包含头信息）；只需要响应体时，使用列表右键的 `Copy Response Body`。JSON 树中的长值显示摘要，节点复制仍使用完整值。

## 常见问题

- **没有请求：** 检查设备授权、所选进程和实际使用的 OkHttpClient，或在 Logcat 搜索 `OKPRFL_`。仅捕获接入拦截器的客户端请求。
- **响应不完整：** 确认依赖为 `1.1.4` 并重新发起请求。Logcat 缓冲区覆盖或断连仍可能丢失数据；无限流不适合全量采集。
- **数据安全：** 仅在调试版本启用，分享截图或日志前移除 Token、Cookie 等敏感信息。清空列表不会清除设备 Logcat 或剪贴板。

## 构建与反馈

使用 JDK 21，在仓库根目录执行 `.\gradlew.bat test buildPlugin verifyPluginStructure -PStudioCompilePath="C:\Program Files\Android\Android Studio"`，产物位于 `build/distributions/OkHttpSniffer-1.2.4.zip`。

[问题反馈](https://github.com/mobcoding/OkHttpSniffer/issues) · [Marketplace 提交指南](marketplace/提交指南.md) · [简体中文接入说明](marketplace/getting-started.html) · [隐私说明](marketplace/privacy.md)

由 [mobcoding](https://github.com/mobcoding) 维护，基于 Eugene Tkachenko 和 Hanna Tkachenko 的 [OkHttp Profiler](https://github.com/itkacher/OkHttpProfiler)。采用 [Apache License 2.0](LICENSE)，上游归属见 [NOTICE](NOTICE)。
