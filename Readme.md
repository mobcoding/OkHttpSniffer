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

Add the profiler library to the app module:

```kotlin
dependencies {
    debugImplementation("io.nerdythings:okhttp-profiler:1.1.1")
}
```

Add the interceptor only to debug builds:

```kotlin
val client = OkHttpClient.Builder().apply {
    if (BuildConfig.DEBUG) {
        addInterceptor(OkHttpProfilerInterceptor())
    }
}.build()
```

For Retrofit, pass this client to `Retrofit.Builder.client(client)`.

Do not enable the profiler interceptor in release builds because captured requests may contain sensitive data.

## License

Apache License 2.0. See [LICENSE](LICENSE).
