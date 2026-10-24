# AGENTS.md — ZPP-Badges

Badges addon for ZonePractice Pro (ZPP). Players unlock badges from ZPP stats, equip them in the ZPP cosmetics menu, and the badges render next to their name (nametag, tab, chat, sidebar). Java 25, Gradle (Kotlin DSL), Paper API, no Lombok.

## Where this repo sits

This repo lives in the workspace folder `ZonePractice-Workspace/ZPP-Badges/`. Every addon is its own private repo in the `ZoneDevelopement` org (this one: `ZoneDevelopement/ZPP-Badges`, branch `master`). The core's gitignored `addons/ZPP-Badges` is a symlink here, so `deployToTestServers` still builds it.

```
ZonePractice-Workspace/
├── AGENTS.md                   workspace overview
├── ZonePractice Pro/           ZPP plugin repo (branch dev, PRs to master); core/ implements the API
├── ZPP-Api/                    ZonePracticePro-Api, separate repo (ZoneDevelopement/ZonePracticePro-Api)
├── ZPP-Backend/                private licensing backend
├── ZPP-Bots/                   bots addon
├── ZPP-Badges/                 this repo
└── test_servers/<version>/     local Paper servers
```

- **Depends on the API only.** Never import `dev.nandi0813.practice.*` (core internals). Everything goes through `dev.nandi0813.api` (`ZonePracticeApi.getInstance()`, events in `dev.nandi0813.api.Event`).
- **API source:** `../ZPP-Api/src/main/java/dev/nandi0813/api/`. Read it instead of guessing signatures. User-facing API docs: `../ZonePractice Pro/gitbook/extra/for-developers.md`.
- **Core side of the hooks this addon uses** (for debugging): `core/.../manager/api/NameDecorationRegistryImpl.java` (where decorations are applied: `NametagManager`, `InventoryUtil`, `ChatFormatUtil`, `AdapterUtil`/`PracticeAdapter` sidebar), `CosmeticsRegistryImpl.java` + `manager/gui/guis/cosmetics/CosmeticsHubGui.java` (hub button; owners pin it with `GUIS.COSMETICS.HUB.ADDON-SLOTS.zppbadges` in ZPP `guis.yml`).

## Build & dependency on the API

```bash
./gradlew build                     # jar: build/libs/ZPP-Badges-<version>.jar
./gradlew build -PlocalApi=false    # build against JitPack instead of ../ZPP-Api (what CI does)
```

- `settings.gradle.kts` substitutes `com.github.ZoneDevelopement:ZonePracticePro-Api` with the local `../ZPP-Api` checkout when it exists, so API changes can be developed together with the addon.
- The version in `build.gradle.kts` must be a released API version on JitPack, or CI fails. Needing a new API method means: change `api/` → implement in ZPP core → release the API (bump `version` in `api/build.gradle.kts`, push) → bump the API version here and in ZPP `gradle/libs.versions.toml`.
- Runtime API check: read `API_VERSION` reflectively (`ZPPBadges.runtimeApiVersion()`). `ZonePracticeApi.API_VERSION` is a compile-time constant and gets inlined, so a direct comparison always passes.

## Testing

From the ZPP repo root, `./gradlew deployToTestServers` (or the IntelliJ `26.3_Test` / `1.21.11_Test` run configs) builds ZPP and every addon and copies this addon into `test_servers/*/plugins/ZPP-Badges.jar`. No unit tests; verify = build + manual test on a test server.

## Layout

`src/main/java/dev/nandi0813/zppbadges/`
- `ZPPBadges.java` — entry point: API check, config load, registers decorator, cosmetics category, listeners, command, PAPI expansion, periodic unlock check.
- `config/` — `Settings` (config.yml), `Messages` (messages.yml, MiniMessage).
- `badge/` — `Badge`, `BadgeManager` (badges.yml), `Requirement` + `RequirementType` (unlock rules, progress lines), `BadgeService` (unlock, equip, max active, admin actions, rendering), `BadgeDecorator` (the `NameDecorator`).
- `data/` — `PlayerData`, `PlayerDataStorage` (`playerdata/<uuid>.yml`), `PlayerDataManager` (cache, async load/save, quit/rejoin race handling).
- `gui/` — `BadgeMenu` (paged menu), `BadgeMenuListener`, `BadgeCategory` (button in the ZPP cosmetics hub).
- `listener/BadgeListener.java` — join/quit, ZPP `MatchEndEvent`, `EloChangeEvent`, `FightDeathEvent` (FFA kills), `EventEndEvent` (event wins).
- `command/BadgesCommand.java`, `papi/BadgesExpansion.java`.
- `src/main/resources/` — `plugin.yml`, `config.yml`, `badges.yml`, `messages.yml`.

## Conventions

- Same rules as the ZPP repo `AGENTS.md` (agent workflow, threading, cleanup, style, Conventional Commits).
- Bukkit and ZPP API calls on the main thread. File IO async (except `onDisable`). `BadgeDecorator` and `BadgeService.renderActive` run on the async chat thread for `NameLocation.CHAT`: no Bukkit API there, only the thread-safe cache.
- After changing a player's active badges call `BadgeService.refreshName(player)` so the nametag and tab update.
- All player text in `messages.yml` (MiniMessage), no hardcoded strings or legacy `&` colors. New config/message keys go in the bundled yml files; user-visible changes go in `README.md`.
- Clear player-keyed maps on quit.

## Releasing

Bump `version` in `build.gradle.kts` and push to `master`. `.github/workflows/release.yml` builds, creates the GitHub release `<version>` with the jar and git-cliff notes (`cliff.toml`), and commits `CHANGELOG.md` (pull afterwards). It skips versions that already have a tag. `build.yml` builds every push/PR against JitPack.
