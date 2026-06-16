pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "Meteor Development"
            url = uri("https://maven.meteordev.org/releases")
        }
    }
}

rootProject.name = "meteor-farm-addon"
