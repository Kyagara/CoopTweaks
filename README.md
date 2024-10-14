# CoopTweaks

Sync advancements, chat relay for Discord and more. Check [here](https://github.com/Kyagara/CoopTweaks/actions) for the latest builds.

## About

This project is inspired by the [CooperativeAdvancements](https://modrinth.com/mod/cooperative-advancements) mod, my
goal with this is to add on the coop experience I really enjoyed from CooperativeAdvancements by giving more features in just one package.

Some features in this mod come from my other mod, [Fred](https://github.com/Kyagara/fred).

## Features

- Bridges a Discord channel to the Minecraft server chat, allowing for chat between the two.
- Send events like advancements, join/leave, death, from the server to Discord.
- Sync advancements completion, all players share the same advancement progress.
- Discord commands to retrieve information about the server.
- Link items in the chat.

## TODO

- More options to enable/disable certain parts of the mod.
- Add Discord commands to retrieve general information about the server, TPS, etc.
- Maybe use a small database library for storage as it might be useful for other ideas.

## Configuration

Configuration is located in a folder called `cooptweaks`, contains the following:

- `saves/`: Folder containing the advancements reached by the players, files are named by the world seed.
- `advancements.toml`: Configuration for the advancement module.
- `discord.toml`: Configuration for Discord related features.

The Discord bot requires the permission to create slash commands and `MESSAGE_CONTENT` and `GUILD_MEMBERS` intents.

## Client

#### Keybinds

- `Left Shift + Left Alt`: Links the item being hovered by the player.

#### Commands

- `shrug`: Shrugs.
- `flip`: Flips the table.
- `unflip`: Unflips said table.
- `coords`: Sends the player's coordinates in a dimension in chat.

## Server

#### Commands

All commands are prefixed with `/cooptweaks`.

#### `advancements <subcommand>`

- `progress`: Shows the advancement progress of the world.

#### `link`

Links the item being held by the player in the chat.

## Discord Commands

- `/status`: Shows information about the server like motd, uptime, address, etc.
