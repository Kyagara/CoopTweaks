package com.cooptweaks.keybinds.misc;

import com.cooptweaks.mixins.client.accessor.HandledScreenAccessor;
import com.cooptweaks.packets.LinkPacket;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class Link {
	public static void sendPacket(MinecraftClient client) {
		if (client.player == null) {
			return;
		}

		if (client.currentScreen == null) {
			return;
		}

		Slot slot = ((HandledScreenAccessor) client.currentScreen).getFocusedSlot();

		if (slot != null && slot.hasStack()) {
			NetworkManager.sendToServer(new LinkPacket(slot.getStack()));
		}
	}

	public static void handlePacket(LinkPacket link, NetworkManager.PacketContext context) {
		context.queue(() -> {
			PlayerEntity player = context.getPlayer();
			MinecraftServer server = player.getServer();
			if (server == null) {
				return;
			}

			ItemStack stack = link.value();

			server.getPlayerManager().broadcast(getHoverableText(stack.toHoverableText(), player.getDisplayName()), false);
		});
	}

	public static Text getHoverableText(Text stack, Text player) {
		MutableText text = Text.empty();
		text.append(player);
		text.append(Text.literal(" linked "));
		text.append(stack);
		return text;
	}
}
