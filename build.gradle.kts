plugins {
    java
}

group = "dev.mintychochip.masonry"
version = "0.1.0"

subprojects {
    apply(plugin = "java-library")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(25)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.11.4")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher:1.11.4")
    }
}

tasks.register("verifyModuleBoundaries") {
    group = "verification"
    description = "Fail if api or common resolve Paper, Bukkit, or NMS dependencies."
    dependsOn(":api:compileJava", ":common:compileJava")
    doLast {
        val forbiddenPrefixes = listOf("io.papermc", "org.bukkit", "org.spigotmc", "net.minecraft")
        listOf("api", "common").forEach { name ->
            val compileClasspath = project(name).configurations.getByName("compileClasspath")
            compileClasspath.incoming.resolutionResult.allDependencies.forEach { dependency ->
                if (dependency is org.gradle.api.artifacts.result.ResolvedDependencyResult) {
                    val group = dependency.selected.moduleVersion?.group ?: return@forEach
                    if (forbiddenPrefixes.any { prefix -> group == prefix || group.startsWith("$prefix.") }) {
                        throw GradleException("Module :$name depends on forbidden group $group")
                    }
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn("verifyModuleBoundaries")
}
