# BuildTools Gradle Module Separation Implementation Plan

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
    permission: buildtools.command
permissions:
  buildtools.command:
    description: Use the /bt command root
    default: true
```

Do not register replace/fill/copy/paste subcommands. The root command exists so the plugin descriptor is real.

- [ ] **Step 4: Generate the Gradle wrapper**

From the repository root, with Gradle 9.7.0 available:

```bash
gradle wrapper --gradle-version 9.7.0 --distribution-type bin
```

Expected: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, and `gradle/wrapper/gradle-wrapper.properties` exist. The properties file must contain:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-9.7.0-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

If system Gradle is not installed, install 9.7.0 first. Do not check in a different wrapper version. Gradle 8.x cannot compile a Java 25 toolchain.

- [ ] **Step 5: Run the scaffold build**

```bash
./gradlew build
```

Expected: `:api:build`, `:common:build`, and `:paper:build` succeed. `:paper:jar` writes `paper/build/libs/buildtools-0.1.0.jar`. No test failures. `api` and `common` compile with no Paper artifacts.

If Paper `26.2.build.112-stable` cannot be resolved, stop.

- [ ] **Step 6: Commit the scaffold**

```bash
git add .gitignore gradle.properties settings.gradle.kts build.gradle.kts \
  api/build.gradle.kts common/build.gradle.kts paper/build.gradle.kts \
  paper/src/main/java/dev/mintychochip/buildtools/paper/BuildToolsPlugin.java \
  paper/src/main/resources/plugin.yml \
  gradlew gradlew.bat gradle/wrapper
git commit -m "Split buildtools into api, common, and paper modules"
```

---

### Task 2: API value types

**Files:**
- Create: `api/src/main/java/dev/mintychochip/buildtools/api/ActorId.java`
- Create: `api/src/main/java/dev/mintychochip/buildtools/api/world/BlockPosition.java`
- Create: `api/src/main/java/dev/mintychochip/buildtools/api/world/BlockState.java`
- Create: `api/src/main/java/dev/mintychochip/buildtools/api/selection/CuboidSelection.java`
- Create: `api/src/main/java/dev/mintychochip/buildtools/api/cost/ResourceCost.java`
- Create: `api/src/main/java/dev/mintychochip/buildtools/api/limits/OperationLimits.java`
- Create: `api/src/main/java/dev/mintychochip/buildtools/api/operation/BlockChange.java`
- Create: `api/src/main/java/dev/mintychochip/buildtools/api/operation/OperationRecord.java`
- Test: `api/src/test/java/dev/mintychochip/buildtools/api/world/BlockPositionTest.java`
- Test: `api/src/test/java/dev/mintychochip/buildtools/api/selection/CuboidSelectionTest.java`
- Test: `api/src/test/java/dev/mintychochip/buildtools/api/cost/ResourceCostTest.java`
- Test: `api/src/test/java/dev/mintychochip/buildtools/api/operation/OperationRecordTest.java`
- Test: `api/src/test/java/dev/mintychochip/buildtools/api/limits/OperationLimitsTest.java`

- [ ] **Step 1: Write the failing value-type tests**

Create `api/src/test/java/dev/mintychochip/buildtools/api/world/BlockPositionTest.java`:

```java
package dev.mintychochip.buildtools.api.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BlockPositionTest {
    @Test
    void equalityIsWorldAndCoordinates() {
        BlockPosition left = new BlockPosition("world", 1, 64, -3);
        BlockPosition same = new BlockPosition("world", 1, 64, -3);
        BlockPosition otherWorld = new BlockPosition("nether", 1, 64, -3);
        BlockPosition otherCoord = new BlockPosition("world", 2, 64, -3);

        assertEquals(left, same);
        assertEquals(left.hashCode(), same.hashCode());
        assertNotEquals(left, otherWorld);
        assertNotEquals(left, otherCoord);
    }

    @Test
    void rejectsNullWorldId() {
        assertThrows(NullPointerException.class, () -> new BlockPosition(null, 0, 0, 0));
    }
}
```

Create `api/src/test/java/dev/mintychochip/buildtools/api/selection/CuboidSelectionTest.java`:

```java
package dev.mintychochip.buildtools.api.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.mintychochip.buildtools.api.world.BlockPosition;
import org.junit.jupiter.api.Test;

class CuboidSelectionTest {
    @Test
    void volumeAndExtentAreInclusive() {
        CuboidSelection selection = new CuboidSelection(
                new BlockPosition("world", 0, 64, 0),
                new BlockPosition("world", 3, 65, 1));

        assertEquals(4, selection.width());
        assertEquals(2, selection.height());
        assertEquals(2, selection.depth());
        assertEquals(16, selection.volume());
        assertEquals(4, selection.extent());
        assertEquals("world", selection.worldId());
    }

