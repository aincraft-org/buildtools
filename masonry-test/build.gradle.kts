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

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.112-stable")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.jar {
    archiveBaseName.set("masonry-test")
}

runPaper {
    disablePluginJarDetection()
}

tasks.runServer {
    minecraftVersion("26.2")
    serverJar(file("run/cache/paper-26.2-112.jar"))
    pluginJars.from(project(":masonry-paper").tasks.named<Jar>("jar").flatMap { it.archiveFile })
    pluginJars.from(tasks.jar.flatMap { it.archiveFile })
}

tasks.register<xyz.jpenilla.runpaper.task.RunServer>("runServer4") {
    minecraftVersion("26.2")
    serverJar(file("run/cache/paper-26.2-112.jar"))
    runDirectory(file("run4"))
    pluginJars.from(project(":masonry-paper").tasks.named<Jar>("jar").flatMap { it.archiveFile })
    pluginJars.from(tasks.jar.flatMap { it.archiveFile })
    args("--port", "25568")
}
