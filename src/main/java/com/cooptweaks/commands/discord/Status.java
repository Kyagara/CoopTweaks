package com.cooptweaks.commands.discord;

import com.cooptweaks.Advancements;
import com.cooptweaks.Main;
import com.cooptweaks.commands.SlashCommand;
import com.cooptweaks.types.Result;
import com.cooptweaks.utils.TimeSince;
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
		String advancements = Advancements.getAdvancementsProgress();
		String uptime = new TimeSince(Main.STARTUP).toString();

		return Result.success(EmbedCreateSpec.builder()
				.color(Color.DEEP_LILAC)
				.title("Server Status")
				.description(motd)
				.addField("Address", address, true)
				.addField("Version", version, true)
				.addField("", "", false)
				.addField("Players", players, true)
				.addField("Advancements", advancements, true)
				.addField("Uptime", uptime, false)
				.build());
	}
}
