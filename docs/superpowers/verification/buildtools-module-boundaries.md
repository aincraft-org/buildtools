# BuildTools module-boundary verification

Date: 2026-08-19

## Commands

- `./gradlew test` (twice, `--rerun-tasks`)
- `./gradlew build` (twice, `--rerun-tasks`)
- `./gradlew verifyModuleBoundaries`
- `git grep -n -E 'org\.bukkit|io\.papermc|net\.minecraft' -- api common`

## Results

- `./gradlew test` run 1: PASS (`TEST1_EXIT=0`)
- `./gradlew test` run 2: PASS (`TEST2_EXIT=0`)
- `./gradlew build` run 1: PASS (`BUILD1_EXIT=0`)
- `./gradlew build` run 2: PASS (`BUILD2_EXIT=0`)
- `verifyModuleBoundaries`: PASS
- Source grep on `api` and `common`: no matches
- `paper/build/libs/buildtools-0.1.0.jar` contains `plugin.yml` with `name: BuildTools` and `api-version: "26.2"`, plus `BuildToolsPlugin`, `FillTool`, and `CuboidSelection`

## Not claimed

Live Paper-server BlockDisplay rendering and third-party event cancellation. Paper jars exist elsewhere on the machine; this repo has no isolated server run configuration.
