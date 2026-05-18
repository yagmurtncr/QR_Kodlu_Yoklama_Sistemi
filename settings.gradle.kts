pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // MPAndroidChart ve bazı özel kütüphaneler için gerekli market
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "QR_Kodlu_Yoklama_Sistemi"
include(":app")