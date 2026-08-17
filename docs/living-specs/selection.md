# Selection — Living Spec

> Status: active
> Last updated: 2026-08-16
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
- The selection boundary must be visible to the owning player while active.
- The visual preview must exactly match the selected region.
- Selection size is bounded by server limit.

## Implementation guidance

- Selection state lives in a per-player `PlayerSession`.
- Support two-point (cuboid) selection via left/right click or commands (`/bt pos1`, `/bt pos2`).
- Render the boundary with `BlockDisplay` entities at the corners/edges, falling back to particle outlines.
- Update or remove displays when the selection changes or the player logs out.
- Serialize selection as two `BlockVector3`s.

## Current

- [ ] Two-point cuboid selection
- [ ] Visual boundary with block displays / particles
- [ ] Per-player session storage

## Next

- [ ] Selection expand/contract commands
- [ ] Sphere/cylinder selection modes
- [ ] Command-based precise coordinate selection
- [ ] Selection info command (size, block count)

## Future

- [ ] Multi-shape selection (poly, convex hull)
- [ ] Clipboard selection from copy
- [ ] Face/extend selection

## Decisions log

| Date | Decision | Why |
|------|----------|-----|
| 2026-08-16 | Block displays with particle fallback | Best visual clarity on modern Paper |
| 2026-08-16 | Cuboid first, other shapes later | Most common use case, simplest invariant set |

## Open questions

- [ ] Use a physical wand item (e.g., wooden axe) or command-only?
- [ ] Should selection persist across logins?
- [ ] Maximum selection volume and dimension limits?
