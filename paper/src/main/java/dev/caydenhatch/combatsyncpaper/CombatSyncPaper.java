package dev.caydenhatch.combatsyncpaper;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

/** Sends feedback only; Paper still processes all combat actions and explosions normally. */
public final class CombatSyncPaper extends JavaPlugin implements Listener {
    private static final String CHANNEL = "combatsync:feedback";
    private static final byte VERSION = 2;
    private static final byte EXPLOSION = 1;
    private static final byte REJECT_CRYSTAL_HIT = 2;
    private static final byte REJECT_ANCHOR = 3;
    private static final byte REJECT_PEARL = 4;
    private static final byte REJECT_CRYSTAL_PLACE = 5;
    private static final byte REJECT_BLOCK_PLACE = 6;
    private static final byte REJECT_BLOCK_BREAK = 7;
    private static final byte REJECT_FOOD = 8;
    private int maxBlocks;
    private int maxBytesPerTick;
    private double rangeSquared;
    private boolean explosionFeedback;
    private final Map<UUID, Budget> feedbackBudgets = new HashMap<>();

    private static final class Budget {
        int tick;
        int bytes;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        explosionFeedback = getConfig().getBoolean("explosion-feedback", true);
        maxBlocks = Math.max(1, Math.min(2048, getConfig().getInt("max-explosion-blocks", 2048)));
        maxBytesPerTick = Math.max(1024, Math.min(65536,
            getConfig().getInt("max-feedback-bytes-per-tick", 16384)));
        double range = Math.max(16, Math.min(256, getConfig().getDouble("feedback-range", 96)));
        rangeSquared = range * range;
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onCrystalExplosion(EntityExplodeEvent event) {
        if (!explosionFeedback
                || !(event.getEntity() instanceof EnderCrystal)) return;
        sendExplosion(event.getLocation(), event.isCancelled() ? Collections.emptyList() : event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onAnchorExplosion(BlockExplodeEvent event) {
        if (!explosionFeedback
                || event.getExplodedBlockState().getType() != Material.RESPAWN_ANCHOR) return;
        sendExplosion(event.getBlock().getLocation().add(0.5, 0.5, 0.5),
            event.isCancelled() ? Collections.emptyList() : event.blockList());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onCrystalHit(EntityDamageByEntityEvent event) {
        if (!event.isCancelled() || !(event.getDamager() instanceof Player player)
                || !(event.getEntity() instanceof EnderCrystal)) return;
        sendInt(player, REJECT_CRYSTAL_HIT, event.getEntity().getEntityId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onAnchorUse(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null
                || block.getType() != Material.RESPAWN_ANCHOR
                || event.useInteractedBlock() != Event.Result.DENY) return;
        sendLong(event.getPlayer(), REJECT_ANCHOR, pack(block));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPearlLaunch(ProjectileLaunchEvent event) {
        if (event.isCancelled() && event.getEntity() instanceof EnderPearl pearl
                && pearl.getShooter() instanceof Player player) {
            sendByte(player, REJECT_PEARL);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onCrystalPlace(PlayerInteractEvent event) {
        if (event.useItemInHand() != Event.Result.DENY || event.getItem() == null) return;
        if (event.getItem().getType() == Material.ENDER_PEARL) {
            sendByte(event.getPlayer(), REJECT_PEARL);
        } else if (event.getItem().getType() == Material.END_CRYSTAL
                && event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            Block target = event.getClickedBlock().getRelative(event.getBlockFace());
            sendLong(event.getPlayer(), REJECT_CRYSTAL_PLACE, pack(target));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled() || !event.canBuild()) {
            sendLong(event.getPlayer(), REJECT_BLOCK_PLACE, pack(event.getBlockPlaced()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            sendLong(event.getPlayer(), REJECT_BLOCK_BREAK, pack(event.getBlock()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onFood(PlayerItemConsumeEvent event) {
        if (event.isCancelled()) {
            write(event.getPlayer(), REJECT_FOOD,
                out -> out.writeByte(event.getHand() == EquipmentSlot.OFF_HAND ? 1 : 0));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        feedbackBudgets.remove(event.getPlayer().getUniqueId());
    }

    private void sendExplosion(Location location, List<Block> blocks) {
        if (blocks.size() > maxBlocks) return;
        List<Player> recipients = new ArrayList<>();
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getListeningPluginChannels().contains(CHANNEL)
                    && player.getLocation().distanceSquared(location) <= rangeSquared) {
                recipients.add(player);
            }
        }
        if (recipients.isEmpty()) return;
        int ox = location.getBlockX();
        int oy = location.getBlockY();
        int oz = location.getBlockZ();
        for (Block block : blocks) {
            if (Math.abs(block.getX() - ox) > 127 || Math.abs(block.getY() - oy) > 127
                    || Math.abs(block.getZ() - oz) > 127) return;
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(28 + blocks.size() * 3);
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(VERSION);
            out.writeByte(EXPLOSION);
            out.writeDouble(location.getX());
            out.writeDouble(location.getY());
            out.writeDouble(location.getZ());
            out.writeShort(blocks.size());
            for (Block block : blocks) {
                out.writeByte(block.getX() - ox);
                out.writeByte(block.getY() - oy);
                out.writeByte(block.getZ() - oz);
            }
            byte[] message = bytes.toByteArray();
            for (Player player : recipients) {
                if (allowExplosionFeedback(player, message.length)) {
                    player.sendPluginMessage(this, CHANNEL, message);
                }
            }
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private void sendInt(Player player, byte kind, int value) {
        write(player, kind, out -> out.writeInt(value));
    }

    private void sendLong(Player player, byte kind, long value) {
        write(player, kind, out -> out.writeLong(value));
    }

    private void sendByte(Player player, byte kind) {
        write(player, kind, out -> {});
    }

    private interface Body { void write(DataOutputStream out) throws IOException; }

    private void write(Player player, byte kind, Body body) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(16);
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(VERSION);
            out.writeByte(kind);
            body.write(out);
            send(player, bytes.toByteArray());
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private void send(Player player, byte[] bytes) {
        if (player.getListeningPluginChannels().contains(CHANNEL)) {
            player.sendPluginMessage(this, CHANNEL, bytes);
        }
    }

    private boolean allowExplosionFeedback(Player player, int size) {
        Budget budget = feedbackBudgets.computeIfAbsent(player.getUniqueId(), ignored -> new Budget());
        int tick = Bukkit.getCurrentTick();
        if (budget.tick != tick) {
            budget.tick = tick;
            budget.bytes = 0;
        }
        if (budget.bytes + size > maxBytesPerTick) return false;
        budget.bytes += size;
        return true;
    }

    private static long pack(Block block) {
        return ((long) block.getX() & 0x3FFFFFFL) << 38
            | ((long) block.getZ() & 0x3FFFFFFL) << 12
            | ((long) block.getY() & 0xFFFL);
    }
}
