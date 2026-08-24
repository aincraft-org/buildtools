# BuildTools Gadget Implementation Plan

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

        BlockPosition a = harness.pos(0, 64, 0);
        BlockPosition b = harness.pos(1, 65, 0);

        harness.world.put(a, BlockState.of("minecraft:air"));
        harness.world.put(b, BlockState.of("minecraft:stone"));
        harness.world.withReplaceable("minecraft:air");
        harness.survival.give(TestHarness.ACTOR, "minecraft:stone", 16);

        CuboidSelection selection = new CuboidSelection(a, b);
        ToolRequest request = new ToolRequest(
                TestHarness.ACTOR, "survival_fill", selection, Map.of("block", "minecraft:stone"));

        var record = harness.executor.execute(request, harness.world, harness.survival);
        assertTrue(record.isPresent(), record.toString());
        assertEquals(3, record.get().changes().size(), "only the three air positions should change");
        assertEquals(BlockState.of("minecraft:stone"), harness.world.getBlock(a));
        assertEquals(BlockState.of("minecraft:stone"), harness.world.getBlock(new BlockPosition(TestHarness.WORLD, 1, 64, 0)));
        assertEquals(BlockState.of("minecraft:stone"), harness.world.getBlock(new BlockPosition(TestHarness.WORLD, 0, 65, 0)));
        assertEquals(BlockState.of("minecraft:stone"), harness.world.getBlock(b));
        assertEquals(13, harness.survival.count(TestHarness.ACTOR, "minecraft:stone"), "16 stone minus 3 placed");
    }
}
```

The selection `a` to `b` includes (0,64,0), (1,64,0), (0,65,0), (1,65,0). (0,64,0) and (1,64,0) and (0,65,0) are air; (1,65,0) is stone and not replaceable, so it stays stone. Only the three air positions become stone; 13 stone remain in inventory.

- [ ] **Step 4: Run the test and compile**

```bash
./gradlew :common:test --tests "dev.mintychochip.buildtools.common.tool.SurvivalFillToolTest"
```

Expected: `SurvivalFillToolTest > fillOnlyReplacesAirAndReplaceableBlocks PASSED`.

- [ ] **Step 5: Commit**

```bash
git add common/src/main/java/dev/mintychochip/buildtools/common/tool/SurvivalFillTool.java \
        common/src/test/java/dev/mintychochip/buildtools/common/tool/SurvivalFillToolTest.java \
        paper/src/main/java/dev/mintychochip/buildtools/paper/BuildToolsPlugin.java
git commit -m "common: add survival fill tool and tests"
```

---

### Task 4: Create the `GadgetItem`

**Files:**
- Create: `paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/GadgetItem.java`

**Interfaces:**
- Consumes: `JavaPlugin` for `NamespacedKey`
- Produces: `ItemStack` with PDC tag `buildtools:gadget` and `GadgetItem.isGadget(ItemStack)`

- [ ] **Step 1: Implement `GadgetItem`**

```java
package dev.mintychochip.buildtools.paper.adapter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class GadgetItem {
    private static final String GADGET_KEY = "buildtools_gadget";

    private GadgetItem() {}

    public static ItemStack create(JavaPlugin plugin) {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        item.editMeta(meta -> {
            meta.displayName(Component.text("BuildTools Gadget", NamedTextColor.AQUA));
            meta.lore(List.of(
                    Component.text("Shift-left: cycle mode", NamedTextColor.GRAY),
                    Component.text("Shift-right: set pos1", NamedTextColor.GRAY),
                    Component.text("Right-click: set pos2 / apply", NamedTextColor.GRAY)));
            meta.setUnbreakable(true);
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, GADGET_KEY),
                    PersistentDataType.BYTE, (byte) 1);
        });
        return item;
    }

    public static boolean isGadget(JavaPlugin plugin, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(
                new NamespacedKey(plugin, GADGET_KEY),
                PersistentDataType.BYTE);
    }
}
```

- [ ] **Step 2: Compile the `paper` module**

```bash
./gradlew :paper:compileJava
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/GadgetItem.java
git commit -m "paper: add BuildTools gadget item"
```

---

### Task 5: Create the `GadgetListener`

**Files:**
- Create: `paper/src/main/java/dev/mintychochip/buildtools/paper/GadgetListener.java`

**Interfaces:**
- Consumes: `BuildToolsCommands`, `PlayerSessionStore`, `OperationLimits`, `WorldAccess`, `SurvivalTransaction`
- Produces: `PlayerInteractEvent` handler

- [ ] **Step 1: Implement `GadgetListener`**

```java
package dev.mintychochip.buildtools.paper;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.command.CommandContext;
import dev.mintychochip.buildtools.api.command.CommandResult;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.common.command.BuildToolsCommands;
import dev.mintychochip.buildtools.common.session.PlayerSessionStore;
import dev.mintychochip.buildtools.common.session.ToolMode;
import dev.mintychochip.buildtools.paper.adapter.GadgetItem;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

