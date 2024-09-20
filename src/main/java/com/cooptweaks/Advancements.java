package com.cooptweaks;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import discord4j.rest.util.Color;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public final class Advancements {
	private static MinecraftServer SERVER;

	public static Map<Identifier, AdvancementEntry> ALL_ADVANCEMENTS = new HashMap<>();
	public static Map<String, AdvancementEntry> COMPLETED_ADVANCEMENTS = new HashMap<>();

	private static FileChannel CURRENT_SEED_FILE;

	private static Advancements INSTANCE = null;

	private Advancements() {
	}

	public static Advancements getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new Advancements();
		}

		return INSTANCE;
	}

	private static synchronized void appendToSave(String text) throws IOException {
		ByteBuffer buffer = ByteBuffer.wrap(text.getBytes());
		CURRENT_SEED_FILE.write(buffer);
	}

	public void LoadAdvancements(MinecraftServer server) {
		SERVER = server;

		Collection<AdvancementEntry> advancements = server.getAdvancementLoader().getAdvancements();
		int totalAdvancements = advancements.size();

		ALL_ADVANCEMENTS = new HashMap<>(totalAdvancements);
		COMPLETED_ADVANCEMENTS = new HashMap<>(totalAdvancements);

		for (AdvancementEntry entry : advancements) {
			ALL_ADVANCEMENTS.put(entry.id(), entry);
		}

		Server.LOGGER.info("Loaded {} advancements.", totalAdvancements);

		Path save = Config.SAVES.resolve(String.valueOf(server.getOverworld().getSeed()));

		if (!Files.exists(save)) {
			try {
				CURRENT_SEED_FILE = FileChannel.open(save, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			// No need to continue since we have no data to load.
			return;
		}

		try {
			BufferedReader reader = new BufferedReader(new FileReader(save.toString()));
			String line = reader.readLine();

			while (line != null) {
				String[] arr = line.split(",");
				String entryName = arr[0];
				String criterionName = arr[1];

				AdvancementEntry entry = ALL_ADVANCEMENTS.get(Identifier.of(entryName));
				COMPLETED_ADVANCEMENTS.put(criterionName, entry);

				line = reader.readLine();
			}

			reader.close();

			CURRENT_SEED_FILE = FileChannel.open(save, StandardOpenOption.APPEND);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		Server.LOGGER.info("{} completed advancements.", COMPLETED_ADVANCEMENTS.size());
	}

	public void SyncPlayerOnJoin(ServerPlayerEntity player) {
		if (COMPLETED_ADVANCEMENTS.isEmpty()) {
			return;
		}

		PlayerAdvancementTracker tracker = player.getAdvancementTracker();

		Server.LOGGER.info("Syncing {} advancements.", player.getName().getString());
		COMPLETED_ADVANCEMENTS.forEach((key, value) -> tracker.grantCriterion(value, key));
	}

	public void OnCriterion(ServerPlayerEntity currentPlayer, AdvancementEntry entry, String name, boolean isDone) {
		if (!COMPLETED_ADVANCEMENTS.containsKey(name)) {
			COMPLETED_ADVANCEMENTS.put(name, entry);

			try {
				String line = String.format("%s,%s%s", entry.id().toString(), name, System.lineSeparator());
				appendToSave(line);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			Advancement advancement = entry.value();
			Optional<AdvancementDisplay> display = advancement.display();

			if (display.isPresent() && isDone) {
				AdvancementDisplay displayObj = display.get();
				if (!displayObj.shouldAnnounceToChat()) {
					return;
				}

				String playerName = currentPlayer.getName().getString();

				// Send the announcement to the server.
				MutableText text = Text.literal(playerName + " has made the advancement ").append(advancement.name().get());
				SERVER.getPlayerManager().broadcast(text, false);

				// Send the announcement to the Discord channel.
				String title = displayObj.getTitle().getString();
				if (title.isEmpty()) {
					title = name;
				}

				String description = displayObj.getDescription().getString();
				String message = String.format("**%s** has made the advancement **%s**!\n*%s*", playerName, title, description);
				Discord.getInstance().SendEmbed(message, Color.GREEN);
			}

			List<ServerPlayerEntity> players = SERVER.getPlayerManager().getPlayerList();

			for (ServerPlayerEntity player : players) {
				if (currentPlayer != player) {
					player.getAdvancementTracker().grantCriterion(entry, name);
				}
			}
		}
	}

	public void RegisterCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
		dispatcher.register(CommandManager.literal("cooptweaks")
				.then(CommandManager.literal("progress").executes(this::progressCommand))
		);
	}

	private int progressCommand(CommandContext<ServerCommandSource> context) {
		int total = ALL_ADVANCEMENTS.size();
		int completed = COMPLETED_ADVANCEMENTS.size();
		context.getSource().sendFeedback(() -> Text.literal(String.format("%d from %d advancements completed so far.", completed, total)), false);
		return 1;
	}
}
