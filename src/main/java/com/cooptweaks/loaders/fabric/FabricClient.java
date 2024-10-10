//? if fabric {
package com.cooptweaks.loaders.fabric;

import com.cooptweaks.Client;
import net.fabricmc.api.ClientModInitializer;

public class FabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		Client.init();
	}
}
//?}
