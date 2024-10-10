package com.cooptweaks;

import com.cooptweaks.keybinds.misc.Link;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientScreenInputEvent;
import dev.architectury.platform.Platform;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

//? if fabric
import net.fabricmc.api.EnvType;
//? if neoforge
/*import net.neoforged.api.distmarker.Dist;*/

public class Client {
	public static final KeyBinding LINK_ITEM_KEY = new KeyBinding(
			"key.cooptweaks.link_item",
			InputUtil.Type.KEYSYM,
			InputUtil.GLFW_KEY_LEFT_ALT,
			"category.cooptweaks.misc"
	);

	public static void init() {
		//? if fabric
		if (Platform.getEnv() != EnvType.CLIENT) {
		//? if neoforge
		/*if (Platform.getEnv() != Dist.CLIENT) {*/
			return;
		}

		KeyMappingRegistry.register(LINK_ITEM_KEY);

		ClientScreenInputEvent.KEY_RELEASED_POST.register((client, screen, key, scanCode, modifiers) -> {
			if (screen instanceof HandledScreen<?>) {
				if (LINK_ITEM_KEY.matchesKey(key, scanCode) && Screen.hasShiftDown()) {
					Link.sendPacket(client);
				}
			}

			return EventResult.pass();
		});
	}
}
