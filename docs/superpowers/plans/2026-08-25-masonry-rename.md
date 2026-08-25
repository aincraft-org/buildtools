# Masonry Project Rename Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Atomically rename the `buildtools` Paper plugin to `Masonry` (plugin id, jar, command root, permissions, Gradle group, root project name, Java package roots, three class names) with one commit and no `/bt` alias.

**Architecture:** Single-rename cutover, no shims. Java packages move from `dev.mintychochip.buildtools.*` to `dev.mintychochip.masonry.*`. Three class names move with them (`BuildToolsPlugin` → `MasonryPlugin`, `BuildToolsBrigadierCommand` → `MasonryBrigadierCommand`, `BuildToolsCommands` → `MasonryCommands`); every other class identifier stays. Permission namespace is singular `masonry.*` — no `masonry.commands.*` plural sub-prefix. `/masonry` is the only command. Player-facing strings carry "Masonry", not "BuildTools". One commit at the end.

**Tech Stack:** Paper API 26.2 (`io.papermc.paper:paper-api:26.2.build.112-stable`), Java 25, Gradle multi-project (`api`, `common`, `paper`, `runpaper`), run-paper 3.1.0.

## Global Constraints

- `settings.gradle.kts` `rootProject.name` = `masonry`.
- Root `build.gradle.kts` `group` = `dev.mintychochip.masonry`; all subprojects inherit.
- Java packages: `dev.mintychochip.masonry.api.*`, `dev.mintychochip.masonry.common.*`, `dev.mintychochip.masonry.paper.*`. Module sub-packages preserved.
- Class renames (only these three): `BuildToolsPlugin` → `MasonryPlugin`, `BuildToolsBrigadierCommand` → `MasonryBrigadierCommand`, `BuildToolsCommands` → `MasonryCommands`. Files move to the new package path. Test class `BuildToolsCommandsTest` → `MasonryCommandsTest`.
- `paper/build.gradle.kts` `tasks.jar { archiveBaseName.set("masonry") }`.
- `plugin.yml`: `name: Masonry`, `main: dev.mintychochip.masonry.paper.MasonryPlugin`, single `commands:` root `masonry:`, eight permission keys (in this order, exactly): `masonry.command`, `masonry.tool.survival_fill`, `masonry.tool.copy`, `masonry.tool.fill`, `masonry.tool.replace`, `masonry.tool.paste`, `masonry.bypass.creative`, `masonry.bypass.survival`. Defaults: `masonry.command`, `masonry.tool.survival_fill`, `masonry.tool.copy` = `true`; the other five = `op`. No `masonry.commands.*` plural entry. No `buildtools.*` entries. No `description:` text containing the literal `BuildTools` / `buildtools`.
- `config.yml` top-level schema is `limits:` with `interaction-distance`, `selection-extent`, `max-operation-blocks`. Header text reflects "Masonry". No `buildtools.*` keys (there were none; this is a guardrail).
- Player-facing message strings in any paper adapter must not contain `BuildTools` or `buildtools`. Logger messages are exempt (developer-facing only).
- Single migration commit at the end: `rename: buildtools -> masonry`.
- `plugins/BuildTools/` on-disk dev folder renamed to `plugins/Masonry/`. `runpaper/run/` cache cleaned before the smoke gate. Historical SDD/plans files keep their original names; append a `> Historical record — active project is Masonry.` header note.

---

## Task 0: Pre-flight baseline

**Files:** none.

- [ ] **Step 1: Confirm a clean working tree on the rename branch**

  Run: `git status --porcelain`
  Expected: empty. If not, stop and resolve before continuing. The rename is a single commit on top of `HEAD`.

- [ ] **Step 2: Confirm the existing test suite passes on the pre-rename baseline**

  Run: `./gradlew check`
  Expected: exits 0. If not, fix the pre-rename state first; the rename is not the time to debug a broken baseline.

- [ ] **Step 3: Record the pre-rename test file count**

  Run: `find . -name '*Test.java' | wc -l`
  Expected: a number N. Record it. Post-rename parity in the final gate will compare against N.

- [ ] **Step 4: Create a worktree**

  Run from the repo root: `git worktree add ../masonry-rename -b rename/buildtools-to-masonry HEAD`
  Expected: a new worktree directory `../masonry-rename` checked out to a fresh branch. All subsequent edits happen inside that worktree, not in the current session directory.

- [ ] **Step 5: cd into the worktree for the rest of the plan**

  From here on, every path is relative to the worktree root (e.g. `paper/src/main/resources/plugin.yml`, not the original session path).

---

## Task 1: Gradle coordinates and build metadata

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `paper/build.gradle.kts`
- Modify: `.gitignore`

- [ ] **Step 1: Update `settings.gradle.kts`**

  Set `rootProject.name = "masonry"`. The file becomes:

  ```kotlin
  rootProject.name = "masonry"

  include("api")
  include("common")
  include("paper")
  include("runpaper")
  ```

- [ ] **Step 2: Update the root `build.gradle.kts` group**

  Change the `group = "dev.mintychochip"` line to `group = "dev.mintychochip.masonry"`. Do not touch `version`, the `subprojects { ... }` block, the `verifyModuleBoundaries` task, or the `tasks.named("check") { dependsOn("verifyModuleBoundaries") }` line.

- [ ] **Step 3: Update `paper/build.gradle.kts` jar base name**

  In `tasks.jar { ... }`, change `archiveBaseName.set("buildtools")` to `archiveBaseName.set("masonry")`. Leave the rest of the file untouched (repositories, dependencies, `tasks.processResources`).

- [ ] **Step 4: Add `.superpowers/sdd/` to `.gitignore`**

  The subagent-driven workflow writes a progress ledger, task briefs, and task reports under `.superpowers/sdd/` in the worktree. None of those are part of the rename and none should be committed. Add a single line to the existing `.gitignore` (which already excludes `.worktrees/`, `paper/run/`, `runpaper/run/`, `.gradle/`, `build/`, `.idea/`, and similar dev artifacts):

  ```
  .superpowers/sdd/
  ```

  Append this line at the end of the file. Do not edit any other line. Use `edit` with the existing trailing-newline pattern so the appended line is followed by a newline.

  Verify with: `git check-ignore -v .superpowers/sdd/progress.md .superpowers/sdd/task-0-brief.md` — both must report a hit, and the rule reported in the second column must be `.gitignore:NN:.superpowers/sdd/`. If the check-ignore output reports `.gitignore:NN:.superpowers` (without the trailing slash and `sd`), tighten the entry to `.superpowers/sdd/` so it matches the directory exactly.

