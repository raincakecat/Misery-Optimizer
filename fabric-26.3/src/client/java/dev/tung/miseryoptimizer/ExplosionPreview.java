package dev.tung.miseryoptimizer;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

/** Reversible local block preview. Never sends block edits to the server. */
final class ExplosionPreview {
    private static final int MAX_STEPS_PER_RAY = 64;
    private static final int MAX_VISIBLE_OUTLINES = 128;
    private static final RenderStateDataKey<List<Vec3>> SAFE_OUTLINES = RenderStateDataKey.create();
    private static final double[] RAYS = createRays();
    private static final Map<Long, PredictedBlock> PENDING = new LinkedHashMap<>();
    private static final List<Batch> BATCHES = new ArrayList<>();
    private static final ArrayDeque<Scan> SCANS = new ArrayDeque<>();
    private static ClientLevel predictedLevel;

    private record PredictedBlock(BlockState original, long deadline, boolean safe) {}
    private record Batch(Vec3 center, int crystalId, long anchorPos, Set<Long> blocks, long deadline) {}
    private record BlockInfo(BlockState state, float resistance, boolean outOfWorld) {}

    private static final class Scan {
        final ClientLevel level;
        final Vec3 center;
        final float radius;
        final boolean removeOriginBlock;
        final int crystalId;
        final long deadline;
        final BlockPos origin;
        final BlockPos.MutableBlockPos scratch = new BlockPos.MutableBlockPos();
        final Long2ObjectOpenHashMap<BlockInfo> blockCache = new Long2ObjectOpenHashMap<>();
        final LongOpenHashSet candidates = new LongOpenHashSet();
        int ray;

        Scan(ClientLevel level, Vec3 center, float radius, boolean removeOriginBlock,
                int crystalId, long deadline) {
            this.level = level;
            this.center = center;
            this.radius = radius;
            this.removeOriginBlock = removeOriginBlock;
            this.crystalId = crystalId;
            this.deadline = deadline;
            this.origin = BlockPos.containing(center);
        }
    }

