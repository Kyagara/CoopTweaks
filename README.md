# CoopTweaks

Sync advancements, chat relay for Discord and more.

## About

This project is inspired by the [CooperativeAdvancements](https://modrinth.com/mod/cooperative-advancements) mod, my
goal with this is to add on the coop experience I really enjoyed from CooperativeAdvancements by giving more features in just one package.

## Features

- Bridges a Discord channel to the Minecraft server chat, allowing for chat between the two.
- Send events like advancements, join/leave, death, from the server to Discord.
- Sync advancements completion, all players share the same advancement progress.
- Discord commands to retrieve information about the server.

## TODO

- Build system needs some work, merging jars is really slow, shadowing is probably not done right.
- Add Discord commands to retrieve general information about the server, TPS, uptime, etc.
- Maybe move dimension logic to a separate class, maybe in the Discord package.
- Utils class might need to become a package as more features are added.

## Configuration

At startup, a config folder called `cooptweaks` will be created, it will include the following:

- `saves`: Folder containing the advancements reached by the players, files are named by the world seed.
- `discord.toml`: Configure or enable/disable the Discord bridge.

The Discord bot requires `MESSAGE_CONTENT` and `GUILD_MEMBERS` intents.

## Commands

### `/cooptweaks advancements <subcommand>`

- `progress`: Shows the advancement progress of the world.

## Slash Commands

- `/status`: Shows information about the server like motd, address, etc.
