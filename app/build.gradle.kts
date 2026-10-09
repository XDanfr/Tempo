plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val releaseSigning = listOf(
    "TEMPO_KEYSTORE_PATH", "TEMPO_KEYSTORE_PASSWORD", "TEMPO_KEY_ALIAS", "TEMPO_KEY_PASSWORD"
).associateWith { providers.environmentVariable(it).orNull }
val hasReleaseSigning = releaseSigning.values.any { !it.isNullOrBlank() }
require(!hasReleaseSigning || releaseSigning.values.all { !it.isNullOrBlank() }) {
    "Release signing requires TEMPO_KEYSTORE_PATH, TEMPO_KEYSTORE_PASSWORD, TEMPO_KEY_ALIAS and TEMPO_KEY_PASSWORD."
}

android {
    namespace = "cc.xdan.tempo"
    compileSdk = 36
    defaultConfig {
        applicationId = "cc.xdan.tempo"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
    }
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseSigning.getValue("TEMPO_KEYSTORE_PATH")!!)
                storePassword = releaseSigning.getValue("TEMPO_KEYSTORE_PASSWORD")
                keyAlias = releaseSigning.getValue("TEMPO_KEY_ALIAS")
                keyPassword = releaseSigning.getValue("TEMPO_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:schedule"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.activity.compose)
    implementation("androidx.core:core-ktx:1.17.0")
    implementation(libs.lifecycle.compose)
    implementation(libs.lifecycle.viewmodel)
    debugImplementation(libs.compose.tooling)
}