public final class GadgetListener implements Listener {
    private final JavaPlugin plugin;
    private final BuildToolsCommands commands;
    private final PlayerSessionStore sessions;
    private final OperationLimits limits;
    private final WorldAccess world;
    private final SurvivalTransaction survival;

    public GadgetListener(
            JavaPlugin plugin,
            BuildToolsCommands commands,
            PlayerSessionStore sessions,
            OperationLimits limits,
            WorldAccess world,
            SurvivalTransaction survival) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.commands = Objects.requireNonNull(commands, "commands");
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.world = Objects.requireNonNull(world, "world");
        this.survival = Objects.requireNonNull(survival, "survival");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();
        if (!GadgetItem.isGadget(plugin, main)) {
            return;
        }
        if (!player.hasPermission("buildtools.command")) {
            return;
        }

        event.setCancelled(true);
        Action action = event.getAction();
        boolean sneaking = player.isSneaking();

        if (sneaking && (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK)) {
            cycleMode(player);
            return;
        }

        if (sneaking && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            handleShiftRightClick(player, targetOf(player));
            return;
        }

        if (!sneaking && (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)) {
            handleRightClick(player, targetOf(player));
        }
    }

    private void cycleMode(Player player) {
        ToolMode next = sessions.session(actorOf(player)).mode().next();
        sessions.session(actorOf(player)).setMode(next);
        player.sendActionBar(Component.text("BuildTools mode: " + next.name().toLowerCase(), NamedTextColor.GREEN));
    }

    private void handleShiftRightClick(Player player, BlockPosition target) {
        if (target == null) {
            player.sendActionBar(Component.text("No target block", NamedTextColor.RED));
            return;
        }
        ToolMode mode = sessions.session(actorOf(player)).mode();
        if (mode == ToolMode.PASTE) {
            sessions.session(actorOf(player)).setPos1(target);
            dispatch(player, target, List.of("paste"));
            return;
        }
        dispatch(player, target, List.of("pos1"));
    }

    private void handleRightClick(Player player, BlockPosition target) {
        if (target == null) {
            player.sendActionBar(Component.text("No target block", NamedTextColor.RED));
            return;
        }
        ToolMode mode = sessions.session(actorOf(player)).mode();
        switch (mode) {
            case FILL -> handleFill(player, target);
            case REPLACE -> handleReplace(player, target);
            case COPY -> dispatch(player, target, List.of("pos2"), List.of("copy"));
            default -> player.sendActionBar(Component.text("Use shift-right-click to set pos1", NamedTextColor.RED));
        }
    }

    private void handleFill(Player player, BlockPosition target) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType().isAir() || !offhand.getType().isBlock()) {
            player.sendActionBar(Component.text("Hold a block in your offhand to fill", NamedTextColor.RED));
            return;
        }
        if (sessions.session(actorOf(player)).pos1().isEmpty()) {
            player.sendActionBar(Component.text("Set pos1 with shift-right-click first", NamedTextColor.RED));
            return;
        }
        String material = "minecraft:" + offhand.getType().getKey().getKey();
        dispatch(player, target, List.of("pos2"), List.of("survival_fill", material));
    }

    private void handleReplace(Player player, BlockPosition target) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType().isAir() || !offhand.getType().isBlock()) {
            player.sendActionBar(Component.text("Hold a block in your offhand for replace-to", NamedTextColor.RED));
            return;
        }
        if (sessions.session(actorOf(player)).pos1().isEmpty()) {
            player.sendActionBar(Component.text("Set pos1 with shift-right-click first", NamedTextColor.RED));
            return;
        }
        BlockPosition pos1 = sessions.session(actorOf(player)).pos1().orElseThrow();
        String from = world.getBlock(pos1).namespacedKey();
        String to = "minecraft:" + offhand.getType().getKey().getKey();
        dispatch(player, target, List.of("pos2"), List.of("replace", from, to));
    }

    private void dispatch(Player player, BlockPosition target, List<String> preArgs, List<String> toolArgs) {
        for (String pre : preArgs) {
            CommandResult result = commands.execute(context(player, target, List.of(pre)));
            if (!result.success()) {
                player.sendActionBar(Component.text(result.message(), NamedTextColor.RED));
                return;
            }
        }
        CommandResult result = commands.execute(context(player, target, toolArgs));
        player.sendActionBar(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private void dispatch(Player player, BlockPosition target, List<String> args) {
        CommandResult result = commands.execute(context(player, target, args));
        player.sendActionBar(Component.text(result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private CommandContext context(Player player, BlockPosition target, List<String> args) {
        BlockPosition origin = new BlockPosition(
                player.getWorld().getName(),
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ());
        return new CommandContext(
                actorOf(player), player.getWorld().getName(), origin, target, args);
    }

    private BlockPosition targetOf(Player player) {
        RayTraceResult hit = player.rayTraceBlocks(limits.interactionDistance());
        Block block = hit != null ? hit.getHitBlock() : null;
        if (block == null) {
            return null;
        }
        return new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    private static ActorId actorOf(Player player) {
        return new ActorId(player.getUniqueId());
    }
}
```

- [ ] **Step 2: Fix any compile errors in `paper`**

```bash
./gradlew :paper:compileJava
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add paper/src/main/java/dev/mintychochip/buildtools/paper/GadgetListener.java
git commit -m "paper: add gadget interaction listener"
```

---

### Task 6: Wire the Gadget in `BuildToolsPlugin`

**Files:**
- Modify: `paper/src/main/java/dev/mintychochip/buildtools/paper/BuildToolsPlugin.java`

**Interfaces:**
- Consumes: `GadgetListener`, `SurvivalFillTool`
- Produces: registered event listener, `OperationHistory(20)`

- [ ] **Step 1: Update `BuildToolsCommand` constructor and `BuildToolsPlugin#onEnable`**

In `BuildToolsCommand` add a `JavaPlugin` field and constructor:

```java
private final JavaPlugin plugin;

public BuildToolsCommand(BuildToolsCommands commands, OperationLimits limits, JavaPlugin plugin) {
    this.commands = Objects.requireNonNull(commands, "commands");
    this.limits = Objects.requireNonNull(limits, "limits");
    this.plugin = Objects.requireNonNull(plugin, "plugin");
}
```

In `BuildToolsPlugin#onEnable`, update wiring:

```java
this.history = new OperationHistory(20);
this.toolRegistry.register(new SurvivalFillTool());

PluginCommand command = Objects.requireNonNull(getCommand("bt"), "plugin.yml must declare /bt");
command.setExecutor(new BuildToolsCommand(commands, limits, this));

GadgetListener gadgetListener = new GadgetListener(
        this,
        commands,
        sessions,
        limits,
        worldAccess,
        survivalTransaction);
getServer().getPluginManager().registerEvents(gadgetListener, this);
```

- [ ] **Step 2: Compile and test `paper` unit tests**

```bash
./gradlew :paper:test
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add paper/src/main/java/dev/mintychochip/buildtools/paper/BuildToolsPlugin.java
git commit -m "paper: wire survival fill and gadget listener"
```

---

### Task 7: Add `/bt wand` to `BuildToolsCommand`

**Files:**
- Modify: `paper/src/main/java/dev/mintychochip/buildtools/paper/command/BuildToolsCommand.java`
- Modify: `paper/src/main/resources/plugin.yml`

**Interfaces:**
- Consumes: `GadgetItem`
- Produces: `/bt wand` gives the item

- [ ] **Step 1: Add a `JavaPlugin` field and intercept `wand` in `BuildToolsCommand#onCommand`**


```java
public final class BuildToolsCommand implements CommandExecutor {
    private final BuildToolsCommands commands;
    private final OperationLimits limits;
    private final JavaPlugin plugin;

    public BuildToolsCommand(BuildToolsCommands commands, OperationLimits limits, JavaPlugin plugin) {
        this.commands = Objects.requireNonNull(commands, "commands");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("BuildTools commands are player-only.", NamedTextColor.RED));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("wand")) {
            player.getInventory().addItem(GadgetItem.create(plugin));
            sender.sendMessage(Component.text("Given BuildTools Gadget.", NamedTextColor.GREEN));
            return true;
        }
        CommandResult result = commands.execute(toContext(player, args));
        sender.sendMessage(Component.text(
                result.message(), result.success() ? NamedTextColor.GREEN : NamedTextColor.RED));
        return true;
    }
    // ...
}
```

- [ ] **Step 2: Update `plugin.yml` `/bt` usage**

```yaml
commands:
  bt:
    description: BuildTools command root
    usage: /bt <pos1|pos2|replace|fill|copy|paste|undo|redo|blueprint|wand>
```

- [ ] **Step 3: Compile and run a quick smoke test on the live server**

```bash
./gradlew :paper:jar
```

Then on the running server:

```
bt wand
```

Expected: you receive a `BuildTools Gadget` blaze rod.

- [ ] **Step 4: Commit**

```bash
git add paper/src/main/java/dev/mintychochip/buildtools/paper/command/BuildToolsCommand.java \
        paper/src/main/resources/plugin.yml
git commit -m "paper: add /bt wand and update usage"
```

---

### Task 8: Smoke Test on the Live Server

**Files:** none

**Interfaces:**
- Consumes: running `mc-buildtools` tmux server
- Produces: verified end-to-end gadget behavior

- [ ] **Step 1: Hot-reload the plugin**

On the running server console:

```
reload
```

- [ ] **Step 2: Test the gadget**

1. Run `/bt wand`.
2. Hold the gadget, put `stone` in offhand.
3. Shift-right-click a grass block (pos1).
4. Right-click another grass block (pos2).
5. Observe: only air/replaceable blocks in the cuboid become stone.
6. Shift-left-click to cycle to Copy.
7. Shift-right-click pos1, right-click pos2 to copy.
8. Shift-left-click to Paste.
9. Shift-right-click a target block to paste.

- [ ] **Step 3: Verify logs**

```bash
tail -n 30 paper/run/logs/latest.log
```

Expected: `BuildTools` messages with `Filled`, `Copied`, or `Pasted`.

- [ ] **Step 4: Commit any log/config updates only if needed**

If you changed `paper/run` for testing, do **not** commit generated server files.

---

### Task 9: Self-Review and Full Test Suite

- [ ] **Step 1: Run the full test suite**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run the module-boundary check**

```bash
./gradlew check
```

Expected: `BUILD SUCCESSFUL` (includes `verifyModuleBoundaries`).

- [ ] **Step 3: Final commit if all green**

```bash
git commit -m "feat: BuildTools gadget shift-click controls" --allow-empty
```

---

## Spec Coverage Checklist

- [x] Shift-left-click cycles mode → `GadgetListener#cycleMode`
- [x] Shift-right-click sets pos1 / paste → `GadgetListener#handleShiftRightClick`
- [x] Right-click sets pos2 and applies → `GadgetListener#handleRightClick`
- [x] Offhand block as fill/replace material → `handleFill`, `handleReplace`
- [x] Survival fill only replaces air/replaceable → `SurvivalFillTool#plan`
- [x] History stores last 20 actions → `BuildToolsPlugin` `new OperationHistory(20)`
- [x] `/bt wand` gives gadget → `BuildToolsCommand` `wand` path
- [x] `/bt` remains as fallback → unchanged `BuildToolsCommands`
