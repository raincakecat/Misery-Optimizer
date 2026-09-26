package dev.tung.miseryoptimizer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.lwjgl.glfw.GLFW;

public final class MiseryOptimizerClient implements ClientModInitializer {
    private static final PredictionRegistry PREDICTIONS =
        new PredictionRegistry(() -> System.nanoTime() / 1_000_000L);
    private static final MiseryOptimizerConfig CONFIG = MiseryOptimizerConfig.load();
    private static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("tung", "settings"));
    private static final List<CrystalEffect> CRYSTAL_EFFECTS = new ArrayList<>();
    private static KeyMapping openConfig;

    private record CrystalEffect(int entityId, Vec3 center, long deadline) {}

    @Override
    public void onInitializeClient() {
        MiseryOptimizerFeedback.register();
        openConfig = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.tung.open_config", GLFW.GLFW_KEY_O, KEY_CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PREDICTIONS.expire();
            GhostPearl.tickPending();
            GhostCrystal.tickPending();
            ExplosionPreview.tick();
            BlockActionPreview.tick();
            FoodUsePreview.tick();
            long now = System.nanoTime() / 1_000_000L;
            CRYSTAL_EFFECTS.removeIf(effect -> effect.deadline <= now);
            if (client.screen == null && openConfig.consumeClick()) {
                client.setScreen(new MiseryOptimizerConfigScreen(null, CONFIG));
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            PREDICTIONS.clear();
            GhostPearl.clear();
            GhostCrystal.clear();
            ExplosionPreview.clear();
            BlockActionPreview.clear();
            FoodUsePreview.clear();
            CRYSTAL_EFFECTS.clear();
        });
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            // Fail at startup if a packet or interaction mixin no longer matches 1.21.11.
            try {
                Class.forName("net.minecraft.client.multiplayer.ClientPacketListener", false,
                    MiseryOptimizerClient.class.getClassLoader());
                Class.forName("net.minecraft.client.multiplayer.MultiPlayerGameMode", false,
                    MiseryOptimizerClient.class.getClassLoader());
                Class.forName("net.minecraft.client.multiplayer.ClientLevel", false,
                    MiseryOptimizerClient.class.getClassLoader());
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("Minecraft 1.21.11 classes unavailable", ex);
            }
        }
    }

    public static void predictCrystal(EndCrystal crystal) {
        boolean fresh = !PREDICTIONS.isCrystalPending(crystal.getId());
        if (CONFIG.crystals) {
            PREDICTIONS.predictCrystal(crystal.getId(), currentPing(), CONFIG.extraGraceMillis);
        }
        if (CONFIG.crystalEffects && fresh) {
            Minecraft minecraft = Minecraft.getInstance();
            Vec3 center = crystal.position();
            minecraft.level.playLocalSound(center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F,
                (1.0F + (minecraft.level.random.nextFloat() - minecraft.level.random.nextFloat()) * 0.2F) * 0.7F,
                false);
            minecraft.level.addParticle(ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y, center.z, 1.0, 0.0, 0.0);
            CRYSTAL_EFFECTS.add(new CrystalEffect(crystal.getId(), center,
                System.nanoTime() / 1_000_000L + predictionTimeoutMillis()));
            if (CRYSTAL_EFFECTS.size() > 64) CRYSTAL_EFFECTS.removeFirst();
        }
        ExplosionPreview.predictCrystal(crystal.position(), crystal.getId());
    }

    public static boolean hideCrystal(Entity entity) {
        return CONFIG.crystals && entity instanceof EndCrystal && PREDICTIONS.isCrystalPending(entity.getId());
    }

    public static boolean hideServerPearl(Entity entity) {
        return CONFIG.pearls && GhostPearl.hideServerPearl(entity);
    }

    public static void confirmCrystal(int entityId) {
        PREDICTIONS.confirmCrystal(entityId);
    }

    public static void rejectCrystal(int entityId) {
        PREDICTIONS.confirmCrystal(entityId);
        ExplosionPreview.rejectCrystal(entityId);
        CRYSTAL_EFFECTS.removeIf(effect -> effect.entityId == entityId);
    }

    public static void rejectAnchor(BlockPos pos) {
        PREDICTIONS.confirmAnchor(pos.asLong());
        ExplosionPreview.rejectAnchor(pos);
    }

    public static void predictPearl() {
        if (CONFIG.pearls) GhostPearl.predict();
    }

    public static void confirmPearl(net.minecraft.network.protocol.game.ClientboundAddEntityPacket packet) {
        GhostPearl.confirm(packet);
    }

    public static void predictPlacedCrystal(BlockPos crystalBlock) {
        if (CONFIG.crystalPlacement) GhostCrystal.predict(crystalBlock);
    }

    public static void confirmPlacedCrystal(net.minecraft.network.protocol.game.ClientboundAddEntityPacket packet) {
        GhostCrystal.confirm(packet);
    }

    public static void confirmBlock(BlockPos pos) {
        ExplosionPreview.confirm(pos);
        BlockActionPreview.confirmBlock(pos);
    }

    public static void confirmChunk(int chunkX, int chunkZ) {
        ExplosionPreview.confirmChunk(chunkX, chunkZ);
        BlockActionPreview.confirmChunk(chunkX, chunkZ);
    }

    public static void predictAnchor(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Vec3 center = pos.getCenter();
        if (CONFIG.anchors && !PREDICTIONS.isAnchorPending(pos.asLong())) {
            PREDICTIONS.predictAnchor(pos.asLong(), currentPing(), CONFIG.extraGraceMillis);
            minecraft.level.playLocalSound(center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F,
                (1.0F + (minecraft.level.random.nextFloat() - minecraft.level.random.nextFloat()) * 0.2F) * 0.7F,
                false);
            minecraft.level.addParticle(ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y, center.z, 1.0, 0.0, 0.0);
        }
        ExplosionPreview.predict(center, 5.0F, true);
    }

    public static boolean predictedAnchorExplosion(Vec3 center) {
        if (center.x != Math.floor(center.x) + 0.5
            || center.y != Math.floor(center.y) + 0.5
            || center.z != Math.floor(center.z) + 0.5) return false;
        return PREDICTIONS.isAnchorPending(BlockPos.containing(center).asLong());
    }

    public static void confirmAnchorExplosion(Vec3 center) {
        if (predictedAnchorExplosion(center)) {
            PREDICTIONS.confirmAnchor(BlockPos.containing(center).asLong());
        }
    }

    public static boolean predictedCrystalExplosion(Vec3 center) {
        for (CrystalEffect effect : CRYSTAL_EFFECTS) {
            if (effect.center.distanceToSqr(center) < 0.75 * 0.75) return true;
        }
        return false;
    }

    public static void confirmCrystalExplosion(Vec3 center) {
        Iterator<CrystalEffect> iterator = CRYSTAL_EFFECTS.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().center.distanceToSqr(center) < 0.75 * 0.75) {
                iterator.remove();
                return;
            }
        }
    }

    static int currentPing() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) return 150;
        PlayerInfo info = minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID());
        return info == null ? 150 : info.getLatency();
    }

    static MiseryOptimizerConfig config() {
        return CONFIG;
    }

    public static boolean slotSyncEnabled() {
        return CONFIG.slotSync;
    }

    static long predictionTimeoutMillis() {
        return PredictionRegistry.timeoutMillis(currentPing(), CONFIG.extraGraceMillis);
    }
}
