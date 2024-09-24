package com.cooptweaks.discord.commands;

import com.cooptweaks.Utils;
import com.cooptweaks.discord.SlashCommand;
import com.cooptweaks.types.Result;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;
import net.minecraft.server.MinecraftServer;

public class Status implements SlashCommand {
	@Override
	public String getName() {
		return "status";
	}

	@Override
	public String getDescription() {
		return "Shows information about the server.";
	}

	@Override
	public ApplicationCommandRequest build() {
		return ApplicationCommandRequest.builder()
				.name(getName())
				.description(getDescription())
				.build();
	}

	@Override
	public Result<EmbedCreateSpec> execute(MinecraftServer server) {
		if (server == null) {
			return Result.error("Server is not running.");
		}

		String motd = server.getServerMotd();
		String address = String.format("%s:%d", server.getServerIp(), server.getServerPort());
		String version = server.getVersion();
		String players = String.format("%d/%d", server.getCurrentPlayerCount(), server.getMaxPlayerCount());
		String advancements = Utils.getAdvancementsProgress();

		String message = String.format("`MOTD`: %s%n`Address`: %s\n`Version`: %s\n`Players`: %s\n`Advancements`: %s",
				motd, address, version, players, advancements);

		return Result.success(EmbedCreateSpec.builder()
				.color(Color.BLUE)
				.title("Server Status")
				.description(message)
				.build());
	}
}
