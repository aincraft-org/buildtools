# Blueprints — Living Spec

> Status: active
> Last updated: 2026-08-19
> Owners:

## Intent

Let players save their selections and load them again anywhere, so builds can be reused in survival.

## Boundaries

### In scope
- Copy a selection to a clipboard blueprint.
- Save a selection as a named blueprint.
- Load a named blueprint and paste it.
- Player-scoped local storage.

### Out of scope / non-goals
- Rotation/flip in V1.
- Cross-server blueprint sharing.
- Marketplace or public blueprint libraries in V1.
- Versioned blueprint history.

## Invariants

- A blueprint must store block state accurately and compactly.
- Loading a blueprint must pay the same survival cost as placing those blocks by hand.
- Blueprint ownership is tracked; players can only manage their own unless granted.

## Implementation guidance

- Storage format: Sponge Schematic (`.schem`) or vanilla NBT Structure Block format.
- Metadata file (JSON) per blueprint for owner, name, timestamp, dimensions.
- `BlueprintManager` handles save, load, list, delete.
- Paste tool delegates to `BlueprintManager` and then runs through survival cost.
- Store blueprints in `plugins/BuildTools/blueprints/<player-uuid>/`.

## Current

- [x] Copy-to-clipboard (implicit unnamed blueprint)
- [x] Save selection as named blueprint
- [x] Load and paste named blueprint
- [x] List and delete player blueprints

## Next

- [ ] Rotate and flip before paste
- [ ] Browse and manage blueprint library
- [ ] Share blueprint with other players
- [ ] Server-wide public blueprints

## Future

- [ ] Cross-server import/export
- [ ] Blueprint versioning and diff
- [ ] Generate blueprints from structures

## Decisions log

| Date | Decision | Why |
|------|----------|-----|
| 2026-08-16 | Sponge Schematic/NBT for storage | Widely supported, preserves block state |
| 2026-08-16 | Player-scoped storage by default | Keeps V1 simple and respects ownership |
| 2026-08-16 | Rotation/flip deferred to Next | Keeps V1 paste simple and avoids illegal block-state edge cases |

## Open questions

- [ ] Max blueprint size and per-player storage quota?
- [x] Allow pasting block entities (chests, signs) in V1? — no; plain block state only
- [x] Should blueprints be stored in SQLite or flat files? — flat files (`.schem` + JSON metadata)
