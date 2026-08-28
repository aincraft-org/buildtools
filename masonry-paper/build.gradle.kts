plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.22"
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    mavenCentral()
    maven {
        name = "enginehub"
        url = uri("https://maven.enginehub.org/repo/")
    }
}

dependencies {
    implementation(project(":masonry-common"))
    implementation(project(":masonry-api"))
    paperweight.paperDevBundle("26.2.build.119-stable")
    compileOnly("io.papermc.paper:paper-api:26.2.build.112-stable")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.15.3") { isTransitive = false }
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.14") { isTransitive = false }
    testImplementation("io.papermc.paper:paper-api:26.2.build.112-stable")
}

paperweight.reobfArtifactConfiguration =
    io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.jar {
    archiveBaseName.set("masonry")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(configurations.runtimeClasspath)
    from(configurations.runtimeClasspath.map { files ->
        files.filter { it.exists() }.map { if (it.isDirectory) it else zipTree(it) }
    })
}