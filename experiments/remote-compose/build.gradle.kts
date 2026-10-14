plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val copyShapeRules by tasks.registering(Copy::class) {
    from(rootProject.file("app/src/main/java/cc/xdan/tempo/widgets/WidgetShapeRules.kt"))
    into(layout.buildDirectory.dir("generated/widgetGeometry"))
}
android {
    namespace = "cc.xdan.tempo.remote"
    compileSdk = 36
    defaultConfig {
        applicationId = "cc.xdan.tempo.remoteexperiment"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }
    testOptions.unitTests.isIncludeAndroidResources = true
    sourceSets["main"].java.srcDir(layout.buildDirectory.dir("generated/widgetGeometry"))
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
kotlin { jvmToolchain(17) }
tasks.named("preBuild").configure { dependsOn(copyShapeRules) }
dependencies {
    implementation("androidx.compose.remote:remote-creation-android:1.0.0-alpha21")
    implementation("androidx.compose.remote:remote-player-view:1.0.0-alpha21")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
}
