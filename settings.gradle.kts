pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    // Lets Gradle auto-provision the right JDK for whoever builds this project.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "DummyTvApp"

include(":shared")
include(":androidApp")

// NOTE: there is no separate ":webApp" Gradle module. The `shared` module's
// own wasmJs target (see shared/build.gradle.kts) is configured with
// `binaries.executable()`, so it directly produces the runnable browser
// distribution at shared/build/dist/wasmJs/productionExecutable -- adding a
// second module just to re-wrap that would be unneeded indirection for this
// POC (see the brief's "avoid ... unnecessary abstractions").
//
// NOTE: iosApp is intentionally NOT included here either.
// It is a plain Xcode project (iosApp/iosApp.xcodeproj), not a Gradle module.
// Xcode invokes Gradle itself (via a "Run Script" build phase) to produce the
// Kotlin/Native framework and embed it into the iOS app bundle. See README.md.
