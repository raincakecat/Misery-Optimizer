package dev.caydenhatch.combatsync;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A visual projectile only. Teleportation always belongs to the server. */
final class GhostPearl extends ThrownEnderpearl {
    private static final Deque<Pending> PENDING = new ArrayDeque<>();
    private static int nextId = -2_000_000_000;

    private static final class Pending {
        final GhostPearl ghost;
        final Vec3 origin;
        final long deadline;
        int serverEntityId = Integer.MIN_VALUE;
        int blendTicks;

        Pending(GhostPearl ghost, Vec3 origin, long deadline) {
            this.ghost = ghost;
            this.origin = origin;
            this.deadline = deadline;
        }
    }

    private GhostPearl(ClientLevel level) {
        super(EntityType.ENDER_PEARL, level);
    }

    static void predict() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;
        GhostPearl pearl = new GhostPearl(client.level);
        pearl.setId(nextId++);
        pearl.setOwner(client.player);
        pearl.setItem(new ItemStack(Items.ENDER_PEARL));
        pearl.setPos(client.player.getX(), client.player.getEyeY() - 0.1F, client.player.getZ());
        pearl.shootFromRotation(client.player, client.player.getXRot(), client.player.getYRot(),
            0.0F, 1.5F, 1.0F);
        client.level.addEntity(pearl);
        PENDING.addLast(new Pending(pearl, pearl.position(), System.nanoTime() / 1_000_000L
            + CombatSyncClient.predictionTimeoutMillis()));
        while (PENDING.size() > 16) remove(PENDING.removeFirst());
    }

    static void confirm(ClientboundAddEntityPacket packet) {
        Minecraft client = Minecraft.getInstance();
        if (packet.getType() == EntityType.ENDER_PEARL && client.player != null
            && packet.getData() == client.player.getId()) {
            Pending closest = null;
            double distance = Double.MAX_VALUE;
            Vec3 spawn = new Vec3(packet.getX(), packet.getY(), packet.getZ());
            for (Pending candidate : PENDING) {
                double candidateDistance = candidate.origin.distanceToSqr(spawn);
                if (candidateDistance < distance) {
                    closest = candidate;
                    distance = candidateDistance;
                }
            }
            if (closest != null) {
                int ticks = CombatSyncClient.config().pearlHandoffTicks;
                if (ticks == 0 || closest.ghost.isRemoved()) {
                    PENDING.remove(closest);
                    remove(closest);
                } else {
                    closest.serverEntityId = packet.getId();
                    closest.blendTicks = ticks;
                }
            }
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
            if (candidate.serverEntityId != Integer.MIN_VALUE) {
                Entity serverPearl = level == null ? null : level.getEntity(candidate.serverEntityId);
                if (serverPearl == null || serverPearl.isRemoved() || candidate.ghost.isRemoved()) {
                    remove(candidate);
                    return true;
                }
                candidate.ghost.setDeltaMovement(Vec3.ZERO);
                candidate.ghost.setPos(candidate.ghost.position().lerp(serverPearl.position(),
                    1.0 / Math.max(1, candidate.blendTicks)));
                if (--candidate.blendTicks <= 0) {
                    remove(candidate);
                    return true;
                }
                return false;
            }
            if (candidate.deadline > now) return false;
            remove(candidate);
            return true;
        });
    }

    static boolean hideServerPearl(Entity entity) {
        if (entity.getType() != EntityType.ENDER_PEARL) return false;
        for (Pending candidate : PENDING) {
            if (candidate.serverEntityId == entity.getId() && candidate.blendTicks > 0) return true;
        }
        return false;
    }

    static void clear() {
        for (Pending candidate : PENDING) remove(candidate);
        PENDING.clear();
    }

    static void rejectOldest() {
        if (!PENDING.isEmpty()) remove(PENDING.removeFirst());
    }

    private static void remove(Pending candidate) {
        Minecraft client = Minecraft.getInstance();
        GhostPearl ghost = candidate.ghost;
        if (client.level != null && client.level.getEntity(ghost.getId()) == ghost) {
            client.level.removeEntity(ghost.getId(), Entity.RemovalReason.DISCARDED);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        // A visual pearl must not invoke even the zero-damage client hit path.
    }

    @Override
    protected void onHit(HitResult hit) {
        if (PENDING.stream().noneMatch(candidate -> candidate.ghost == this
                && candidate.serverEntityId != Integer.MIN_VALUE)) {
            discard();
        }
    }
}
