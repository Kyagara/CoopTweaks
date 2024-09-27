package com.cooptweaks.advancements;

import com.cooptweaks.Configuration;
import com.cooptweaks.Main;
import com.cooptweaks.discord.Discord;
import discord4j.rest.util.Color;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.server.MinecraftServer;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 Manages advancements and criteria syncing.
 <p>
 <p>
 Startup:
 <ol>
 <li>Load all advancements and criteria from the server loaded by mods and the game.</li>
 <li>Load all completed advancements from the save file, the file is named by the world seed.</li>
 </ol>
 p>
 Syncing:
 <ol>
 <li>When an advancement is completed, append the save file with the advancement {@link Identifier}.</li>
 <li>Send a message to the server chat(disabling the original announceAdvancement broadcast) and Discord channel.</li>
 <li>When a player joins the server, go though all completed advancements and grant the criteria for each to the player.</li>
 </ol>
 */
public final class Advancements {
	private static final Discord DISCORD = Main.DISCORD;

	private static MinecraftServer SERVER;

	/** Map of all advancements. Loaded at startup. */
	private static final HashMap<Identifier, AdvancementEntry> ALL_ADVANCEMENTS = HashMap.newHashMap(122);

	/**
	 Map of criteria for each advancement. Loaded at startup.
	 <p>
	 Maps the advancement {@link AdvancementEntry#id() identifier} to a list of criteria.
	 */
	private static final HashMap<Identifier, List<String>> ALL_CRITERIA = new HashMap<>(122);

	/**
	 Map of all completed advancements. Loaded from the save file.
	 <p>
	 Maps the advancement {@link AdvancementEntry#id() identifier} to the {@link AdvancementEntry}.
	 */
	private static final ConcurrentHashMap<Identifier, AdvancementEntry> COMPLETED_ADVANCEMENTS = new ConcurrentHashMap<>(122);

	private static FileChannel CURRENT_SEED_FILE;

	private static synchronized void appendToSave(String text) throws IOException {
		ByteBuffer buffer = ByteBuffer.wrap(text.getBytes());
		CURRENT_SEED_FILE.write(buffer);
	}

	public void LoadAdvancements(MinecraftServer server) {
		SERVER = server;

		int totalAdvancements = loadServerAdvancements(server);
		if (totalAdvancements == 0) {
			Main.LOGGER.error("No advancements loaded from the server.");
			return;
		} else {
			Main.LOGGER.info("Loaded {} advancements from the server.", totalAdvancements);
		}

		if (ALL_CRITERIA.isEmpty()) {
			Main.LOGGER.error("No criteria loaded from the server.");
		} else {
			Main.LOGGER.info("Loaded {} criteria from the server.", ALL_CRITERIA.size());
		}

		try {
			int savedAdvancements = loadSaveAdvancements(server);
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

			// If the advancement has a display, add it to the advancement map.
			advancement.display().ifPresent(display -> {
				ALL_ADVANCEMENTS.put(entry.id(), entry);

				// Add all criteria for this advancement to the criteria map.
				Map<String, AdvancementCriterion<?>> criteria = advancement.criteria();
				criteria.forEach((key, criterion) -> {
					ALL_CRITERIA.computeIfAbsent(entry.id(), id -> new ArrayList<>()).add(key);
				});
			});
		}

		return ALL_ADVANCEMENTS.size();
	}

	private static int loadSaveAdvancements(MinecraftServer server) throws IOException {
		Path save = Configuration.ADVANCEMENTS_SAVE_PATH.resolve(String.valueOf(server.getOverworld().getSeed()));

		if (!Files.exists(save)) {
			try {
				CURRENT_SEED_FILE = FileChannel.open(save, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			return 0;
		}

		BufferedReader reader = new BufferedReader(new FileReader(save.toString()));
		String entryName = reader.readLine();

		while (entryName != null) {
			Identifier id = Identifier.of(entryName);

			AdvancementEntry entry = ALL_ADVANCEMENTS.get(id);
			if (entry != null) {
				COMPLETED_ADVANCEMENTS.put(id, entry);
			} else {
				Main.LOGGER.error("Advancement '{}' not found.", entryName);
			}

			entryName = reader.readLine();
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

		Main.LOGGER.info("Syncing player '{}' advancements.", name);

		// Loop through all completed advancements.
		COMPLETED_ADVANCEMENTS.forEach((id, entry) -> {
			// Loop through all criteria for this advancement.
			ALL_CRITERIA.get(id).forEach(criterionName -> {
				// Grant the criterion to the player.
				tracker.grantCriterion(entry, criterionName);
			});
		});
	}

	public void OnCriterion(ServerPlayerEntity currentPlayer, AdvancementEntry entry) {
		Identifier id = entry.id();

		if (COMPLETED_ADVANCEMENTS.containsKey(id)) {
			return;
		}

		Advancement advancement = entry.value();
		advancement.display().ifPresent(display -> {
			PlayerAdvancementTracker tracker = currentPlayer.getAdvancementTracker();
			if (tracker.getProgress(entry).isDone()) {
				String playerName = currentPlayer.getName().getString();

				COMPLETED_ADVANCEMENTS.put(id, entry);

				Collection<AdvancementCriterion<?>> criteria = advancement.criteria().values();

				// Grant the advancement to all players.
				List<ServerPlayerEntity> players = SERVER.getPlayerManager().getPlayerList();
				for (ServerPlayerEntity player : players) {
					if (currentPlayer != player) {
						criteria.forEach(criterion -> player.getAdvancementTracker().grantCriterion(entry, criterion.toString()));
					}
				}

				try {
					String line = String.format("%s%n", id);
					appendToSave(line);
				} catch (IOException e) {
					// This should be handled in another way, has to be recoverable, so we can try again.
					throw new RuntimeException(e);
				}

				Optional<Text> advancementName = advancement.name();
				if (advancementName.isEmpty()) {
					advancementName = Optional.of(Text.literal(id.toString()));
				}

				// Send announcement to the server chat.
				MutableText text = Text.literal(playerName + " has made the advancement ")
						.append(advancementName.get());

				SERVER.getPlayerManager().broadcast(text, false);

				// Send announcement to the Discord channel.
				String title = display.getTitle().getString();
				if (title.isEmpty()) {
					title = id.toString();
				}

				String description = display.getDescription().getString();
				String message = String.format("**%s** has made the advancement **%s**!%n*%s*", playerName, title, description);
				DISCORD.SendEmbed(message, Color.GREEN);
			}
		});
	}

	/**
	 Gets the advancements progress of the server.

	 @return The advancements progress of the server. Example: "10/122".
	 */
	public static String getAdvancementsProgress() {
		return String.format("%d/%d", COMPLETED_ADVANCEMENTS.size(), ALL_ADVANCEMENTS.size());
	}
}
