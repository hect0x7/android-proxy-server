import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
  alias(libs.plugins.android)
}

android {
  namespace = "com.hect0x7.proxy.core"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig { minSdk = libs.versions.minSdk.get().toInt() }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  buildFeatures { buildConfig = false }

  packaging {
    resources.pickFirsts +=
        setOf(
            "META-INF/INDEX.LIST",
            "META-INF/io.netty.versions.properties",
        )
  }
}

kotlin {
  compilerOptions {
    languageVersion = KotlinVersion.KOTLIN_2_3
    jvmTarget = JvmTarget.JVM_21
  }
}

dependencies {
  api(libs.kotlinx.coroutines)
  implementation(libs.netty.codec.http)
  implementation(libs.netty.codec.socks)
  implementation(libs.slf4j.nop)

  testImplementation(libs.junit)
  testImplementation(kotlin("test-junit"))
  testImplementation(libs.kotlinx.coroutines.test)
}
