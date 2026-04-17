# Changelog

All notable changes to BubbleEggs will be documented in this file.

## [1.1.3] - 2026-04-17

### Fixed
- Lang file now force-overwrites from the jar on every startup/reload — stale or broken `lang/en.yml` files on the server are always replaced with the current version
- `en.yml` YAML parse error (error section key was merged onto one line in 1.1.2)

### Added
- `spawn-eggs.enforce-valid-spawn-locations` config option (default: `true`) — set to `false` to allow spawning aquatic mobs (dolphins, axolotls, etc.) in water and other non-air locations

---
## [1.1.2] - 2026-04-15

### Added
- Per-mob `spawner-enabled` setting in `mobs.yml` — prevents specific mob eggs from being used to change spawner types while still allowing them to be caught and spawned normally
- `VILLAGER` and `ZOMBIE_VILLAGER` default to `spawner-enabled: false` out of the box
- `spawner.mob-disabled` language message shown when a blocked mob egg is used on a spawner
- `getMobAllowsSpawnerChange()` helper in `ConfigManager` (defaults to `true` for any mob without the key set)

---

## [1.1.1] - 2026-01-14

### Fixed
- Updated bundled NBT-API (`de.tr7zw:item-nbt-api`) to 2.15.5 to support Paper 1.21.11 and remove the runtime "server version not supported" warning

---

## [1.1.0] - 2025-12-24

### Added
- Configurable crafting recipes for Catch Capsules
  - Supports `SHAPED` or `SHAPELESS`
  - Auto-registers on startup and refreshes on `/mte reload`
  - Config lives in `crafting.catch-capsule.*`
- Persistent player stats saved to `stats.yml`
  - `catches` — total successful catches
  - `rare-catches` — catches meeting the rare threshold
- New commands:
  - `/mte stats [player]` — view player catch statistics
  - `/mte top [catches|rare] [page]` — leaderboards, 10 entries per page
- Optional PlaceholderAPI integration (softdepend)
  - `%bubbleeggs_player_catches%`
  - `%bubbleeggs_rare_catches%`
  - Plugin still enables cleanly when PlaceholderAPI is not installed
- Cooldown is now per-world aware
- Optional XP level cost per catch attempt
  - Permission bypass: `bubbleeggs.bypass.xp`
- Per-world catching overrides via `catching.worlds.<worldName>.*`
  - `enabled`
  - `cooldown`
  - `xp-cost-levels`
  - `chance-multiplier`
  - `rare-threshold`
  - Legacy `catching.disabled-worlds` is still respected
- Catch chance multiplier support
  - `finalChance = mobChance * chance-multiplier` (clamped 0–1)
- Rare catch system: a catch counts as rare if `finalChance <= rare-threshold`
