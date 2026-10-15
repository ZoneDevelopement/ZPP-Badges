# ZPP-Badges

Badges addon for [ZonePractice Pro](https://github.com/ZoneDevelopement/ZonePracticePro). Players unlock badges by
reaching goals (wins, kills, ELO, divisions, leaderboard positions and more), equip up to a set number of them, and
show them next to their name in the nametag, tab list and chat. Badges are picked from a menu that opens from
`/badges` or from the ZonePractice Pro cosmetics menu.

## Requirements

- ZonePractice Pro 7.10.0-dev-248-03ac46f or newer (API level 4)
- PlaceholderAPI (optional, for placeholders)

## Installation

1. Put the ZPP-Badges jar into the `plugins/` folder next to ZonePractice Pro.
2. Start the server. The default `config.yml`, `badges.yml` and `messages.yml` are created in `plugins/ZPPBadges/`.
3. Edit the files to your liking, then run `/badges reload` (or restart the server).

The Badges button appears in the ZonePractice Pro cosmetics menu. To move it, set `zppbadges: <slot>` under `GUIS.COSMETICS.HUB.ADDON-SLOTS` in ZonePractice Pro's `guis.yml`.

## Configuration

### config.yml

| Key | Default | Description |
|---|---|---|
| `MAX-ACTIVE` | `3` | Number of badges a player can equip at once. |
| `MAX-PERMISSION-LIMIT` | `20` | Highest `zppbadges.max.<n>` number that is checked. |
| `CHECK-INTERVAL-SECONDS` | `60` | How often online players are checked for newly met requirements. Badges are also checked after matches, ELO changes, kills and events. |
| `UNLOCK-SOUND` | `entity.player.levelup` | Sound played on unlock. Leave empty to disable. |
| `BADGE-SEPARATOR` | `""` | Text placed between two equipped badges. |
| `LOCATIONS.<NAMETAG\|TAB\|CHAT\|SIDEBAR>` | see file | Where badges are shown. `ENABLED`, `POSITION` (`BEFORE_NAME` or `AFTER_NAME`) and `SEPARATOR` (text between the badges and the name). |
| `GUI.TITLE` | `<dark_gray>Badges <gray>(<page>/<pages>)` | Menu title. |
| `GUI.HUB-ICON` | Name tag | Button shown in the cosmetics menu (`MATERIAL`, `NAME`, `LORE`). |
| `GUI.LOCKED-MATERIAL` | `GRAY_DYE` | Icon of badges that are not unlocked yet. |
| `GUI.FILLER-MATERIAL` | `BLACK_STAINED_GLASS_PANE` | Filler of the menu's bottom row. |

Player-facing text (messages, menu names and lore) lives in `messages.yml`.

### badges.yml

Each badge is an entry under `BADGES`:

```yaml
BADGES:
  gladiator:
    DISPLAY: "<gradient:#ffd700:#ff4500>⚔</gradient>"
    NAME: "<gradient:#ffd700:#ff4500>Gladiator</gradient>"
    DESCRIPTION:
      - "<gray>Win 1000 matches."
      - "<gray>Reach a 30 win streak."
      - "<gray>Reach the Diamond I division."
    ICON: NETHERITE_CHESTPLATE
    REQUIREMENTS:
      - TYPE: WINS
        AMOUNT: 1000
      - TYPE: BEST_WIN_STREAK
        AMOUNT: 30
      - TYPE: DIVISION
        DIVISION: "Diamond I"
```

- `DISPLAY`: MiniMessage shown next to the name (keep it short).
- `NAME`: name shown in the menu and in messages.
- `DESCRIPTION`: menu lore lines.
- `ICON`: material of the menu item.
- `REQUIREMENTS`: all of them must be met to unlock the badge. An empty list makes a staff-only badge, which is
  granted with `/badges give`.

A badge is kept once unlocked, even if its requirements are no longer met.

#### Requirement types

| `TYPE` | Fields | Meaning |
|---|---|---|
| `WINS` | `AMOUNT`, `LADDER` (optional) | Matches won, on all ladders or on one ladder. |
| `RANKED_WINS` | `AMOUNT`, `LADDER` (optional) | Ranked matches won, on all ranked ladders or on one ladder. |
| `KILLS` | `AMOUNT`, `LADDER` (optional) | Kills, on all ladders or on one ladder. |
| `ELO` | `AMOUNT`, `LADDER` (required) | Reach this ELO on a ladder. |
| `EXPERIENCE` | `AMOUNT` | Reach this experience. |
| `BEST_WIN_STREAK` | `AMOUNT`, `LADDER` (optional) | Reach this best win streak, globally or on one ladder. |
| `BEST_KILL_STREAK` | `AMOUNT`, `LADDER` (optional) | Reach this best FFA kill streak, globally or on one ladder. |
| `MATCHES_PLAYED` | `AMOUNT` | Play this many matches. |
| `DAYS_SINCE_JOIN` | `AMOUNT` | Have been on the server for this many days. |
| `EVENT_WINS` | `AMOUNT` | Win this many events. |
| `FFA_KILLS` | `AMOUNT` | Get this many FFA kills. |
| `DIVISION` | `DIVISION` | Reach this division, e.g. `"Gold I"`. `AMOUNT` is not used. |
| `LEADERBOARD_TOP` | `AMOUNT`, `STAT`, `LADDER` (optional) | Be in the top `AMOUNT` of the leaderboard. `STAT` is one of `ELO`, `WINS`, `KILLS`, `DEATHS`, `WIN_STREAK`, `BEST_WIN_STREAK`, `LOSE_STREAK`, `BEST_LOSE_STREAK`. |

A requirement without its required field, or with an invalid `TYPE`, is skipped and logged. A badge whose
requirements are all invalid is skipped.

#### Staff-only badge

```yaml
  staff_badge:
    DISPLAY: "<light_purple>❣"
    NAME: "<light_purple>Staff Badge"
    DESCRIPTION:
      - "<gray>Awarded by the staff."
    ICON: NAME_TAG
    REQUIREMENTS: []
```

## Commands and permissions

| Command | Permission | Description |
|---|---|---|
| `/badges` (alias `/badge`) | `zppbadges.use` (default: everyone) | Open the badges menu. |
| `/badges give <player> <badge>` | `zppbadges.admin` | Unlock a badge for an online player. |
| `/badges take <player> <badge>` | `zppbadges.admin` | Remove a badge from an online player. |
| `/badges reset <player>` | `zppbadges.admin` | Remove all badges of an online player. |
| `/badges list <player>` | `zppbadges.admin` | List the unlocked badges of an online player. |
| `/badges reload` | `zppbadges.admin` | Reload the configuration files. |
| - | `zppbadges.admin` (default: op) | Required for every admin subcommand above. |
| - | `zppbadges.max.<n>` | Lets the player equip up to `n` badges (`n` up to `MAX-PERMISSION-LIMIT`). The highest number wins. |

## Placeholders

Requires PlaceholderAPI. Placeholders are available for offline players too, but values are read from the cached
badge data, so they are empty until the player's data is loaded.

| Placeholder | Value |
|---|---|
| `%zppbadges_active%` | Equipped badges, with legacy color codes. Empty if none. |
| `%zppbadges_count%` | Number of unlocked badges. |
| `%zppbadges_total%` | Number of configured badges. |
| `%zppbadges_active_count%` | Number of equipped badges. |
| `%zppbadges_max%` | Number of badges the player can equip. |
| `%zppbadges_unlocked_<id>%` | `true` or `false`, whether the badge with this id is unlocked. |

## Storage

Each player's data is saved to `plugins/ZPPBadges/playerdata/<uuid>.yml`. The file stores the unlocked badges
(`UNLOCKED`), the equipped badges (`ACTIVE`), and the event wins (`EVENT-WINS`) and FFA kills (`FFA-KILLS`) used by
the matching requirements.

## Building

```bash
./gradlew build
```

The build uses the ZonePractice Pro API from a local `api/` checkout when one exists next to the repository
(`../../api`). Use `-PlocalApi=false` to build against the published `ZonePracticePro-Api` artifact from JitPack instead.
