# Selection — Living Spec

> Status: active
> Last updated: 2026-08-19
> Owners:

## Intent

Give players a precise, visible way to define the region they are about to edit.

## Boundaries

### In scope
- Primary selection shape: cuboid.
- Visual boundary of the selected region.
- Player-scoped active selection.

### Out of scope / non-goals
- Custom shapes (polygons, freeform) in V1.
- Multiple concurrent selections per player.
- Moving the selection with the player.
- Selection expand/contract in V1.

## Invariants

- A player must have an active selection before a region tool can execute.
- `interaction_distance` limits the initial raycast/selection target; it does not cap the selected region's `selection_extent`.
- The selection boundary must be visible to the owning player while active.
- The visual preview must exactly match the selected region.
- Selection extent and volume are bounded by server limits.

## Implementation guidance

- Selection state lives in a per-player `PlayerSession`.
- Support two-point (cuboid) selection via left/right click or commands (`/bt pos1`, `/bt pos2`).
- The server raycasts the initial target and validates it against `interaction_distance`.
- Render selection boundaries with aggregated outlines or a capped number of `BlockDisplay` entities; do not create one display entity per block in large selections.
- Update or remove preview entities when the selection changes or the player logs out.
- Serialize selection as two `BlockVector3`s.

## Current

- [x] Two-point cuboid selection
- [x] Visual boundary with block displays / particles
- [x] Per-player session storage

## Next

- [ ] Selection expand/contract commands
- [ ] Sphere/cylinder selection modes
- [ ] Connected-block selection mode using bounded six-directional traversal
- [ ] Command-based precise coordinate selection
- [ ] Selection info command (extent, volume, block count)

## Future

- [ ] Multi-shape selection (poly, convex hull)
- [ ] Clipboard selection from copy
- [ ] Face/extend selection

## Decisions log

| Date | Decision | Why |
|------|----------|-----|
| 2026-08-16 | Block displays with particle fallback | Best visual clarity on modern Paper |
| 2026-08-16 | Cuboid first, other shapes later | Most common use case, simplest invariant set |
| 2026-08-17 | Interaction distance and selection extent are separate limits | A player can select a bounded remote region without granting unlimited reach |
| 2026-08-17 | Large selection previews use bounded aggregated rendering | Keeps the server responsive and avoids one entity per block |

## Open questions

- [x] Use a physical wand item (e.g., wooden axe) or command-only? — command-only in V1
- [ ] Should selection persist across logins?
- [x] Maximum selection volume and dimension limits? — extent 64, operation 32768 blocks
