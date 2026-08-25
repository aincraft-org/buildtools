# BuildTools Gadget Implementation Plan

> Historical record — active project is Masonry.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a single BuildTools gadget item that lets players shift-left/right-click to set pos1/pos2 and run Fill/Replace/Copy/Paste without typing `/bt` subcommands.

**Architecture:** A Paper `PlayerInteractEvent` listener detects a custom PDC-tagged item. The listener uses the existing `BuildToolsCommands` dispatcher by constructing `CommandContext` with the clicked block and the player's offhand material. A new `SurvivalFillTool` provides an `air-or-replaceable` fill. `PlayerSession` stores the active `ToolMode`.

**Tech Stack:** Java 25, Gradle 9.7.0, Paper 26.2, JUnit 5.

## Global Constraints
- No new module boundaries; changes stay in `api`, `common`, `paper`.
- `api` and `common` must not import `io.papermc`, `org.bukkit`, `org.spigotmc`, or `net.minecraft`.
- Tool plan logic stays in `common`; Bukkit interaction stays in `paper`.
- All new behavior must have a deterministic, isolated test in `common/src/test` unless it requires a running server.

---

### Task 1: Add `isReplaceable` port to `WorldAccess`

**Files:**
- Modify: `api/src/main/java/dev/mintychochip/buildtools/api/service/WorldAccess.java`
- Modify: `paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperWorldAccess.java`
- Modify: `common/src/test/java/dev/mintychochip/buildtools/common/support/InMemoryWorldAccess.java`

**Interfaces:**
- Consumes: `BlockPosition` from `api`
- Produces: `boolean isReplaceable(BlockPosition position)` on `WorldAccess`

- [ ] **Step 1: Declare `isReplaceable` in the `WorldAccess` interface**

```java
/**
 * @param position block coordinate
 * @return {@code true} if the block can be replaced by another block without being broken first
 */
boolean isReplaceable(BlockPosition position);
```

- [ ] **Step 2: Implement in `PaperWorldAccess`**

In `PaperWorldAccess` add:

```java
@Override
public boolean isReplaceable(BlockPosition position) {
    return blockAt(position).getBlockData().isReplaceable();
}
```

- [ ] **Step 3: Implement in `InMemoryWorldAccess` with a configurable set**

Add fields and helpers:

```java
private final Set<String> replaceableNames = new HashSet<>();

public InMemoryWorldAccess withReplaceable(String namespacedKey) {
    replaceableNames.add(namespacedKey);
    return this;
}

@Override
public boolean isReplaceable(BlockPosition position) {
    BlockState state = getBlock(position);
    return state.isAir() || replaceableNames.contains(state.namespacedKey());
}
```

- [ ] **Step 4: Run the `api` and `common` tests to verify the interface still compiles**

```bash
./gradlew :api:compileJava :common:compileTestJava
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/dev/mintychochip/buildtools/api/service/WorldAccess.java \
        paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/PaperWorldAccess.java \
        common/src/test/java/dev/mintychochip/buildtools/common/support/InMemoryWorldAccess.java
git commit -m "api: add WorldAccess.isReplaceable port"
```

---

### Task 2: Add `ToolMode` enum to `PlayerSession`

**Files:**
- Create: `common/src/main/java/dev/mintychochip/buildtools/common/session/ToolMode.java`
- Modify: `common/src/main/java/dev/mintychochip/buildtools/common/session/PlayerSession.java`

**Interfaces:**
- Consumes: nothing
- Produces: `ToolMode` enum with `FILL`, `REPLACE`, `COPY`, `PASTE`; `PlayerSession#mode()`, `setMode(ToolMode)`, `clear()`

- [ ] **Step 1: Write the `ToolMode` enum**

```java
package dev.mintychochip.buildtools.common.session;

public enum ToolMode {
    FILL, REPLACE, COPY, PASTE;

    public ToolMode next() {
        ToolMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
```

- [ ] **Step 2: Add mode state and accessors to `PlayerSession`**

Fields and methods (also add `import java.util.Objects;` if it is not already present):

```java
private ToolMode mode = ToolMode.FILL;

public ToolMode mode() {
    return mode;
}

public void setMode(ToolMode mode) {
    this.mode = Objects.requireNonNull(mode, "mode");
}

@Override
public void clear() {
    pos1 = null;
    pos2 = null;
    clipboard = null;
    mode = ToolMode.FILL;
}
```

- [ ] **Step 3: Add a small test that `clear()` resets the mode to `FILL`**

Create `common/src/test/java/dev/mintychochip/buildtools/common/session/PlayerSessionTest.java`:

