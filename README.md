# CoopTweaks

Cooperative advancements, chat relay for a Discord channel and more.

## About

This project is inspired by the [CooperativeAdvancements](https://modrinth.com/mod/cooperative-advancements) mod, my
goal with it mod is to add on the coop experience I really enjoyed from CooperativeAdvancements by giving more
configurable features in just one package.

## Features

- Bridges a Discord channel to the Minecraft server chat, allowing for chat between the two.
- Send events like advancements, join/leave, death, from the server to Discord.
- Sync advancements(criterion), so all players can share the same advancement progress.
- Discord commands to retrieve information about the server.

## TODO

- Use the `criterion` instead of `advancement` name in some places.
- Maybe the revoke of the advancement criterion must also be tracked.
- There are 122 advancements but the `progress` command shows all criterion for them, giving a much bigger number, might
  rework how things are done in the class to fix this.
- Add Discord commands to retrieve general information about the server, TPS, uptime, etc.

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