    @Test
    void rejectsCornersInDifferentWorlds() {
        assertThrows(IllegalArgumentException.class, () -> new CuboidSelection(
                new BlockPosition("world", 0, 64, 0),
                new BlockPosition("nether", 1, 64, 0)));
    }
}
```

Create `api/src/test/java/dev/mintychochip/buildtools/api/cost/ResourceCostTest.java`:

```java
package dev.mintychochip.buildtools.api.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ResourceCostTest {
    @Test
    void copiesAndExposesItemCounts() {
        Map<String, Integer> input = new HashMap<>();
        input.put("minecraft:stone", 8);
        ResourceCost cost = new ResourceCost(input);

        input.put("minecraft:dirt", 1);
        assertEquals(Map.of("minecraft:stone", 8), cost.itemCounts());
        assertTrue(ResourceCost.none().isEmpty());
    }

    @Test
    void rejectsNegativeCounts() {
        assertThrows(IllegalArgumentException.class, () -> new ResourceCost(Map.of("minecraft:stone", -1)));
    }

    @Test
    void itemCountsAreUnmodifiable() {
        ResourceCost cost = new ResourceCost(Map.of("minecraft:stone", 1));
        assertThrows(UnsupportedOperationException.class, () -> cost.itemCounts().put("minecraft:dirt", 1));
    }
}
```

Create `api/src/test/java/dev/mintychochip/buildtools/api/operation/OperationRecordTest.java`:

```java
package dev.mintychochip.buildtools.api.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.cost.ResourceCost;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.api.world.BlockState;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OperationRecordTest {
    @Test
    void preservesOrderedBlockChangesAndRejectsMutation() {
        BlockChange first = new BlockChange(
                new BlockPosition("world", 0, 64, 0),
                BlockState.of("minecraft:dirt"),
                BlockState.of("minecraft:stone"));
        BlockChange second = new BlockChange(
                new BlockPosition("world", 1, 64, 0),
                BlockState.of("minecraft:grass_block"),
                BlockState.of("minecraft:air"));
        List<BlockChange> changes = new ArrayList<>();
        changes.add(first);
        changes.add(second);

        OperationRecord record = new OperationRecord(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                new ActorId(UUID.fromString("00000000-0000-0000-0000-0000000000aa")),
                "replace",
                changes,
                new ResourceCost(java.util.Map.of("minecraft:stone", 1)));

        changes.clear();
        assertEquals(List.of(first, second), record.changes());
        assertThrows(UnsupportedOperationException.class, () -> record.changes().add(first));
    }
}
```

Create `api/src/test/java/dev/mintychochip/buildtools/api/limits/OperationLimitsTest.java`:

```java
package dev.mintychochip.buildtools.api.limits;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OperationLimitsTest {
    @Test
    void defaultsMatchLockedValues() {
        OperationLimits limits = OperationLimits.defaults();
        assertEquals(6, limits.interactionDistance());
        assertEquals(64, limits.selectionExtent());
        assertEquals(32_768, limits.maxOperationBlocks());
    }

    @Test
    void rejectsNonPositiveLimits() {
        assertThrows(IllegalArgumentException.class, () -> new OperationLimits(0, 64, 1));
        assertThrows(IllegalArgumentException.class, () -> new OperationLimits(6, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new OperationLimits(6, 64, 0));
    }
}
```

- [ ] **Step 2: Run the API tests and confirm they fail**

```bash
./gradlew :api:test
```

Expected: FAIL because the types do not exist (`package dev.mintychochip.buildtools.api... does not exist` or `cannot find symbol`).

- [ ] **Step 3: Implement the value types**

Create `api/src/main/java/dev/mintychochip/buildtools/api/ActorId.java`:

```java
package dev.mintychochip.buildtools.api;

import java.util.Objects;
import java.util.UUID;

public record ActorId(UUID value) {
    public ActorId {
        Objects.requireNonNull(value, "value");
    }
}
```

Create `api/src/main/java/dev/mintychochip/buildtools/api/world/BlockPosition.java`:

```java
package dev.mintychochip.buildtools.api.world;

import java.util.Objects;

public record BlockPosition(String worldId, int x, int y, int z) {
    public BlockPosition {
        Objects.requireNonNull(worldId, "worldId");
    }
}
```

Create `api/src/main/java/dev/mintychochip/buildtools/api/world/BlockState.java`:

```java
package dev.mintychochip.buildtools.api.world;

import java.util.Map;
import java.util.Objects;

public record BlockState(String namespacedKey, Map<String, String> properties) {
    public BlockState {
        Objects.requireNonNull(namespacedKey, "namespacedKey");
        if (namespacedKey.isBlank()) {
            throw new IllegalArgumentException("namespacedKey must be present");
        }
        properties = Map.copyOf(properties == null ? Map.of() : properties);
    }

    public static BlockState of(String namespacedKey) {
        return new BlockState(namespacedKey, Map.of());
    }
}
```

Create `api/src/main/java/dev/mintychochip/buildtools/api/selection/CuboidSelection.java`:

```java
package dev.mintychochip.buildtools.api.selection;

import dev.mintychochip.buildtools.api.world.BlockPosition;
import java.util.Objects;

public record CuboidSelection(BlockPosition pos1, BlockPosition pos2) {
    public CuboidSelection {
        Objects.requireNonNull(pos1, "pos1");
        Objects.requireNonNull(pos2, "pos2");
        if (!pos1.worldId().equals(pos2.worldId())) {
            throw new IllegalArgumentException("Selection corners must share a world");
        }
    }

    public String worldId() {
        return pos1.worldId();
    }

    public int width() {
        return Math.abs(pos1.x() - pos2.x()) + 1;
    }

    public int height() {
        return Math.abs(pos1.y() - pos2.y()) + 1;
    }

    public int depth() {
        return Math.abs(pos1.z() - pos2.z()) + 1;
    }

    public int volume() {
        return width() * height() * depth();
    }

    public int extent() {
        return Math.max(width(), Math.max(height(), depth()));
    }
}
```

Create `api/src/main/java/dev/mintychochip/buildtools/api/cost/ResourceCost.java`:

```java
package dev.mintychochip.buildtools.api.cost;

import java.util.Map;
import java.util.Objects;

public record ResourceCost(Map<String, Integer> itemCounts) {
    public ResourceCost {
        Objects.requireNonNull(itemCounts, "itemCounts");
        itemCounts.forEach((key, count) -> {
            Objects.requireNonNull(key, "item key");
            if (count == null || count < 0) {
                throw new IllegalArgumentException("Item count must be >= 0: " + key);
            }
        });
        itemCounts = Map.copyOf(itemCounts);
    }

    public static ResourceCost none() {
        return new ResourceCost(Map.of());
    }

    public boolean isEmpty() {
        return itemCounts.isEmpty();
    }
}
```

Create `api/src/main/java/dev/mintychochip/buildtools/api/limits/OperationLimits.java`:

```java
package dev.mintychochip.buildtools.api.limits;

public record OperationLimits(int interactionDistance, int selectionExtent, int maxOperationBlocks) {
    public OperationLimits {
        if (interactionDistance <= 0 || selectionExtent <= 0 || maxOperationBlocks <= 0) {
            throw new IllegalArgumentException("All operation limits must be positive");
        }
    }

    public static OperationLimits defaults() {
        return new OperationLimits(6, 64, 32_768);
    }
}
```

Create `api/src/main/java/dev/mintychochip/buildtools/api/operation/BlockChange.java`:

```java
package dev.mintychochip.buildtools.api.operation;

import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.api.world.BlockState;
import java.util.Objects;

public record BlockChange(BlockPosition position, BlockState before, BlockState after) {
    public BlockChange {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
    }
}
```

Create `api/src/main/java/dev/mintychochip/buildtools/api/operation/OperationRecord.java`:

```java
package dev.mintychochip.buildtools.api.operation;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.cost.ResourceCost;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record OperationRecord(
        UUID operationId,
        ActorId actorId,
        String toolName,
        List<BlockChange> changes,
        ResourceCost cost) {
    public OperationRecord {
        Objects.requireNonNull(operationId, "operationId");
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(toolName, "toolName");
        Objects.requireNonNull(changes, "changes");
        Objects.requireNonNull(cost, "cost");
        if (toolName.isBlank()) {
            throw new IllegalArgumentException("toolName must be present");
        }
        changes = List.copyOf(changes);
    }
}
```

- [ ] **Step 4: Run the API value-type tests**

```bash
./gradlew :api:test --tests dev.mintychochip.buildtools.api.world.BlockPositionTest \
  --tests dev.mintychochip.buildtools.api.selection.CuboidSelectionTest \
  --tests dev.mintychochip.buildtools.api.cost.ResourceCostTest \
  --tests dev.mintychochip.buildtools.api.operation.OperationRecordTest \
  --tests dev.mintychochip.buildtools.api.limits.OperationLimitsTest
```

Expected: PASS. All five test classes succeed.

- [ ] **Step 5: Commit the value types**

```bash
git add api
git commit -m "Add platform-neutral BuildTools value types"
```

---

### Task 3: API tool contracts and ports

**Files:**
- Create: `api/src/main/java/dev/mintychochip/buildtools/api/tool/ValidationResult.java`
- Create: `api/src/main/java/dev/mintychochip/buildtools/api/tool/ToolRequest.java`
- Create: `api/src/main/java/dev/mintychochip/buildtools/api/tool/ToolPreview.java`
- Create: `api/src/main/java/dev/mintychochip/buildtools/api/tool/Tool.java`
- Create: `api/src/main/java/dev/mintychochip/buildtools/api/service/WorldAccess.java`
- Create: `api/src/main/java/dev/mintychochip/buildtools/api/service/SurvivalTransaction.java`
- Create: `api/src/main/java/dev/mintychochip/buildtools/api/service/PreviewRenderer.java`
- Create: `api/src/main/java/dev/mintychochip/buildtools/api/service/TaskScheduler.java`
- Create: `api/src/main/java/dev/mintychochip/buildtools/api/service/PermissionService.java`
- Test: `api/src/test/java/dev/mintychochip/buildtools/api/tool/ValidationResultTest.java`

- [ ] **Step 1: Write the failing ValidationResult test**

Create `api/src/test/java/dev/mintychochip/buildtools/api/tool/ValidationResultTest.java`:

```java
package dev.mintychochip.buildtools.api.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ValidationResultTest {
    @Test
    void validHasNoErrors() {
        ValidationResult result = ValidationResult.valid();
        assertTrue(result.valid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void invalidRequiresAtLeastOneError() {
        ValidationResult result = ValidationResult.invalid("too large");
        assertFalse(result.valid());
        assertEquals(List.of("too large"), result.errors());
        assertThrows(IllegalArgumentException.class, () -> ValidationResult.invalid(List.of()));
    }

    @Test
    void errorsAreUnmodifiable() {
        ValidationResult result = ValidationResult.invalid("denied");
        assertThrows(UnsupportedOperationException.class, () -> result.errors().add("other"));
    }
}
```

- [ ] **Step 2: Run the test and confirm it fails**

```bash
./gradlew :api:test --tests dev.mintychochip.buildtools.api.tool.ValidationResultTest
```

Expected: FAIL because `ValidationResult` does not exist.

- [ ] **Step 3: Implement contracts and ports**

Create `api/src/main/java/dev/mintychochip/buildtools/api/tool/ValidationResult.java`:

```java
package dev.mintychochip.buildtools.api.tool;

import java.util.List;
import java.util.Objects;

public record ValidationResult(boolean valid, List<String> errors) {
    public ValidationResult {
        Objects.requireNonNull(errors, "errors");
        errors = List.copyOf(errors);
        if (valid && !errors.isEmpty()) {
            throw new IllegalArgumentException("Valid results cannot include errors");
        }
        if (!valid && errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid results require at least one error");
        }
    }

    public static ValidationResult valid() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult invalid(String error) {
        return new ValidationResult(false, List.of(error));
    }

    public static ValidationResult invalid(List<String> errors) {
        return new ValidationResult(false, errors);
    }
}
```

Create `api/src/main/java/dev/mintychochip/buildtools/api/tool/ToolRequest.java`:

```java
package dev.mintychochip.buildtools.api.tool;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import java.util.Map;
import java.util.Objects;

public record ToolRequest(
        ActorId actorId,
        String toolName,
        CuboidSelection selection,
        Map<String, String> arguments) {
    public ToolRequest {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(toolName, "toolName");
        Objects.requireNonNull(selection, "selection");
        if (toolName.isBlank()) {
            throw new IllegalArgumentException("toolName must be present");
        }
        arguments = Map.copyOf(arguments == null ? Map.of() : arguments);
    }
}
```

Create `api/src/main/java/dev/mintychochip/buildtools/api/tool/ToolPreview.java`:

```java
package dev.mintychochip.buildtools.api.tool;

import dev.mintychochip.buildtools.api.cost.ResourceCost;
import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import java.util.Objects;

public record ToolPreview(CuboidSelection region, int affectedCount, ResourceCost estimatedCost) {
    public ToolPreview {
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(estimatedCost, "estimatedCost");
        if (affectedCount < 0) {
            throw new IllegalArgumentException("affectedCount must be >= 0");
        }
    }
}
```

Create `api/src/main/java/dev/mintychochip/buildtools/api/service/WorldAccess.java`:

```java
package dev.mintychochip.buildtools.api.service;

import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.api.world.BlockState;

public interface WorldAccess {
    BlockState getBlock(BlockPosition position);

    void setBlock(BlockPosition position, BlockState state);

    boolean isLoaded(BlockPosition position);
}
```

Create `api/src/main/java/dev/mintychochip/buildtools/api/service/SurvivalTransaction.java`:

```java
package dev.mintychochip.buildtools.api.service;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.cost.ResourceCost;

public interface SurvivalTransaction {
    boolean canAfford(ActorId actor, ResourceCost cost);

    void charge(ActorId actor, ResourceCost cost);

    void refund(ActorId actor, ResourceCost cost);
}
```

Create `api/src/main/java/dev/mintychochip/buildtools/api/service/PreviewRenderer.java`:

```java
package dev.mintychochip.buildtools.api.service;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.tool.ToolPreview;

public interface PreviewRenderer {
    void show(ActorId actor, ToolPreview preview);

    void clear(ActorId actor);
}
```

Create `api/src/main/java/dev/mintychochip/buildtools/api/service/TaskScheduler.java`:

```java
package dev.mintychochip.buildtools.api.service;

public interface TaskScheduler {
    void runOnMain(Runnable task);

    void runLater(Runnable task, long delayTicks);
}
```

Create `api/src/main/java/dev/mintychochip/buildtools/api/service/PermissionService.java`:

```java
package dev.mintychochip.buildtools.api.service;

import dev.mintychochip.buildtools.api.ActorId;

public interface PermissionService {
    boolean has(ActorId actor, String node);
}
```

Create `api/src/main/java/dev/mintychochip/buildtools/api/tool/Tool.java`:

```java
package dev.mintychochip.buildtools.api.tool;

import dev.mintychochip.buildtools.api.operation.OperationRecord;
import dev.mintychochip.buildtools.api.service.SurvivalTransaction;
import dev.mintychochip.buildtools.api.service.WorldAccess;

public interface Tool {
    String name();

    ToolPreview preview(ToolRequest request, WorldAccess world);

    ValidationResult validate(ToolRequest request, WorldAccess world, SurvivalTransaction survival);

    OperationRecord execute(ToolRequest request, WorldAccess world, SurvivalTransaction survival);

    void undo(OperationRecord record, WorldAccess world, SurvivalTransaction survival);
}
```

Do not put `org.bukkit`, `io.papermc`, or `net.minecraft` types in any of these signatures.

- [ ] **Step 4: Run API tests and inspect dependencies**

```bash
./gradlew :api:test :api:dependencies --configuration compileClasspath
```

Expected: all API tests PASS. The `compileClasspath` report for `:api` contains no `io.papermc`, `org.bukkit`, `org.spigotmc`, or `net.minecraft` artifact.

- [ ] **Step 5: Commit the contracts**

```bash
git add api
git commit -m "Define platform-neutral BuildTools tool contracts"
```

---

### Task 4: Common registry and operation history

**Files:**
- Create: `common/src/main/java/dev/mintychochip/buildtools/common/tool/ToolRegistry.java`
- Create: `common/src/main/java/dev/mintychochip/buildtools/common/operation/OperationHistory.java`
- Test: `common/src/test/java/dev/mintychochip/buildtools/common/tool/RecordingTool.java`
- Test: `common/src/test/java/dev/mintychochip/buildtools/common/tool/ToolRegistryTest.java`
- Test: `common/src/test/java/dev/mintychochip/buildtools/common/operation/OperationHistoryTest.java`

- [ ] **Step 1: Write failing registry and history tests**

Create `common/src/test/java/dev/mintychochip/buildtools/common/tool/RecordingTool.java`:

```java
package dev.mintychochip.buildtools.common.tool;

import dev.mintychochip.buildtools.api.cost.ResourceCost;
import dev.mintychochip.buildtools.api.operation.BlockChange;
import dev.mintychochip.buildtools.api.operation.OperationRecord;
import dev.mintychochip.buildtools.api.service.SurvivalTransaction;
import dev.mintychochip.buildtools.api.service.WorldAccess;
import dev.mintychochip.buildtools.api.tool.Tool;
import dev.mintychochip.buildtools.api.tool.ToolPreview;
import dev.mintychochip.buildtools.api.tool.ToolRequest;
import dev.mintychochip.buildtools.api.tool.ValidationResult;
import dev.mintychochip.buildtools.api.world.BlockState;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RecordingTool implements Tool {
    private final String name;
    private ValidationResult validation = ValidationResult.valid();
    private ResourceCost cost = ResourceCost.none();
    private int executions;
    private int undos;
    private BlockState afterState = BlockState.of("minecraft:stone");

    public RecordingTool(String name) {
        this.name = name;
    }

    public RecordingTool withValidation(ValidationResult validation) {
        this.validation = validation;
        return this;
    }

    public RecordingTool withCost(ResourceCost cost) {
        this.cost = cost;
        return this;
    }

    public RecordingTool withAfterState(BlockState afterState) {
        this.afterState = afterState;
        return this;
    }

    public int executions() {
        return executions;
    }

    public int undos() {
        return undos;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public ToolPreview preview(ToolRequest request, WorldAccess world) {
        return new ToolPreview(request.selection(), request.selection().volume(), cost);
    }

    @Override
    public ValidationResult validate(ToolRequest request, WorldAccess world, SurvivalTransaction survival) {
        return validation;
    }

    @Override
    public OperationRecord execute(ToolRequest request, WorldAccess world, SurvivalTransaction survival) {
        executions++;
        List<BlockChange> changes = new ArrayList<>();
        BlockChange change = new BlockChange(
                request.selection().pos1(),
                world.getBlock(request.selection().pos1()),
                afterState);
        world.setBlock(change.position(), change.after());
        changes.add(change);
        return new OperationRecord(UUID.randomUUID(), request.actorId(), name, changes, cost);
    }

    @Override
    public void undo(OperationRecord record, WorldAccess world, SurvivalTransaction survival) {
        undos++;
        for (BlockChange change : record.changes()) {
            world.setBlock(change.position(), change.before());
        }
    }
}
```

Create `common/src/test/java/dev/mintychochip/buildtools/common/tool/ToolRegistryTest.java`:

```java
package dev.mintychochip.buildtools.common.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ToolRegistryTest {
    @Test
    void registersAndResolvesByName() {
        ToolRegistry registry = new ToolRegistry();
        RecordingTool tool = new RecordingTool("fill");
        registry.register(tool);

        assertEquals(tool, registry.require("fill"));
        assertTrue(registry.find("fill").isPresent());
        assertTrue(registry.names().contains("fill"));
    }

    @Test
    void rejectsDuplicateNamesAndUnknownLookups() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new RecordingTool("fill"));

        assertThrows(IllegalStateException.class, () -> registry.register(new RecordingTool("fill")));
        assertThrows(IllegalArgumentException.class, () -> registry.require("replace"));
    }
}
```

Create `common/src/test/java/dev/mintychochip/buildtools/common/operation/OperationHistoryTest.java`:

```java
package dev.mintychochip.buildtools.common.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.cost.ResourceCost;
import dev.mintychochip.buildtools.api.operation.OperationRecord;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OperationHistoryTest {
    private static final ActorId ACTOR = new ActorId(UUID.fromString("00000000-0000-0000-0000-0000000000aa"));

    @Test
    void undoThenRedoRestoresRecordAndNewMutationClearsRedo() {
        OperationHistory history = new OperationHistory(8);
        OperationRecord first = record("one");
        OperationRecord second = record("two");

        history.record(ACTOR, first);
        history.record(ACTOR, second);

        assertEquals(second, history.undo(ACTOR).orElseThrow());
        assertEquals(second, history.redo(ACTOR).orElseThrow());
        assertEquals(second, history.undo(ACTOR).orElseThrow());

        history.record(ACTOR, record("three"));
        assertTrue(history.redo(ACTOR).isEmpty());
    }

    @Test
    void evictsOldestRecordsWhenMaxSizeExceeded() {
        OperationHistory history = new OperationHistory(2);
        OperationRecord first = record("one");
        OperationRecord second = record("two");
        OperationRecord third = record("three");

        history.record(ACTOR, first);
        history.record(ACTOR, second);
        history.record(ACTOR, third);

        assertEquals(third, history.undo(ACTOR).orElseThrow());
        assertEquals(second, history.undo(ACTOR).orElseThrow());
        assertTrue(history.undo(ACTOR).isEmpty());
    }

    private static OperationRecord record(String toolName) {
        return new OperationRecord(UUID.randomUUID(), ACTOR, toolName, List.of(), ResourceCost.none());
    }
}
```

- [ ] **Step 2: Run the common tests and confirm they fail**

```bash
./gradlew :common:test --tests dev.mintychochip.buildtools.common.tool.ToolRegistryTest \
  --tests dev.mintychochip.buildtools.common.operation.OperationHistoryTest
```

Expected: FAIL because `ToolRegistry` and `OperationHistory` do not exist.

- [ ] **Step 3: Implement registry and history**

Create `common/src/main/java/dev/mintychochip/buildtools/common/tool/ToolRegistry.java`:

```java
package dev.mintychochip.buildtools.common.tool;

import dev.mintychochip.buildtools.api.tool.Tool;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public void register(Tool tool) {
        Objects.requireNonNull(tool, "tool");
        String name = tool.name();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tool name must be present");
        }
        if (tools.putIfAbsent(name, tool) != null) {
            throw new IllegalStateException("Tool already registered: " + name);
        }
    }

    public Optional<Tool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public Tool require(String name) {
        return find(name).orElseThrow(() -> new IllegalArgumentException("Unknown tool: " + name));
    }

    public Set<String> names() {
        return Set.copyOf(tools.keySet());
    }
}
```

Create `common/src/main/java/dev/mintychochip/buildtools/common/operation/OperationHistory.java`:

```java
package dev.mintychochip.buildtools.common.operation;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.operation.OperationRecord;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class OperationHistory {
    private final int maxSize;
    private final Map<ActorId, ArrayDeque<OperationRecord>> undoStacks = new HashMap<>();
    private final Map<ActorId, ArrayDeque<OperationRecord>> redoStacks = new HashMap<>();

    public OperationHistory(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive");
        }
        this.maxSize = maxSize;
    }

    public void record(ActorId actor, OperationRecord record) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(record, "record");
        ArrayDeque<OperationRecord> undo = undoStacks.computeIfAbsent(actor, ignored -> new ArrayDeque<>());
        undo.addLast(record);
        while (undo.size() > maxSize) {
            undo.removeFirst();
        }
        redoStacks.remove(actor);
    }

    public Optional<OperationRecord> undo(ActorId actor) {
        ArrayDeque<OperationRecord> undo = undoStacks.get(actor);
        if (undo == null || undo.isEmpty()) {
            return Optional.empty();
        }
        OperationRecord record = undo.removeLast();
        redoStacks.computeIfAbsent(actor, ignored -> new ArrayDeque<>()).addLast(record);
        return Optional.of(record);
    }

    public Optional<OperationRecord> redo(ActorId actor) {
        ArrayDeque<OperationRecord> redo = redoStacks.get(actor);
        if (redo == null || redo.isEmpty()) {
            return Optional.empty();
        }
        OperationRecord record = redo.removeLast();
        undoStacks.computeIfAbsent(actor, ignored -> new ArrayDeque<>()).addLast(record);
        return Optional.of(record);
    }

    public void clear(ActorId actor) {
        undoStacks.remove(actor);
        redoStacks.remove(actor);
    }
}
```

- [ ] **Step 4: Run the registry and history tests**

```bash
./gradlew :common:test --tests dev.mintychochip.buildtools.common.tool.ToolRegistryTest \
  --tests dev.mintychochip.buildtools.common.operation.OperationHistoryTest
