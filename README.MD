# Duels

A fully customizable duel plugin, allowing players to duel each other with custom kits and arenas.

## Features

- **Duel System**: Challenge other players to 1v1 matches with ease.
- **Queue System**: Join queues for specific kits and maps for automatic matchmaking.
- **Practice Mode**: Start individual practice duels to hone your skills.
- **Map Management**: Configurable map system using **FastAsyncWorldEdit** for efficient map handling and creation.
- **Wager System**: Optional economy-based duels integrated with **Vault**.

## Requirements

- **Server**: [PaperMC](https://papermc.io/) (1.21 or newer)
- **Required Plugins**: 
  - [FastAsyncWorldEdit](https://intellectualsites.github.io/download/fawe.html)
- **Optional Plugins**:
  - [Vault](https://www.spigotmc.org/resources/vault.3431/) (for wager functionality)

## Installation

1. Download the latest release JAR file.
2. Place the JAR file in your server's `plugins` folder.
3. Restart your server.

## Commands

### Player Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/duel <player>` | Challenge a player to a duel | `duels.use` |
| `/duel accept <player>` | Accept a pending duel request | `duels.use` |
| `/duel deny <player>` | Deny a pending duel request | `duels.use` |
| `/duel practice` | Enter practice mode | `duels.use` |
| `/duel practiceleave` | Leave practice mode | `duels.use` |
| `/duel kiteditor <kit>` | Edit your personal layout for a kit | `duels.use` |
| `/duel kiteditor save` | Save your current kit editor layout | `duels.use` |
| `/duelqueue` | Open the queue selection menu | `duels.use` |

### Admin Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/dueladmin createkit <name> <icon>` | Create a new kit from your inventory | `duels.admin` |
| `/dueladmin deletekit <name>` | Delete an existing kit | `duels.admin` |
| `/dueladmin listkits` | List all available kits | `duels.admin` |
| `/dueladmin createmap <name> <icon>` | Create a map from your WorldEdit selection | `duels.admin` |
| `/dueladmin deletemap <name>` | Delete an existing map | `duels.admin` |
| `/dueladmin listmaps` | List all available maps | `duels.admin` |
| `/dueladmin setspawn1 <map>` | Set the first spawn point for a map | `duels.admin` |
| `/dueladmin setspawn2 <map>` | Set the second spawn point for a map | `duels.admin` |
| `/dueladmin createqueue <kit> <map> <name> <icon>` | Create a new matchmaking queue | `duels.admin` |
| `/dueladmin deletequeue <name>` | Delete a matchmaking queue | `duels.admin` |
| `/dueladmin wager` | Toggle the wager system | `duels.admin` |
| `/dueladmin pregencheck` | Check the status of pregenerated arenas | `duels.admin` |

## Permissions

- `duels.use`: Allows access to basic duel commands (Default: everyone).
- `duels.admin`: Allows access to all administrative commands (Default: operators).

## Contact

If you wish to contact me message me on discord: BoesBoes102

### Building from source
```bash
./gradlew shadowJar
```
The compiled JAR will be located in `build/libs/`.
