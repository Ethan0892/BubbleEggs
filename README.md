# BubbleEggs - Minecraft Plugin

BubbleEggs is a comprehensive Minecraft plugin for Minecraft 1.21.9 that allows players to catch mobs and turn them into spawn eggs using configurable projectiles.

## Features

- ✅ **Catch mobs and give players spawn eggs**
- ✅ **Turn mob catching on and off per mob**
- ✅ **Change the projectile that catches the mob** (snowball, egg, ender pearl, etc)
- ✅ **Define the catch chance for each mob**
- ✅ **Set an item or money cost for catching mobs**
- ✅ **Works right out of the box**
- ✅ **Customizable messages with HEX color support**
- ✅ **All Minecraft mobs are supported!**
- ✅ **Supports Enderman catching**
- ✅ **Supports catching mobs with no vanilla spawn egg** (boss mobs, golems, giants)
- ✅ **Allow or deny spawner changing**
- ✅ **Particles and sounds** when mobs are caught or failed to be caught
- ✅ **Give the mob catching capsule a custom name and/or lore**
- ✅ **Save mob data to spawn eggs** with a toggle to turn on/off
- ✅ **Options to punch or throw** the catch capsule in order to catch the mob
- ✅ **CustomModelData support** for both catch capsule and mob spawn eggs
- ✅ **Support for WorldGuard regions** and per world catching
- ✅ **Support for HEX color codes** (Use &#HEXCODE i.e. &#FFFFFF for white)

## Usage

Throw the catch capsule projectile defined in the config at the mob you want to catch, and it will have a chance to be encapsulated into the mob egg.

You can also punch mobs with the catch capsule if enabled in the configuration.

## Requirements

- **Minecraft 1.21.9 Spigot/Paper server**
- **Java 21 or higher**

### Optional Dependencies

- **Vault** (for economy features)
- **NBTAPI** (for saving entity data)
- **Any economy plugin that works with Vault** (for economy features)
- **WorldGuard** (for region-based permissions)

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/mte` | Show plugin information | None |
| `/mte help` | Display the plugin help menu | None |
| `/mte reload` | Reload plugin config file | `bubbleeggs.*` |
| `/mte give <amount> [player]` | Give catch capsules to a player | `bubbleeggs.give` |
| `/mte bulkupdate <chance\|money\|item> <value>` | Update all mob settings | `bubbleeggs.*` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `bubbleeggs.*` | Access to all BubbleEggs permissions | op |
| `bubbleeggs.use` | Allows player to catch mobs | true |
| `bubbleeggs.give` | Allows player to give catch capsules | op |
| `bubbleeggs.reload` | Allows player to reload the plugin | op |
| `bubbleeggs.bypass.cost` | Bypass catching costs | op |
| `bubbleeggs.bypass.cooldown` | Bypass catching cooldowns | op |
| `bubbleeggs.bypass.world` | Catch mobs in any world | op |
| `bubbleeggs.bypass.region` | Catch mobs in any region | op |
| `bubbleeggs.spawner.change` | Change spawner types with spawn eggs | op |
| `bubbleeggs.spawn` | Spawn mobs using BubbleEggs spawn eggs | true |

## Configuration

The plugin comes with extensive configuration options:

### Main Configuration (`config.yml`)
- **Catch capsule settings** (material, name, lore, custom model data)
- **Catching mechanics** (default chances, cooldowns, distance limits)
- **Economy integration** (costs, messages)
- **Item costs** (required items for catching)
- **Spawn egg customization** (names, lore, data saving)
- **Effects** (particles and sounds)
- **WorldGuard integration**
- **Advanced settings**

### Mob Configuration (`mobs.yml`)
Individual settings for each mob type:
- Enable/disable catching per mob
- Custom catch chances
- Money costs
- Item costs
- Health percentage requirements

### Language Configuration (`lang/en.yml`)
- Fully customizable messages
- HEX color code support
- Placeholder support

## Installation

1. Download the latest release
2. Place the `.jar` file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin by editing the generated config files
5. Reload with `/mte reload` or restart the server

## Building from Source

1. Clone the repository
2. Make sure you have Maven installed
3. Run `mvn clean package`
4. The compiled `.jar` will be in the `target` folder

## Configuration Examples

### Catch Capsule Setup
```yaml
catch-capsule:
  material: "SNOWBALL"
  name: "&#FF6B6B&lCatch Capsule"
  lore:
    - "&#FFFF00Throw at a mob to catch it!"
    - "&#FFB6C1Right click to throw"
  usage-mode: "BOTH"  # THROW, PUNCH, or BOTH
```

### Mob-Specific Settings
```yaml
ENDERMAN:
  enabled: true
  catch-chance: 0.2
  money-cost: 300.0
  item-cost: "ENDER_PEARL:3"
  max-health-percentage: 0.3
```

### Economy Integration
```yaml
economy:
  enabled: true
  default-cost: 50.0
```

### WorldGuard Integration
```yaml
worldguard:
  enabled: true
  flag-name: "mob-catching"
  default-flag-value: true
```

## Support

For support, bug reports, or feature requests, please visit our GitHub repository or join our Discord server.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.