```

Expected: PASS.

- [ ] **Step 5: Commit registry and history**

```bash
git add common
git commit -m "Add tool registry and per-actor operation history"
```

---

### Task 5: Common executor and operation-limit guard

**Files:**
- Create: `common/src/main/java/dev/mintychochip/buildtools/common/operation/OperationGuard.java`
- Create: `common/src/main/java/dev/mintychochip/buildtools/common/tool/ToolExecutor.java`
- Test: `common/src/test/java/dev/mintychochip/buildtools/common/tool/RecordingWorldAccess.java`
- Test: `common/src/test/java/dev/mintychochip/buildtools/common/tool/RecordingSurvivalTransaction.java`
- Test: `common/src/test/java/dev/mintychochip/buildtools/common/tool/AllowAllPermissions.java`
- Test: `common/src/test/java/dev/mintychochip/buildtools/common/operation/OperationGuardTest.java`
- Test: `common/src/test/java/dev/mintychochip/buildtools/common/tool/ToolExecutorTest.java`

Executor rules to implement and test:

1. Unknown tool names are invalid and do not execute.
2. Missing `buildtools.tool.<name>` is invalid and does not execute.
3. `OperationGuard` rejects interaction distance, selection extent, and operation size independently.
4. A tool that returns an invalid `ValidationResult` does not execute and is not recorded.
5. A valid execute records exactly one `OperationRecord`.
6. Undo asks the tool to restore `before` states and refunds the recorded cost.
7. Redo reapplies `after` states, charges the recorded cost, and is cleared by a new mutation.

Charge and refund live in `ToolExecutor`, not in `RecordingTool`. Tools mutate blocks and produce records.

- [ ] **Step 1: Write failing guard and executor tests**

Create `common/src/test/java/dev/mintychochip/buildtools/common/tool/RecordingWorldAccess.java`:

```java
package dev.mintychochip.buildtools.common.tool;

