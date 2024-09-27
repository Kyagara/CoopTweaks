package com.cooptweaks.discord;

import com.cooptweaks.types.Result;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandRequest;
import net.minecraft.server.MinecraftServer;

/** A Discord slash command. */
public interface SlashCommand {
	/** The name of the command. */
	String getName();

	/** The description of the command. */
	String getDescription();

	/** The {@link ApplicationCommandRequest} to be sent to Discord, can include description, permissions, options, etc. */
	ApplicationCommandRequest build();

	/**
	 Executes the command by building an embed for the response.

	 @param server The server that the command is being executed on.
	 @return Returns a {@link Result} containing the {@link EmbedCreateSpec} value if successful, or a {@link String} error if not.
	 */
	Result<EmbedCreateSpec> execute(MinecraftServer server);
}