- [ ] **Step 5: Verify no other `buildtools` literal lives in the Gradle build**

  Run: `rg -n 'buildtools' settings.gradle.kts build.gradle.kts paper/build.gradle.kts common/build.gradle.kts api/build.gradle.kts runpaper/build.gradle.kts gradle.properties 2>/dev/null || true`
  Expected: no matches. If any match remains, fix it before moving on.
---

## Task 2: Paper plugin.yml and config.yml

**Files:**
- Modify: `paper/src/main/resources/plugin.yml`
- Modify: `paper/src/main/resources/config.yml`

- [ ] **Step 1: Rewrite `plugin.yml`**

  Replace the file contents with:

  ```yaml
  name: Masonry
  version: ${version}
  main: dev.mintychochip.masonry.paper.MasonryPlugin
  api-version: "26.2"
  authors:
    - mintychochip
  description: Survival-friendly building assistant
  commands:
    masonry:
      description: Masonry command root
      usage: /masonry <pos1|pos2|replace|fill|survival_fill|copy|paste|undo|redo|blueprint|wand|previewmode>
      permission: masonry.command
  permissions:
    masonry.command:
      description: Use the /masonry command root
      default: true
    masonry.tool.survival_fill:
      description: Use /masonry survival_fill and the gadget's safe fill
      default: true
    masonry.tool.copy:
      description: Use /masonry copy
      default: true
    masonry.tool.fill:
      description: Use /masonry fill (overwrites any block)
      default: op
    masonry.tool.replace:
      description: Use /masonry replace (replaces matched blocks)
      default: op
    masonry.tool.paste:
      description: Use /masonry paste
      default: op
    masonry.bypass.creative:
      description: Skip survival inventory cost
      default: op
    masonry.bypass.survival:
      description: Skip survival inventory cost
      default: op
  ```

  Required invariants: `name: Masonry`, `main: dev.mintychochip.masonry.paper.MasonryPlugin`, the single command root `masonry:`, the eight permission keys in the listed order with the listed defaults, no `buildtools.*` keys, no plural `masonry.commands.*` sub-prefix.

- [ ] **Step 2: Update the `config.yml` header**

  Change the first comment line from `# BuildTools server limits. Every value must be a positive integer.` to `# Masonry server limits. Every value must be a positive integer.`. The remaining contents (the `limits:` block with `interaction-distance: 6`, `selection-extent: 64`, `max-operation-blocks: 32768` and their comments) are unchanged. No `buildtools.*` key is added or renamed.

- [ ] **Step 3: Verify no `buildtools` literal in resources**

  Run: `rg -n 'buildtools|BuildTools' paper/src/main/resources/`
  Expected: no matches.

---

## Task 3: Java package rename — `api` and `common`

**Files:**
- Modify: every `*.java` under `api/src/`
- Modify: every `*.java` under `common/src/`
- Move: any directory tree whose path begins with `api/src/main/java/dev/mintychochip/buildtools/` to `api/src/main/java/dev/mintychochip/masonry/`; same for `api/src/test/java/...`, `common/src/main/java/...`, `common/src/test/java/...`.

- [ ] **Step 1: Move the `api` source tree**

  Run from the worktree root:
  ```
  mkdir -p api/src/main/java/dev/mintychochip api/src/test/java/dev/mintychochip
  git mv api/src/main/java/dev/mintychochip/buildtools/api api/src/main/java/dev/mintychochip/masonry/api
  git mv api/src/test/java/dev/mintychochip/buildtools/api api/src/test/java/dev/mintychochip/masonry/api
  rmdir api/src/main/java/dev/mintychochip/buildtools || true
  rmdir api/src/test/java/dev/mintychochip/buildtools || true
  ```
  Expected: directories renamed; the `git mv` preserves rename detection in the commit.

- [ ] **Step 2: Move the `common` source tree**

  Run:
  ```
  mkdir -p common/src/main/java/dev/mintychochip/masonry common/src/test/java/dev/mintychochip/masonry
  git mv common/src/main/java/dev/mintychochip/buildtools/common common/src/main/java/dev/mintychochip/masonry/common
  git mv common/src/test/java/dev/mintychochip/buildtools/common common/src/test/java/dev/mintychochip/masonry/common
  rmdir common/src/main/java/dev/mintychochip/buildtools || true
  rmdir common/src/test/java/dev/mintychochip/buildtools || true
  ```

- [ ] **Step 3: Rewrite `package` and `import` declarations in `api` and `common`**

  For every `*.java` under `api/src/` and `common/src/`, replace the literal `dev.mintychochip.buildtools` with `dev.mintychochip.masonry` everywhere it appears in `package` declarations and `import` lines. The fastest reliable approach is a one-pass textual rewrite:

  Run:
  ```
  find api/src common/src -name '*.java' -print0 | xargs -0 sed -i 's|dev\.mintychochip\.buildtools|dev.mintychochip.masonry|g'
  ```

  Required invariants: the new `package` line in every file matches the directory path; no `dev.mintychochip.buildtools` literal remains in any source file under `api/src` or `common/src`; class names are unchanged (only package paths and imports are rewritten).

