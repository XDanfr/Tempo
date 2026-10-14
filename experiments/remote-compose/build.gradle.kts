plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

abstract class CopyWidgetGeometry : DefaultTask() {
    @get:InputFile abstract val source: RegularFileProperty
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty
    @TaskAction fun copyGeometry() {
        val destination = outputDirectory.file("WidgetShapeRules.kt").get().asFile
        destination.parentFile.mkdirs()
        source.get().asFile.copyTo(destination, overwrite = true)
    }
}
val copyShapeRules = tasks.register<CopyWidgetGeometry>("copyShapeRules") {
    source.set(rootProject.layout.projectDirectory.file("app/src/main/java/cc/xdan/tempo/widgets/WidgetShapeRules.kt"))
    outputDirectory.set(layout.buildDirectory.dir("generated/widgetGeometry"))
}
androidComponents.onVariants { variant ->
    variant.sources.kotlin?.addGeneratedSourceDirectory(copyShapeRules, CopyWidgetGeometry::outputDirectory)
}
android {
    namespace = "cc.xdan.tempo.remote"
    compileSdk = 37
    defaultConfig {
        applicationId = "cc.xdan.tempo.remoteexperiment"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }
    buildFeatures { compose = true }
    testOptions.unitTests.isIncludeAndroidResources = true
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation("androidx.glance:glance-appwidget:1.3.0-alpha02")
    implementation("androidx.glance.adaptive:adaptive-appwidget:1.0.0-alpha03")
    implementation("androidx.compose.remote:remote-creation-android:1.0.0-alpha21")
    implementation("androidx.compose.remote:remote-player-view:1.0.0-alpha21")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.robolectric:robolectric:4.17")
}

// Android 16/17 framework simulation requires Java 21 in Robolectric, independently
// of the APK's Java 17 bytecode/toolchain and the production app's test launcher.
tasks.withType<Test>().configureEach {
    jvmArgs("--add-opens=java.base/jdk.internal.access=ALL-UNNAMED")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
}
