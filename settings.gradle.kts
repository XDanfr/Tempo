pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "Tempo"
include(":app", ":core:model", ":core:schedule", ":core:data", ":core:designsystem")

// The alpha document/player spike is excluded from normal builds and production APKs.
if (providers.gradleProperty("tempoRemoteCompose").orNull == "true") {
    include(":experiments:remote-compose")
}
