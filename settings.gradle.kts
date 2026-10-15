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

// Build against the local ZonePractice Pro api/ checkout when it exists. Disable with -PlocalApi=false.
if (file("../../api/settings.gradle.kts").exists() && providers.gradleProperty("localApi").orNull != "false") {
    includeBuild("../../api") {
        dependencySubstitution {
            substitute(module("com.github.ZoneDevelopement:ZonePracticePro-Api")).using(project(":"))
        }
    }
}
