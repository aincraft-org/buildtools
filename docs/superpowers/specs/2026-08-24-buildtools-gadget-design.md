# BuildTools Gadget Design

> Historical record — active project is Masonry.

## Goal
Replace the chat-command-only interaction for common BuildTools actions with a single, holdable "BuildTools Gadget" that uses shift-left/right clicks. The existing `/bt` command remains as a fallback and for advanced/edge cases.

## Controls

- Give yourself the gadget: `/bt wand`
- Hold the gadget in your main hand.
- **Shift + left-click** (air or block): cycle tool mode. Current mode is shown in the action bar.
- **Shift + right-click**: set pos1.
- **Right-click** (no shift): set pos2 and, for modes that need it, apply the tool.

## Modes and behavior

### Fill
- Offhand block is the material to place.
- Shift-right-click pos1.
- Right-click pos2.
- Fills the selection only where the existing block is air or replaceable (grass, flowers, snow, torches, etc.).
- Does **not** overwrite solid blocks.
- This is a new, gadget-only "soft fill"; `/bt fill` keeps its current overwrite behavior.

### Replace
- Offhand block is the material to replace **with**.
- Shift-right-click a block of the type you want to replace. That block becomes both the `from` material and pos1.
- Right-click pos2.
- Replaces all matching `from` blocks in the selection with the offhand `to` block.

### Copy
- Shift-right-click pos1.
- Right-click pos2.
- Copies the selection to the clipboard.

### Paste
- Shift-right-click the block you want as the paste origin.
- Pastes the clipboard at that point.

### Blueprint
Not mapped to the gadget in this version. `/bt blueprint save|load|list|delete` remains.

### Undo / Redo
Not mapped to the gadget in this version. `/bt undo` and `/bt redo` remain, and the last 20 operations are kept per player.

## Architecture changes

- **New listener**: `paper/src/main/java/dev/mintychochip/buildtools/paper/GadgetListener.java`
  - Listens to `PlayerInteractEvent`.
  - Detects the gadget by a `NamespacedKey` stored on its `PersistentDataContainer`.
  - Cancels the default interaction to avoid placing/breaking blocks.
  - Dispatches to `BuildToolsCommands` with synthesized `CommandContext` arguments.
- `WorldAccess` interface gets `boolean isReplaceable(BlockPosition position)`.
  - `PaperWorldAccess` delegates to `block.getBlockData().isReplaceable()`.
  - `InMemoryWorldAccess` treats air and any block named in an explicit set as replaceable for tests.
- **New tool**: `common/src/main/java/dev/mintychochip/buildtools/common/tool/SurvivalFillTool.java`
  - Like `FillTool`, but `plan()` skips positions where `!world.isReplaceable(position)`.
  - Registered in `BuildToolsPlugin`.
- **History limit**: change `BuildToolsPlugin` to `new OperationHistory(20)`.
- **`BuildToolsCommand` (Paper)**: add `/bt wand` subcommand. It creates and gives the player a `GadgetItem`; other subcommands still dispatch to `BuildToolsCommands`.
- **`plugin.yml`**: update `/bt` usage to mention `wand`.

## Survival economy
- Fill and Replace charge the offhand material from inventory using the existing `SurvivalTransaction`.
- Copy is free; Paste charges the placed blocks.
- The gadget itself is never consumed.

## Visual feedback
- Action bar shows `Mode: <mode>` and one-line hint when the player holds the gadget.
- Existing `PaperPreviewRenderer` shows the selection and affected blocks.

## Session changes
- `common/src/main/java/dev/mintychochip/buildtools/common/session/PlayerSession.java`:
  - `ToolMode mode` enum: `FILL`, `REPLACE`, `COPY`, `PASTE`.
  - `clear()` also resets `mode`.

## Fallback commands
- `/bt wand` — give gadget.
- `/bt fill <block>` — legacy fill that overwrites any differing block.
- `/bt replace <from> <to>` — legacy replace.
- `/bt copy`, `/bt paste`, `/bt blueprint ...`, `/bt undo`, `/bt redo`.

## Files changed
- `paper/build.gradle.kts` (already configured for run-paper, no new change here).
- `paper/src/main/java/dev/mintychochip/buildtools/paper/BuildToolsPlugin.java`
- `paper/src/main/java/dev/mintychochip/buildtools/paper/command/BuildToolsCommand.java`
- `paper/src/main/resources/plugin.yml`
- `common/src/main/java/dev/mintychochip/buildtools/common/session/PlayerSession.java`
- `common/src/main/java/dev/mintychochip/buildtools/common/tool/SurvivalFillTool.java` (new)
- `paper/src/main/java/dev/mintychochip/buildtools/paper/GadgetListener.java` (new)
- `paper/src/main/java/dev/mintychochip/buildtools/paper/adapter/GadgetItem.java` (new)

## Scope exclusions
- No client-side keybinds (not possible with a server-only plugin).
- No blueprint gadget mode in this pass.
- No undo/redo gadget mode in this pass.
- No crafting recipe for the gadget; obtained via `/bt wand`.
