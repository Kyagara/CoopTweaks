package com.cooptweaks.commands.misc;

import com.cooptweaks.Dimension;
import com.cooptweaks.commands.ClientCommand;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandRegistryAccess;

public class Coordinates implements ClientCommand {
	@Override
	public void register(CommandDispatcher<ClientCommandRegistrationEvent.ClientCommandSourceStack> dispatcher, CommandRegistryAccess context) {
		dispatcher.register(ClientCommandRegistrationEvent.literal("coords").executes(this::execute));
	}

	@Override
	public int execute(CommandContext<ClientCommandRegistrationEvent.ClientCommandSourceStack> context) {
		ClientPlayerEntity player = context.getSource().arch$getPlayer();

		String dimension = Dimension.getPlayerDimension(player.getName().getString());
		String coordinates = String.format("X: %d Y: %d Z: %d @ %s", (int) player.getX(), (int) player.getY(), (int) player.getZ(), dimension);

		player.networkHandler.sendChatMessage(coordinates);
		return Command.SINGLE_SUCCESS;
	}
}
