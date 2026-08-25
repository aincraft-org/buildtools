# Masonry — Project Rename Design

> Status: draft
> Date: 2026-08-25
> Owners: mintychochip
> Supersedes nothing on its own. Sits alongside the existing gradle-modules and gadget designs.

## Goal

Rename the existing `buildtools` project — `Minecraft Paper plugin, group `dev.mintychochip`, root project `buildtools` — to **`Masonry`** as a single, atomic cutover. After the rename, the plugin id, jar name, command root, permission namespace, Gradle group, root project name, and Java package roots all reflect the new brand. No shims, no compatibility aliases, no parallel name.

## Scope

### In scope

- Plugin id, jar archive base name, plugin display name.
- Command root `/masonry`. No alias for `/bt`.
- Permission keys: full rename to `masonry.*`. Defaults match the current `buildtools.*` defaults per tool.
- Java package roots across `api`, `common`, `paper`, and their tests. Class names are renamed in three places (see FR-006); every other class identifier is unchanged.
- `config.yml` schema review (the existing file has only a `limits:` map; there is no `buildtools.*` key to rename). The `plugins/BuildTools/` data folder is a *plugin-id folder* that Paper derives from the plugin id, so its on-disk rename on dev machines is in scope (see FR-007).
- Doc surface that names the project: root living spec, per-feature living specs that reference `buildtools.*` permission names or `plugins/BuildTools/`, prior SDD specs and plans (history retention), and the verification document for the gradle-module split.

### Out of scope

- `/bt` Brigadier alias. The user explicitly chose a clean cutover.
- `config.yml` key renames. There are no `buildtools.*` keys to rename; the top-level is `limits:` and that is the operator-visible schema.
- Renaming class identifiers beyond the three named in FR-006.
- Migrating deployed server data. V1 has not shipped; there is no installed-base to migrate.
- Behavior changes. The rename does not alter the tool surface, survival economics, gadget UI, blueprints, or limits semantics.
- New tooling, CI, or release artifacts beyond what the existing gradle setup already produces.

## Functional requirements

### FR-001 — Plugin id and jar name

The Paper `plugin.yml` `name:` field shall be `Masonry`. The Gradle `paper` module `tasks.jar { archiveBaseName.set(...) }` shall produce a jar named `masonry-<version>.jar`. The `main:` field in `plugin.yml` shall point at the renamed entry-point class.

- **AC-1:** `./gradlew :paper:jar` produces `paper/build/libs/masonry-0.1.0.jar`.
- **AC-2:** `plugin.yml` in the produced jar contains `name: Masonry` and `main: dev.mintychochip.masonry.paper.MasonryPlugin`.
- **AC-3:** Paper loads the plugin under id `Masonry`; `Plugins` list shows `Masonry`.

### FR-002 — Command root and per-command surface

`/masonry` is the only command. Subcommand literals (pos1, pos2, replace, fill, survival_fill, copy, paste, undo, redo, blueprint, wand, previewmode) keep their names. No `/bt` alias is registered.

- **AC-1:** `/masonry pos1` succeeds; `/bt pos1` fails with "Unknown command" on Paper.
- **AC-2:** Tab completion offers the same subcommands as today.
- **AC-3:** The plugin.yml `commands:` block has a single root `masonry:` with the same subcommand usage description, and the eight permission entries from FR-003 (no `/bt` entry).

### FR-003 — Permission namespace

Every permission key in `plugin.yml` and every permission check in code moves from `buildtools.*` to `masonry.*`. Defaults match today:

- `masonry.command` — default `true`.
- `masonry.tool.survival_fill` — default `true`.
- `masonry.tool.copy` — default `true`.
- `masonry.tool.fill` — default `op`.
- `masonry.tool.replace` — default `op`.
- `masonry.tool.paste` — default `op`.
- `masonry.bypass.creative` — default `op`.
- `masonry.bypass.survival` — default `op`.

- **AC-1:** `plugin.yml` lists **exactly** these eight keys, in this order, with no `buildtools.*` entries and no plural/`masonry.commands.*` entries. The namespace rule is **singular `masonry.*`** — there is no `masonry.commands.*` or any other sub-prefix; `masonry.command` covers all subcommands under `/masonry`.
- **AC-2:** `rg 'buildtools\.' paper/src common/src api/src` returns no permission-check references. Test references in `.superpowers/sdd/task-1-brief.md` are design history, not code, and are out of scope of this grep.
- **AC-3:** A player with `masonry.command=true` (default) and no other grants can run `/masonry wand` and use the gadget for `survival_fill` and `copy`; `fill`/`replace`/`paste` deny.
### FR-004 — Gradle coordinates

`settings.gradle.kts` `rootProject.name` becomes `masonry`. The root `build.gradle.kts` `group` becomes `dev.mintychochip.masonry`. All subprojects inherit the new `group` and `version`.

- **AC-1:** `./gradlew projects` output begins with `Root project 'masonry'`, followed by the four subprojects `Project ':api'`, `Project ':common'`, `Project ':paper'`, `Project ':runpaper'`. Running `./gradlew :paper:properties -q | grep ^group` (and the same for each subproject) reports `group=dev.mintychochip.masonry`.
- **AC-2:** `./gradlew :paper:dependencies --configuration runtimeClasspath` shows `project :api` and `project :common` as project dependencies whose group resolves to `dev.mintychochip.masonry` (verify with `--all` or by inspecting the resolved coordinates; the prior `dev.mintychochip` group must not appear).
- **AC-3:** `./gradlew verifyModuleBoundaries` exits 0; the rule that `:api` and `:common` resolve no `io.papermc.*` / `org.bukkit.*` / `org.spigotmc.*` / `net.minecraft.*` artifacts is preserved by the rename.

### FR-005 — Java package roots

Every Java source file's `package` declaration moves from `dev.mintychochip.buildtools.*` to `dev.mintychochip.masonry.*`. The module sub-packages (`api`, `common`, `paper`) are preserved. Every `import` line in source and tests is updated to match.

- **AC-1:** `rg 'package dev\.mintychochip\.buildtools' api/src common/src paper/src` returns no matches.
- **AC-2:** `rg 'dev\.mintychochip\.buildtools' api/src common/src paper/src` returns no matches in Java sources.
- **AC-3:** `./gradlew compileJava compileTestJava` succeeds for all four modules with no compile errors.
- **AC-4:** `./gradlew check` succeeds: every existing test passes against the new package root.

### FR-006 — Entry-point and command class rename

`BuildToolsPlugin` → `MasonryPlugin`. `BuildToolsBrigadierCommand` → `MasonryBrigadierCommand`. `BuildToolsCommands` (common) → `MasonryCommands`. The plugin's `JavaPlugin#getLogger()` and message strings may continue to say "BuildTools" only in the historical *intent* sense; for player-facing messages and the `description:` field in `plugin.yml`, the brand is "Masonry".

