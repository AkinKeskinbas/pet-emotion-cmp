import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)

}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        named { it.lowercase().startsWith("ios") }.configureEach {
            languageSettings {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            // Camera
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation("androidx.camera:camera-extensions:1.5.0")

            // DataStore for Android
            implementation("androidx.datastore:datastore-preferences:1.1.1")

            // Ktor HTTP client engine for Android
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            // Ktor HTTP client engine for iOS
            implementation("io.ktor:ktor-client-darwin:3.2.3")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.androidx.navigation.compose)

            implementation(libs.koin.compose)
            implementation(libs.koin.compose.vm)
            implementation(libs.koin.compose.nav)
            implementation(libs.koin.core)

            implementation(libs.sky.dove.bottom.sheet)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.logging)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")

            implementation(libs.purchases.core)
            implementation(libs.purchases.datetime)   // Optional
            implementation(libs.purchases.either)     // Optional
            implementation(libs.purchases.result)

            // DateTime and UUID
            implementation(libs.kotlinx.datetime)
            implementation(libs.uuid)

            // DataStore KMP
            implementation(libs.androidx.datastore.preferences.core)

            // For iOS file paths
            implementation("com.squareup.okio:okio:3.6.0")

            // Camera K for cross-platform camera functionality (experimental)
            // implementation("io.github.kashif-mehmood-km:camerak:0.0.12")

            // Permissions - TODO: Fix repository or find alternative
            // implementation(libs.permissions.compose)

            // Charts - TODO: Fix repository or find alternative
            // implementation(libs.vico.compose)
            // implementation(libs.vico.compose.m3)
            // implementation(libs.vico.core)

        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.keak.petemotions"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.keak.petemotions"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    lint {
        disable.add("NullSafeMutableLiveData")
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

