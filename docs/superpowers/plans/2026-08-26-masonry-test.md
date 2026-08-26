# Masonry-Test Module and Smoke-Test Plugin Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename the `runpaper` Gradle module to `masonry-test` and add a thin startup smoke-test Paper plugin that verifies the main `Masonry` plugin is loaded.

**Architecture:** A separate Gradle module named `masonry-test` builds a thin plugin jar. The `run-paper` harness loads both the `:masonry-paper` jar and the `masonry-test` jar in the same server. The test plugin's `onEnable` checks `PluginManager.getPlugin("Masonry")` and logs the result.

**Tech Stack:** Gradle Kotlin DSL, `xyz.jpenilla.run-paper` 3.1.0, `io.papermc.paper:paper-api:26.2.build.112-stable`, Java 25.

## Global Constraints

- Module name: `masonry-test` (renamed from `runpaper`).
- Group: `dev.mintychochip.masonry` (inherited from root `build.gradle.kts`).
- Java toolchain: Java 25.
- Paper API version: `io.papermc.paper:paper-api:26.2.build.112-stable`.
- The `masonry-test` jar must remain thin and contain only the plugin class and `plugin.yml`; no bundled `paper-api`, `masonry-common`, or `masonry-api` classes.
- `processResources` in `masonry-test/build.gradle.kts` must mirror `:masonry-paper`’s pattern exactly (`props` map, `inputs.properties(props)`, `filesMatching("plugin.yml") { expand(props) }`).
- `run-paper` loads only the `:masonry-paper` jar and the `masonry-test` jar.
- `plugin.yml` for `MasonryTest` must use `softdepend: [Masonry]` so the server starts even if Masonry is missing.

---

### Task 1: Rename `runpaper` to `masonry-test` and update project files

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `.gitignore`
- Command: `mv runpaper masonry-test`

**Interfaces:** none

- [ ] **Step 1: Write the failing test**

Run: `./gradlew projects`
Expected: output contains `Project ':runpaper'` and does **not** contain `Project ':masonry-test'`.

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew projects
```

Expected: `Project ':runpaper'` is listed; `Project ':masonry-test'` is not.

- [ ] **Step 3: Write minimal implementation**

Update `.gitignore` to include run directories for `masonry-paper` and `masonry-test`:

```text
masonry-paper/run/
masonry-test/run/
masonry-test/run2/
masonry-test/run3/
masonry-test/run4/
```


Then move the module directory (this moves both tracked and ignored files):

```bash
mv runpaper masonry-test
```

Then change `settings.gradle.kts` line 6 from `include("runpaper")` to `include("masonry-test")`:

```kotlin
rootProject.name = "masonry"
include("masonry-api")
include("masonry-common")
include("masonry-paper")
include("masonry-test")
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew projects
git status --short
```

Expected: `Project ':masonry-test'` is listed; `Project ':runpaper'` is not. `git status` should show the `masonry-test/` directory rename and no untracked `runpaper/` or `masonry-test/run*/` files.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "rename: runpaper -> masonry-test"
```

---

### Task 2: Configure the `masonry-test` build and add `plugin.yml`

**Files:**
- Modify: `masonry-test/build.gradle.kts`
- Create: `masonry-test/src/main/resources/plugin.yml`

**Interfaces:** none

- [ ] **Step 1: Write the failing test**

```bash
./gradlew :masonry-test:build
```

Expected: a jar is produced, but `unzip -p masonry-test/build/libs/masonry-test-0.1.0.jar plugin.yml` either does not exist or still contains `${version}` because `processResources` is not configured.

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :masonry-test:build
unzip -p masonry-test/build/libs/masonry-test-0.1.0.jar plugin.yml
```

Expected: `plugin.yml` is present but its `version:` line shows `${version}` (unexpanded) or the jar has no `plugin.yml`.

- [ ] **Step 3: Write minimal implementation**

Replace the contents of `masonry-test/build.gradle.kts` with:

```kotlin
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
}

tasks.register<xyz.jpenilla.runpaper.task.RunServer>("runServer4") {
    minecraftVersion("26.2")
    serverJar(file("run/cache/paper-26.2-112.jar"))
    runDirectory(file("run4"))
    pluginJars.from(project(":masonry-paper").tasks.named<Jar>("jar").flatMap { it.archiveFile })
    args("--port", "25568")
}
```

Create `masonry-test/src/main/resources/plugin.yml`:

```yaml
name: MasonryTest
version: ${version}
main: dev.mintychochip.masonry.test.MasonryTestPlugin
api-version: "26.2"
description: Startup smoke-test plugin for Masonry
softdepend: [Masonry]
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :masonry-test:build
unzip -p masonry-test/build/libs/masonry-test-0.1.0.jar plugin.yml
```

Expected: build succeeds and the printed `plugin.yml` contains `version: 0.1.0` (expanded).

- [ ] **Step 5: Commit**

```bash
git add masonry-test/build.gradle.kts masonry-test/src/main/resources/plugin.yml
git commit -m "build: masonry-test build config and plugin.yml"
```

---

### Task 3: Add `MasonryTestPlugin` smoke-test class

**Files:**
- Create: `masonry-test/src/main/java/dev/mintychochip/masonry/test/MasonryTestPlugin.java`

**Interfaces:** none

- [ ] **Step 1: Write the failing test**

```bash
./gradlew :masonry-test:build
```

Expected: build succeeds, but `unzip -l masonry-test/build/libs/masonry-test-0.1.0.jar | grep MasonryTestPlugin.class` finds nothing because `MasonryTestPlugin.java` does not exist.

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :masonry-test:build
unzip -l masonry-test/build/libs/masonry-test-0.1.0.jar | grep MasonryTestPlugin.class
```

