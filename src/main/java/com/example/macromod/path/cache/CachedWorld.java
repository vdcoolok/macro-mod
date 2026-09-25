package com.example.macromod.path.cache;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class CachedWorld {

    private static CachedWorld INSTANCE;
    private final Map<Long, CachedChunk> chunks = new ConcurrentHashMap<>();
    private final Set<Long> queued = ConcurrentHashMap.newKeySet();
    private final LinkedBlockingQueue<LevelChunk> packQueue = new LinkedBlockingQueue<>();
    private final AtomicLong packedCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final ExecutorService packer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MacroMod-ChunkPacker");
        t.setDaemon(true);
        return t;
    });

    private CachedWorld() {
        System.out.println("[MacroMod] CachedWorld: starting packer thread");
        packer.submit(this::packLoop);
    }

    public static CachedWorld get() {
        if (INSTANCE == null) INSTANCE = new CachedWorld();
        return INSTANCE;
    }

    public void clear() {
        chunks.clear();
        queued.clear();
        packQueue.clear();
    }

    public void queue(LevelChunk chunk) {
        int cx = chunk.getPos().x();
        int cz = chunk.getPos().z();
        long k = key(cx, cz);
        if (chunks.containsKey(k)) return;
        if (!queued.add(k)) return;
        packQueue.offer(chunk);
    }

    public CachedChunk getChunk(int x, int z) {
        return chunks.get(key(x, z));
    }

    public void markAir(int x, int y, int z) {
        CachedChunk c = chunks.get(key(x >> 4, z >> 4));
        if (c == null || !c.isLoaded()) return;
        c.set(x & 15, y, z & 15, PathingBlockType.AIR);
        c.setBreakTicks(x & 15, y, z & 15, (byte) 0);
    }

    public boolean isLoaded(int x, int z) {
        CachedChunk c = chunks.get(key(x, z));
        return c != null && c.isLoaded();
    }

    public boolean isQueued(int x, int z) {
        return queued.contains(key(x, z));
    }

    public int queueSize() {
        return packQueue.size();
    }

    public int cacheSize() {
        return chunks.size();
    }

    public long packedCount() { return packedCount.get(); }
    public long errorCount() { return errorCount.get(); }

    private void packLoop() {
        Minecraft mc = Minecraft.getInstance();
        System.out.println("[MacroMod] CachedWorld: packer thread running");
        while (true) {
            try {
                LevelChunk chunk = packQueue.take();
                int cx = chunk.getPos().x();
                int cz = chunk.getPos().z();
                long k = key(cx, cz);

                if (mc.level == null) {
                    queued.remove(k);
                    continue;
                }

                try {
                    CachedChunk packed = ChunkPacker.pack(chunk, mc.level);
                    chunks.put(k, packed);
                    long n = packedCount.incrementAndGet();
                    if (n % 100 == 0) {
                        System.out.println("[MacroMod] CachedWorld: packed " + n
                            + " chunks (queue " + packQueue.size() + ")");
                    }
                } catch (Exception e) {
                    long n = errorCount.incrementAndGet();
                    if (n <= 5) {
                        System.err.println("[MacroMod] Chunk pack error at ("
                            + cx + ", " + cz + "): " + e);
                    }
                } finally {
                    queued.remove(k);
                }
            } catch (InterruptedException e) {
                System.out.println("[MacroMod] CachedWorld: packer thread interrupted");
                return;
            } catch (Throwable t) {
                System.err.println("[MacroMod] CachedWorld: packer thread fatal: " + t);
                t.printStackTrace();
                return;
            }
        }
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}