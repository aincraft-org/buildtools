# Masonry-Test Module and Smoke-Test Plugin Design

> Status: draft
> Date: 2026-08-26
> Owners: mintychochip
> Supersedes: nothing

## Goal

Rename the existing `runpaper` Gradle module to `masonry-test` and add a minimal, separate Paper plugin (`MasonryTest`) that performs a startup smoke test by verifying the main `Masonry` plugin is loaded.

## Scope

### In scope
- Rename `runpaper` directory and Gradle module to `masonry-test`.
- Update `settings.gradle.kts`.
- Update `masonry-test/build.gradle.kts` to:
  - build a thin `masonry-test` plugin jar,
  - mirror `:masonry-paper`’s `processResources` and `plugin.yml` version expansion,
  - load both the `:masonry-paper` and the `masonry-test` jars via `run-paper`.
- Add `masonry-test/src/main/resources/plugin.yml`.
- Add `MasonryTestPlugin` that logs whether `Masonry` is present on enable.
- Clean stale `run*/plugins/` directories after the move.

### Out of scope
- In-game `/masonrytest` commands.
- Direct API or integration tests that call Masonry internals.
- MockBukkit or other test frameworks.
- Changes to the main `Masonry` plugin or other modules.

## Design

### Module layout

```text
masonry-test/
  build.gradle.kts
  src/main/
    java/dev/mintychochip/masonry/test/
      MasonryTestPlugin.java
    resources/
      plugin.yml
  run/ (existing, cleaned)
  run2/ (existing, cleaned)
  run3/ (existing, cleaned)
  run4/ (existing, cleaned)
```

### Build configuration

`masonry-test/build.gradle.kts` applies the same plugins and repositories as today:

- `xyz.jpenilla.run-paper` 3.1.0
- `papermc` repository
- `compileOnly("io.papermc.paper:paper-api:26.2.build.112-stable")`

The `processResources` block is identical to `:masonry-paper`’s:

```kotlin
tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}
```

The jar task sets `archiveBaseName.set("masonry-test")`. The jar is thin and contains only the test plugin class and `plugin.yml`. No dependencies are bundled.

The `run-paper` configuration loads both the `paper` module jar and the `masonry-test` jar:

```kotlin
runPaper {
    disablePluginJarDetection()
}

tasks.runServer {
    minecraftVersion("26.2")
    serverJar(file("run/cache/paper-26.2-112.jar"))
    pluginJars.from(project(":masonry-paper").tasks.named<Jar>("jar").flatMap { it.archiveFile })
    pluginJars.from(tasks.jar)
}
```

### Plugin metadata

`masonry-test/src/main/resources/plugin.yml`:

```yaml
name: MasonryTest
version: ${version}
main: dev.mintychochip.masonry.test.MasonryTestPlugin
api-version: "26.2"
description: Startup smoke-test plugin for Masonry
softdepend: [Masonry]
```

### Plugin class

`MasonryTestPlugin.java` in package `dev.mintychochip.masonry.test`.

- Extends `org.bukkit.plugin.java.JavaPlugin`.
- `onEnable`: log `"MasonryTest enabled"`, look up `getServer().getPluginManager().getPlugin("Masonry")`, and log whether it is present and enabled (using `getDescription().getVersion()` if available).
- `onDisable`: log `"MasonryTest disabled"`.
- No commands or event listeners.

### Run cache cleanup

After `git mv runpaper masonry-test`, delete `masonry-test/run*/plugins/` so `run-paper` can load the freshly built `Masonry.jar` and `masonry-test-*.jar` without stale plugins. `world/`, server configs, and `cache/` are left in place to preserve test worlds and avoid re-downloading the server jar. If `cache/` contains stale artifacts, the user can delete it manually.

## Non-functional requirements

- The `masonry-test` jar must stay thin and not bundle `paper-api`, `masonry-common`, or `masonry-api` classes.
- The build must remain compatible with Java 25 and the existing toolchain.
- The smoke test must not prevent the server from starting if Masonry is absent (hence `softdepend`, not `depend`).

## Verification

1. `./gradlew :masonry-test:build` succeeds and produces a thin jar.
2. `./gradlew :masonry-test:runServer` starts and reaches Paper's "Done" log line.
3. The server log contains the `MasonryTest enabled` and `Masonry ... is present` lines.
4. `plugins/` under the chosen run directory contains `masonry-0.1.0.jar` and `masonry-test-0.1.0.jar`.

## Open decisions

- Whether to add a `/masonrytest` command for in-game smoke tests in a later iteration (out of scope for this change).
- Whether to delete `masonry-test/run*/cache/` if run-paper fails due to stale libraries.