- **AC-1:** `rg 'class BuildToolsPlugin' paper/src` returns no matches; `class MasonryPlugin` exists at the renamed path.
- **AC-2:** `plugin.yml` `main:` is `dev.mintychochip.masonry.paper.MasonryPlugin`.
- **AC-3:** Common dispatcher test `BuildToolsCommandsTest` is renamed to `MasonryCommandsTest` and the file path matches.
- **AC-4:** Player-facing message sent from any paper adapter (e.g. `/masonry wand` reply, error messages) does not contain the string "BuildTools" or "buildtools".

### FR-007 — Config and data folder migration

The on-disk folder `plugins/BuildTools/` is renamed to `plugins/Masonry/`. The migration is performed once as part of the rename, on dev machines, before running tests that load the plugin in the run-paper harness. The `runpaper/run/` cache is also cleaned because it pins the previous plugin id in its `plugins/` subdirectory.

- **AC-1:** `paper/src/main/resources/config.yml` header reflects the new brand ("Masonry server limits"). The schema (top-level `limits:` map and its three keys) is unchanged.
- **AC-2:** No `buildtools.*` key appears in `config.yml` (there were none; this guards against accidental introduction).
- **AC-3:** `find . -path ./.worktrees -prune -o -type d -name BuildTools -print` returns no matches after the rename.
- **AC-4:** `runpaper/run/plugins/` is regenerated by the run-paper task and contains `Masonry.jar`, not `BuildTools.jar`.

### FR-008 — Documentation rename

The root living spec `docs/living-specs/buildtools.md` is renamed to `docs/living-specs/masonry.md`. The per-feature living specs (`selection.md`, `tools.md`, `survival.md`, `blueprints.md`) are updated in place to reference the new permission namespace (`masonry.tool.<name>`, `masonry.bypass.creative`, `masonry.bypass.survival`) and the new config path (`plugins/Masonry/config.yml`). Prior design and plan documents (`docs/superpowers/specs/2026-08-17-buildtools-gradle-modules-design.md`, `docs/superpowers/plans/2026-08-17-buildtools-gradle-modules.md`, `docs/superpowers/specs/2026-08-24-buildtools-gadget-design.md`, `docs/superpowers/plans/2026-08-24-buildtools-gadget.md`, `docs/superpowers/verification/buildtools-module-boundaries.md`) are kept as historical record and not renamed. The verification document header gains a one-line note that the active plugin is Masonry.

- **AC-1:** `docs/living-specs/masonry.md` exists; `docs/living-specs/buildtools.md` does not.
- **AC-2:** `rg 'buildtools\.' docs/living-specs/{selection,tools,survival,blueprints}.md` returns no matches.
- **AC-3:** `rg 'plugins/BuildTools' docs/living-specs/` returns no matches.
- **AC-4:** Historical files retain their original names and a `> Historical record — active project is Masonry.` note at the top.

## Non-functional requirements

### NFR-001 — Single atomic cutover

