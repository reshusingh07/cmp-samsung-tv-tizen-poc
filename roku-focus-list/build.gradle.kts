import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Vendored copy of https://github.com/souravnoobcoder/roku-focus-list (v2.1.0,
// commit dfd5685), Apache 2.0 -- see LICENSE in this directory.
//
// WHY VENDORED rather than `implementation("io.github.souravnoobcoder:roku-focus-list:2.1.0")`:
// the published artifact declares androidTarget, jvm("desktop") and the three
// iOS targets only. It ships no tvOS klib, and the compose-tvos redirect plugin
// cannot help -- it redirects the `org.jetbrains.*` Compose coordinates the
// library depends on, but there is no tvOS build of the library itself for it to
// point at. Until roku-focus-list publishes tvOS artifacts of its own, a source
// copy is the only way to run it on Apple TV.
//
// `src/` is byte-for-byte upstream; every difference lives in this file:
//   - tvosArm64/tvosSimulatorArm64 added, jvm("desktop") and iosX64 dropped
//     (Compose Multiplatform 1.12 has no Apple x86_64 targets at all)
//   - wasmJs added, so this POC's Samsung TV target can use it too. Upstream's
//     README already documents this as a targets-only change, which it is.
//   - publishing/signing config removed: nothing here is published
//   - Compose dependencies come from this project's version catalog, so the
//     library builds against the same 1.12.0 line as the rest of the app
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.rokufocus"
        compileSdk = 37
        minSdk = 24

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        // Upstream carries this: the Compose lint check misreads BoxWithConstraints
        // scope usage in RokuLazyRow / RokuLazyColumn, and @SuppressLint is not
        // available from commonMain.
        lint {
            disable += "UnusedBoxWithConstraintsScope"
        }
    }

    iosArm64()
    iosSimulatorArm64()
    tvosArm64()
    tvosSimulatorArm64()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.runtime.saveable)
            api(libs.compose.foundation)
            api(libs.compose.ui)
            api(libs.compose.animation)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// The library's own tests are pure state-math unit tests (`kotlin.test` only --
// no Compose UI test, no rendering). They run on the iosSimulatorArm64 and
// tvosSimulatorArm64 targets, which is the coverage that matters here.
//
// They are NOT run on wasmJs: the Compose Gradle plugin's
// `checkComposeUiTestConfigurationForWasmJs` fails the build unless the target
// declares `binaries.executable()`, so that webpack can bundle a Skiko runtime
// for Compose UI tests. This module is a library with no executable, and these
// tests never touch Skiko, so the check is a false positive -- and adding an
// executable binary to a library just to satisfy it would be worse. Upstream
// has the same tests running on a desktop JVM target that this project does not
// declare.
tasks.matching {
    it.name.contains("WasmJs", ignoreCase = true) && it.name.contains("test", ignoreCase = true)
}.configureEach {
    enabled = false
}
