package com.cooptweaks.advancements;

import com.cooptweaks.Main;
import com.cooptweaks.Utils;
import com.cooptweaks.discord.Discord;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import discord4j.rest.util.Color;
import net.minecraft.advancement.Advancement;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public final class Advancements {
	private Advancements() {
	}

	private static Advancements INSTANCE = null;

	public static Advancements getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new Advancements();
		}

		return INSTANCE;
	}

	private static final Discord DISCORD = Main.DISCORD;

	private static MinecraftServer SERVER;

	public static HashMap<Identifier, AdvancementEntry> ALL_ADVANCEMENTS = new HashMap<>(122);
	public static HashMap<String, AdvancementEntry> COMPLETED_ADVANCEMENTS = new HashMap<>(122);

	private static FileChannel CURRENT_SEED_FILE;

	private static synchronized void appendToSave(String text) throws IOException {
		ByteBuffer buffer = ByteBuffer.wrap(text.getBytes());
		CURRENT_SEED_FILE.write(buffer);
	}

	public void LoadAdvancements(MinecraftServer server, Path savePath) {
		SERVER = server;

		int totalAdvancements = loadServerAdvancements(server);
		if (totalAdvancements == 0) {
			Main.LOGGER.error("No advancements loaded from the server.");
			return;
		} else {
			Main.LOGGER.info("Loaded {} advancements from the server.", totalAdvancements);
		}

		try {
			int savedAdvancements = loadSaveAdvancements(server, savePath);
			if (savedAdvancements == 0) {
				Main.LOGGER.info("No completed advancements data to load. Initialized new save file.");
			} else {
				Main.LOGGER.info("{} completed advancements loaded from the save file.", savedAdvancements);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static int loadServerAdvancements(MinecraftServer server) {
		Collection<AdvancementEntry> advancements = server.getAdvancementLoader().getAdvancements();

		for (AdvancementEntry entry : advancements) {
			Advancement advancement = entry.value();

			// If the advancement has a display, add it to the list of advancements
			advancement.display().ifPresent(display -> {
				ALL_ADVANCEMENTS.put(entry.id(), entry);
			});
		}

		return ALL_ADVANCEMENTS.size();
	}

	private static int loadSaveAdvancements(MinecraftServer server, Path saveFolder) throws IOException {
		Path save = saveFolder.resolve(String.valueOf(server.getOverworld().getSeed()));

		if (!Files.exists(save)) {
			try {
				CURRENT_SEED_FILE = FileChannel.open(save, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			return 0;
		}

		BufferedReader reader = new BufferedReader(new FileReader(save.toString()));
		String line = reader.readLine();

		while (line != null) {
			String[] arr = line.split(",");
			if (arr.length < 2) {
				Main.LOGGER.warn("Skipping malformed line: {}", line);
				continue;
			}

			String entryName = arr[0];
			String criterionName = arr[1];

			AdvancementEntry entry = ALL_ADVANCEMENTS.get(Identifier.of(entryName));
			if (entry != null) {
				COMPLETED_ADVANCEMENTS.put(criterionName, entry);
			} else {
				Main.LOGGER.warn("Advancement '{}' not found for criterion '{}'.", entryName, criterionName);
			}

			line = reader.readLine();
		}

		reader.close();
		CURRENT_SEED_FILE = FileChannel.open(save, StandardOpenOption.APPEND);
		return COMPLETED_ADVANCEMENTS.size();
	}

	public void SyncPlayerOnJoin(ServerPlayerEntity player, String name) {
		if (COMPLETED_ADVANCEMENTS.isEmpty()) {
			return;
		}

		PlayerAdvancementTracker tracker = player.getAdvancementTracker();

		Main.LOGGER.info("Syncing {} advancements.", name);
		COMPLETED_ADVANCEMENTS.forEach((criterionName, entry) -> tracker.grantCriterion(entry, criterionName));
	}

	public void OnCriterion(ServerPlayerEntity currentPlayer, AdvancementEntry entry, String criterionName, boolean isDone) {
		String id = entry.id().toString();
		if (COMPLETED_ADVANCEMENTS.containsKey(id)) {
			return;
		}

		// turn this into a lambda expression or use functional programming
		Advancement advancement = entry.value();
		advancement.display().ifPresent(display -> {
			if (isDone) {
				String playerName = currentPlayer.getName().getString();

				COMPLETED_ADVANCEMENTS.put(id, entry);

				// Grant the advancement to all players
				List<ServerPlayerEntity> players = SERVER.getPlayerManager().getPlayerList();
				for (ServerPlayerEntity player : players) {
					if (currentPlayer != player) {
						player.getAdvancementTracker().grantCriterion(entry, criterionName);
					}
				}

				try {
					String line = String.format("%s,%s\n", id, criterionName);
					appendToSave(line);
				} catch (IOException e) {
					// This should be handled in another way, has to be recoverable, so we can try again.
					throw new RuntimeException(e);
				}

				Optional<Text> advancementName = advancement.name();
				if (advancementName.isEmpty()) {
					advancementName = Optional.of(Text.literal(criterionName));
				}

				// Send announcement to the server chat
				MutableText text = Text.literal(playerName + " has made the advancement ")
						.append(advancementName.get());

				SERVER.getPlayerManager().broadcast(text, false);

				// Send announcement to the Discord channel
				String title = display.getTitle().getString();
				if (title.isEmpty()) {
					title = criterionName;
				}

				String description = display.getDescription().getString();
				String message = String.format("**%s** has made the advancement **%s**!\n*%s*", playerName, title, description);
				DISCORD.SendEmbed(message, Color.GREEN);
			}
		});
	}

	public void RegisterCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
		dispatcher.register(CommandManager.literal("cooptweaks")
				.then(CommandManager.literal("advancements")
						.then(CommandManager.literal("progress").executes(this::progressCommand))
				)
		);
	}

	private int progressCommand(CommandContext<ServerCommandSource> context) {
		context.getSource().sendFeedback(() -> Text.literal(String.format("%s advancements completed so far.", Utils.GetAdvancementsProgress())), false);
		return 1;
	}
}
