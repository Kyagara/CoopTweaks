package com.cooptweaks.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

/* A Minecraft server command. */
public interface ServerCommand {
	/** Registers the command. Some commands are not registered in the "cooptweaks" group. */
	void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment);

	/** Executes the command. */
	int execute(CommandContext<ServerCommandSource> context);
}
