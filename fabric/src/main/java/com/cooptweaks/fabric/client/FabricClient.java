package com.cooptweaks.fabric.client;

import com.cooptweaks.Client;
import net.fabricmc.api.ClientModInitializer;

public final class FabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		Client.init();
	}
}
