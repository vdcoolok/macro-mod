package com.example.macromod;

import com.example.macromod.path.cache.CachedWorld;
import com.example.macromod.path.render.PathRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MacroMod implements ClientModInitializer {

    private static int cacheTick = 0;

    @Override
    public void onInitializeClient() {
        System.out.println("[MacroMod] Initializing for Minecraft 26.2...");
        MacroCommand.register();
        CachedWorld.get();

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            CachedWorld.get().clear();
            MacroExecutor.stop();
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            CachedWorld.get().clear();
        });

        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            CachedWorld.get().queue(chunk);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MacroExecutor.tick(client);
            BackgroundInputHandler.tick(client);
            CombatController.tick(client);
            FollowController.tick(client);
            cacheLoadedChunks(client);
        });

        LevelExtractionEvents.END_EXTRACTION.register(PathRenderer::extract);
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(PathRenderer::render);
    }

    private static void cacheLoadedChunks(Minecraft client) {
        if (client.player == null || client.level == null) return;

        cacheTick++;
        if (cacheTick % 5 != 0) return;

        CachedWorld cache = CachedWorld.get();
        int radius = client.options.getEffectiveRenderDistance() + 1;
        int pcx = (int) Math.floor(client.player.getX()) >> 4;
        int pcz = (int) Math.floor(client.player.getZ()) >> 4;

        List<int[]> pending = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = pcx + dx;
                int cz = pcz + dz;
                if (cache.isLoaded(cx, cz)) continue;
                if (cache.isQueued(cx, cz)) continue;

                LevelChunk chunk = client.level.getChunkSource().getChunk(cx, cz, false);
                if (chunk != null) {
                    pending.add(new int[]{cx, cz, dx * dx + dz * dz});
                }
            }
        }

        pending.sort(Comparator.comparingInt(a -> a[2]));

        for (int[] c : pending) {
            LevelChunk chunk = client.level.getChunkSource().getChunk(c[0], c[1], false);
            if (chunk != null) {
                cache.queue(chunk);
            }
        }
    }
}