import dev.mintychochip.buildtools.api.service.WorldAccess;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.api.world.BlockState;
import java.util.HashMap;
import java.util.Map;

public final class RecordingWorldAccess implements WorldAccess {
    private final Map<BlockPosition, BlockState> blocks = new HashMap<>();

    public RecordingWorldAccess put(BlockPosition position, BlockState state) {
        blocks.put(position, state);
        return this;
    }

    @Override
    public BlockState getBlock(BlockPosition position) {
        return blocks.getOrDefault(position, BlockState.of("minecraft:air"));
    }

    @Override
    public void setBlock(BlockPosition position, BlockState state) {
        blocks.put(position, state);
    }

    @Override
    public boolean isLoaded(BlockPosition position) {
        return true;
    }
}
```

Create `common/src/test/java/dev/mintychochip/buildtools/common/tool/RecordingSurvivalTransaction.java`:

```java
package dev.mintychochip.buildtools.common.tool;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.cost.ResourceCost;
import dev.mintychochip.buildtools.api.service.SurvivalTransaction;
import java.util.ArrayList;
import java.util.List;

public final class RecordingSurvivalTransaction implements SurvivalTransaction {
    private boolean affordable = true;
    private final List<ResourceCost> charges = new ArrayList<>();
    private final List<ResourceCost> refunds = new ArrayList<>();

    public RecordingSurvivalTransaction withAffordable(boolean affordable) {
        this.affordable = affordable;
        return this;
    }

    public List<ResourceCost> charges() {
        return charges;
    }

    public List<ResourceCost> refunds() {
        return refunds;
    }

    @Override
    public boolean canAfford(ActorId actor, ResourceCost cost) {
        return affordable || cost.isEmpty();
    }

    @Override
    public void charge(ActorId actor, ResourceCost cost) {
        if (!canAfford(actor, cost)) {
            throw new IllegalStateException("cannot afford " + cost);
        }
        charges.add(cost);
    }

    @Override
    public void refund(ActorId actor, ResourceCost cost) {
        refunds.add(cost);
    }
}
```

Create `common/src/test/java/dev/mintychochip/buildtools/common/tool/AllowAllPermissions.java`:

```java
package dev.mintychochip.buildtools.common.tool;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.service.PermissionService;

public final class AllowAllPermissions implements PermissionService {
    @Override
    public boolean has(ActorId actor, String node) {
        return true;
    }
}
```

Create `common/src/test/java/dev/mintychochip/buildtools/common/operation/OperationGuardTest.java`:

```java
package dev.mintychochip.buildtools.common.operation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.buildtools.api.cost.ResourceCost;
import dev.mintychochip.buildtools.api.limits.OperationLimits;
import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import dev.mintychochip.buildtools.api.tool.ToolPreview;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import org.junit.jupiter.api.Test;

