package com.cooptweaks.commands.misc;

import com.cooptweaks.commands.ServerCommand;
import com.cooptweaks.keybinds.misc.Link;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class LinkCommand implements ServerCommand {
	@Override
	public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
		dispatcher
				.register(CommandManager.literal("cooptweaks")
						.then(CommandManager.literal("link")
								.executes(this::execute)));
	}

	@Override
	public int execute(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();
		if (player == null) {
			return 0;
		}

		ItemStack item = player.getMainHandStack();

		if (item.isEmpty()) {
			context.getSource().sendFeedback(() -> Text.of("You are not holding anything."), false);
			return 0;
		}

		Text text = Link.getHoverableText(item, player.getDisplayName());

		source.getServer().getPlayerManager().broadcast(text, false);
		return Command.SINGLE_SUCCESS;
	}
}
