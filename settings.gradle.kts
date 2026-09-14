rootProject.name = "kortex"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":library:code")
project(":library:code").projectDir = file("library/code")

include(":library:code-compose")
project(":library:code-compose").projectDir = file("library/code-compose")

include(":library:core")
project(":library:core").projectDir = file("library/core")

include(":library:core-compose")
project(":library:core-compose").projectDir = file("library/core-compose")

include(":example")
project(":example").projectDir = file("example")