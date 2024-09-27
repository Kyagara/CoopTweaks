package com.cooptweaks.commands.misc;

import com.cooptweaks.commands.ServerCommand;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class Link implements ServerCommand {
	@Override
	public void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
		dispatcher
				.register(CommandManager.literal("link")
						.executes(this::execute));
	}

	@Override
	public int execute(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayer();
		if (player == null) {
			return 1;
		}

		ItemStack item = player.getMainHandStack();

		if (item.isEmpty()) {
			context.getSource().sendFeedback(() -> Text.of("You are not holding anything."), false);
			return 1;
		}

		MutableText text = Text.empty();
		text.append(player.getDisplayName());
		text.append(Text.literal(" linked "));
		text.append(item.toHoverableText());

		source.getServer().getPlayerManager().broadcast(text, false);
		return 0;
	}
}