Expected: the `unzip -l` command returns no match.

- [ ] **Step 3: Write minimal implementation**

Create `masonry-test/src/main/java/dev/mintychochip/masonry/test/MasonryTestPlugin.java`:

```java
package dev.mintychochip.masonry.test;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class MasonryTestPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("MasonryTest enabled");

        Plugin masonry = getServer().getPluginManager().getPlugin("Masonry");
        if (masonry == null) {
            getLogger().warning("Masonry not found; smoke test failed");
        } else if (!masonry.isEnabled()) {
            getLogger().warning("Masonry is present but not enabled");
        } else {
            getLogger().info("Masonry " + masonry.getDescription().getVersion() + " is present and enabled");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("MasonryTest disabled");
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :masonry-test:compileJava
./gradlew :masonry-test:build
unzip -l masonry-test/build/libs/masonry-test-0.1.0.jar | grep MasonryTestPlugin.class
```

Expected: both tasks succeed and the jar contains `dev/mintychochip/masonry/test/MasonryTestPlugin.class`.

- [ ] **Step 5: Commit**

```bash
git add masonry-test/src/main/java/dev/mintychochip/masonry/test/MasonryTestPlugin.java
git commit -m "feat: add MasonryTest startup smoke-test plugin"
```

---

### Task 4: Wire `run-paper` to load the test jar and clean stale run caches

**Files:**
- Modify: `masonry-test/build.gradle.kts`

**Interfaces:** none

- [ ] **Step 1: Write the failing test**

```bash
./gradlew :masonry-test:runServer
```

After the server reaches `Done`, check the log:

```bash
tail -n 50 masonry-test/run/logs/latest.log | grep -q "MasonryTest enabled"
```

Expected: the grep fails because the test plugin jar is not loaded yet (`pluginJars.from(tasks.jar)` is missing). The log may show only `Masonry` loading, not `MasonryTest`.

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :masonry-test:runServer &
GRADLE_PID=$!
```

Wait for the server to print `Done`. Then:

```bash
tail -n 50 masonry-test/run/logs/latest.log | grep "MasonryTest enabled"
```

Expected: no output. Then stop the background server:

```bash
kill $GRADLE_PID
```

- [ ] **Step 3: Write minimal implementation**

First, delete stale plugin directories from the old `runpaper` cache so the renamed module gets a clean `plugins/` folder:

```bash
rm -rf masonry-test/run/plugins masonry-test/run2/plugins masonry-test/run3/plugins masonry-test/run4/plugins
```

Then update the two `runServer` blocks in `masonry-test/build.gradle.kts` to load the `masonry-test` jar. The final `masonry-test/build.gradle.kts` becomes:

```kotlin
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
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :masonry-test:runServer &
GRADLE_PID=$!
```

Wait for `Done`. Then:

```bash
tail -n 50 masonry-test/run/logs/latest.log | grep -E "MasonryTest enabled|Masonry .* is present and enabled"
ls masonry-test/run/plugins/
```

Expected: the log contains both messages and the plugins directory contains `masonry-0.1.0.jar` and `masonry-test-0.1.0.jar` (versions may vary).

Then stop the background server:

```bash
kill $GRADLE_PID
```

- [ ] **Step 5: Commit**

```bash
git add masonry-test/build.gradle.kts
git commit -m "build: load masonry-test jar in run-paper and clean run caches"
```

---

### Task 5: Final verification

**Files:** none

**Interfaces:** none

- [ ] **Step 1: Write the failing test**

```bash
./gradlew check
```

Before this task, `check` might already pass, but this step ensures the whole project still builds after all changes.

- [ ] **Step 2: Run test to verify it fails (or passes)**

```bash
./gradlew check
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Write minimal implementation**

No code changes. If `check` fails, fix the issue and re-run.

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew check
./gradlew verifyModuleBoundaries
```

Expected: both tasks pass.

- [ ] **Step 5: Commit**

```bash
git diff --exit-code || (git add -u && git commit -m "verify: project checks pass with masonry-test")
```

---

## Self-Review

**1. Spec coverage:**
- Module rename and `settings.gradle.kts` update — Task 1.
- Thin `masonry-test` jar with `archiveBaseName.set("masonry-test")` — Task 2.
- Mirror `:masonry-paper` `processResources` pattern — Task 2.
- `plugin.yml` with `softdepend: [Masonry]` — Task 2.
- `MasonryTestPlugin` that logs Masonry presence — Task 3.
- `run-paper` loads both `:masonry-paper` and `masonry-test` jars — Task 4.
- Clean stale `run*/plugins/` caches — Task 4.

**2. Placeholder scan:** no `TBD`, `TODO`, `implement later`, or un-described code. Each task contains the exact file contents and commands.

**3. Type consistency:** the only new Java type is `dev.mintychochip.masonry.test.MasonryTestPlugin`; it is referenced by `plugin.yml` and created in Task 3. No cross-task name drift.

**4. Gap:** none.
