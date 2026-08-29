# Masonry

**Survival-friendly building assistant for Paper/Spigot.**

Masonry gives players a set of survival-mode building tools — selection, replace, fill, horizontal extension, copy, paste, undo/redo, blueprints, and a wand gadget — with live previews, inventory costs, and permission checks. It is a server-side plugin.

Created by **mintychochip**.

## Requirements

- Paper 1.26.2 (build 112 or compatible)
- Java 25

## Installation

1. Build the plugin jar: `./gradlew :masonry-paper:build` and copy `masonry-paper/build/libs/masonry-0.1.0.jar`.
2. Drop it in your server's `plugins/` directory.
3. Restart the server.
4. Optional: edit `plugins/Masonry/config.yml` to adjust default limits.

## Quick start

- Get the gadget: `/masonry wand`
- Or use commands:
  - Set selection corners: `/masonry pos1` and `/masonry pos2`
  - Replace blocks: `/masonry replace <from> <to>`
  - Fill a region: `/masonry fill <block>`
  - Extend the block you are aiming at: hold `minecraft:brick` in the main hand to show the preview, right-click to grow one row, sneak-scroll to extend or retract, normal scroll to change hotbar slots, and sneak+right-click to commit; matching placeable blocks are charged from your inventory. Top-face clicks extend horizontally along your aim; side-face clicks extend out from that face.
  - Copy a region: `/masonry copy`
  - Paste it: `/masonry paste`
  - Undo: `/masonry undo`
  - Redo: `/masonry redo`

## Commands & permissions
The root command is `/masonry`. Subcommands: `pos1`, `pos2`, `replace`, `fill`, `survival_fill`, `extend`, `copy`, `paste`, `cut`, `move`, `repeat`, `undo`, `redo`, `blueprint`, `wand`, `previewmode`.

| Permission | Default | Description |
|---|---|---|
| `masonry.command` | `true` | Use the `/masonry` command root |
| `masonry.tool.survival_fill` | `true` | Use `/masonry survival_fill` and the gadget's safe fill |
| `masonry.tool.extend` | `true` | Use brick-token extension mode; the aimed block supplies the material from inventory |
| `masonry.tool.copy` | `true` | Use `/masonry copy` |
| `masonry.tool.fill` | `op` | Use `/masonry fill` (overwrites any block) |
| `masonry.tool.replace` | `op` | Use `/masonry replace` (replaces matched blocks) |
| `masonry.tool.cut` | `op` | Use `/masonry cut` (copy and clear the selection) |
| `masonry.tool.move` | `op` | Use `/masonry move` (cut and paste elsewhere) |
| `masonry.tool.paste` | `op` | Use `/masonry paste` |
| `masonry.bypass.creative` | `op` | Skip survival inventory cost |
| `masonry.bypass.survival` | `op` | Skip survival inventory cost |

## Configuration

`plugins/Masonry/config.yml`:

```yaml
# Masonry server limits. Every value must be a positive integer.
# A missing or invalid entry falls back to its default and logs a warning.
limits:
  # Maximum distance (in blocks) at which a player may target pos1/pos2.
  interaction-distance: 6
  # Maximum inclusive edge length of the cuboid selection.
  selection-extent: 64
  # Maximum number of blocks a single tool run may change.
  max-operation-blocks: 32768
```

See [`docs/living-specs/`](docs/living-specs/) for detailed behavior and design docs.

## Development

Masonry is a Gradle multi-project using Java 25.

```bash
./gradlew build
```

Modules:

- `masonry-api` — platform-neutral contracts
- `masonry-common` — domain behavior, no Minecraft dependencies
- `masonry-paper` — Paper plugin and platform adapters
- `masonry-test` — smoke-test plugin and `run-paper` server harness

Run a local test server:

```bash
./gradlew :masonry-test:runServer
```

The smoke-test plugin logs whether the main `Masonry` plugin is present and enabled.

## Links

- [`docs/living-specs/masonry.md`](docs/living-specs/masonry.md) — project overview and boundaries
- [`docs/living-specs/tools.md`](docs/living-specs/tools.md) — tool framework and available tools
- [`docs/superpowers/specs/2026-08-26-masonry-test-design.md`](docs/superpowers/specs/2026-08-26-masonry-test-design.md) — latest design docs
