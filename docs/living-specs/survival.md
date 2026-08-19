# Survival — Living Spec

> Status: active
> Last updated: 2026-08-19
> Owners:

## Intent

Make every building operation feel like real survival work: players must have the blocks, and the server must enforce permissions.

## Boundaries

### In scope
- Inventory validation before an operation.
- Block cost on execute, refund on undo.
- Permission nodes per tool and per limit.
- Creative-mode bypass option.

### Out of scope / non-goals
- Money or experience economies.
- Claim/anti-grief integration in V1.
- Shared team resource pools.
- Complex crafting recipes for tool use.

## Invariants

- Player must have the required blocks in inventory before an operation places them.
- Removed blocks must be refunded to the player unless the server config says otherwise.
- No operation can proceed without the `buildtools.tool.<name>` permission.
- Refund must not exceed original cost (no duplication).

## Implementation guidance

- Use Paper inventory API to count and remove items before execution.
- Compute cost from the target block count and type, accounting for block state changes (e.g., slabs, stairs).
- Record block diffs during execution; use them to refund or re-charge on undo.
- Permission nodes follow `buildtools.tool.<name>` and `buildtools.limit.<max>` patterns.
- Provide a `buildtools.bypass.survival` or creative-mode bypass.
- In V1, fire standard block place/break events and respect cancellation by other protection plugins; explicit `ClaimProvider` integrations come in Next.
- Build claim/anti-grief integration behind a `ClaimProvider` abstraction for later use.
- Validate `interaction_distance`, `selection_extent`, and `max_operation_blocks` independently; none should silently turn into unlimited reach.
- For connected-block tools, charge only the validated operation plan and re-check inventory immediately before execution.

## Current

- [x] Inventory cost check for replace, fill, paste
- [x] Refund on undo
- [x] Permission node per tool
- [x] Creative bypass

## Next

- [ ] Claim plugin abstraction and integrations (WorldGuard, GriefPrevention, Lands, Towny)
- [ ] Configurable `interaction_distance`, `selection_extent`, `max_operation_blocks`, and `max_chain_distance`
- [ ] Rate limiting and anti-spam

## Future

- [ ] Economy integration
- [ ] Team resource pools
- [ ] Experience costs for special operations

## Decisions log

| Date | Decision | Why |
|------|----------|-----|
| 2026-08-16 | Refund on undo | If undo restores blocks, player must get back what was spent |
| 2026-08-16 | Claim-provider abstraction, integration in Next | Avoid hard dependency on a single plugin; not in V1 scope |
| 2026-08-17 | Reach and operation limits are separate configuration values | Prevents confusing interaction reach with the size of an approved edit |
| 2026-08-17 | Chained operations are charged from their computed plan | Players see and pay for exactly the blocks that will be changed |

## Open questions

- [x] Refund when inventory is full — drop items or block undo? — drop leftovers at the player
- [ ] Which claim plugins to support first?
- [x] Should creative-mode players still pay from inventory or bypass entirely? — bypass via creative or `buildtools.bypass.creative` / `buildtools.bypass.survival`
