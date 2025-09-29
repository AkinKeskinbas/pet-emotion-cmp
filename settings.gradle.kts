rootProject.name = "PetEmotions"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()

        // For MOKO permissions
        maven("https://maven.pkg.jetbrains.space/public/p/ktor/eap")

        // For Vico charts
        maven("https://s01.oss.sonatype.org/content/repositories/releases/")

        // Additional common repositories
        gradlePluginPortal()
        google()
    }
}

include(":composeApp")