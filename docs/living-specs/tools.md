# Tools — Living Spec

> Status: active
> Last updated: 2026-08-29
> Owners:

## Intent

Provide a consistent, extensible set of building operations that all share the same lifecycle: preview, validate, execute, undo.

## Boundaries

### In scope
- Tool framework: registration, state, commands, preview, execution, undo.
- V1 tools: replace, fill, copy, paste, horizontal extension.
- Undo/redo for every mutating tool.
- Per-tool permission nodes and survival cost.

### Out of scope / non-goals
- Shape generators in V1.
- Terrain tools.
- Client-side GUIs in V1.
- Scripting / macros.
- Rotation/flip in V1 (handled by blueprints later).

## Invariants

- Every mutating tool must be undoable.
- Every tool that affects blocks must show a preview before the player confirms.
- A tool cannot execute without a valid selection or target block, as appropriate.
- `interaction_distance` validates the initial raycast/target; `selection_extent` and `max_operation_blocks` validate the planned operation.
- Survival cost must be validated before execution and applied on success.
- No duplication or loss of items through tool use or undo.

## Implementation guidance

- Define `Tool` interface: `preview(Player, args)`, `validate(Player, args)`, `execute(Player, args)`, `undo(Player, record)`.
- Commands map to tools: `/bt replace <from> <to>`, `/bt fill <block>`, `/bt copy`, `/bt paste`.
- Previews re-use the selection rendering system with complete outer faces sent as per-player fake block packets or particles; enforce the surface packet budget and never send one fake block per volume block.
- Large operations are split into chunk tasks to avoid tick lag.
- Each player keeps an undo stack of operation records; records store block diffs (old state, new state, position).
- Tool results are translated into survival transactions.
- Connected-block tools use bounded six-directional BFS/flood-fill traversal with a visited set.
- Chaining stops at `max_operation_blocks`, `max_chain_distance`, unloaded/forbidden areas, or a non-matching block.
- A chain preview is calculated before mutation and displays the affected outline, count, and resource cost.
- Horizontal extension uses the ordinary `minecraft:brick` item as a main-hand mode token, copies the aimed block state, charges matching placeable items from the player's inventory, aims from the player toward a horizontal anchor, and plans a bounded one-block-high plane before commit.

## Current

- [x] Tool framework and registry
- [x] Replace tool
- [x] Fill tool with affected-block preview
- [x] Copy tool (region to clipboard)
- [x] Paste tool (clipboard to world)
- [x] Undo/redo
- [x] Horizontal extension from the brick mode token with aimed-block material and scroll-sized preview

## Next

- [ ] Move / stack tool
- [ ] Pattern fill
- [ ] Connected-block replace/fill tool
- [ ] Walls, floors, circles, spheres, lines
- [ ] Mirror / symmetry
- [ ] Paste with rotation / flip

## Future

- [ ] Custom player-defined tools
- [ ] Macro recording and playback
- [ ] Terrain brushes

## Decisions log

| Date | Decision | Why |
|------|----------|-----|
| 2026-08-16 | Common tool lifecycle (preview/validate/execute/undo) | Makes every tool predictable and testable |
| 2026-08-16 | Fill tool shows affected-block outline using selection renderer | User requirement: see what will be filled |
| 2026-08-17 | Connected-block chaining is explicit and bounded | Prevents accidental traversal of an entire structure or world |
| 2026-08-17 | Chain traversal starts with six orthogonal neighbors | Predictable connectivity; diagonal modes can be added later |
| 2026-08-17 | Reach is validated separately from operation extent | Initial interaction can be reachable while the approved operation is larger |
| 2026-08-29 | Horizontal extension uses an armed preview and scroll-sized commit | Keeps repeated right-click progression safe while preserving exact preview, validation, cost, and undo |

## Open questions

- [ ] Confirmation flow for large operations?
- [ ] Should copy also copy block entities (chests, signs) in V1?
- [ ] Per-tool cooldowns or rate limits?
