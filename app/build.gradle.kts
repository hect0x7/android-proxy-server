import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
}

val releaseSigningAvailable =
    listOf(
            "ANDROID_KEYSTORE_PATH",
            "ANDROID_KEYSTORE_PASSWORD",
            "ANDROID_KEY_ALIAS",
            "ANDROID_KEY_PASSWORD",
        )
        .all { !providers.environmentVariable(it).orNull.isNullOrBlank() }

android {
  namespace = "com.hect0x7.proxy"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "com.hect0x7.proxy"
    minSdk = libs.versions.minSdk.get().toInt()
    targetSdk = libs.versions.targetSdk.get().toInt()
    versionCode = 3
    versionName = "1.1.1"
  }

  signingConfigs {
    if (releaseSigningAvailable) {
      create("release") {
        storeFile = rootProject.file(providers.environmentVariable("ANDROID_KEYSTORE_PATH").get())
        storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").get()
        keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").get()
        keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").get()
      }
    }
  }

  buildTypes {
    debug {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-debug"
    }

    release {
      if (releaseSigningAvailable) {
        signingConfig = signingConfigs.getByName("release")
      }
      isMinifyEnabled = false
    }
  }

  buildFeatures { compose = true }

  packaging {
    resources.excludes += setOf(
        "META-INF/INDEX.LIST",
        "META-INF/io.netty.versions.properties",
    )
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
}

kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_21 } }

configurations.configureEach {
  exclude(group = "androidx.profileinstaller", module = "profileinstaller")
}

dependencies {
  implementation(project(":proxycore"))

  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.core)

  implementation(libs.compose.foundation)
  implementation(libs.compose.material3)
  implementation(libs.compose.ui)

  implementation(libs.kotlinx.coroutines.android)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