- [ ] **Step 4: Rename the three dispatcher classes**

  - `common/src/main/java/dev/mintychochip/masonry/common/command/BuildToolsCommands.java` → `common/src/main/java/dev/mintychochip/masonry/common/command/MasonryCommands.java`
  - `common/src/test/java/dev/mintychochip/masonry/common/command/BuildToolsCommandsTest.java` → `common/src/test/java/dev/mintychochip/masonry/common/command/MasonryCommandsTest.java`

  For each, `git mv` the file then sed-replace `BuildToolsCommands` → `MasonryCommands` inside the moved file. The class declaration, all references inside the file, and the file name must all change. The `package` line is already `dev.mintychochip.masonry.common.command` after Step 3 and does not need to change.

  Run:
  ```
  git mv common/src/main/java/dev/mintychochip/masonry/common/command/BuildToolsCommands.java common/src/main/java/dev/mintychochip/masonry/common/command/MasonryCommands.java
  sed -i 's|\bBuildToolsCommands\b|MasonryCommands|g' common/src/main/java/dev/mintychochip/masonry/common/command/MasonryCommands.java
  git mv common/src/test/java/dev/mintychochip/masonry/common/command/BuildToolsCommandsTest.java common/src/test/java/dev/mintychochip/masonry/common/command/MasonryCommandsTest.java
  sed -i 's|\bBuildToolsCommands\b|MasonryCommands|g' common/src/test/java/dev/mintychochip/masonry/common/command/MasonryCommandsTest.java
  sed -i 's|\bBuildToolsCommandsTest\b|MasonryCommandsTest|g' common/src/test/java/dev/mintychochip/masonry/common/command/MasonryCommandsTest.java
- [ ] **Step 5: Sweep every Java file for `BuildToolsCommands` references**

  Step 4 renamed the class declaration inside `MasonryCommands.java` and the test class declaration inside `MasonryCommandsTest.java`. It did **not** touch any *other* file that imports, type-references, instantiates, or Javadocs `BuildToolsCommands`. Those references still resolve against the old class name and will fail to compile or to wire the dispatcher after the rename. The current callers are:

  - `common/src/test/java/dev/mintychochip/masonry/common/support/TestHarness.java` — import, field, constructor call.
  - `common/src/test/java/dev/mintychochip/masonry/common/survival/SurvivalAtomicityTest.java` — import, constructor call.
  - `common/src/test/java/dev/mintychochip/masonry/common/tool/ToolsLifecycleTest.java` — Javadoc reference to `BuildToolsCommands` in a class header.
  - `paper/src/main/java/dev/mintychochip/masonry/paper/BuildToolsPlugin.java` — import, field, constructor call (the plugin class itself, which has not yet been renamed at this point; that comes in Task 4).
  - `paper/src/main/java/dev/mintychochip/masonry/paper/GadgetListener.java` — import, field, constructor argument.
  - `paper/src/main/java/dev/mintychochip/masonry/paper/command/BuildToolsBrigadierCommand.java` — import, field.
  - `common/src/main/java/dev/mintychochip/masonry/common/command/MasonryCommands.java` — Javadoc references to `BuildToolsBrigadierCommand` and `/bt`.

  Apply a repo-wide identifier sweep with word boundaries. This must run across **all** Java files in `api/src`, `common/src`, and `paper/src`, not just the renamed files, so that every import, type, field, constructor call, and Javadoc `{@code …}` reference is updated in lockstep. It is safe to run after Step 4 because the class declaration inside `MasonryCommands.java` and `MasonryCommandsTest.java` has already moved to `MasonryCommands` / `MasonryCommandsTest`; the regex does not match those names.

  Run:
  ```
  find api/src common/src paper/src -name '*.java' -print0 | xargs -0 sed -i \
    -e 's|\bBuildToolsCommands\b|MasonryCommands|g' \
    -e 's|\bBuildToolsCommandsTest\b|MasonryCommandsTest|g'
  ```

  Required invariants: `rg -n '\bBuildToolsCommands\b' api/src common/src paper/src` returns no matches after this step. Re-read each of the seven listed files end-to-end to confirm imports, field types, constructor calls, and Javadoc references all read `MasonryCommands` / `MasonryCommandsTest`.

  Do not, at this step, attempt to rename `BuildToolsPlugin` or `BuildToolsBrigadierCommand` — those still reference the unrenamed `BuildToolsCommands` and have not yet been moved by Task 4. Their identifier rewrite is the first action of Task 4 (the new Step 3 below).

- [ ] **Step 6: Verify residue**

  Run: `rg -n 'dev\.mintychochip\.buildtools|\bBuildToolsCommands\b' api/src common/src`
  Expected: no matches.
---

## Task 4: Java package rename — `paper`, plus the two remaining class renames

**Files:**
- Modify: every `*.java` under `paper/src/`
- Move: `paper/src/main/java/dev/mintychochip/buildtools/paper/` → `paper/src/main/java/dev/mintychochip/masonry/paper/` (and the test tree).
- Rename: `BuildToolsPlugin` → `MasonryPlugin`, `BuildToolsBrigadierCommand` → `MasonryBrigadierCommand`.

- [ ] **Step 1: Move the `paper` source tree**

  Run:
  ```
  mkdir -p paper/src/main/java/dev/mintychochip/masonry paper/src/test/java/dev/mintychochip/masonry
  git mv paper/src/main/java/dev/mintychochip/buildtools/paper paper/src/main/java/dev/mintychochip/masonry/paper
  git mv paper/src/test/java/dev/mintychochip/buildtools/paper paper/src/test/java/dev/mintychochip/masonry/paper
  rmdir paper/src/main/java/dev/mintychochip/buildtools || true
  rmdir paper/src/test/java/dev/mintychochip/buildtools || true
  ```

- [ ] **Step 2: Rewrite `package` and `import` declarations in `paper`**

  Run:
  ```
  find paper/src -name '*.java' -print0 | xargs -0 sed -i 's|dev\.mintychochip\.buildtools|dev.mintychochip.masonry|g'
  ```

- [ ] **Step 3: Rename the Paper plugin class**

  `paper/src/main/java/dev/mintychochip/masonry/paper/BuildToolsPlugin.java` → `paper/src/main/java/dev/mintychochip/masonry/paper/MasonryPlugin.java`.

  Run:
  ```
  git mv paper/src/main/java/dev/mintychochip/masonry/paper/BuildToolsPlugin.java paper/src/main/java/dev/mintychochip/masonry/paper/MasonryPlugin.java
  sed -i 's|\bBuildToolsPlugin\b|MasonryPlugin|g' paper/src/main/java/dev/mintychochip/masonry/paper/MasonryPlugin.java
  ```

  Required: the public class declaration in the file reads `public final class MasonryPlugin extends JavaPlugin implements Listener`. No `BuildToolsPlugin` symbol remains anywhere under `paper/src/`.
- [ ] **Step 4: Rename the Paper Brigadier command class**

  `paper/src/main/java/dev/mintychochip/masonry/paper/command/BuildToolsBrigadierCommand.java` → `paper/src/main/java/dev/mintychochip/masonry/paper/command/MasonryBrigadierCommand.java`.

  Run:
  ```
  git mv paper/src/main/java/dev/mintychochip/masonry/paper/command/BuildToolsBrigadierCommand.java paper/src/main/java/dev/mintychochip/masonry/paper/command/MasonryBrigadierCommand.java
  sed -i 's|\bBuildToolsBrigadierCommand\b|MasonryBrigadierCommand|g' paper/src/main/java/dev/mintychochip/masonry/paper/command/MasonryBrigadierCommand.java
  ```

  Required: the class declaration in the file reads `public final class MasonryBrigadierCommand`; the file path matches. No `BuildToolsBrigadierCommand` symbol remains inside the file. (Other files in the repo may still reference this class as a type; that is the next step's job.)

- [ ] **Step 5: Sweep every Java file for `BuildToolsPlugin` and `BuildToolsBrigadierCommand` references**

  Steps 3 and 4 renamed the two class declarations inside their new files. They did **not** touch every other file that imports, type-references, instantiates, or Javadocs those classes. The current callers (after the dispatcher sweep in Task 3 Step 5) include:

  - `paper/src/main/java/dev/mintychochip/masonry/paper/GadgetListener.java` — references to `BuildToolsPlugin` and `BuildToolsBrigadierCommand` as types/imports.
  - `paper/src/main/java/dev/mintychochip/masonry/paper/command/package-info.java` — Javadoc reference to the dispatcher.
  - `paper/src/test/java/dev/mintychochip/masonry/paper/PaperBoundaryTest.java` — the test loads `plugin.yml` and `config.yml` via `BuildToolsPlugin.class.getClassLoader()`, asserts the literal strings `name: BuildTools`, `main: dev.mintychochip.buildtools.paper.BuildToolsPlugin`, and `bt`; and uses `BuildToolsPlugin.class` in the boundary check. **All of these need updating in a dedicated step (Step 8 below).**

  Apply a repo-wide identifier sweep with word boundaries for the two Paper-side class names, mirroring the Task 3 Step 5 dispatcher sweep. It is safe to run after Steps 3 and 4 because the class declarations inside `MasonryPlugin.java` and `MasonryBrigadierCommand.java` have already moved to the new names; the regex does not match those.

  Run:
  ```
  find api/src common/src paper/src -name '*.java' -print0 | xargs -0 sed -i \
    -e 's|\bBuildToolsPlugin\b|MasonryPlugin|g' \
    -e 's|\bBuildToolsBrigadierCommand\b|MasonryBrigadierCommand|g' \
    -e 's|\bBuildToolsCommands\b|MasonryCommands|g' \
    -e 's|\bBuildToolsCommandsTest\b|MasonryCommandsTest|g'
  ```

  The third and fourth `-e` clauses catch the dispatcher class (`BuildToolsCommands`) and its test (`BuildToolsCommandsTest`) wherever they are referenced in `paper/src/`. They are essential because Task 3's `BuildToolsCommands` sweep, while scoped to `api/src common/src paper/src`, did not actually rewrite the paper files: the implementer's environment hit a portability snag with the `\b` pattern (BSD sed ignores `\b`; the implementer switched to perl for the dispatcher class-rename sed but the Step 5 sweep evidently ran with a sed invocation that did not match), and 9 references in `paper/src/main/java/dev/mintychochip/buildtools/paper/` survived. After Task 4's Step 2 package rewrite, the surviving imports become `dev.mintychochip.masonry.common.command.BuildToolsCommands` — a bare `\bBuildToolsCommands\b` reference. Without the third `-e` clause here, the paper module fails to compile because the class is now `MasonryCommands`, not `BuildToolsCommands`. Re-run the full four-clause sweep now to close that gap.

  Required invariants: `rg -n '\bBuildTools(Plugin|BrigadierCommand|Commands)\b' api/src common/src paper/src` returns no matches after this step. The `MasonryCommands` sweep from Task 3 Step 5 is preserved by this step because the regex only matches the unrenamed identifiers.

- [ ] **Step 6: Rewrite runtime string literals in Paper sources**

  The package-rewrite sed (Step 2), the per-file class-rename seds (Steps 3, 4), and the repo-wide identifier sweep (Step 5) do not touch *string* literals. The Brigadier command root and the runtime permission checks use hard-coded string keys that must change in lockstep with `plugin.yml` and the class renames. Failing to rewrite any of them leaves the renamed plugin unable to enforce permissions or to register the new command root.

  Apply the following six replacements across `paper/src/main/java/`. Each pattern is anchored to disambiguate it from the `{@code /masonry}` Javadoc that will already exist after this step runs (sed does not re-visit a line it has just rewritten when patterns are non-overlapping, but the leading whitespace on the Javadoc pattern avoids any chance of double-application).

  1. `paper/src/main/java/dev/mintychochip/masonry/paper/command/MasonryBrigadierCommand.java`:
     - Javadoc `{@code /bt}` → `{@code /masonry}`.
     - `Commands.literal("bt")` → `Commands.literal("masonry")`.
     - `hasPermission("buildtools.command")` → `hasPermission("masonry.command")`.
  2. `paper/src/main/java/dev/mintychochip/masonry/paper/GadgetListener.java`:
     - Both occurrences of `hasPermission("buildtools.command")` → `hasPermission("masonry.command")`.
  3. `paper/src/main/java/dev/mintychochip/masonry/paper/HoverPreviewDriver.java`:
     - The single `hasPermission("buildtools.command")` → `hasPermission("masonry.command")`.
  4. `paper/src/main/java/dev/mintychochip/masonry/paper/adapter/PaperSurvivalTransaction.java`:
     - `hasPermission("buildtools.bypass.creative")` → `hasPermission("masonry.bypass.creative")`.
     - `hasPermission("buildtools.bypass.survival")` → `hasPermission("masonry.bypass.survival")`.

  Mechanical one-liner that performs all six replacements in one pass. To avoid the shell's brace-expansion eating `{@code`, run the command from a script file or wrap the brace-containing pattern in single quotes (the patterns below are already single-quoted inside the shell). The leading anchor `\|` in the Javadoc pattern is `\|` (escaped pipe inside `sed -E` is not used; this is BRE so `|` is the alternation operator, and `\|` is its escaped form). If the maintainer's `sed` is BSD-flavored (macOS), use `-i ''` instead of `-i`; on Linux GNU sed `-i` without a backup argument is correct.

  Write the script to a temp file first to side-step any shell-quoting hazard, then run it:

  ```
  cat > /tmp/masonry-rename-literals.sed <<'EOF'
  s|"/bt"|"/masonry"|g
  s|{@code /bt}|{@code /masonry}|g
  s|Commands\.literal("bt")|Commands.literal("masonry")|g
  s|hasPermission("buildtools\.command")|hasPermission("masonry.command")|g
  s|hasPermission("buildtools\.bypass\.creative")|hasPermission("masonry.bypass.creative")|g
  s|hasPermission("buildtools\.bypass\.survival")|hasPermission("masonry.bypass.survival")|g
  EOF
  find paper/src/main/java -name '*.java' -print0 | xargs -0 sed -i -f /tmp/masonry-rename-literals.sed
  ```

  After the sed, re-read each of the four named files end-to-end and confirm the six literal replacements landed. The package-rewrite sed has already moved the `package` line to `dev.mintychochip.masonry.paper...`; do not touch it again.

  Required invariants: no source file under `paper/src/main/java/` contains the literal `"/bt"`, `{@code /bt}`, `Commands.literal("bt")`, or `hasPermission("buildtools.*")`. The literal `Masonry` / `masonry` brand appears in user-facing strings and permission keys. `getLogger()` calls remain free to use any developer-facing wording and are not part of this gate.

- [ ] **Step 7: Sweep any player-facing literal `BuildTools` / `buildtools` in Paper sources**

  Player-facing strings in any paper adapter (GadgetListener, MasonryBrigadierCommand, HoverPreviewDriver, adapter/*, MasonryPlugin) must not contain the literal `BuildTools` or `buildtools`. `getLogger()` calls are exempt (developer-facing only).

  Run: `rg -n 'BuildTools|buildtools' paper/src/main/java/`
  Expected: no matches in player-facing string literals. For every match that remains, decide: (a) the string is a `getLogger().info(...)` log call → keep it; (b) the string is a chat `Component.text(...)` or a `description:`-style user-facing string → rewrite it to use `Masonry` / `masonry`.

  Common callsites to audit: the `description:` field is already updated via `plugin.yml` (Task 2). The remaining risk is in `GadgetListener` (the gadget `ItemStack` display name set inside `GadgetItem`) and any other `ItemMeta.displayName(Component.text("BuildTools …"))` calls. Apply targeted edits as needed.

- [ ] **Step 8: Update `PaperBoundaryTest` assertions and class references**

  The Paper boundary test loads `plugin.yml` and `config.yml` as resources, asserts three metadata strings on the plugin.yml payload, and references the renamed `BuildToolsPlugin` class in two places. After Steps 1–7 the file is still asserting the old brand and using the old class symbol; `./gradlew :paper:test` will fail to compile.

  Edit `paper/src/test/java/dev/mintychochip/masonry/paper/PaperBoundaryTest.java` so that:

  - Line ~30: `BuildToolsPlugin.class.getClassLoader()` → `MasonryPlugin.class.getClassLoader()`.
  - Line ~33: the `assertTrue(yaml.contains("main: dev.mintychochip.buildtools.paper.BuildToolsPlugin"))` becomes `assertTrue(yaml.contains("main: dev.mintychochip.masonry.paper.MasonryPlugin"))`.
- Line ~38: the `assertTrue(yaml.contains("bt"))` becomes a *compound* assertion that simultaneously proves the command root is `masonry:` and that no `bt:` root is registered. The single replacement is `assertTrue(yaml.contains("masonry:") && !yaml.contains("bt:"), yaml)`. This is one assertion, not four, so the spec's NFR-002 test/assertion-count parity is preserved (no new `@Test` method, no new imports).
- Line ~45: `BuildToolsPlugin.class.getClassLoader()` → `MasonryPlugin.class.getClassLoader()`.
- Line ~56: `assertTrue(JavaPlugin.class.isAssignableFrom(BuildToolsPlugin.class))` → `assertTrue(JavaPlugin.class.isAssignableFrom(MasonryPlugin.class))`.

  Apply those edits in a single sed pass. The four `-e` clauses perform all six required edits (the first clause covers lines ~30, ~45, and ~56 because they all reference `BuildToolsPlugin.class`; the other three clauses cover the string assertions):

  ```
  sed -i \
    -e 's|BuildToolsPlugin\.class|MasonryPlugin.class|g' \
    -e 's|"main: dev\.mintychochip\.buildtools\.paper\.BuildToolsPlugin"|"main: dev.mintychochip.masonry.paper.MasonryPlugin"|g' \
    -e 's|"name: BuildTools"|"name: Masonry"|g' \
    -e 's|yaml\.contains("bt")|yaml.contains("masonry:") \&\& !yaml.contains("bt:")|g' \
    paper/src/test/java/dev/mintychochip/masonry/paper/PaperBoundaryTest.java
  ```

  Re-read the file end-to-end. Confirm: zero `BuildTools` substrings in the existing test methods; the four `-e` clauses above landed; the second `config.yml` block (lines ~43–52) still asserts the three `limits:` keys unchanged; the line ~38 assertion now reads `assertTrue(yaml.contains("masonry:") && !yaml.contains("bt:"), yaml)`. The `&& !yaml.contains("bt:")` half is the FR-002 AC-1 / AC-3 "no `/bt` alias" check; the `yaml.contains("masonry:")` half is the positive proof of the new command root. Together they cover the spec's command-root and no-alias requirements in a single assertion.

- [ ] **Step 9: Verify residue**

  Run: `rg -n 'dev\.mintychochip\.buildtools|\bBuildTools(Plugin|BrigadierCommand|Commands)\b' api/src common/src paper/src`
  Expected: no matches.

---

- [ ] **Step 1: Run the static-residue greps from the spec's verification policy**

  Run each in order; each must return empty:
  ```
  rg -n 'dev\.mintychochip\.buildtools' api/src common/src paper/src
  rg -n 'buildtools\.' api/src common/src paper/src
  rg -n 'class BuildTools' api/src common/src paper/src
  rg -n '"/bt"' api/src common/src paper/src
  rg -n 'Commands\.literal\("bt"\)' api/src common/src paper/src
  rg -n 'hasPermission\("buildtools\.' api/src common/src paper/src
  rg -n '{@code /bt}' api/src common/src paper/src
  ```
  The last three greps are the runtime-string forms the spec's FR-002 and FR-003 require. A grep on `"/bt"` alone misses `Commands.literal("bt")`, Javadoc `{@code /bt}`, and every `hasPermission("buildtools.*")` call — all of which would survive the rename silently and break registration or permission enforcement at runtime.

  If any of these returns matches, the rename is incomplete; jump back to the failing task, fix, and re-run. Do not proceed until all seven are empty.
- [ ] **Step 2: Clean and build the plugin jar**

  Run: `./gradlew clean :paper:jar`
  Expected: exits 0. Verify the artifact exists at `paper/build/libs/masonry-0.1.0.jar` (the exact version segment depends on the `version` in `gradle.properties`; the prefix `masonry-` and the absence of any `buildtools-` artifact are the load-bearing checks).

  Run: `ls paper/build/libs/`
  Expected: at least one entry whose name starts with `masonry-`; no entry starting with `buildtools-`.

- [ ] **Step 3: Inspect the produced jar's `plugin.yml`**

  Run: `unzip -p paper/build/libs/masonry-0.1.0.jar plugin.yml | rg -n 'name:|main:|commands:|masonry|buildtools'`
  Expected: `name: Masonry`, `main: dev.mintychochip.masonry.paper.MasonryPlugin`, the single `commands:` block rooted at `masonry:`, eight `masonry.*` permission entries, and zero `buildtools` lines.

- [ ] **Step 4: Confirm no `buildtools` byte string leaks into the jar**

  Run: `unzip -p paper/build/libs/masonry-0.1.0.jar | strings | rg -c buildtools || true`
  Expected: 0. If the count is non-zero, list the matches with `unzip -p paper/build/libs/masonry-0.1.0.jar | strings | rg buildtools` and patch the offending source file (most likely a player-facing message or a literal in a permission description).

---

## Task 6: Test suite and module boundary

**Files:** none.

- [ ] **Step 1: Run the full test suite**

  Run: `./gradlew check`
  Expected: exits 0. If a test fails, the rename has broken an assertion that referenced a renamed symbol; inspect the failure, fix the test or the renamed source, and re-run. The test classes themselves were renamed by `git mv` in Task 3 and Task 4 and their assertions are unchanged.

- [ ] **Step 2: Run the module-boundary verifier**

  Run: `./gradlew verifyModuleBoundaries`
  Expected: exits 0. The forbidden-prefix list (`io.papermc.*`, `org.bukkit.*`, `org.spigotmc.*`, `net.minecraft.*`) must not appear in `:api` or `:common` resolved coordinates.

- [ ] **Step 3: Confirm test file count parity**

  Run: `find . -name '*Test.java' | wc -l`
  Expected: equals the N recorded in Task 0 Step 3. If the count differs, an extra test file was added or one was lost; investigate and resolve.

- [ ] **Step 4: Confirm `./gradlew projects` reports the new root and group**

  Run: `./gradlew projects | head -20`
  Expected: first line is `Root project 'masonry'`; the next four lines are `Project ':api'`, `Project ':common'`, `Project ':paper'`, `Project ':runpaper'`. Then `group` resolution:

  Run: `./gradlew :api:properties -q | rg ^group ; ./gradlew :common:properties -q | rg ^group ; ./gradlew :paper:properties -q | rg ^group ; ./gradlew :runpaper:properties -q | rg ^group`
  Expected: each line reads `group=dev.mintychochip.masonry`. The root group's line is reported via `./gradlew properties -q | rg ^group` (no project selector) and must also read `group=dev.mintychochip.masonry`.

---

## Task 7: Run-paper smoke gate

**Files:** none. Requires a real Paper server boot.

- [ ] **Step 1: Migrate any existing `plugins/BuildTools/` folder to `plugins/Masonry/`**

  Paper derives the on-disk data folder name from the plugin id. After the rename, Paper will create and use `plugins/Masonry/`, not `plugins/BuildTools/`. Per the spec's FR-007, this migration is in scope on dev machines. The plan covers the realistic dev paths:

  1. **Audit: enumerate any leftover `BuildTools/` directories in the repo.**

     Run:
     ```
     find . \
       \( -path ./.worktrees -o -path ./.gradle -o -path '*/versions' \) -prune -o \
       -type d -name 'BuildTools' -print
     ```
     Expected: no matches. If a path is printed, that directory still carries the old plugin id and is a candidate for migration. The realistic locations on this project are:

     - `runpaper/run/plugins/BuildTools/` — the run-paper harness's plugin folder; the most likely place a stale folder exists.
     - `runpaper/run/world/plugins/BuildTools/` — if a previous Paper boot created a world directory.
     - `paper/build/.../plugins/BuildTools/` — if a previous `:runpaper:runServer` extracted a cache here.

     For each match, decide: (a) the directory contains only `plugin.yml` and `config.yml` with no operator-edited data → `git rm -rf` it, since the new boot will write a fresh `plugins/Masonry/`; (b) the directory contains operator-edited data (custom `config.yml`, saved blueprints under `blueprints/`) → migrate it via Step 2; (c) the directory is a build artifact (e.g. `paper/build/.../plugins/`) → `rm -rf` it; the build is reproducible.

  2. **Migrate every leftover `BuildTools/` directory to a single canonical path, `runpaper/run/plugins/Masonry/`.**

     Stale folders can appear in three places: `runpaper/run/plugins/`, `runpaper/run/world/plugins/`, or under a build-output tree such as `paper/build/`. To keep Step 2's cache cleanup simple, every migration target is collapsed to **one** path that Step 2 will not delete: `runpaper/run/plugins/Masonry/`. This is the only folder Paper writes to under a fresh boot, so consolidating here means a later `:runpaper:runServer` invocation sees the operator's data on the first try.

     The realistic dev paths under `runpaper/run/` are dev cache and are `.gitignore`d. Use plain `mv` for those — do **not** use `git mv`; `git mv` on a git-ignored path errors out. Reserve `git mv` for tracked paths outside `runpaper/run/`, of which there are none on this project.

     Run:
     ```
     mkdir -p runpaper/run/plugins

     # 1. If a plugins/BuildTools/ exists, rename it in place.
     if [ -d runpaper/run/plugins/BuildTools ]; then
       mv runpaper/run/plugins/BuildTools runpaper/run/plugins/Masonry
     fi

     # 2. If a world/plugins/BuildTools/ exists, fold it into plugins/Masonry/.
     #    Merge rather than rename, so any prior plugins/Masonry/ content is preserved.
     if [ -d runpaper/run/world/plugins/BuildTools ]; then
       if [ -d runpaper/run/plugins/Masonry ]; then
             # Both exist: copy the world variant's contents into the canonical folder,
             # then delete the world variant. cp -rT treats the destination as a directory.
             cp -rT runpaper/run/world/plugins/BuildTools runpaper/run/plugins/Masonry
             rm -rf runpaper/run/world/plugins/BuildTools
           else
             mv runpaper/run/world/plugins/BuildTools runpaper/run/plugins/Masonry
           fi
     fi

     # 3. If a build-output BuildTools/ exists, delete it. Build artifacts are reproducible.
     find . -path ./.worktrees -prune -o -path '*/build/' -name 'BuildTools' -type d -print -exec rm -rf {} +
     ```

     The order above is deliberate: detect first, then act. Each block checks for the path before issuing `mv` or `rm -rf`, so a clean checkout (no leftover folders) is a documented no-op and the script does not fail.

  3. **Documented no-op when no leftover folder exists.**

     On a clean checkout (no prior `:runpaper:runServer` boot, no `paper/build/.../plugins/BuildTools/`), Step 1 produces no migration and no removal. The audit `find` in sub-step 1 returns no matches, the `if [ -d ... ]` guards in sub-step 2 all evaluate false, and the `find … -exec rm -rf` in sub-step 2's last block matches nothing. This is the expected state for a fresh rename branch and is not a problem.

  Required invariants at the end of this step:
  - `find . \( -path ./.worktrees -o -path ./.gradle \) -prune -o -type d -name 'BuildTools' -print` returns no matches.
  - If a stale folder existed anywhere, the operator's data now lives at `runpaper/run/plugins/Masonry/`. A `world/plugins/BuildTools/` was *merged* into the canonical folder, not left in `world/`.
  - `git status` shows no unexpected untracked or modified files inside `runpaper/run/` (a regenerated `eula.txt` or fresh `server.properties` after the next boot is expected and is not part of this step).
- [ ] **Step 2: Clear the run-paper cache**

  The cache pins the old plugin id and may include a stale `plugins/BuildTools/` (handled in Step 1) plus a cached Paper jar, worlds, and logs. Run:
  ```
  rm -rf runpaper/run/cache runpaper/run/world runpaper/run/logs runpaper/run/crash-reports runpaper/run/versions
  ```
  Note: `runpaper/run/plugins/` is **not** in this list. Step 1 already migrated any operator data inside it; deleting it now would discard that migration. The next `:runpaper:runServer` boot writes a fresh plugin folder under `plugins/Masonry/` automatically.

  Keep `runpaper/run/eula.txt` if it is already accepted; the first run of the next step will write a fresh one if needed.

- [ ] **Step 3: Accept the EULA if needed**

  Run: `grep -q 'eula=true' runpaper/run/eula.txt 2>/dev/null || echo 'eula=true' > runpaper/run/eula.txt`
  This is a dev-machine-only file; do not commit it.

- [ ] **Step 4: Boot the run-paper server in the background**

  Run from a separate terminal (or as a backgrounded command): `./gradlew :runpaper:runServer`. Wait for Paper to log `Done (X.XXXs)! For help, type "help"`. A typical boot completes in 20–60 seconds; do not time out before 90 seconds. After the boot, verify the plugin folder is `plugins/Masonry/`, not `plugins/BuildTools/`:

  Run: `ls runpaper/run/plugins/`
  Expected: an entry named `Masonry` (the freshly-installed plugin folder). A `BuildTools` entry here means Step 1 missed a stale folder; stop the server, redo Step 1, and re-run this step.

- [ ] **Step 5: Verify `/masonry` is registered and `/bt` is not**

  From a server console: `minecraft:command` is not what we need; we need to send a player-level command. The run-paper harness starts a server console; send the command with the console sending as the default OP player, or join as a player via the run-paper console and send:

  - `/masonry wand` — expected: plugin gives the player the Masonry gadget item, with a success chat message naming the plugin "Masonry".
  - `/bt` — expected: `Unknown or incomplete command, see below for reasons` or similar; the specific text is fine as long as the command is unrecognized.

  If `/masonry wand` errors with `Could not pass event ... to Masonry v...` or similar, the rename is functional but a player-facing message still contains `BuildTools`; go back to Task 4 Step 7 and re-audit.

- [ ] **Step 6: Stop the server**

  Send `stop` in the run-paper console. Wait for the process to exit cleanly. Do not force-kill; a forced kill can leave a stale `session.lock` that prevents the next boot.


---

## Task 8: Documentation

**Files:**
- Rename: `docs/living-specs/buildtools.md` → `docs/living-specs/masonry.md`
- Modify: `docs/living-specs/{selection,tools,survival,blueprints}.md`
- Modify (append a one-line header note, do not rename): `docs/superpowers/specs/2026-08-17-buildtools-gradle-modules-design.md`, `docs/superpowers/plans/2026-08-17-buildtools-gradle-modules.md`, `docs/superpowers/specs/2026-08-24-buildtools-gadget-design.md`, `docs/superpowers/plans/2026-08-24-buildtools-gadget.md`, `docs/superpowers/verification/buildtools-module-boundaries.md`.

- [ ] **Step 1: Rename the root living spec**

  Run: `git mv docs/living-specs/buildtools.md docs/living-specs/masonry.md`

- [ ] **Step 2: Update the per-feature living specs**

  In each of `docs/living-specs/selection.md`, `tools.md`, `survival.md`, `blueprints.md`:

  - Replace every `buildtools.tool.<name>` reference with `masonry.tool.<name>`.
  - Replace every `buildtools.bypass.creative` reference with `masonry.bypass.creative`.
  - Replace every `buildtools.bypass.survival` reference with `masonry.bypass.survival`.
  - Replace every `buildtools.limit.<size>` reference with `masonry.limit.<size>` (if any).
  - Replace every `plugins/BuildTools/` reference with `plugins/Masonry/`.
  - Do not rename or rewrite the historical SDD/plans files; those are out of scope for content edits.

  A single sed pass is safe for these files because they do not contain other `buildtools`/`BuildTools` literals that should be preserved:

  ```
  for f in docs/living-specs/selection.md docs/living-specs/tools.md docs/living-specs/survival.md docs/living-specs/blueprints.md; do
    sed -i 's|buildtools\.tool\.|masonry.tool.|g; s|buildtools\.bypass\.|masonry.bypass.|g; s|buildtools\.limit\.|masonry.limit.|g; s|plugins/BuildTools/|plugins/Masonry/|g' "$f"
  done
  ```

  After the sed pass, re-read each file end-to-end to confirm no historical prose was clobbered.

- [ ] **Step 3: Add a header note to the historical SDD/plans/verification files**

  For each of the five historical files, prepend a one-line blockquote note immediately after the first `#` heading (and before any existing content). The header note is the **only** content edit to these files; do not rename them, do not reword their prose, do not touch their git history references.

  ```
  > Historical record — active project is Masonry.
  ```

  The append is a content edit to a single line per file; the prohibition on "any other content" means no further rewrites of the file body, not a prohibition on this header.
- [ ] **Step 4: Verify documentation residue**

  Run: `rg -n 'plugins/BuildTools' docs/living-specs/`
  Expected: no matches.

  Run: `rg -n 'buildtools\.' docs/living-specs/selection.md docs/living-specs/tools.md docs/living-specs/survival.md docs/living-specs/blueprints.md`
  Expected: no matches.

  Run: `ls docs/living-specs/masonry.md docs/living-specs/buildtools.md 2>&1`
  Expected: the `masonry.md` line is a path; the `buildtools.md` line is `ls: cannot access ...: No such file or directory`.

---

## Task 9: Final gates and the atomic commit

**Files:** none (this task performs verification and the single commit).

- [ ] **Step 1: Run the spec's full verification policy**

  All of these must hold:
  - `find . -name '*Test.java' | wc -l` equals the baseline N.
  - `./gradlew check` exits 0. This includes the existing `PaperBoundaryTest.pluginMetadataPointsAtPaperEntryPoint` whose line ~38 assertion is now the compound `assertTrue(yaml.contains("masonry:") && !yaml.contains("bt:"), yaml)`, covering both the FR-002 command-root proof and the no-`/bt`-alias gate. No new `@Test` method is added (NFR-002 test/assertion-count parity).
  - `./gradlew verifyModuleBoundaries` exits 0.
  - `./gradlew projects | head -1` prints `Root project 'masonry'`.
  - `unzip -p paper/build/libs/masonry-0.1.0.jar | strings | rg -c buildtools` is 0.
  - `rg -n 'dev\.mintychochip\.buildtools' api/src common/src paper/src` is empty.
  - `rg -n 'buildtools\.' api/src common/src paper/src` is empty (with the documented exception of design history in `.superpowers/sdd/`, which is intentionally out of scope).
  - `rg -n 'class BuildTools' api/src common/src paper/src` is empty.
  - `rg -n '"/bt"' api/src common/src paper/src` is empty.
  - `rg -n 'Commands\.literal\("bt"\)' api/src common/src paper/src` is empty.
  - `rg -n 'hasPermission\("buildtools\.' api/src common/src paper/src` is empty.
  - `rg -n '{@code /bt}' api/src common/src paper/src` is empty.
  - `rg -n 'plugins/BuildTools' docs/living-specs/` is empty.
  - `ls docs/living-specs/masonry.md` succeeds; `ls docs/living-specs/buildtools.md` fails.

- [ ] **Step 2: Stage every change**

  Use a pathspec-exclusion `git add` that explicitly skips `.superpowers/sdd/` (the progress ledger, task briefs, and task reports written by the subagent workflow) along with the other dev artifacts already covered by `.gitignore` (`paper/run/`, `runpaper/run/`, `.worktrees/`). The rename is a code/docs change; those workflow artifacts must not land in the commit.

  Run: `git add -A -- ':!.superpowers' ':!.worktrees' ':!paper/run' ':!runpaper/run'`
  Run: `git status --short`
  Expected: a long list of `R` (rename) and `M` (modify) entries spanning `api/`, `common/`, `paper/`, root Gradle files, `docs/living-specs/`, the historical SDD/plans/verification files, and the newly-copied `docs/superpowers/specs/2026-08-25-masonry-rename-design.md` plus `docs/superpowers/plans/2026-08-25-masonry-rename.md`. No untracked files; no `??` lines; no entries under `.superpowers/`, `.worktrees/`, `paper/run/`, or `runpaper/run/`.

  Verify the exclusion worked: `git status --short | rg '\.superpowers|\.worktrees|paper/run|runpaper/run' || true` must return no matches. If it does return matches, unstage with `git restore --staged <path>` for each match before proceeding.
- [ ] **Step 3: Commit atomically**

  Run:
  ```
  git commit -m "rename: buildtools -> masonry

  - Plugin id, command root, permission namespace, jar name, Gradle group,
    root project name, and Java package roots all move to masonry.
  - Three class names move with the rename: BuildToolsPlugin ->
    MasonryPlugin, BuildToolsBrigadierCommand -> MasonryBrigadierCommand,
    BuildToolsCommands -> MasonryCommands.
  - No /bt alias. config.yml schema is unchanged (limits: map only).
  - Historical SDD/plans/verification files keep their names and gain a
    header note that the active project is Masonry.
  - Single atomic cutover; tests and module-boundary checks pass."
  ```

  Expected: one commit on `rename/buildtools-to-masonry`. The commit body should be a single commit; verify with `git log --oneline -1`.

- [ ] **Step 4: Verify the rename detection in the commit**

  Run: `git show --stat HEAD | head -40`
  Expected: a high rename ratio (most lines show `api/src/.../X.java -> api/src/.../Y.java` style renames). If the commit shows the changes as thousands of deletions plus thousands of additions instead of renames, the `git mv` calls in Tasks 3 and 4 were missed; rewrite the commit with `git reset --soft HEAD~1` and redo the moves via `git mv` before re-committing.

- [ ] **Step 5: Report completion**
  - all Step 1 gates pass, including the existing `PaperBoundaryTest.pluginMetadataPointsAtPaperEntryPoint` whose line ~38 assertion is the compound `assertTrue(yaml.contains("masonry:") && !yaml.contains("bt:"), yaml)`,
  - `:runpaper:runServer` boots and `ls runpaper/run/plugins/` shows a `Masonry/` directory (not `BuildTools/`), confirming Paper loaded the renamed jar under the new plugin id and wrote its data folder at the new path,
  - the jar produced is `masonry-<version>.jar` with no `buildtools` byte string,
  - the root living spec is `docs/living-specs/masonry.md` and the four feature specs reference `masonry.*` and `plugins/Masonry/`.

  The player-level `/masonry wand` interaction is **advisory only** (not in any gate): the run-paper harness does not provide a reproducible player sender, and adding RCON configuration is explicitly out of scope per the spec's "no new tooling" rule. To exercise the command interactively, the operator can join the running server with a real client and run `/masonry wand` to confirm the gadget item is granted.
  Do not merge or push the branch; the user reviews the rename before any merge.
