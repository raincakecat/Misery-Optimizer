package dev.tung.miseryoptimizer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Saves vanilla's local block predictions so rejected actions can be restored promptly. */
public final class BlockActionPreview {
    private static final Deque<Batch> PLACEMENTS = new ArrayDeque<>();
    private static final Map<Long, BreakPrediction> BREAKS = new LinkedHashMap<>();
    private static Map<Long, BlockState> capturing;
    private static BlockState breakingOriginal;
    private static ClientLevel level;

    private record Batch(Map<Long, BlockState> originals, long deadline) {}
    private record BreakPrediction(BlockState original, long deadline) {}

    public static void beginPlacement(ClientLevel clientLevel) {
        if (!MiseryOptimizerClient.config().blockCorrections) return;
        if (level != clientLevel) clear();
        level = clientLevel;
        capturing = new LinkedHashMap<>();
    }

    public static void recordSetBlock(ClientLevel clientLevel, BlockPos pos, BlockState original) {
        if (capturing != null && clientLevel == level && capturing.size() < 16) {
            capturing.putIfAbsent(pos.asLong(), original);
        }
    }

    public static void endPlacement(boolean consumed) {
        if (capturing == null) return;
        if (consumed && !capturing.isEmpty()) {
            PLACEMENTS.addLast(new Batch(capturing,
                System.nanoTime() / 1_000_000L + MiseryOptimizerClient.predictionTimeoutMillis()));
            while (PLACEMENTS.size() > 32) PLACEMENTS.removeFirst();
        }
        capturing = null;
    }

    public static void beginBreak(ClientLevel clientLevel, BlockPos pos) {
        if (!MiseryOptimizerClient.config().blockCorrections) return;
        if (level != clientLevel) clear();
        level = clientLevel;
        breakingOriginal = clientLevel.getBlockState(pos);
    }

    public static void endBreak(BlockPos pos, boolean succeeded) {
        if (breakingOriginal != null && succeeded && level != null
                && !level.getBlockState(pos).equals(breakingOriginal)) {
            BREAKS.put(pos.asLong(), new BreakPrediction(breakingOriginal,
                System.nanoTime() / 1_000_000L + MiseryOptimizerClient.predictionTimeoutMillis()));
            while (BREAKS.size() > 64) BREAKS.remove(BREAKS.keySet().iterator().next());
        }
        breakingOriginal = null;
    }

    public static void rejectPlacement(BlockPos pos) {
        Iterator<Batch> iterator = PLACEMENTS.iterator();
        while (iterator.hasNext()) {
            Batch batch = iterator.next();
            if (!batch.originals.containsKey(pos.asLong())) continue;
            iterator.remove();
            for (Map.Entry<Long, BlockState> entry : batch.originals.entrySet()) {
                if (PLACEMENTS.stream().noneMatch(other -> other.originals.containsKey(entry.getKey()))) {
                    restore(entry.getKey(), entry.getValue());
                }
            }
            return;
        }
    }

    public static void rejectBreak(BlockPos pos) {
        BreakPrediction prediction = BREAKS.remove(pos.asLong());
        if (prediction != null) restore(pos.asLong(), prediction.original);
    }

    public static void confirmBlock(BlockPos pos) {
        long key = pos.asLong();
        BREAKS.remove(key);
        PLACEMENTS.removeIf(batch -> {
            batch.originals.remove(key);
            return batch.originals.isEmpty();
        });
    }

    public static void confirmChunk(int chunkX, int chunkZ) {
        BREAKS.keySet().removeIf(key -> BlockPos.getX(key) >> 4 == chunkX
            && BlockPos.getZ(key) >> 4 == chunkZ);
        PLACEMENTS.removeIf(batch -> {
            batch.originals.keySet().removeIf(key -> BlockPos.getX(key) >> 4 == chunkX
                && BlockPos.getZ(key) >> 4 == chunkZ);
            return batch.originals.isEmpty();
        });
    }

    public static void tick() {
        if (level != Minecraft.getInstance().level) {
            clear();
            return;
        }
        long now = System.nanoTime() / 1_000_000L;
        PLACEMENTS.removeIf(batch -> batch.deadline <= now);
        BREAKS.values().removeIf(prediction -> prediction.deadline <= now);
    }

    public static void clear() {
        PLACEMENTS.clear();
        BREAKS.clear();
        capturing = null;
        breakingOriginal = null;
        level = null;
    }

    private static void restore(long key, BlockState original) {
        if (level == null || level != Minecraft.getInstance().level) return;
        BlockPos pos = BlockPos.of(key);
        if (level.hasChunkAt(pos) && !level.getBlockState(pos).equals(original)) {
            level.setBlock(pos, original, 2);
        }
    }
}
