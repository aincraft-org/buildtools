plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.22"
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    implementation(project(":masonry-common"))
    implementation(project(":masonry-api"))
    paperweight.paperDevBundle("26.2.build.119-stable")
    compileOnly("io.papermc.paper:paper-api:26.2.build.112-stable")
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
