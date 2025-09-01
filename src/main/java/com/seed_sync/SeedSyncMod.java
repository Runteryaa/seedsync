package com.seed_sync;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

import java.util.concurrent.CompletableFuture;

public class SeedSyncMod implements ClientModInitializer {

    public static final String MOD_ID = "seed_sync";
    public static final String SYNC_URL = "https://seed-syncmc.com/sync";

    @Override
    public void onInitializeClient() {
        // Sync when the Minecraft client has started.
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> triggerSyncAsync("client_started"));

        // Also sync whenever the user joins a world.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> triggerSyncAsync("world_joined"));
    }

    private void triggerSyncAsync(String reason) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;

        Session session = mc.getSession();
        final String username = session != null ? session.getUsername() : "Unknown";
        final String uuid = session != null ? session.getUuid() : "Unknown";

        CompletableFuture.runAsync(() -> {
            try {
                var payload = WorldScanner.collectSyncPayload(username, uuid);
                HttpClientUtil.postJson(SYNC_URL, payload);
            } catch (Exception e) {
                System.err.println("[SeedSync] Sync failed (" + reason + "): " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}