pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Lets Gradle auto-provision the right JDK for whoever builds this project.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"

    // Adds Apple tvOS (tvosArm64 / tvosSimulatorArm64) support to Compose
    // Multiplatform. JetBrains does not publish tvOS klibs for most Compose
    // modules yet; this settings plugin injects tvOS variants published by the
    // sajidalidev/compose-multiplatform-core fork (Maven Central, group prefix
    // `dev.sajidali.*`) onto the official `org.jetbrains.*` coordinates at
    // dependency-resolution time. Only tvOS configurations are affected --
    // Android, iOS and wasmJs keep resolving the official JetBrains artifacts
    // untouched. It also swaps the `org.jetbrains.compose` Gradle plugin for the
    // fork's build of the same version so Compose Resources can be packaged for
    // tvOS. See docs/CMP_TVOS_GUIDE.md.
    id("dev.sajidali.compose-tvos") version "1.4.2"
}

// composeTvos { strictMode.set(true) } is deliberately NOT enabled. The plugin
// probes every coordinate Gradle visits while resolving, including the
// iOS-only platform leaves (`*-uikitarm64`, `*-uikitsimarm64`) of umbrella
// modules whose tvOS variant resolves fine, and old transitive candidates that
// lose conflict resolution. strictMode fails the build on those even though
// the linked graph is fine (verified: the framework links, and
// `:shared:dependencies --configuration tvosSimulatorArm64CompileKlibraries`
// shows every umbrella resolving a tvOS klib). The default WARN block at the
// end of a tvOS build reports the same list; a genuine gap shows up there as a
// coordinate that also fails to resolve. See docs/CMP_TVOS_GUIDE.md, section 7.

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DummyTvApp"

include(":shared")
include(":androidApp")

// Vendored Roku-style fixed-focus navigation library; see roku-focus-list/build.gradle.kts
// for why it is a source copy rather than a Maven coordinate.
include(":roku-focus-list")

// NOTE: there is no separate ":webApp" Gradle module. The `shared` module's
// own wasmJs target (see shared/build.gradle.kts) is configured with
// `binaries.executable()`, so it directly produces the runnable browser
// distribution at shared/build/dist/wasmJs/productionExecutable -- adding a
// second module just to re-wrap that would be unneeded indirection for this
// POC (see the brief's "avoid ... unnecessary abstractions").
//
// NOTE: iosApp and tvosApp are intentionally NOT included here either.
// They are plain Xcode projects (iosApp/iosApp.xcodeproj,
// tvosApp/tvosApp.xcodeproj), not Gradle modules. Xcode invokes Gradle itself
// (via a "Run Script" build phase) to produce the Kotlin/Native framework and
// embed it into the app bundle. See README.md.