class OperationGuardTest {
    private final OperationGuard guard = new OperationGuard(new OperationLimits(6, 4, 8));

    @Test
    void interactionDistanceIsIndependentOfSelectionExtent() {
        BlockPosition origin = new BlockPosition("world", 0, 64, 0);
        assertTrue(guard.validateInteraction(origin, new BlockPosition("world", 4, 64, 0)).valid());
        assertFalse(guard.validateInteraction(origin, new BlockPosition("world", 7, 64, 0)).valid());
    }

    @Test
    void selectionExtentDoesNotUseInteractionDistance() {
        CuboidSelection allowed = new CuboidSelection(
                new BlockPosition("world", 0, 64, 0),
                new BlockPosition("world", 3, 64, 0));
        CuboidSelection tooLong = new CuboidSelection(
                new BlockPosition("world", 0, 64, 0),
                new BlockPosition("world", 4, 64, 0));

        assertTrue(guard.validateSelection(allowed).valid());
        assertFalse(guard.validateSelection(tooLong).valid());
    }

    @Test
    void previewCountIsCappedByMaxOperationBlocks() {
        CuboidSelection region = new CuboidSelection(
                new BlockPosition("world", 0, 64, 0),
                new BlockPosition("world", 1, 64, 0));
        assertTrue(guard.validatePreview(new ToolPreview(region, 8, ResourceCost.none())).valid());
        assertFalse(guard.validatePreview(new ToolPreview(region, 9, ResourceCost.none())).valid());
    }
}
```

Create `common/src/test/java/dev/mintychochip/buildtools/common/tool/ToolExecutorTest.java`:

```java
package dev.mintychochip.buildtools.common.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.cost.ResourceCost;
import dev.mintychochip.buildtools.api.limits.OperationLimits;
import dev.mintychochip.buildtools.api.operation.OperationRecord;
import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import dev.mintychochip.buildtools.api.service.PermissionService;
import dev.mintychochip.buildtools.api.tool.ToolRequest;
import dev.mintychochip.buildtools.api.tool.ValidationResult;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.api.world.BlockState;
import dev.mintychochip.buildtools.common.operation.OperationGuard;
import dev.mintychochip.buildtools.common.operation.OperationHistory;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ToolExecutorTest {
    private static final ActorId ACTOR = new ActorId(UUID.fromString("00000000-0000-0000-0000-0000000000aa"));
    private static final BlockPosition POS = new BlockPosition("world", 0, 64, 0);

    private ToolRegistry registry;
    private OperationHistory history;
    private ToolExecutor executor;
    private RecordingWorldAccess world;
    private RecordingSurvivalTransaction survival;
    private RecordingTool tool;

    @BeforeEach
    void setUp() {
        registry = new ToolRegistry();
        history = new OperationHistory(8);
        executor = new ToolExecutor(
                registry,
                history,
                new OperationGuard(OperationLimits.defaults()),
                new AllowAllPermissions());
        world = new RecordingWorldAccess().put(POS, BlockState.of("minecraft:dirt"));
        survival = new RecordingSurvivalTransaction();
        tool = new RecordingTool("replace").withCost(new ResourceCost(Map.of("minecraft:stone", 1)));
        registry.register(tool);
    }

    @Test
    void invalidToolValidationDoesNotExecuteOrRecord() {
        tool.withValidation(ValidationResult.invalid("cannot replace air"));

        Optional<OperationRecord> result = executor.execute(request(), world, survival);

        assertTrue(result.isEmpty());
        assertEquals(0, tool.executions());
        assertTrue(history.undo(ACTOR).isEmpty());
        assertTrue(survival.charges().isEmpty());
        assertEquals(BlockState.of("minecraft:dirt"), world.getBlock(POS));
    }

    @Test
    void missingPermissionDoesNotExecute() {
        executor = new ToolExecutor(
                registry,
                history,
                new OperationGuard(OperationLimits.defaults()),
                (PermissionService) (actor, node) -> false);

        Optional<OperationRecord> result = executor.execute(request(), world, survival);

        assertTrue(result.isEmpty());
        assertEquals(0, tool.executions());
    }

    @Test
    void validExecuteRecordsOneOperationAndUndoRestoresWorld() {
        OperationRecord record = executor.execute(request(), world, survival).orElseThrow();

        assertEquals(1, tool.executions());
        assertEquals(BlockState.of("minecraft:stone"), world.getBlock(POS));
        assertEquals(1, survival.charges().size());
        assertEquals(record, history.undo(ACTOR).orElseThrow());
        history.record(ACTOR, record);

        OperationRecord undone = executor.undo(ACTOR, world, survival).orElseThrow();
        assertEquals(record.operationId(), undone.operationId());
        assertEquals(1, tool.undos());
        assertEquals(BlockState.of("minecraft:dirt"), world.getBlock(POS));
        assertEquals(1, survival.refunds().size());
    }

    @Test
    void redoIsInvalidatedByANewMutation() {
        executor.execute(request(), world, survival);
        executor.undo(ACTOR, world, survival);
        executor.execute(request(), world, survival);

        assertTrue(executor.redo(ACTOR, world, survival).isEmpty());
    }

    @Test
    void unaffordableRedoDoesNotMutateWorldAndCanBeRetried() {
        executor.execute(request(), world, survival);
        executor.undo(ACTOR, world, survival);
        survival.withAffordable(false);

        assertTrue(executor.redo(ACTOR, world, survival).isEmpty());
        assertEquals(BlockState.of("minecraft:dirt"), world.getBlock(POS));
        assertEquals(1, survival.charges().size());

        survival.withAffordable(true);
        OperationRecord redone = executor.redo(ACTOR, world, survival).orElseThrow();
        assertEquals(BlockState.of("minecraft:stone"), world.getBlock(POS));
        assertEquals("replace", redone.toolName());
        assertEquals(2, survival.charges().size());
    }

    private static ToolRequest request() {
        return new ToolRequest(
                ACTOR,
                "replace",
                new CuboidSelection(POS, POS),
                Map.of());
    }
}
```

- [ ] **Step 2: Run the new common tests and confirm they fail**

```bash
./gradlew :common:test --tests dev.mintychochip.buildtools.common.operation.OperationGuardTest \
  --tests dev.mintychochip.buildtools.common.tool.ToolExecutorTest
```

Expected: FAIL because `OperationGuard` and `ToolExecutor` do not exist.

- [ ] **Step 3: Implement the guard and executor**

Create `common/src/main/java/dev/mintychochip/buildtools/common/operation/OperationGuard.java`:

```java
package dev.mintychochip.buildtools.common.operation;

import dev.mintychochip.buildtools.api.limits.OperationLimits;
import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import dev.mintychochip.buildtools.api.tool.ToolPreview;
import dev.mintychochip.buildtools.api.tool.ValidationResult;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import java.util.Objects;

public final class OperationGuard {
    private final OperationLimits limits;

    public OperationGuard(OperationLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    public ValidationResult validateInteraction(BlockPosition origin, BlockPosition target) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(target, "target");
        if (!origin.worldId().equals(target.worldId())) {
            return ValidationResult.invalid("Interaction must stay in the same world");
        }
        double dx = (origin.x() + 0.5) - (target.x() + 0.5);
        double dy = (origin.y() + 0.5) - (target.y() + 0.5);
        double dz = (origin.z() + 0.5) - (target.z() + 0.5);
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance > limits.interactionDistance()) {
            return ValidationResult.invalid("Target is beyond interaction distance");
        }
        return ValidationResult.valid();
    }

    public ValidationResult validateSelection(CuboidSelection selection) {
        Objects.requireNonNull(selection, "selection");
        if (selection.extent() > limits.selectionExtent()) {
            return ValidationResult.invalid("Selection exceeds maximum extent");
        }
        if (selection.volume() > limits.maxOperationBlocks()) {
            return ValidationResult.invalid("Selection exceeds maximum operation size");
        }
        return ValidationResult.valid();
    }

    public ValidationResult validatePreview(ToolPreview preview) {
        Objects.requireNonNull(preview, "preview");
        ValidationResult selection = validateSelection(preview.region());
        if (!selection.valid()) {
            return selection;
        }
        if (preview.affectedCount() > limits.maxOperationBlocks()) {
            return ValidationResult.invalid("Preview exceeds maximum operation size");
        }
        return ValidationResult.valid();
    }
}
```

Create `common/src/main/java/dev/mintychochip/buildtools/common/tool/ToolExecutor.java`:

```java
package dev.mintychochip.buildtools.common.tool;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.operation.BlockChange;
import dev.mintychochip.buildtools.api.operation.OperationRecord;
import dev.mintychochip.buildtools.api.service.PermissionService;
import dev.mintychochip.buildtools.api.service.SurvivalTransaction;
import dev.mintychochip.buildtools.api.service.WorldAccess;
import dev.mintychochip.buildtools.api.tool.Tool;
import dev.mintychochip.buildtools.api.tool.ToolPreview;
import dev.mintychochip.buildtools.api.tool.ToolRequest;
import dev.mintychochip.buildtools.api.tool.ValidationResult;
import dev.mintychochip.buildtools.common.operation.OperationGuard;
import dev.mintychochip.buildtools.common.operation.OperationHistory;
import java.util.Objects;
import java.util.Optional;

