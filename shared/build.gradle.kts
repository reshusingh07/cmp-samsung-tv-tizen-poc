import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    // ---- Android -----------------------------------------------------
    android {
        namespace = "com.example.dummytvapp.shared"
        compileSdk = 37
        minSdk = 24

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // ---- Apple: iOS + tvOS -----------------------------------------------
    // iOS: arm64 (real devices) + simulatorArm64 (Apple Silicon Mac simulator,
    // which is what current Xcode defaults to). Compose Multiplatform 1.11+
    // dropped support for Apple x86_64 targets entirely (see its release
    // notes), so there is no iosX64 here for an Intel Mac simulator either.
    //
    // tvOS: the same two shapes (Apple TV hardware + Apple Silicon simulator).
    // JetBrains does not ship tvOS Compose klibs; they come from the
    // `dev.sajidali.*` fork via the `dev.sajidali.compose-tvos` settings plugin
    // applied in settings.gradle.kts, which only publishes tvosArm64 and
    // tvosSimulatorArm64 (no tvosX64). See docs/CMP_TVOS_GUIDE.md.
    //
    // Each target gets a static framework named `shared`; the Xcode projects in
    // iosApp/ and tvosApp/ link it via `-framework shared`, and Kotlin's
    // `embedAndSignAppleFrameworkForXcode` task picks the right target from the
    // SDK Xcode is building for (iphoneos / iphonesimulator / appletvos /
    // appletvsimulator).
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        tvosArm64(),
        tvosSimulatorArm64(),
    ).forEach { appleTarget ->
        appleTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    // ---- Web / Wasm -----------------------------------------------------
    // This is the target that ultimately becomes the Tizen Web Application.
    // NOTE: `wasmJs` is still an opt-in ("@OptIn(ExperimentalWasmDsl::class)")
    // Gradle DSL entry point as of Kotlin 2.4.10 / Compose Multiplatform 1.12.0
    // -- Kotlin/Wasm itself is Beta (see kotlinlang.org/docs/wasm-overview.html)
    // even though it is what Compose Multiplatform's web target compiles to.
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "shared"
        browser {
            commonWebpackConfig {
                outputFileName = "shared.js"
            }
        }
        binaries.executable()
    }

    compilerOptions {
        // Needed for the expect/actual top-level functions we declare under
        // shared/.../platform (PlatformBackHandler, PlatformInputBridge).
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            // Explicit coordinates rather than the Compose Gradle plugin's
            // `compose.runtime` / `compose.material3` accessors: those are
            // deprecated in 1.12.0, and the material3 one would pin a version
            // that has no tvOS klib (see gradle/libs.versions.toml).
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.ui)
            api(libs.compose.components.resources)
            // Roku-style fixed-focus D-pad navigation (vendored; see :roku-focus-list).
            api(project(":roku-focus-list"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
        }
        // iosMain and tvosMain both inherit from appleMain (Kotlin's default
        // source-set hierarchy), which is where the Swift-facing entry point and
        // the Apple actuals of the platform/ expect declarations live -- they
        // are identical for iOS and tvOS, so there is nothing to duplicate.
    }
}
