package com.cooptweaks;

import com.cooptweaks.commands.misc.Coordinates;
import com.cooptweaks.commands.misc.FlipTable;
import com.cooptweaks.commands.misc.Shrug;
import com.cooptweaks.commands.misc.UnflipTable;
import com.cooptweaks.keybinds.misc.Link;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import dev.architectury.event.events.client.ClientScreenInputEvent;
import dev.architectury.platform.Platform;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.utils.Env;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class Client {
	public static final KeyBinding LINK_ITEM_KEY = new KeyBinding(
			"key.cooptweaks.link_item",
			InputUtil.Type.KEYSYM,
			InputUtil.GLFW_KEY_LEFT_ALT,
			"category.cooptweaks.misc"
	);

	public static void init() {
		if (Platform.getEnvironment() == Env.SERVER) {
			return;
		}

		KeyMappingRegistry.register(LINK_ITEM_KEY);

		ClientScreenInputEvent.KEY_RELEASED_POST.register((client, screen, keyCode, scanCode, modifiers) -> {
			if (screen instanceof HandledScreen<?> && LINK_ITEM_KEY.matchesKey(keyCode, scanCode) && Screen.hasShiftDown()) {
				Link.sendPacket(client);
			}


			return EventResult.pass();
		});

		ClientCommandRegistrationEvent.EVENT.register((dispatcher, context) -> {
			new Shrug().register(dispatcher, context);

			new FlipTable().register(dispatcher, context);

			new UnflipTable().register(dispatcher, context);

			new Coordinates().register(dispatcher, context);
		});
	}
}
