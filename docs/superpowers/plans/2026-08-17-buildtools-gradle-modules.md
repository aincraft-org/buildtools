# BuildTools Gradle Module Separation Implementation Plan

> Historical record — active project is Masonry.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a Java 25 Gradle Kotlin DSL multi-project for `dev.mintychochip` with strict `api`, `common`, and `paper` module boundaries, plus the minimum contracts and adapters needed to keep those boundaries real.

**Architecture:** `api` owns platform-neutral contracts and value types. `common` owns JVM-only lifecycle, history, and limit checks and talks to the world only through API ports. `paper` is the only module that depends on Paper/Bukkit; it hosts `BuildToolsPlugin`, command registration, and adapters that translate Paper types at the module edge. This plan does **not** implement replace, fill, copy, paste, selection UX, or blueprint persistence.

**Tech Stack:** Gradle 9.7.0, Gradle Kotlin DSL, Java 25, Paper API `26.2.build.112-stable`, JUnit 5.11.4, `plugin.yml`.

**Spec:** `docs/superpowers/specs/2026-08-17-buildtools-gradle-modules-design.md`

**Living spec:** `docs/living-specs/buildtools.md`

If executing in isolation, create the worktree with `superpowers:using-git-worktrees` at execution time.

---

## Scope

This is one plan for one subsystem: the Gradle module split and the scaffolding that proves the split. Do **not** implement the living-spec Current tools in this plan. Those are later plans:

1. Selection (cuboid + preview rendering)
2. Tools (replace / fill / copy / paste)
3. Survival economics (wired through the ports this plan introduces)
4. Blueprints (save / load)

## Locked implementation decisions

These were left open in the spec. Treat them as decided for this plan.

| Decision | Choice | Why |
|---|---|---|
| Paper API | `io.papermc.paper:paper-api:26.2.build.112-stable` | Current Paper 26.2 stable line. Do not use the pre-26.1 `{version}-R0.1-SNAPSHOT` coordinate. |
| `plugin.yml` `api-version` | `26.2` | Matches the Paper 26.2 plugin.yml contract. |
| Java | 25 | Required by Paper 26.2. Gradle 9.1.0+ is required for a Java 25 toolchain; this plan uses Gradle 9.7.0. |
| Plugin name / jar name | `BuildTools` / `buildtools-<version>.jar` | Server owners install one plugin named BuildTools. The Gradle project stays `paper`. |
| Paper test strategy | No MockBukkit / no live server | Paper tests cover metadata, type boundaries, and pure translators. World mutation stays in later selection/tool work. |
| Persistence backend | Deferred | Out of this spec's initial implementation scope. |
| Claim/anti-grief integration | Deferred | Living spec places `ClaimProvider` in Next. |
| Default limits | `interactionDistance=6`, `selectionExtent=64`, `maxOperationBlocks=32768` | Concrete values so `OperationLimits.defaults()` is testable. Paper config comes later. |
| Full-inventory refund | Drop leftover items at the player | Closes the living-spec open question so `PaperSurvivalTransaction.refund` has one behavior. |
| History size | 32 operations per actor | Bounded undo without a config file yet. |

If `26.2.build.112-stable` cannot be resolved from `https://repo.papermc.io/repository/maven-public/`, stop and ask. Do not silently change Java or fall back to a 1.20/1.21 `R0.1-SNAPSHOT` coordinate.

---

## File structure

Create these files. Each file has one responsibility. Do not add extra packages, tools, or config files.

