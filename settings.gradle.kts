rootProject.name = "ZPP-Badges"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://jitpack.io") {
            content { includeGroupByRegex("com\\.github\\..*") }
        }
    }
}

// Workspace layout: ZPP-Backend, ZPP-Api, "ZonePractice Pro" (core) and this addon are sibling folders.
// canonicalFile resolves the core's addons/<Name> symlink, so the paths hold when built from there too.
val workspace = settingsDir.canonicalFile.parentFile
val localApi = workspace.resolve("ZPP-Api")

// Build against the local api/ checkout when it exists. Disable with -PlocalApi=false.
if (localApi.resolve("settings.gradle.kts").exists() && providers.gradleProperty("localApi").orNull != "false") {
    includeBuild(localApi) {
        dependencySubstitution {
            substitute(module("com.github.ZoneDevelopement:ZonePracticePro-Api")).using(project(":"))
        }
    }
}
