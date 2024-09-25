package com.cooptweaks.fabric;

import com.cooptweaks.Main;
import net.fabricmc.api.ModInitializer;

public final class FabricMain implements ModInitializer {
	@Override
	public void onInitialize() {
		Main.init();
	}
}
