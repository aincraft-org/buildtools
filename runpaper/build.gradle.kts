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
tasks.register<xyz.jpenilla.runpaper.task.RunServer>("runServer4") {
    minecraftVersion("26.2")
    serverJar(file("run/cache/paper-26.2-112.jar"))
    runDirectory(file("run4"))
    pluginJars.from(project(":paper").tasks.named<Jar>("jar").flatMap { it.archiveFile })
    args("--port", "25568")
}
