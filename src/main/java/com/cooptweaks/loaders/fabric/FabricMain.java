//? if fabric {
package com.cooptweaks.loaders.fabric;

import com.cooptweaks.Main;
import net.fabricmc.api.ModInitializer;

public class FabricMain implements ModInitializer {
	@Override
	public void onInitialize() {
		Main.init();
	}
}
//?}
