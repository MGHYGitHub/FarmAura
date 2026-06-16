plugins {
    id("fabric-loom") version "1.6.12"
    id("maven-publish")
}

version = project.properties["version"] as String
group = project.properties["maven_group"] as String

base {
    archivesName.set(project.properties["archives_base_name"] as String)
}

repositories {
    mavenCentral()
    maven {
        name = "Meteor Development"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
    maven {
        name = "Oss Sonatype"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    // 添加 Baritone 仓库
    maven {
        name = "Baritone"
        url = uri("https://maven.cabaletta.tech/releases")
    }
}

dependencies {
    // Fabric 基础依赖保持不变
    minecraft("com.mojang:minecraft:${project.properties["minecraft_version"]}")
    mappings("net.fabricmc:yarn:${project.properties["minecraft_version"]}+build.1:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.properties["loader_version"]}")

    // 注释掉所有网络依赖
    // modImplementation("baritone:baritone-api-fabric:1.20.1-SNAPSHOT") {
    //     exclude(group = "net.fabricmc.fabric-api")
    // }
    // modImplementation("meteordevelopment:meteor-client:${project.properties["meteor_version"]}") {
    //     exclude(group = "net.fabricmc.fabric-api")
    // }

    // 使用本地 JAR 文件
    modImplementation(files("libs/meteor-client-0.5.4-1.20.1.jar"))
}

tasks {
    processResources {
        inputs.property("version", project.version)
        inputs.property("id", rootProject.name)
        inputs.property("name", "Range Farm Assistant")
        inputs.property("description", "自动范围锄地、种植和除草 - Meteor Client 插件")

        filesMatching("fabric.mod.json") {
            expand(
                "version" to project.version,
                "id" to rootProject.name,
                "name" to "Range Farm Assistant",
                "description" to "自动范围锄地、种植和除草 - Meteor Client 插件"
            )
        }
    }

    jar {
        from("LICENSE") {
            rename { "${it}_${base.archivesName.get()}" }
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = base.archivesName.get()
            version = project.version.toString()
            // 使用 getByName 修复语法错误
            from(components.getByName("java"))
        }
    }
}