public final class ToolExecutor {
    private final ToolRegistry registry;
    private final OperationHistory history;
    private final OperationGuard guard;
    private final PermissionService permissions;

    public ToolExecutor(
            ToolRegistry registry,
            OperationHistory history,
            OperationGuard guard,
            PermissionService permissions) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.history = Objects.requireNonNull(history, "history");
        this.guard = Objects.requireNonNull(guard, "guard");
        this.permissions = Objects.requireNonNull(permissions, "permissions");
    }

    public ToolPreview preview(ToolRequest request, WorldAccess world) {
        return registry.require(request.toolName()).preview(request, world);
    }

    public ValidationResult validate(ToolRequest request, WorldAccess world, SurvivalTransaction survival) {
        Objects.requireNonNull(request, "request");
        Optional<Tool> tool = registry.find(request.toolName());
        if (tool.isEmpty()) {
            return ValidationResult.invalid("Unknown tool: " + request.toolName());
        }
        String node = "buildtools.tool." + request.toolName();
        if (!permissions.has(request.actorId(), node)) {
            return ValidationResult.invalid("Missing permission " + node);
        }
        ValidationResult selection = guard.validateSelection(request.selection());
        if (!selection.valid()) {
            return selection;
        }
        ToolPreview preview = tool.get().preview(request, world);
        ValidationResult previewLimit = guard.validatePreview(preview);
        if (!previewLimit.valid()) {
            return previewLimit;
        }
        if (!survival.canAfford(request.actorId(), preview.estimatedCost())) {
            return ValidationResult.invalid("Insufficient blocks for operation");
        }
        return tool.get().validate(request, world, survival);
    }

    public Optional<OperationRecord> execute(
            ToolRequest request, WorldAccess world, SurvivalTransaction survival) {
        ValidationResult validation = validate(request, world, survival);
        if (!validation.valid()) {
            return Optional.empty();
        }
        Tool tool = registry.require(request.toolName());
        ToolPreview preview = tool.preview(request, world);
        survival.charge(request.actorId(), preview.estimatedCost());
        OperationRecord record = tool.execute(request, world, survival);
        history.record(request.actorId(), record);
        return Optional.of(record);
    }

    public Optional<OperationRecord> undo(ActorId actor, WorldAccess world, SurvivalTransaction survival) {
        Optional<OperationRecord> record = history.undo(actor);
        record.ifPresent(value -> {
            registry.require(value.toolName()).undo(value, world, survival);
            survival.refund(actor, value.cost());
        });
        return record;
    }

    public Optional<OperationRecord> redo(ActorId actor, WorldAccess world, SurvivalTransaction survival) {
        Optional<OperationRecord> record = history.redo(actor);
        if (record.isEmpty()) {
            return Optional.empty();
        }
        OperationRecord value = record.get();
        if (!survival.canAfford(actor, value.cost())) {
            history.undo(actor);
            return Optional.empty();
        }
        survival.charge(actor, value.cost());
        for (BlockChange change : value.changes()) {
            world.setBlock(change.position(), change.after());
        }
        return Optional.of(value);
    }
}
```

`history.redo` moves the record onto the undo stack. If the actor cannot afford the redo, `history.undo` moves it back onto the redo stack so a later `redo` can try again and a later `undo` stays empty. That is the only retry path in this class.

- [ ] **Step 4: Run common tests and inspect dependencies**

```bash
./gradlew :common:test :common:dependencies --configuration compileClasspath
```

Expected: all common tests PASS. `compileClasspath` includes `project :api` plus the JDK. It must not include `io.papermc`, `org.bukkit`, `org.spigotmc`, or `net.minecraft`.

- [ ] **Step 5: Commit executor and guard**

```bash
git add common
git commit -m "Add platform-neutral tool executor and operation limits"
```

---

### Task 6: Paper adapters and plugin composition root

**Files:**
- Modify: `paper/src/main/java/dev/mintychochip/buildtools/paper/BuildToolsPlugin.java`
- Create: `paper/src/main/java/dev/mintychochip/buildtools/paper/command/BuildToolsCommand.java`
- Create: `paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperBlockStates.java`
- Create: `paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperWorldAccess.java`
- Create: `paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperSurvivalTransaction.java`
- Create: `paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperPreviewRenderer.java`
- Create: `paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperTaskScheduler.java`
- Create: `paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperPermissionService.java`
- Test: `paper/src/test/java/dev/mintychochip/buildtools/paper/PaperBoundaryTest.java`
- Test: `paper/src/test/java/dev/mintychochip/buildtools/paper/adapter/PaperBlockStatesTest.java`

Paper-only translation rules:

- `PaperBlockStates` converts `minecraft:oak_stairs[facing=east,half=bottom]` strings to API `BlockState` values and back.
- `PaperWorldAccess` uses `Server.getWorld`, `World.getBlockAt`, and `Block.setBlockData`.
- `PaperSurvivalTransaction` uses player inventories. Unknown item keys throw `IllegalArgumentException`. Overflow refunds drop at the player.
- `PaperPreviewRenderer.show` and `clear` are no-ops that reject nulls. Do not spawn display entities in this plan.
- `BuildToolsCommand` does not pretend tools work. It reports that the plugin loaded and lists registered tool names (empty today).

- [ ] **Step 1: Write failing Paper boundary tests**

Create `paper/src/test/java/dev/mintychochip/buildtools/paper/adapter/PaperBlockStatesTest.java`:

```java
package dev.mintychochip.buildtools.paper.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.buildtools.api.world.BlockState;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PaperBlockStatesTest {
    @Test
    void parsesPlainAndStatefulKeysIntoApiOwnedValues() {
        BlockState stone = PaperBlockStates.parse("minecraft:stone");
        assertEquals("minecraft:stone", stone.namespacedKey());
        assertTrue(stone.properties().isEmpty());

        BlockState stairs = PaperBlockStates.parse("minecraft:oak_stairs[facing=east,half=bottom]");
        assertEquals("minecraft:oak_stairs", stairs.namespacedKey());
        assertEquals(Map.of("facing", "east", "half", "bottom"), stairs.properties());
        assertEquals("minecraft:oak_stairs[facing=east,half=bottom]", PaperBlockStates.toBukkitString(stairs));
    }

    @Test
    void rejectsBlankKeys() {
        assertThrows(IllegalArgumentException.class, () -> PaperBlockStates.parse(" "));
    }
}
```

Create `paper/src/test/java/dev/mintychochip/buildtools/paper/PaperBoundaryTest.java`:

```java
package dev.mintychochip.buildtools.paper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.buildtools.api.service.PermissionService;
import dev.mintychochip.buildtools.api.service.PreviewRenderer;
import dev.mintychochip.buildtools.api.service.SurvivalTransaction;
import dev.mintychochip.buildtools.api.service.TaskScheduler;
import dev.mintychochip.buildtools.api.service.WorldAccess;
import dev.mintychochip.buildtools.paper.adapter.PaperPermissionService;
import dev.mintychochip.buildtools.paper.adapter.PaperPreviewRenderer;
import dev.mintychochip.buildtools.paper.adapter.PaperSurvivalTransaction;
import dev.mintychochip.buildtools.paper.adapter.PaperTaskScheduler;
import dev.mintychochip.buildtools.paper.adapter.PaperWorldAccess;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

class PaperBoundaryTest {
    @Test
    void pluginMetadataPointsAtPaperEntryPoint() throws Exception {
        try (InputStream in = BuildToolsPlugin.class.getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(in);
            String yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(yaml.contains("main: dev.mintychochip.buildtools.paper.BuildToolsPlugin"));
            assertTrue(yaml.contains("api-version: \"26.2\"") || yaml.contains("api-version: '26.2'")
                    || yaml.contains("api-version: 26.2"));
            assertTrue(yaml.contains("bt"));
            assertTrue(yaml.contains("0.1.0"));
        }
    }