    private static double[] createRays() {
        double[] result = new double[1352 * 3];
        int index = 0;
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    if (x != 0 && x != 15 && y != 0 && y != 15 && z != 0 && z != 15) continue;
                    double dx = x / 15.0 * 2.0 - 1.0;
                    double dy = y / 15.0 * 2.0 - 1.0;
                    double dz = z / 15.0 * 2.0 - 1.0;
                    double scale = 0.3 / Math.sqrt(dx * dx + dy * dy + dz * dz);
                    result[index++] = dx * scale;
                    result[index++] = dy * scale;
                    result[index++] = dz * scale;
                }
            }
        }
        return result;
    }

    static void predict(Vec3 center, float radius) {
        predict(center, radius, false, -1);
    }

    static void predict(Vec3 center, float radius, boolean removeOriginBlock) {
        predict(center, radius, removeOriginBlock, -1);
    }

    static void predictCrystal(Vec3 center, int entityId) {
        predict(center, 6.0F, false, entityId);
    }

    private static void predict(Vec3 center, float radius, boolean removeOriginBlock, int crystalId) {
        if (!MiseryOptimizerClient.config().terrainPreview) return;
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || client.player == null
            || client.player.position().distanceToSqr(center) > 64.0 * 64.0) return;
        if (predictedLevel != level) {
            PENDING.clear();
            BATCHES.clear();
            SCANS.clear();
            predictedLevel = level;
        }
        long now = System.nanoTime() / 1_000_000L;
        if (BATCHES.stream().anyMatch(batch -> batch.deadline > now
                && batch.center.distanceToSqr(center) < 0.01)
            || SCANS.stream().anyMatch(scan -> scan.center.distanceToSqr(center) < 0.01)) return;

        Scan scan = new Scan(level, center, radius, removeOriginBlock, crystalId,
            now + MiseryOptimizerClient.predictionTimeoutMillis());
        if (MiseryOptimizerClient.config().previewFrameBudget) {
            if (SCANS.size() < 4) SCANS.addLast(scan);
        } else {
            scan(scan, Integer.MAX_VALUE, Long.MAX_VALUE);
            apply(scan);
        }
    }

    private static void scan(Scan scan, int maxRays, long stopNanos) {
        long started = System.nanoTime();
        int processed = 0;
        // Same 16-cube boundary directions and 0.3-block step as vanilla.
        // The server's random ray strength and plugin block list are unknowable here.
        while (scan.ray < RAYS.length && processed < maxRays
            && System.nanoTime() < stopNanos) {
            double dx = RAYS[scan.ray];
            double dy = RAYS[scan.ray + 1];
            double dz = RAYS[scan.ray + 2];
            double px = scan.center.x;
            double py = scan.center.y;
            double pz = scan.center.z;
            float power = scan.radius; // middle of vanilla's random ray range
            for (int step = 0; step < MAX_STEPS_PER_RAY && power > 0.0F; step++) {
                scan.scratch.set(Mth.floor(px), Mth.floor(py), Mth.floor(pz));
                long key = scan.scratch.asLong();
                BlockInfo info = scan.blockCache.get(key);
                if (info == null) {
                    if (scan.level.isOutsideBuildHeight(scan.scratch)
                        || !scan.level.hasChunkAt(scan.scratch)) {
                        info = new BlockInfo(null, 0.0F, true);
                    } else {
                        BlockState state = scan.level.getBlockState(scan.scratch);
                        float resistance = scan.removeOriginBlock && scan.scratch.equals(scan.origin) ? 0.0F
                            : Math.max(state.getBlock().getExplosionResistance(),
                                state.getFluidState().getExplosionResistance());
                        info = new BlockInfo(state, resistance, false);
                    }
                    scan.blockCache.put(key, info);
                }
                if (info.outOfWorld) break;
                boolean removedAtSource = scan.removeOriginBlock && scan.scratch.equals(scan.origin);
                if (!info.state.isAir() && !removedAtSource) power -= (info.resistance + 0.3F) * 0.3F;
                if (power > 0.0F && !info.state.isAir()
                    && info.state.getFluidState().isEmpty() && !info.state.hasBlockEntity()) {
                    scan.candidates.add(key);
                }
                power -= 0.22500001F;
                px += dx;
                py += dy;
                pz += dz;
            }
            scan.ray += 3;
            processed++;
        }
        ClientDiagnostics.scan(System.nanoTime() - started);
    }

    private static void apply(Scan scan) {
        if (scan.level != Minecraft.getInstance().level || !MiseryOptimizerClient.config().terrainPreview
            || System.nanoTime() / 1_000_000L > scan.deadline) return;
        ClientLevel level = scan.level;
        Vec3 center = scan.center;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (scan.removeOriginBlock && level.hasChunkAt(scan.origin)
            && !level.getBlockState(scan.origin).isAir()) {
            scan.candidates.add(scan.origin.asLong());
        }

        long deadline = scan.deadline;
        List<Long> ordered = new ArrayList<>(scan.candidates.size());
        for (long key : scan.candidates) ordered.add(key);
        ordered.sort(Comparator.comparingDouble(key -> BlockPos.of(key).distToCenterSqr(center.x, center.y, center.z)));
        AABB playerSupport = client.player.getBoundingBox().inflate(0.15, 0.0, 0.15)
            .expandTowards(0.0, -0.6, 0.0);
        boolean safe = MiseryOptimizerClient.config().safeTerrainPreview;
        Set<Long> batchBlocks = new HashSet<>();
        for (long key : ordered) {
            if (PENDING.size() >= MiseryOptimizerClient.config().maxPreviewBlocks && !PENDING.containsKey(key)) break;
            PredictedBlock existing = PENDING.get(key);
            if (existing != null) {
                PENDING.put(key, new PredictedBlock(existing.original,
                    Math.max(existing.deadline, deadline), existing.safe));
                batchBlocks.add(key);
                continue;
            }
            BlockPos pos = BlockPos.of(key);
            // Keep the player's immediate collision/support blocks until the server updates them.
            if (!safe && playerSupport.intersects(new AABB(pos))) continue;
            BlockState original = level.getBlockState(pos);
            if (safe || level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2)) {
                PENDING.put(key, new PredictedBlock(original, deadline, safe));
                batchBlocks.add(key);
            }
        }
        BATCHES.add(new Batch(center, scan.crystalId,
            scan.removeOriginBlock ? scan.origin.asLong() : Long.MIN_VALUE,
            batchBlocks, deadline));
        if (BATCHES.size() > 32) BATCHES.removeFirst();
    }

    static void acceptExact(Vec3 center, Set<Long> exactBlocks) {
        Minecraft client = Minecraft.getInstance();
        if (predictedLevel == null || predictedLevel != client.level || client.player == null) return;
        Batch match = null;
        double nearest = 0.75 * 0.75;
        for (Batch batch : BATCHES) {
            double distance = batch.center.distanceToSqr(center);
            if (distance < nearest) {
                match = batch;
                nearest = distance;
            }
        }
        if (match == null) {
            // Exact server feedback can arrive before a budgeted scan finishes.
            // Do not apply a stale estimate after the authoritative result.
            SCANS.removeIf(scan -> scan.center.distanceToSqr(center) < 0.75 * 0.75);
            return;
        }
        BATCHES.remove(match);
        if (match.blocks.stream().anyMatch(key -> !exactBlocks.contains(key))) {
            ClientDiagnostics.terrainCorrection();
        }
        for (long key : match.blocks) {
            if (!exactBlocks.contains(key) && BATCHES.stream().noneMatch(batch -> batch.blocks.contains(key))) {
                restore(key);
            }
        }
        // The server's event list is authoritative for this explosion. Normal block packets
        // still confirm each edit and recover if another plugin changes the list afterward.
        AABB playerSupport = client.player.getBoundingBox()
            .inflate(0.15, 0.0, 0.15).expandTowards(0.0, -0.6, 0.0);
        boolean safe = MiseryOptimizerClient.config().safeTerrainPreview;
        for (long key : exactBlocks) {
            if (PENDING.containsKey(key)) continue;
            if (PENDING.size() >= MiseryOptimizerClient.config().maxPreviewBlocks) break;
            BlockPos pos = BlockPos.of(key);
            if (!predictedLevel.hasChunkAt(pos)) continue;
            if (!safe && playerSupport.intersects(new AABB(pos))) continue;
            BlockState original = predictedLevel.getBlockState(pos);
            if (original.isAir() || original.hasBlockEntity()) continue;
            if (safe || predictedLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 2)) {
                PENDING.put(key, new PredictedBlock(original, match.deadline, safe));
            }
        }
    }

    static void rejectCrystal(int id) {
        SCANS.removeIf(scan -> scan.crystalId == id);
        reject(batch -> batch.crystalId == id);
    }

    static void rejectAnchor(BlockPos pos) {
        SCANS.removeIf(scan -> scan.removeOriginBlock && scan.origin.equals(pos));
        reject(batch -> batch.anchorPos == pos.asLong());
    }

    private static void reject(java.util.function.Predicate<Batch> predicate) {
        List<Batch> rejected = new ArrayList<>();
        BATCHES.removeIf(batch -> {
            if (!predicate.test(batch)) return false;
            rejected.add(batch);
            return true;
        });
        for (Batch batch : rejected) {
            ClientDiagnostics.terrainCorrection();
            for (long key : batch.blocks) {
                if (BATCHES.stream().noneMatch(other -> other.blocks.contains(key))) restore(key);
            }
        }
    }

    private static void restore(long key) {
        PredictedBlock prediction = PENDING.remove(key);
        if (prediction == null || predictedLevel == null) return;
        BlockPos pos = BlockPos.of(key);
        if (!prediction.safe && predictedLevel.getBlockState(pos).isAir()) {
            predictedLevel.setBlock(pos, prediction.original, 2);
        }
    }

    static void confirm(BlockPos pos) {
        PENDING.remove(pos.asLong());
    }

    static void confirmChunk(int chunkX, int chunkZ) {
        PENDING.keySet().removeIf(key -> {
            return BlockPos.getX(key) >> 4 == chunkX && BlockPos.getZ(key) >> 4 == chunkZ;
        });
    }

    static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (predictedLevel != client.level) {
            clear();
            return;
        }
        long now = System.nanoTime() / 1_000_000L;
        if (!SCANS.isEmpty()) {
            long stopNanos = System.nanoTime() + 2_000_000L;
            while (!SCANS.isEmpty() && System.nanoTime() < stopNanos) {
                Scan scan = SCANS.peekFirst();
                if (scan.level != client.level || scan.deadline <= now) {
                    SCANS.removeFirst();
                    continue;
                }
                scan(scan, 128, stopNanos);
                if (scan.ray < RAYS.length) break;
                SCANS.removeFirst();
                apply(scan);
            }
        }
        BATCHES.removeIf(batch -> batch.deadline <= now);
        Iterator<Map.Entry<Long, PredictedBlock>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, PredictedBlock> entry = iterator.next();
            if (entry.getValue().deadline > now) continue;
            BlockPos pos = BlockPos.of(entry.getKey());
            if (predictedLevel != null && !entry.getValue().safe
                && predictedLevel.getBlockState(pos).isAir()) {
                predictedLevel.setBlock(pos, entry.getValue().original, 2);
            }
            iterator.remove();
        }
    }

    static void clear() {
        PENDING.clear();
        BATCHES.clear();
        SCANS.clear();
        predictedLevel = null;
    }

    static void restoreAll() {
        if (predictedLevel != null && Minecraft.getInstance().level == predictedLevel) {
            for (Map.Entry<Long, PredictedBlock> entry : PENDING.entrySet()) {
                BlockPos pos = BlockPos.of(entry.getKey());
                if (!entry.getValue().safe && predictedLevel.getBlockState(pos).isAir()) {
                    predictedLevel.setBlock(pos, entry.getValue().original, 2);
                }
            }
        }
        clear();
    }

    static void extractSafePreview(LevelExtractionContext context) {
        FabricRenderState state = (FabricRenderState) context.levelState();
        if (predictedLevel != context.level() || !MiseryOptimizerClient.config().terrainPreview
            || !MiseryOptimizerClient.config().safeTerrainPreview) {
            state.setData(SAFE_OUTLINES, List.of());
            return;
        }
        Vec3 camera = context.camera().position();
        List<Long> visible = new ArrayList<>();
        for (Map.Entry<Long, PredictedBlock> entry : PENDING.entrySet()) {
            if (!entry.getValue().safe) continue;
            BlockPos pos = BlockPos.of(entry.getKey());
            if (pos.distToCenterSqr(camera.x, camera.y, camera.z) > 72.0 * 72.0) continue;
            visible.add(entry.getKey());
        }
        visible.sort(Comparator.comparingDouble(key -> BlockPos.of(key)
            .distToCenterSqr(camera.x, camera.y, camera.z)));
        List<Vec3> offsets = new ArrayList<>(Math.min(visible.size(), MAX_VISIBLE_OUTLINES));
        for (int i = 0; i < visible.size() && i < MAX_VISIBLE_OUTLINES; i++) {
            BlockPos pos = BlockPos.of(visible.get(i));
            offsets.add(new Vec3(pos.getX() - camera.x, pos.getY() - camera.y,
                pos.getZ() - camera.z));
        }
        state.setData(SAFE_OUTLINES, List.copyOf(offsets));
    }

    static void renderSafePreview(LevelRenderContext context) {
        List<Vec3> offsets = ((FabricRenderState) context.levelState()).getData(SAFE_OUTLINES);
        if (offsets == null || offsets.isEmpty()) return;
        for (Vec3 offset : offsets) {
            var pose = new com.mojang.blaze3d.vertex.PoseStack();
            pose.translate(offset.x, offset.y, offset.z);
            context.submitNodeCollector().submitShapeOutline(pose, Shapes.block(),
                RenderTypes.linesTranslucent(), 0xCCFFAA00, 1.5F, false);
        }
    }
}
