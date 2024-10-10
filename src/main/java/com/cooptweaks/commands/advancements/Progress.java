package com.cooptweaks.commands.advancements;

import com.cooptweaks.Advancements;
import com.cooptweaks.commands.ServerCommand;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class Progress implements ServerCommand {
	@Override
	public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
		dispatcher
				.register(CommandManager.literal("cooptweaks")
						.then(CommandManager.literal("advancements")
								.then(CommandManager.literal("progress")
										.executes(this::execute))));
	}

	@Override
	public int execute(CommandContext<ServerCommandSource> context) {
		context.getSource().sendFeedback(() -> Text.literal(String.format("%s advancements completed.", Advancements.getAdvancementsProgress())), false);
		return 0;
	}
}