    @Test
    void pluginAndAdaptersStayOnThePaperSideOfTheBoundary() {
        assertTrue(JavaPlugin.class.isAssignableFrom(BuildToolsPlugin.class));
        assertTrue(WorldAccess.class.isAssignableFrom(PaperWorldAccess.class));
        assertTrue(SurvivalTransaction.class.isAssignableFrom(PaperSurvivalTransaction.class));
        assertTrue(PreviewRenderer.class.isAssignableFrom(PaperPreviewRenderer.class));
        assertTrue(TaskScheduler.class.isAssignableFrom(PaperTaskScheduler.class));
        assertTrue(PermissionService.class.isAssignableFrom(PaperPermissionService.class));
    }
}
```

- [ ] **Step 2: Run the Paper tests and confirm they fail**

```bash
./gradlew :paper:test --tests dev.mintychochip.buildtools.paper.adapter.PaperBlockStatesTest \
  --tests dev.mintychochip.buildtools.paper.PaperBoundaryTest
```

Expected: FAIL because the adapter classes do not exist.

- [ ] **Step 3: Implement adapters, command, and plugin wiring**

Create `paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperBlockStates.java`:

```java
package dev.mintychochip.buildtools.paper.adapter;

import dev.mintychochip.buildtools.api.world.BlockState;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.block.data.BlockData;

public final class PaperBlockStates {
    private PaperBlockStates() {}

    public static BlockState fromBukkit(BlockData data) {
        return parse(data.getAsString());
    }

    public static BlockState parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("namespacedKey must be present");
        }
        int bracket = raw.indexOf('[');
        if (bracket < 0) {
            return BlockState.of(raw);
        }
        if (!raw.endsWith("]")) {
            throw new IllegalArgumentException("Malformed block state: " + raw);
        }
        String key = raw.substring(0, bracket);
        String body = raw.substring(bracket + 1, raw.length() - 1);
        Map<String, String> properties = new LinkedHashMap<>();
        if (!body.isBlank()) {
            for (String part : body.split(",")) {
                String[] kv = part.split("=", 2);
                if (kv.length != 2) {
                    throw new IllegalArgumentException("Malformed block state property: " + part);
                }
                properties.put(kv[0], kv[1]);
            }
        }
        return new BlockState(key, properties);
    }

    public static String toBukkitString(BlockState state) {
        if (state.properties().isEmpty()) {
            return state.namespacedKey();
        }
        String props = state.properties().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(","));
        return state.namespacedKey() + "[" + props + "]";
    }
}
```

Create `paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperWorldAccess.java`:

```java
package dev.mintychochip.buildtools.paper.adapter;

