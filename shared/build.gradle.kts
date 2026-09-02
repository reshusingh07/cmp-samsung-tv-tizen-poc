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

    // ---- iOS -----------------------------------------------------------
    // arm64 (real devices) + simulatorArm64 (Apple Silicon Mac simulator,
    // which is what current Xcode defaults to). Compose Multiplatform 1.11+
    // dropped support for Apple x86_64 targets entirely (see its release
    // notes), so there is no iosX64 here for an Intel Mac simulator either.
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
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
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            api(compose.components.resources)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
        }
    }
}
