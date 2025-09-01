package com.seed_sync;

import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.io.IOException;

public class WorldScanner {

    public static Map<String, Object> collectSyncPayload(String username, String uuid) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("username", username);
        root.put("uuid", uuid);
        root.put("client", "fabric");
        root.put("mc_version", getMcVersionSafe());
        root.put("mod_id", SeedSyncMod.MOD_ID);
        var worlds = scanSavesDirectory();
        root.put("worlds", worlds);
        return root;
    }

    private static String getMcVersionSafe() {
        try {
            return SharedConstants.getGameVersion().getName();
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static List<Map<String, Object>> scanSavesDirectory() throws IOException {
        MinecraftClient mc = MinecraftClient.getInstance();
        Path runDir = mc.getRunDirectory().toPath();
        Path saves = runDir.resolve("saves");

        if (!Files.isDirectory(saves)) return Collections.emptyList();

        try (var dirs = Files.list(saves)) {
            return dirs
                    .filter(Files::isDirectory)
                    .map(WorldScanner::readOneWorld)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
    }

    private static Map<String, Object> readOneWorld(Path worldDir) {
        try {
            Path levelDat = worldDir.resolve("level.dat");
            if (!Files.exists(levelDat)) return null;

            NbtCompound root = NbtIo.readCompressed(levelDat);
            if (root == null) return null;

            NbtCompound data = root.getCompound("Data");

            String worldName = data.getString("LevelName");
            if (worldName == null || worldName.isBlank()) {
                worldName = worldDir.getFileName().toString();
            }

            long seed = extractSeed(data);

            long lastPlayed = data.contains("LastPlayed", NbtElement.LONG_TYPE)
                    ? data.getLong("LastPlayed")
                    : Files.getLastModifiedTime(levelDat).toMillis();

            BasicFileAttributes attrs = Files.readAttributes(worldDir, BasicFileAttributes.class);
            long createdAt = toMillisSafe(attrs.creationTime().toInstant());
            long updatedAt = toMillisSafe(attrs.lastModifiedTime().toInstant());

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("world_name", worldName);
            entry.put("folder_name", worldDir.getFileName().toString());
            entry.put("seed", seed);
            entry.put("last_played", lastPlayed);
            entry.put("created_at", createdAt);
            entry.put("updated_at", updatedAt);

            return entry;
        } catch (Exception e) {
            System.err.println("[SeedSync] Failed to read world at " + worldDir + ": " + e.getMessage());
            return null;
        }
    }

    private static long extractSeed(NbtCompound data) {
        long seed = 0L;
        if (data.contains("WorldGenSettings", NbtElement.COMPOUND_TYPE)) {
            NbtCompound wgs = data.getCompound("WorldGenSettings");
            if (wgs.contains("seed", NbtElement.LONG_TYPE)) {
                seed = wgs.getLong("seed");
            } else if (wgs.contains("seed", NbtElement.INT_TYPE)) {
                seed = wgs.getInt("seed");
            }
        }
        if (seed == 0L && data.contains("RandomSeed", NbtElement.LONG_TYPE)) {
            seed = data.getLong("RandomSeed");
        }
        return seed;
    }

    private static long toMillisSafe(Instant instant) {
        try {
            return instant.toEpochMilli();
        } catch (Throwable t) {
            return System.currentTimeMillis();
        }
    }
}