```java
package dev.mintychochip.buildtools.common.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlayerSessionTest {
    @Test
    void clearResetsModeToFill() {
        PlayerSession session = new PlayerSession();
        session.setMode(ToolMode.PASTE);
        session.clear();
        assertEquals(ToolMode.FILL, session.mode());
    }
}
```

- [ ] **Step 4: Run the new test and compile**

```bash
./gradlew :common:test --tests "dev.mintychochip.buildtools.common.session.PlayerSessionTest"
```

Expected: `PlayerSessionTest > clearResetsModeToFill PASSED`.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/dev/mintychochip/buildtools/common/session/ToolMode.java \
        common/src/main/java/dev/mintychochip/buildtools/common/session/PlayerSession.java \
        common/src/test/java/dev/mintychochip/buildtools/common/session/PlayerSessionTest.java
git commit -m "common: add ToolMode and session mode tracking"
```

---

### Task 3: Create `SurvivalFillTool`

**Files:**
- Create: `common/src/main/java/dev/mintychochip/buildtools/common/tool/SurvivalFillTool.java`
- Create: `common/src/test/java/dev/mintychochip/buildtools/common/tool/SurvivalFillToolTest.java`
- Modify: `paper/src/main/java/dev/mintychochip/buildtools/paper/BuildToolsPlugin.java` (register tool)

**Interfaces:**
- Consumes: `WorldAccess#isReplaceable`, `ToolRequest` with argument `block`
- Produces: `Tool` named `survival_fill`

- [ ] **Step 1: Implement `SurvivalFillTool`**

```java
package dev.mintychochip.buildtools.common.tool;

import dev.mintychochip.buildtools.api.operation.BlockChange;
import dev.mintychochip.buildtools.api.service.SurvivalTransaction;
import dev.mintychochip.buildtools.api.service.WorldAccess;
import dev.mintychochip.buildtools.api.tool.ToolRequest;
import dev.mintychochip.buildtools.api.tool.ValidationResult;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.api.world.BlockState;
import java.util.ArrayList;
import java.util.List;

public final class SurvivalFillTool extends MutatingTool {
    @Override
    public String name() {
        return "survival_fill";
    }

    @Override
    public ValidationResult validate(ToolRequest request, WorldAccess world, SurvivalTransaction survival) {
        ValidationResult base = super.validate(request, world, survival);
        if (!base.valid()) {
            return base;
        }
        if (parseTarget(request) == null) {
            return ValidationResult.invalid("No fill material");
        }
        return ValidationResult.passed();
    }

    @Override
    protected BlockPlan plan(ToolRequest request, WorldAccess world) {
        BlockState target = parseTarget(request);
        if (target == null || request.selection() == null) {
            return new BlockPlan(request.selection(), List.of());
        }
        List<BlockChange> changes = new ArrayList<>();
        for (BlockPosition position : request.selection().positions()) {
            BlockState before = world.getBlock(position);
            if (!before.equals(target) && world.isReplaceable(position)) {
                changes.add(new BlockChange(position, before, target));
            }
        }
        return new BlockPlan(request.selection(), List.copyOf(changes));
    }

    private static BlockState parseTarget(ToolRequest request) {
        String raw = request.argument("block");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return BlockStates.parse(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
```

- [ ] **Step 2: Add `survival_fill` to `BuildToolsCommands` and register the tool**

In `BuildToolsCommands#execute`, add a case:

```java
case "survival_fill" -> fill(context, true);
```

Change the `fill` helper to accept a soft flag and use the right tool name:

```java
private CommandResult fill(CommandContext context, boolean soft) {
    if (context.arguments().size() < 2) {
        return CommandResult.fail("Usage: /bt fill <block>");
    }
    return runTool(context, soft ? "survival_fill" : "fill", Map.of("block", context.argument(1)));
}
```

Leave the existing `case "fill" -> fill(context, false);` line unchanged.

In `BuildToolsPlugin#onEnable`, register the tool:

```java
this.toolRegistry.register(new SurvivalFillTool());
```

- [ ] **Step 3: Write `SurvivalFillToolTest`**

```java
package dev.mintychochip.buildtools.common.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import dev.mintychochip.buildtools.api.tool.ToolRequest;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.api.world.BlockState;
import dev.mintychochip.buildtools.common.support.TestHarness;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SurvivalFillToolTest {
    @Test
    void fillOnlyReplacesAirAndReplaceableBlocks() {
        TestHarness harness = new TestHarness();
        harness.registry.register(new SurvivalFillTool());

[Showing lines 1-300 of 851. Use :301 to continue]