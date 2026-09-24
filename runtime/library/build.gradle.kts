import com.android.build.gradle.LibraryExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

group = System.getenv("GROUP") ?: "com.github.mobcoding"
version = System.getenv("VERSION") ?: "1.1.4"

extensions.configure<LibraryExtension> {
    namespace = "io.nerdythings.okhttp.profiler"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:okhttp:4.12.0")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = project.group.toString()
                artifactId = System.getenv("ARTIFACT") ?: "OkHttpSniffer"
                version = project.version.toString()
                from(components["release"])

                pom {
                    name.set("OkHttpSniffer Runtime")
                    description.set("OkHttp interceptor that sends debug request data to the OkHttpSniffer Android Studio plugin.")
                    url.set("https://github.com/mobcoding/OkHttpSniffer")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    scm {
                        url.set("https://github.com/mobcoding/OkHttpSniffer")
                        connection.set("scm:git:https://github.com/mobcoding/OkHttpSniffer.git")
                    }
                }
            }
        }
    }
}
