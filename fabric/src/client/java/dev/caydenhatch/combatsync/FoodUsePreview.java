package dev.caydenhatch.combatsync;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Ends local eating visuals on the item's normal timer; server food and effects stay authoritative. */
public final class FoodUsePreview {
    private static Pending pending;

    private record Pending(InteractionHand hand, ItemStack item, int duration, int elapsed, long deadline) {
        Pending step() { return new Pending(hand, item, duration, elapsed + 1, deadline); }
    }

    public static void start(LocalPlayer player, InteractionHand hand, ItemStack item) {
        if (!CombatSyncClient.config().foodTiming || !item.has(DataComponents.FOOD)) return;
        int duration = item.getUseDuration(player);
        if (duration <= 0) return;
        pending = new Pending(hand, item.copy(), duration, 0,
            System.nanoTime() / 1_000_000L + duration * 50L
                + CombatSyncClient.predictionTimeoutMillis());
    }

    public static void tick() {
        Pending value = pending;
        if (value == null) return;
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null || System.nanoTime() / 1_000_000L > value.deadline
                || !ItemStack.isSameItem(player.getItemInHand(value.hand), value.item)) {
            pending = null;
            return;
        }
        value = value.step();
        pending = value;
        if (value.elapsed >= value.duration && !player.getUseItem().isEmpty()) {
            player.stopUsingItem();
        }
    }

    public static void confirm() { pending = null; }

    public static void reject(InteractionHand hand) {
        if (pending == null || pending.hand != hand) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && !player.getUseItem().isEmpty()) player.stopUsingItem();
        pending = null;
    }

    public static void clear() { pending = null; }
}
