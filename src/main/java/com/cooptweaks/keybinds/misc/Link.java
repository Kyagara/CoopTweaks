package com.cooptweaks.keybinds.misc;

import com.cooptweaks.mixins.client.accessor.HandledScreenAccessor;
import com.cooptweaks.packets.LinkPacket;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Rarity;

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

			server.getPlayerManager().broadcast(getHoverableText(stack, player.getDisplayName()), false);
		});
	}

	/** Creates a {@link Text} to be sent to the server chat. */
	public static Text getHoverableText(ItemStack stack, Text playerName) {
		MutableText text = Text.empty();
		text.append(playerName);
		text.append(Text.literal(" linked "));

		Rarity rarity = stack.getRarity();

		Formatting color = switch (rarity) {
			case COMMON -> Formatting.DARK_GRAY;
			case RARE -> Formatting.GOLD;
			case EPIC -> Formatting.YELLOW;
			default -> Formatting.GRAY;
		};

		MutableText item = stack.getName().copy()
				.styled(style -> style.withColor(color)
						.withItalic(true)
						.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackContent(stack))));

		text.append(item);
		return text;
	}
}
