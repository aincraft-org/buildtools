repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    implementation(project(":masonry-common"))
    implementation(project(":masonry-api"))
    compileOnly("io.papermc.paper:paper-api:26.2.build.112-stable")
    testImplementation("io.papermc.paper:paper-api:26.2.build.112-stable")
}

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
