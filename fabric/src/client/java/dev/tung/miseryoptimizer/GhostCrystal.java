package dev.tung.miseryoptimizer;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.Vec3;

/** Client-only crystal placement visual; it cannot be attacked before server spawn. */
final class GhostCrystal extends EndCrystal {
    private static final Deque<Pending> PENDING = new ArrayDeque<>();
    private static int nextId = -1_900_000_000;

    private record Pending(GhostCrystal ghost, Vec3 origin, long deadline) {}

    private GhostCrystal(ClientLevel level, Vec3 position) {
        super(level, position.x, position.y, position.z);
        setShowBottom(false);
    }

    static void predict(BlockPos crystalBlock) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        Vec3 position = new Vec3(crystalBlock.getX() + 0.5, crystalBlock.getY(), crystalBlock.getZ() + 0.5);
        GhostCrystal crystal = new GhostCrystal(client.level, position);
        crystal.setId(nextId++);
        client.level.addEntity(crystal);
        PENDING.addLast(new Pending(crystal, position,
            System.nanoTime() / 1_000_000L + MiseryOptimizerClient.predictionTimeoutMillis()));
        while (PENDING.size() > 16) remove(PENDING.removeFirst());
    }

    static void confirm(ClientboundAddEntityPacket packet) {
        if (packet.getType() != EntityType.END_CRYSTAL || PENDING.isEmpty()) return;
        Pending closest = null;
        double distance = 0.5 * 0.5;
        Vec3 spawn = new Vec3(packet.getX(), packet.getY(), packet.getZ());
        for (Pending candidate : PENDING) {
            double candidateDistance = candidate.origin.distanceToSqr(spawn);
            if (candidateDistance <= distance) {
                closest = candidate;
                distance = candidateDistance;
            }
        }
        if (closest != null) {
            PENDING.remove(closest);
            remove(closest);
        }
    }

    static void tickPending() {
        ClientLevel level = Minecraft.getInstance().level;
        if (PENDING.stream().anyMatch(candidate -> candidate.ghost.level() != level)) {
            clear();
            return;
        }
        long now = System.nanoTime() / 1_000_000L;
        PENDING.removeIf(candidate -> {
            if (candidate.deadline > now) return false;
            remove(candidate);
            return true;
        });
    }

    static void clear() {
        for (Pending candidate : PENDING) remove(candidate);
        PENDING.clear();
    }

    static void reject(BlockPos pos) {
        Vec3 origin = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        Pending match = PENDING.stream().filter(p -> p.origin.distanceToSqr(origin) < 0.25).findFirst().orElse(null);
        if (match != null) {
            PENDING.remove(match);
            remove(match);
        }
    }

    private static void remove(Pending candidate) {
        Minecraft client = Minecraft.getInstance();
        GhostCrystal ghost = candidate.ghost;
        if (client.level != null && client.level.getEntity(ghost.getId()) == ghost) {
            client.level.removeEntity(ghost.getId(), Entity.RemovalReason.DISCARDED);
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
