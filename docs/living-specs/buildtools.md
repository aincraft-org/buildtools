# BuildTools — Living Spec

> Status: active
> Last updated: 2026-08-16
> Owners:

## Intent

Build a survival-friendly, Axiom-lite building assistant as a Paper/Spigot plugin. Players select a region and run tools (replace, fill, copy, paste) that respect survival inventory, permissions, and server claim/anti-grief rules.

Success looks like: a server owner can install the plugin, players can quickly select an area and replace/fill/copy/paste blocks without entering creative mode, and the server stays fair.

## Boundaries

### In scope
- Server-side Paper/Spigot plugin.
- Survival-mode building tools: selection, replace, fill, copy, paste, undo.
- Visual previews of the affected region before the operation commits.
- Survival economics: inventory cost, refunds on undo, permission checks.
- Blueprint save/load and basic copy/paste.

### Out of scope / non-goals
- Creative-mode free build.
- Client-side rendering or mods.
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

- Target modern Paper API (1.20.4+) and Java 17+.
- Keep tools stateless; per-player session holds selection, clipboard, and undo history.
- Distinguish `interaction_distance` (server raycast/start-point reach) from `selection_extent` and `max_operation_blocks`; selection extent and operation size are not limited to interaction distance.
- Render bounded previews with aggregated outlines or capped display entities; never create one `BlockDisplay` per affected block for large operations.
- Commands are the primary UI; hotkey/wand item support may follow.
- Prefer async validation and chunked execution for large operations.
- Permission node convention: `buildtools.tool.<name>`, `buildtools.limit.<size>`, `buildtools.bypass.creative`.
- Storage: NBT/Sponge Schematic for blueprints, SQLite or flat files for metadata.

## Current

- [ ] `selection` — two-point cuboid selection with `BlockDisplay` outline
- [ ] `tools` — replace, fill, copy, paste, undo/redo
- [ ] `survival` — inventory cost, refund on undo, permission nodes
- [ ] `blueprints` — copy-to-clipboard and save/load named blueprints
- [ ] this root catalog and project bootstrap

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
| 2026-08-16 | Visual previews via `BlockDisplay` entities, particle fallback | Clear pre-operation feedback; modern Paper supports display entities |
| 2026-08-16 | Survival economics are core, not optional | Every tool must respect inventory or it breaks survival |
| 2026-08-17 | Reach is split into interaction distance, selection extent, and operation block limits | Lets players operate on an approved region beyond their initial raycast without making reach unlimited |
| 2026-08-17 | Large previews use bounded aggregated rendering | Avoids one display entity per affected block and keeps previews performant |

## Open questions

- [ ] Supported Minecraft version range (1.20.4+, or 1.19.x?)
- [ ] Default wand item or command-only selection
- [ ] Which claim plugins to integrate first
- [ ] Exact defaults for `interaction_distance`, `selection_extent`, `max_operation_blocks`, and `max_chain_distance`
- [ ] Storage backend for blueprints and metadata
