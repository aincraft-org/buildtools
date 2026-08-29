# Masonry — Living Spec

> Status: active
> Last updated: 2026-08-29
> Owners:

## Intent

Build a survival-friendly, Axiom-lite building assistant as a Paper/Spigot plugin. Players select a region and run tools (replace, fill, copy, paste) that respect survival inventory, permissions, and server claim/anti-grief rules.

Success looks like: a server owner can install the plugin, players can quickly select an area and replace/fill/copy/paste blocks without entering creative mode, and the server stays fair.

## Boundaries

### In scope
- Server-side Paper/Spigot plugin.
- Survival-mode building tools: selection, replace, fill, horizontal extension, copy, paste, undo.
- Visual previews of the affected region before the operation commits.
- Survival economics: inventory cost, refunds on undo, permission checks.
- Blueprint save/load and basic copy/paste.

### Out of scope / non-goals
- Creative-mode free build.
- Client-side mods or custom client assets.
- Economy (money), experience, or non-block currencies in V1.
- Procedural terrain/village generation in V1.
- Cross-server blueprint sharing.


## Domain specs

- [`selection.md`](selection.md) — region selection and visual boundary
- [`tools.md`](tools.md) — tool framework and the replace/fill/copy/paste tools
- [`survival.md`](survival.md) — inventory cost, refunds, permissions
- [`blueprints.md`](blueprints.md) — save, load, and paste player blueprints

## Invariants

- Every block placed by the plugin must be paid for from the acting player's inventory or an authorized source.
- Every destructive operation (replace, fill, paste, move) must be undoable by the player.
- Previews must match the exact blocks that will be affected before the player confirms.
- The plugin must not bypass claim/anti-grief protection.
- No item or block duplication through tool use or undo.

## Implementation guidance

- Target Paper API 26.2 (`io.papermc.paper:paper-api:26.2.build.112-stable`) and Java 25.
- Gradle modules: `masonry-api` (contracts), `masonry-common` (JVM domain), `masonry-paper` (Paper adapter). See `docs/superpowers/specs/2026-08-17-buildtools-gradle-modules-design.md`.
- Keep tools stateless; per-player session holds selection, clipboard, and undo history.
- Distinguish `interaction_distance` (server raycast/start-point reach) from `selection_extent` and `max_operation_blocks`; selection extent and operation size are not limited to interaction distance.
- For block-backed region previews, render the complete outer faces with per-player BlockDisplay add/metadata/destroy packets or particles; enforce the surface packet budget and fall back to the sparse outline only when it is exceeded.
- Commands remain the fallback and advanced surface; the wand-given Masonry Gadget item is the primary interaction for common tools.
- Prefer async validation and chunked execution for large operations.
- Permission node convention: `masonry.tool.<name>`, `masonry.limit.<size>`, `masonry.bypass.creative`.
- Storage: NBT/Sponge Schematic for blueprints, SQLite or flat files for metadata.

## Current

- [x] `selection` — two-point cuboid outline via per-player BlockDisplay packets
- [x] `tools` — replace, fill, horizontal extension, copy, paste, undo/redo
- [x] `survival` — inventory cost, refund on undo, permission nodes
- [x] `blueprints` — copy-to-clipboard and save/load named blueprints
- [x] this root catalog and project bootstrap
- [x] Gradle multi-project scaffold: masonry-api, masonry-common, masonry-paper
- [x] gadget — `/masonry wand` Masonry Gadget item: shift-click pos1/paste, right-click pos2+apply, mode cycling, and offhand material for gadget fill/replace

### Current notes
V1 command surface is `/masonry` (pos1/pos2, replace, fill, survival_fill, copy, paste, undo/redo, blueprint save/load/list/delete). Defaults: `interactionDistance=6`, `selectionExtent=64`, `maxOperationBlocks=32768`. Held-block extension is a separate input path: hold the ordinary `minecraft:brick` item in the main hand and aim at the block to extend; the preview appears while the token is held, right-click grows one row, sneak-scroll extends or retracts length, normal scroll changes hotbar slots, and sneak+right-click commits through the normal survival/undo lifecycle, charging matching placeable blocks from the player's inventory. Live Paper playthrough is not required to mark Current done. The gadget (2026-08-24 design) adds a shift-click item UI over the same dispatcher; destructive `fill`/`replace`/`paste` are op-only while `survival_fill`, `copy`, and the gadget are default-allowed.

## Next

- [ ] Move / stack tool
- [ ] Pattern fill
- [ ] Connected-block chaining
- [ ] Walls, floors, circles, spheres, lines
- [ ] Mirror / symmetry
- [ ] Paste with rotation / flip
- [ ] Claim plugin integration

## Future

- [ ] Schematic/structure-block import and export
- [ ] Terrain and vegetation tools
- [ ] Team/shared blueprint library
- [ ] Economy and experience integration
- [ ] Custom scripting / macros

## Decisions log

| Date | Decision | Why |
|------|----------|-----|
| 2026-08-16 | Paper/Spigot plugin, server-side | Large server ecosystem, easy permissions and anti-grief integration |
| 2026-08-16 | Living spec catalog: root + per-feature specs | Keeps each domain focused and independently evolvable |
| 2026-08-16 | Visual previews via per-player BlockDisplay packets, particle fallback | Clear pre-operation feedback without server-side display entities |
| 2026-08-27 | Block and ghost previews use client-only BlockDisplay packets with tracked IDs | Preview entities are never registered or ticked by the server |
| 2026-08-27 | Region previews render complete outer faces within a 32768-position packet budget | Full face coverage improves spatial clarity without permitting unbounded packet bursts |
| 2026-08-16 | Survival economics are core, not optional | Every tool must respect inventory or it breaks survival |
| 2026-08-17 | Reach is split into interaction distance, selection extent, and operation block limits | Lets players operate on an approved region beyond their initial raycast without making reach unlimited |
| 2026-08-17 | Large previews use bounded aggregated rendering | Avoids one fake block per affected block and keeps previews performant |
| 2026-08-17 | Paper 26.2 and Java 25 | Matches current Paper API; 26.2 requires Java 25 |
| 2026-08-17 | Strict masonry-api/masonry-common/masonry-paper Gradle split | Keeps Paper at the edge and makes domain tests JVM-only |
| 2026-08-19 | Command-only selection in V1 | Wand item is deferred; `/bt pos1`/`pos2` is enough |
| 2026-08-19 | Defaults 6 / 64 / 32768 | Locked so limits are testable without a config file |
| 2026-08-19 | Flat-file Sponge Schematic blueprints | Player-scoped `.schem` + JSON metadata under the plugin data folder |
| 2026-08-24 | Gadget item as primary UI; `/bt` stays as fallback | Supersedes command-only V1 decision per `docs/superpowers/specs/2026-08-24-buildtools-gadget-design.md` |
| 2026-08-29 | Held-block extension is horizontal, bounded by selection extent, and executor-backed | Supports fast rows without bypassing survival costs, previews, claims, or undo |

## Open questions

- [x] Supported Minecraft version range (1.20.4+, or 1.19.x?) — Paper 26.2 / Java 25
- [x] Default wand item or command-only selection — command-only in V1
- [ ] Which claim plugins to integrate first
- [x] Exact defaults for `interaction_distance`, `selection_extent`, `max_operation_blocks`, and `max_chain_distance` — 6 / 64 / 32768; chain distance stays Next
- [x] Storage backend for blueprints and metadata — flat files + Sponge Schematic v2
