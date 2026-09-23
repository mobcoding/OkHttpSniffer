# OkHttpSniffer Android Studio Plugin

Based on the OkHttp Profiler plugin by Eugene Tkachenko and Hanna Tkachenko.

OkHttpSniffer shows requests from OkHttp directly in the Android Studio tool window. It supports OkHttp v3 and Retrofit v2.

You can inspect request and response headers, raw payloads, JSON trees, and formatted JSON. You can also generate Java or Kotlin models from JSON by right-clicking a tree node and selecting the desired model type and output folder.

![Plugin screenshot](https://github.com/itkacher/OkHttpProfiler/blob/master/demo.png?raw=true)

## Requirements

- Android Studio 2026.1 or newer
- JDK 21

## Build

Set `StudioCompilePath` to the Android Studio installation directory, then run:

```powershell
.\gradlew.bat clean test buildPlugin verifyPluginStructure -PStudioCompilePath="C:\Program Files\Android\Android Studio"
```

The plugin ZIP is generated under `build/distributions`.

## Android App Setup

Add JitPack to dependency resolution:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}
```

Add the OkHttpSniffer runtime library to the app module:

```kotlin
dependencies {
    debugImplementation("com.github.mobcoding:OkHttpSniffer:1.1.3")
}
```

Add the interceptor only to debug builds:

```kotlin
import io.nerdythings.okhttp.profiler.OkHttpProfilerInterceptor

val client = OkHttpClient.Builder().apply {
    if (BuildConfig.DEBUG) {
        addInterceptor(OkHttpProfilerInterceptor())
    }
}.build()
```

For Retrofit, pass this client to `Retrofit.Builder.client(client)`.

Do not enable the profiler interceptor in release builds because captured requests may contain sensitive data.

The JitPack artifact is built from the standalone Android library under `runtime/library`. The Android Studio plugin and runtime library remain independently buildable.

## License

Apache License 2.0. See [LICENSE](LICENSE).