```text
.gitignore
settings.gradle.kts
build.gradle.kts
gradle.properties
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.properties
gradle/wrapper/gradle-wrapper.jar

api/build.gradle.kts
api/src/main/java/dev/mintychochip/buildtools/api/ActorId.java
api/src/main/java/dev/mintychochip/buildtools/api/world/BlockPosition.java
api/src/main/java/dev/mintychochip/buildtools/api/world/BlockState.java
api/src/main/java/dev/mintychochip/buildtools/api/selection/CuboidSelection.java
api/src/main/java/dev/mintychochip/buildtools/api/cost/ResourceCost.java
api/src/main/java/dev/mintychochip/buildtools/api/limits/OperationLimits.java
api/src/main/java/dev/mintychochip/buildtools/api/tool/Tool.java
api/src/main/java/dev/mintychochip/buildtools/api/tool/ToolRequest.java
api/src/main/java/dev/mintychochip/buildtools/api/tool/ToolPreview.java
api/src/main/java/dev/mintychochip/buildtools/api/tool/ValidationResult.java
api/src/main/java/dev/mintychochip/buildtools/api/operation/BlockChange.java
api/src/main/java/dev/mintychochip/buildtools/api/operation/OperationRecord.java
api/src/main/java/dev/mintychochip/buildtools/api/service/WorldAccess.java
api/src/main/java/dev/mintychochip/buildtools/api/service/SurvivalTransaction.java
api/src/main/java/dev/mintychochip/buildtools/api/service/PreviewRenderer.java
api/src/main/java/dev/mintychochip/buildtools/api/service/TaskScheduler.java
api/src/main/java/dev/mintychochip/buildtools/api/service/PermissionService.java
api/src/test/java/dev/mintychochip/buildtools/api/world/BlockPositionTest.java
api/src/test/java/dev/mintychochip/buildtools/api/selection/CuboidSelectionTest.java
api/src/test/java/dev/mintychochip/buildtools/api/cost/ResourceCostTest.java
api/src/test/java/dev/mintychochip/buildtools/api/tool/ValidationResultTest.java
api/src/test/java/dev/mintychochip/buildtools/api/operation/OperationRecordTest.java
api/src/test/java/dev/mintychochip/buildtools/api/limits/OperationLimitsTest.java

common/build.gradle.kts
common/src/main/java/dev/mintychochip/buildtools/common/tool/ToolRegistry.java
common/src/main/java/dev/mintychochip/buildtools/common/tool/ToolExecutor.java
common/src/main/java/dev/mintychochip/buildtools/common/operation/OperationHistory.java
common/src/main/java/dev/mintychochip/buildtools/common/operation/OperationGuard.java
common/src/test/java/dev/mintychochip/buildtools/common/tool/ToolRegistryTest.java
common/src/test/java/dev/mintychochip/buildtools/common/tool/ToolExecutorTest.java
common/src/test/java/dev/mintychochip/buildtools/common/tool/RecordingTool.java
common/src/test/java/dev/mintychochip/buildtools/common/tool/RecordingWorldAccess.java
common/src/test/java/dev/mintychochip/buildtools/common/tool/RecordingSurvivalTransaction.java
common/src/test/java/dev/mintychochip/buildtools/common/tool/AllowAllPermissions.java
common/src/test/java/dev/mintychochip/buildtools/common/operation/OperationHistoryTest.java
common/src/test/java/dev/mintychochip/buildtools/common/operation/OperationGuardTest.java

paper/build.gradle.kts
paper/src/main/java/dev/mintychochip/buildtools/paper/BuildToolsPlugin.java
paper/src/main/java/dev/mintychochip/buildtools/paper/command/BuildToolsCommand.java
paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperBlockStates.java
paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperWorldAccess.java
paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperSurvivalTransaction.java
paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperPreviewRenderer.java
paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperTaskScheduler.java
paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperPermissionService.java
paper/src/main/resources/plugin.yml
paper/src/test/java/dev/mintychochip/buildtools/paper/PaperBoundaryTest.java
paper/src/test/java/dev/mintychochip/buildtools/paper/adapter/PaperBlockStatesTest.java

docs/superpowers/verification/buildtools-module-boundaries.md
docs/living-specs/buildtools.md   # modify: link spec/plan; check bootstrap only
```

Responsibility map:

- `api` types are immutable values or ports. No filesystem, scheduler, or Minecraft types.
- `common` classes orchestrate API types. Tests use in-memory fakes in `src/test`.
- `paper` adapters translate Paper objects. `PaperBlockStates` is the only Paper translator that is unit-tested without a server.
- Root `verifyModuleBoundaries` fails the build if `api` or `common` resolve a Paper/Bukkit/NMS dependency.

---

### Task 1: Gradle multi-project skeleton

**Files:**
- Create: `.gitignore`
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `api/build.gradle.kts`
- Create: `common/build.gradle.kts`
- Create: `paper/build.gradle.kts`
- Create: `paper/src/main/java/dev/mintychochip/buildtools/paper/BuildToolsPlugin.java`
- Create: `paper/src/main/resources/plugin.yml`
- Create: `gradle/wrapper/gradle-wrapper.properties` (via wrapper task)
- Create: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` (via wrapper task)

- [ ] **Step 1: Add ignore rules and project identity**

Create `.gitignore`:

```gitignore
.gradle/
build/
**/build/
out/
.idea/
*.iml
.classpath
.project
.settings/
.vscode/
*.class
```

Create `gradle.properties`:

```properties
org.gradle.parallel=true
org.gradle.caching=true
```

Create `settings.gradle.kts`:

```kotlin
rootProject.name = "buildtools"

include("api")
include("common")
include("paper")
```

- [ ] **Step 2: Add shared Java conventions and module build scripts**

Create `build.gradle.kts`:

```kotlin
plugins {
    java
}

group = "dev.mintychochip"
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
```

Create `api/build.gradle.kts`:

```kotlin
// API contracts inherit root Java/JUnit conventions.
```

Create `common/build.gradle.kts`:

```kotlin
dependencies {
    api(project(":api"))
}
```

Create `paper/build.gradle.kts`:

```kotlin
repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":api"))
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
    archiveBaseName.set("buildtools")
}
```

- [ ] **Step 3: Add the Paper entry point and plugin metadata**

Create `paper/src/main/java/dev/mintychochip/buildtools/paper/BuildToolsPlugin.java`:

```java
package dev.mintychochip.buildtools.paper;

import org.bukkit.plugin.java.JavaPlugin;

public final class BuildToolsPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("BuildTools loaded. Tools are not registered yet.");
    }

    @Override
    public void onDisable() {
        getLogger().info("BuildTools disabled.");
    }
}
```

Create `paper/src/main/resources/plugin.yml`:

```yaml
name: BuildTools
version: ${version}
main: dev.mintychochip.buildtools.paper.BuildToolsPlugin
api-version: "26.2"
authors:
  - mintychochip
description: Survival-friendly building assistant
commands:
  bt:
    description: BuildTools command root
    usage: /bt <subcommand>

[Showing lines 1-300 of 2545. Use :301 to continue]