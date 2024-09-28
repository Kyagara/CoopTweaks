# CoopTweaks

Sync advancements, chat relay for Discord and more. Check [here](https://github.com/Kyagara/CoopTweaks/actions) for the latest version.

## About

This project is inspired by the [CooperativeAdvancements](https://modrinth.com/mod/cooperative-advancements) mod, my
goal with this is to add on the coop experience I really enjoyed from CooperativeAdvancements by giving more features in just one package.

Focusing only on server side for now.

## Features

- Bridges a Discord channel to the Minecraft server chat, allowing for chat between the two.
- Send events like advancements, join/leave, death, from the server to Discord.
- Sync advancements completion, all players share the same advancement progress.
- Discord commands to retrieve information about the server.
- Link items in the chat.

## TODO

- Enable/disable modules, allow option to disable the relay but not the syncing.
- Maybe instead of appending to the save file, maybe rewrite it.
- Maybe use a small database library for storage as it might be useful for other ideas.
- Build system needs some work, shadowing is probably not done right, add sources to the artifacts.
- Prevent toast from showing for synced players, at the moment the player that was synced will get a toast notification after completing the already completed advancement.
- Add Discord commands to retrieve general information about the server, TPS, etc.

## Configuration

Configuration is located in a folder called `cooptweaks`, it contains the following:

- `saves`: Folder containing the advancements reached by the players, files are named by the world seed.
- `discord.toml`: Configure or enable/disable the Discord bridge.

The Discord bot requires the permission to create slash commands and `MESSAGE_CONTENT` and `GUILD_MEMBERS` intents.

## Server Commands

### `/cooptweaks advancements <subcommand>`

- `progress`: Shows the advancement progress of the world.

### `/link`

Links the item being held by the player to the chat.

## Slash Commands (Discord)

- `/status`: Shows information about the server like motd, uptime, address, etc.
