pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            name = "MinecraftForge"
            url = uri("https://maven.minecraftforge.net/")
        }
        maven {
            name = "FancyGradle"
            url = uri("https://maven.gofancy.wtf/releases")
        }
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
    }
}

rootProject.name = "AdvancedRocketry"

if(file("libVulpes").exists()) {
    includeBuild("libVulpes") {
        dependencySubstitution {
            substitute(module("zmaster587.libVulpes:LibVulpes")).using(project(":"))
        }
    }
}

// Composite-build branch for ForgeTestFramework (sibling checkout).
// Opt-in via `./gradlew test -PuseLocalFramework=true` so a random sibling repo
// doesn't get pulled in by accident. Default path = mavenLocal (publishToMavenLocal).
val useLocalFramework = (settings.providers.gradleProperty("useLocalFramework").orNull == "true")
val frameworkDir = file("../ForgeTestFramework")
if (useLocalFramework && frameworkDir.exists()) {
    includeBuild(frameworkDir) {
        dependencySubstitution {
            substitute(module("com.github.stannismod.forge:forge-test-framework"))
                    .using(project(":"))
        }
    }
}