# BuildTools Gradle Module Separation

> Historical record — active project is Masonry.

## Status

Drafted as the initial architecture specification for the `dev.mintychochip` BuildTools project.

## Goal

Establish a Gradle multi-project layout that separates platform-neutral BuildTools contracts and behavior from the Paper server integration.

The project will provide a survival-friendly building assistant for modern Paper servers, following the existing living specs in `docs/living-specs/`.

## Project identity

- Group: `dev.mintychochip`
- Root project: `buildtools`
- Build scripts: Gradle Kotlin DSL
- Java toolchain: Java 25
- Modules: `api`, `common`, `paper`

## Module graph

```text
:paper -> :common -> :api
:paper -> :api
```

The dependency graph is intentionally one-directional. `api` knows nothing about implementations or Minecraft. `common` knows only the API contracts and standard JVM facilities. `paper` is the sole module that depends on Paper/Bukkit APIs.

## Module responsibilities

### `api`

Public, platform-neutral contracts and value types used by implementations and consumers:

- Tool lifecycle contracts for preview, validation, execution, and undo.
- Tool requests, previews, validation results, and operation records.
- Selection, position, block-state, resource-cost, permission, and history abstractions.
- Ports for world access, survival transactions, preview rendering, and scheduling where those ports are required by shared behavior.

Constraints:

- No Paper, Bukkit, NMS, plugin, filesystem, or server scheduler dependency.
- Types must be usable from ordinary JVM tests.
- Public contracts must describe invariants rather than Paper-specific mechanics.

### `common`

Reusable domain behavior independent of a server platform:

- Tool registry and lifecycle orchestration.
- Replace, fill, copy, and paste tool behavior.
- Selection and operation-limit validation.
- Preview planning and bounded affected-block calculation.
- Per-player operation history and undo/redo coordination.
- Survival cost calculation and transaction coordination through API ports.

Constraints:

- No Paper or Bukkit dependency.
- World mutation, player messaging, scheduling, and display entities go through API ports.
- Large operations are represented as chunkable plans; execution policy remains injectable.

### `paper`

Paper runtime adapter and plugin distribution:

- `JavaPlugin` entry point and plugin lifecycle.
- Command registration for the BuildTools command surface.
- Paper implementations of player, world, block-state, inventory, permission, scheduling, and preview ports.
- Preview rendering using bounded aggregated outlines or capped display entities, with a particle fallback.
- Main-thread/chunked execution of world mutations.
- Plugin configuration, persistence adapters, and `plugin.yml`.

Constraints:

- This is the only module allowed to depend on Paper/Bukkit APIs.
- Platform adapters must translate Paper objects at the boundary instead of leaking them into `api` or `common`.

## Domain boundaries

The module split supports the existing domain rules:

- Every placed block must be paid for by the acting player or an authorized source.
- Every destructive operation must be undoable.
- Previews must represent the exact planned affected blocks before confirmation.
- Claim and anti-grief checks must be honored.
- Tool use and undo must not duplicate or lose items.
- Interaction distance, selection extent, and maximum operation size remain separate limits.
- Connected traversal, when added, must be bounded by operation count, chain distance, loaded/allowed areas, and matching block state.

## Initial package layout

```text
api/src/main/java/dev/mintychochip/buildtools/api/...
common/src/main/java/dev/mintychochip/buildtools/common/...
paper/src/main/java/dev/mintychochip/buildtools/paper/...
```

Package names are implementation guidance, not a requirement to expose every package publicly. Only deliberately supported API contracts should be public across module boundaries.

## Build and test rules

- `settings.gradle.kts` includes `api`, `common`, and `paper`.
- The root build config applies shared Java/toolchain and repository conventions.
- `common` tests exercise lifecycle, validation, cost, operation records, and history without a Minecraft server.
- `paper` tests cover adapter behavior and plugin wiring using the project’s chosen Paper test strategy; tests must not move Paper types into common code.
- A dependency verification check must fail if `api` or `common` acquires a Paper/Bukkit dependency.
- The build must support `./gradlew test` and `./gradlew build` from the repository root.

## Initial implementation scope

1. Add the Gradle multi-project scaffold and dependency graph.
2. Add minimal API contracts needed to establish boundaries.
3. Add common service ports and lifecycle scaffolding without fake tool behavior.
4. Add the Paper plugin entry point and adapter package boundaries.
5. Migrate implementation work incrementally according to the living specs; this document does not claim the tools themselves are implemented.

## Non-goals

- Implementing all BuildTools operations in the module-separation change.
- Supporting a second server platform.
- Client-side mods or rendering.
- Economy, experience, or non-block currencies.
- Cross-server blueprint sharing.
- Creative-mode free building.

## Decisions

| Decision | Choice | Reason |
|---|---|---|
| Build script language | Kotlin DSL | Typed configuration and current Gradle convention. |
| Module shape | Strict `api`, `common`, `paper` modules | Enforces platform independence and keeps Paper integration at the edge. |
| Shared implementation dependency | `common` depends on `api` only | Makes domain behavior JVM-testable and reusable. |
| Platform dependency | `paper` only | Prevents accidental Bukkit/Paper leakage into shared contracts. |
| Java baseline | 25 | Required by Paper 26.2. |
| Paper API | `io.papermc.paper:paper-api:26.2.build.112-stable` | Current Paper 26.2 stable line; `plugin.yml` `api-version` is `26.2`. |

## Open decisions for implementation planning

- Whether the initial plugin artifact should be named `buildtools-paper` or `buildtools`.
- Persistence backend for blueprint metadata.
- First claim/anti-grief integration target.
- Default values for interaction distance, selection extent, and operation limits.
