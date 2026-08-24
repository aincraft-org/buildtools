import org.gradle.api.tasks.bundling.Jar

plugins {
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

runPaper {
    disablePluginJarDetection()
}

tasks.runServer {
    minecraftVersion("26.2")
    serverJar(file("run/cache/paper-26.2-112.jar"))
    pluginJars.from(project(":paper").tasks.named<Jar>("jar").flatMap { it.archiveFile })
}
