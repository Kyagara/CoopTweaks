package com.cooptweaks.packets;

import com.cooptweaks.Main;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record LinkPacket(ItemStack value) implements CustomPayload {
	private static final Identifier ID = Identifier.of(Main.MOD_ID, "link_item_packet");
	public static final CustomPayload.Id<LinkPacket> PAYLOAD_ID = new Id<>(ID);
	public static final PacketCodec<RegistryByteBuf, LinkPacket> CODEC = ItemStack.PACKET_CODEC.xmap(LinkPacket::new, LinkPacket::value).cast();

	@Override
	public Id<LinkPacket> getId() {
		return PAYLOAD_ID;
	}
}