import dev.mintychochip.buildtools.api.service.WorldAccess;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.api.world.BlockState;
import java.util.Objects;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class PaperWorldAccess implements WorldAccess {
    private final Server server;

    public PaperWorldAccess(Server server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public BlockState getBlock(BlockPosition position) {
        return PaperBlockStates.fromBukkit(blockAt(position).getBlockData());
    }

    @Override
    public void setBlock(BlockPosition position, BlockState state) {
        blockAt(position).setBlockData(server.createBlockData(PaperBlockStates.toBukkitString(state)), false);
    }

    @Override
    public boolean isLoaded(BlockPosition position) {
        World world = world(position);
        return world.isChunkLoaded(Math.floorDiv(position.x(), 16), Math.floorDiv(position.z(), 16));
    }

    private Block blockAt(BlockPosition position) {
        return world(position).getBlockAt(position.x(), position.y(), position.z());
    }

    private World world(BlockPosition position) {
        World world = server.getWorld(position.worldId());
        if (world == null) {
            throw new IllegalArgumentException("Unknown world: " + position.worldId());
        }
        return world;
    }
}
```

Create `paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperSurvivalTransaction.java`:

```java
package dev.mintychochip.buildtools.paper.adapter;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.cost.ResourceCost;
import dev.mintychochip.buildtools.api.service.SurvivalTransaction;
import java.util.HashMap;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class PaperSurvivalTransaction implements SurvivalTransaction {
    private final Server server;

    public PaperSurvivalTransaction(Server server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public boolean canAfford(ActorId actor, ResourceCost cost) {
        Player player = requirePlayer(actor);
        for (var entry : cost.itemCounts().entrySet()) {
            Material material = requireMaterial(entry.getKey());
            if (!player.getInventory().containsAtLeast(new ItemStack(material), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void charge(ActorId actor, ResourceCost cost) {
        Player player = requirePlayer(actor);
        if (!canAfford(actor, cost)) {
            throw new IllegalStateException("Actor cannot afford " + cost);
        }
        for (var entry : cost.itemCounts().entrySet()) {
            player.getInventory().removeItem(new ItemStack(requireMaterial(entry.getKey()), entry.getValue()));
        }
    }

    @Override
    public void refund(ActorId actor, ResourceCost cost) {
        Player player = requirePlayer(actor);
        for (var entry : cost.itemCounts().entrySet()) {
            HashMap<Integer, ItemStack> leftover =
                    player.getInventory().addItem(new ItemStack(requireMaterial(entry.getKey()), entry.getValue()));
            leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
        }
    }

    private Player requirePlayer(ActorId actor) {
        Player player = server.getPlayer(actor.value());
        if (player == null) {
            throw new IllegalArgumentException("Player is not online: " + actor.value());
        }
        return player;
    }

    private static Material requireMaterial(String key) {
        Material material = Material.matchMaterial(key);
        if (material == null || !material.isItem()) {
            throw new IllegalArgumentException("Unknown item: " + key);
        }
        return material;
    }
}
```

Create `paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperPreviewRenderer.java`:

```java
package dev.mintychochip.buildtools.paper.adapter;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.service.PreviewRenderer;
import dev.mintychochip.buildtools.api.tool.ToolPreview;
import java.util.Objects;

public final class PaperPreviewRenderer implements PreviewRenderer {
    @Override
    public void show(ActorId actor, ToolPreview preview) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(preview, "preview");
    }

    @Override
    public void clear(ActorId actor) {
        Objects.requireNonNull(actor, "actor");
    }
}
```

Create `paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperTaskScheduler.java`:

```java
package dev.mintychochip.buildtools.paper.adapter;

import dev.mintychochip.buildtools.api.service.TaskScheduler;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperTaskScheduler implements TaskScheduler {
    private final JavaPlugin plugin;

    public PaperTaskScheduler(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public void runOnMain(Runnable task) {
        Objects.requireNonNull(task, "task");
        if (plugin.getServer().isPrimaryThread()) {
            task.run();
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    @Override
    public void runLater(Runnable task, long delayTicks) {
        Objects.requireNonNull(task, "task");
        plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
    }
}
```

Create `paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperPermissionService.java`:

```java
package dev.mintychochip.buildtools.paper.adapter;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.service.PermissionService;
import java.util.Objects;
import org.bukkit.Server;
import org.bukkit.entity.Player;

public final class PaperPermissionService implements PermissionService {
    private final Server server;

    public PaperPermissionService(Server server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public boolean has(ActorId actor, String node) {
        Player player = server.getPlayer(actor.value());
        return player != null && player.hasPermission(node);
    }
}
```

Create `paper/src/main/java/dev/mintychochip/buildtools/paper/command/BuildToolsCommand.java`:

```java
package dev.mintychochip.buildtools.paper.command;

import dev.mintychochip.buildtools.common.tool.ToolRegistry;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class BuildToolsCommand implements CommandExecutor {
    private final ToolRegistry registry;

    public BuildToolsCommand(ToolRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (registry.names().isEmpty()) {
            sender.sendMessage(Component.text("BuildTools is loaded. No tools are registered yet."));
            return true;
        }
        sender.sendMessage(Component.text("BuildTools tools: " + String.join(", ", registry.names())));
        return true;
    }
}
```

Replace `paper/src/main/java/dev/mintychochip/buildtools/paper/BuildToolsPlugin.java` with:

```java
package dev.mintychochip.buildtools.paper;

import dev.mintychochip.buildtools.api.limits.OperationLimits;
import dev.mintychochip.buildtools.api.service.PreviewRenderer;
import dev.mintychochip.buildtools.common.operation.OperationGuard;
import dev.mintychochip.buildtools.common.operation.OperationHistory;
import dev.mintychochip.buildtools.common.tool.ToolExecutor;
import dev.mintychochip.buildtools.common.tool.ToolRegistry;
import dev.mintychochip.buildtools.paper.adapter.PaperPermissionService;
import dev.mintychochip.buildtools.paper.adapter.PaperPreviewRenderer;
import dev.mintychochip.buildtools.paper.adapter.PaperSurvivalTransaction;
import dev.mintychochip.buildtools.paper.adapter.PaperTaskScheduler;
import dev.mintychochip.buildtools.paper.adapter.PaperWorldAccess;
import dev.mintychochip.buildtools.paper.command.BuildToolsCommand;
import java.util.Objects;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class BuildToolsPlugin extends JavaPlugin {
    private ToolRegistry toolRegistry;
    private ToolExecutor toolExecutor;
    private OperationHistory history;
    private PreviewRenderer previewRenderer;
    private PaperWorldAccess worldAccess;
    private PaperSurvivalTransaction survivalTransaction;
    private PaperTaskScheduler taskScheduler;

    @Override
    public void onEnable() {
        this.toolRegistry = new ToolRegistry();
        this.history = new OperationHistory(32);
        this.toolExecutor = new ToolExecutor(
                toolRegistry,
                history,
                new OperationGuard(OperationLimits.defaults()),
                new PaperPermissionService(getServer()));
        this.previewRenderer = new PaperPreviewRenderer();
        this.worldAccess = new PaperWorldAccess(getServer());
        this.survivalTransaction = new PaperSurvivalTransaction(getServer());
        this.taskScheduler = new PaperTaskScheduler(this);

        PluginCommand command = Objects.requireNonNull(getCommand("bt"), "plugin.yml must declare /bt");
        command.setExecutor(new BuildToolsCommand(toolRegistry));
        getLogger().info("BuildTools loaded. Tools are not registered yet.");
    }

    @Override
    public void onDisable() {
        this.toolRegistry = null;
        this.toolExecutor = null;
        this.history = null;
        this.previewRenderer = null;
        this.worldAccess = null;
        this.survivalTransaction = null;
        this.taskScheduler = null;
        getLogger().info("BuildTools disabled.");
    }
}
```

Do not register a replace/fill/copy/paste tool. The composition root exists so later plans can `toolRegistry.register(...)`.

- [ ] **Step 4: Run Paper tests and the full module build**

```bash
./gradlew :paper:test :paper:build
```

Expected: Paper tests PASS. `paper/build/libs/buildtools-0.1.0.jar` exists. Confirm metadata:

```bash
jar tf paper/build/libs/buildtools-0.1.0.jar | grep -E 'plugin.yml|BuildToolsPlugin.class'
```

Expected output includes:

```text
plugin.yml
dev/mintychochip/buildtools/paper/BuildToolsPlugin.class
```

- [ ] **Step 5: Commit Paper integration**

```bash
git add paper
git commit -m "Add Paper adapters and plugin composition root"
```

---

### Task 7: Dependency boundary check, verification, and living-spec handoff

**Files:**
- Modify: `build.gradle.kts`
- Create: `docs/superpowers/verification/buildtools-module-boundaries.md`
- Modify: `docs/living-specs/buildtools.md`

- [ ] **Step 1: Add the automated boundary check**

Append this to the root `build.gradle.kts` after the `subprojects { ... }` block:

```kotlin
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
```

Do not add a `build-logic` included build unless this task cannot be expressed in the root script. The root script above is sufficient.

- [ ] **Step 2: Run full verification**

```bash
./gradlew clean build
```

Expected: `:api:test`, `:common:test`, `:paper:test`, `:verifyModuleBoundaries`, and `:paper:jar` all succeed.

Prove the check can fail. Temporarily add this line to `api/build.gradle.kts`:

```kotlin
dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.112-stable")
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}
```

Run:

```bash
./gradlew verifyModuleBoundaries
```

Expected: FAIL with `Module :api depends on forbidden group io.papermc`.

Revert `api/build.gradle.kts` to the empty conventions file from Task 1, then re-run:

```bash
./gradlew verifyModuleBoundaries
```

Expected: SUCCESS.

- [ ] **Step 3: Run the source-level leakage check**

```bash
git grep -n -E 'org\.bukkit|io\.papermc|net\.minecraft' -- api common || true
```

Expected: no matching lines under `api/` or `common/`. Matches under `paper/` are required and allowed.

- [ ] **Step 4: Record verification evidence and update the living spec**

Create `docs/superpowers/verification/buildtools-module-boundaries.md` and fill in the actual command output you observed:

```markdown
# BuildTools module-boundary verification

Date: 2026-08-17

## Commands

- `./gradlew clean build`
- `./gradlew verifyModuleBoundaries`
- `git grep -n -E 'org\.bukkit|io\.papermc|net\.minecraft' -- api common`

## Results

- `./gradlew clean build`: PASS
- Induced Paper dependency on `:api`: `verifyModuleBoundaries` FAILED with `Module :api depends on forbidden group io.papermc`
- After revert, `verifyModuleBoundaries`: PASS
- Source grep on `api` and `common`: no matches

## Not claimed

Replace, fill, copy, paste, selection rendering, and blueprint persistence are not implemented.
```

In `docs/living-specs/buildtools.md`:

1. Set `Last updated: 2026-08-17`. Keep the existing Paper 26.2 / Java 25 guidance; do not revert it to 1.20.4 or Java 17.
2. In **Implementation guidance**, add: `Gradle modules: api (contracts), common (JVM domain), paper (Paper adapter). See docs/superpowers/specs/2026-08-17-buildtools-gradle-modules-design.md.`
3. In **Current**, change `this root catalog and project bootstrap` to checked, and add a checked item `Gradle multi-project scaffold: api, common, paper`.
4. Leave `selection`, `tools`, `survival`, and `blueprints` unchecked.
5. Add a **Current notes** paragraph linking this plan and the architecture spec.
6. Add a Decisions log row: `2026-08-17 | Strict api/common/paper Gradle split | Keeps Paper at the edge and makes domain tests JVM-only`. Keep the existing Paper 26.2 / Java 25 decision row.

- [ ] **Step 5: Commit verification and catalog updates**

```bash
git add build.gradle.kts docs/superpowers/verification/buildtools-module-boundaries.md docs/living-specs/buildtools.md
git commit -m "Verify api and common stay free of Paper dependencies"
```

Do not add `docs/superpowers/plans` or `docs/superpowers/specs` in this commit unless they are still untracked and you are including only those two existing documents. Prefer a separate docs commit if they have not been committed yet:

```bash
git add docs/superpowers/specs/2026-08-17-buildtools-gradle-modules-design.md \
  docs/superpowers/plans/2026-08-17-buildtools-gradle-modules.md
git commit -m "Document BuildTools Gradle module architecture and plan"
```

---

## Self-review

### Spec coverage

| Spec requirement | Task |
|---|---|
| `settings.gradle.kts` includes `api`, `common`, `paper` | Task 1 |
| Root Java 25 / Kotlin DSL / group `dev.mintychochip` | Task 1 |
| `:paper -> :common -> :api` and `:paper -> :api` | Task 1 |
| Only `paper` depends on Paper | Tasks 1, 7 |
| Minimal API contracts: tool lifecycle, request/preview/validation, operation records | Tasks 2–3 |
| Selection, position, block-state, resource-cost, permission, history abstractions | Tasks 2–4 |
| Ports for world, survival, preview, scheduling | Task 3 |
| Common registry, lifecycle, limits, history | Tasks 4–5 |
| No fake production tools | Tasks 5–6 use test doubles only |
| Paper `JavaPlugin`, commands, adapters, `plugin.yml` | Tasks 1 and 6 |
| Preview rendering stays bounded / not one entity per block | Task 6 `PaperPreviewRenderer` is a no-op; no entity spam |
| `./gradlew test` and `./gradlew build` | Tasks 1, 5, 6, 7 |
| Dependency verification fails on Paper leaking into `api`/`common` | Task 7 |
| Living spec not marked complete for unimplemented tools | Task 7 |
| Persistence, second platform, economy, claims, creative free-build | Out of scope; not planned |

### Placeholder scan

No TBD/TODO steps. Tests, implementation, commands, and commit messages are written out. Task 5 documents the exact `redo` method to type, including the unaffordable path.

### Type consistency

Used everywhere:

- `ActorId`, `BlockPosition`, `BlockState`, `CuboidSelection`, `ResourceCost`, `OperationLimits`
- `BlockChange`, `OperationRecord`
- `Tool`, `ToolRequest`, `ToolPreview`, `ValidationResult`
- `WorldAccess`, `SurvivalTransaction`, `PreviewRenderer`, `TaskScheduler`, `PermissionService`
- `ToolRegistry`, `ToolExecutor`, `OperationHistory`, `OperationGuard`
- Paper adapters: `PaperWorldAccess`, `PaperSurvivalTransaction`, `PaperPreviewRenderer`, `PaperTaskScheduler`, `PaperPermissionService`, `PaperBlockStates`
- Plugin: `BuildToolsPlugin`, command `BuildToolsCommand`, permission node `buildtools.tool.<name>`
