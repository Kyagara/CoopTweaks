# CoopTweaks

Cooperative advancements, chat relay for a Discord channel and more.

## About

This project is inspired by the [CooperativeAdvancements](https://modrinth.com/mod/cooperative-advancements) mod, my
goal with it mod is to add on the coop experience I really enjoyed from CooperativeAdvancements by giving more
configurable features in just one package.

Very much a work in progress and not thoroughly tested.

## Features

- Relays chat messages, advancements and some events from the server to a Discord channel.
- Relays messages from the Discord channel to the server.

## TODO

- Add commands to retrieve general information about the server, TPS, uptime, etc. Maybe some helpers like getting a
  wiki link for an advancement.
- Transform the dimension name to a readable name.
- Reduce/remove blocks in the Discord bridge.
- Retrieve the GuildMember from the Discord message, this way we can get the display name and role color for better
  readability in the server.

## Configuration

At startup, a config folder called `cooptweaks` will be created, it will include the following:

- `discord.toml`: Contains the bot token and channel ID to send events and relay messages between the server and the
  Discord channel.
- `saves`: Folder where files with the advancements reached by the players. The files are named with the world seed.

## Commands

### `/cooptweaks <subcommand>`

- `progress`: Shows the advancement progress of the world.