The rename is a single coherent change. No intermediate state where `/bt` works but `/masonry` doesn't, or where `buildtools.tool.copy` resolves but `masonry.tool.copy` doesn't.

- **AC-1:** All four Gradle modules and the run-paper harness are updated in the same change.
- **AC-2:** The migration commit is one commit on the working branch.
- **AC-3:** No `buildtools.*` symbol appears in the compiled jar's bytecode (verified with `unzip -p paper/build/libs/masonry-0.1.0.jar | strings | grep -c buildtools` returning 0, modulo the literal string "buildtools" in any user-facing message that intentionally references the old name; there are none per FR-006).

### NFR-002 — Test parity

Every existing test passes against the renamed surface. No test is deleted; the rename is mechanical.

- **AC-1:** `./gradlew check` exits 0.
- **AC-2:** Test class count and assertion count are unchanged from before the rename (verified by `find . -name '*Test.java' | wc -l` before and after).
- **AC-3:** `PaperBoundaryTest` and the module-boundary verification task both pass; the boundary rule (no Paper/Bukkit/NMS in `api`/`common`) is preserved by the rename.

### NFR-003 — Build time and artifact size

The rename should not materially change build time or jar size. The 200 KB-of-source rename is a one-pass operation; nothing is added or removed.

- **AC-1:** `./gradlew clean :paper:jar` completes within the same wall-clock order of magnitude as before (no specific ms target; observable sanity check).
- **AC-2:** Jar size delta < 1% of pre-rename size.

## Failure handling

- **Missed callsite in source.** `rg` gates in AC-002/AC-002/AC-002 of FR-003/FR-005/FR-006 catch the residue before `./gradlew compileJava` is run. If a call survives the grep, `compileJava` fails with a "cannot find symbol" pointing at the missing reference.
- **Missed callsite in docs.** `rg` gates in FR-008 catch the residue; the doc update is part of the same commit, so a docs drift is caught by the same grep step.
- **Stale on-disk folder.** FR-007 explicit `find` gate catches any lingering `BuildTools/` directory under the repo (excluding `.worktrees/` and `.gradle/`).
- **Run-paper cache pinning old id.** The run-paper cache under `runpaper/run/` is deleted as part of the rename task before re-running `./gradlew :runpaper:runServer`. If not, the cache will pin the old plugin id and the server will fail to load the renamed jar with a "plugin not found" error referencing the old id.
- **Test name drift.** `BuildToolsCommandsTest` is renamed to `MasonryCommandsTest` and the file moves with it; the build fails if the file name and the public class name disagree.

## Verification policy

The rename is verified by the following commands run in order on a clean checkout:

1. **Pre-rename baseline:** record `find . -name '*Test.java' | wc -l` and `./gradlew check` exits 0.
2. **Static residue checks (all must return empty):**
   - `rg -n 'dev\.mintychochip\.buildtools' api/src common/src paper/src`
   - `rg -n 'buildtools\.' api/src common/src paper/src` (permission and config checks)
   - `rg -n 'class BuildTools' api/src common/src paper/src`
   - `rg -n '"/bt"' api/src common/src paper/src`
   - `rg -n 'plugins/BuildTools' docs/living-specs/`
3. **Build:** `./gradlew clean :paper:jar` exits 0; jar is `paper/build/libs/masonry-0.1.0.jar`.
4. **Module check:** `./gradlew projects` lists root as `:masonry` with `group = 'dev.mintychochip.masonry'`.
5. **Test suite:** `./gradlew check` exits 0.
6. **Boundary check:** `./gradlew verifyModuleBoundaries` exits 0.
7. **Smoke:** `./gradlew :runpaper:runServer` (background) reaches Paper's "Done" log line. `/masonry wand` succeeds in the running server; `/bt` reports "Unknown command". Stop the server.
8. **Post-rename parity:** `find . -name '*Test.java' | wc -l` matches the baseline count.
9. **Doc gates:**
   - `rg -n 'plugins/BuildTools' docs/living-specs/` is empty.
   - `rg -n 'buildtools\.' docs/living-specs/{selection,tools,survival,blueprints}.md` is empty.
   - `ls docs/living-specs/masonry.md` succeeds; `ls docs/living-specs/buildtools.md` fails.

## Open decisions for the plan phase

- **Class-name rename for the dispatcher pair.** Spec assumes `BuildToolsCommands` → `MasonryCommands` and `BuildToolsBrigadierCommand` → `MasonryBrigadierCommand`. Confirm in plan review; alternative is a no-rename to minimize diff.
- **Worktree workflow.** Repo has `.worktrees/`. The plan should decide whether to use a git worktree for the rename or work on the current branch. Default: a fresh worktree, since the rename is large and the repo already uses that pattern.
- **Commit message shape.** One atomic commit `rename: buildtools -> masonry` or one commit per FR? Default: one commit because the rename is non-divisible.
- **Migration of historical SDD/plans files.** Spec keeps them in place. Plan should add a short header note rather than renaming, to preserve git history references.
