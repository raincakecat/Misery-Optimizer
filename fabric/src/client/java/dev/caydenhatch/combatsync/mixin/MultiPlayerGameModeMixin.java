package dev.caydenhatch.combatsync.mixin;

import dev.caydenhatch.combatsync.CombatSyncClient;
import dev.caydenhatch.combatsync.BlockActionPreview;
import dev.caydenhatch.combatsync.FoodUsePreview;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private ClientPacketListener connection;
    @Shadow private int carriedIndex;
    @Unique private boolean combatSync$usingPearl;
    @Unique private boolean combatSync$placingCrystal;
    @Unique private BlockPos combatSync$anchorPos;

    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void combatSync$syncSlotBeforeMining(BlockPos pos, net.minecraft.core.Direction direction,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if (minecraft.player == null || !CombatSyncClient.slotSyncEnabled()) return;
        int selected = minecraft.player.getInventory().getSelectedSlot();
        if (selected != carriedIndex) {
            carriedIndex = selected;
            connection.send(new ServerboundSetCarriedItemPacket(selected));
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"))
    private void combatSync$recordPearl(Player player, InteractionHand hand,
                                        CallbackInfoReturnable<InteractionResult> cir) {
        combatSync$usingPearl = player.getItemInHand(hand).is(Items.ENDER_PEARL);
    }

    @Inject(method = "useItem", at = @At("RETURN"))
    private void combatSync$showPearl(Player player, InteractionHand hand,
                                      CallbackInfoReturnable<InteractionResult> cir) {
        if (combatSync$usingPearl && cir.getReturnValue().consumesAction()) {
            CombatSyncClient.predictPearl();
        }
        if (player instanceof LocalPlayer localPlayer && cir.getReturnValue().consumesAction()) {
            FoodUsePreview.start(localPlayer, hand, player.getItemInHand(hand));
        }
        combatSync$usingPearl = false;
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void combatSync$attack(Player player, Entity target, CallbackInfo ci) {
        if (!player.isSpectator() && target instanceof EndCrystal crystal && !crystal.isRemoved()) {
            CombatSyncClient.predictCrystal(crystal);
        }
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void combatSync$anchorUse(LocalPlayer player, InteractionHand hand,
                                      BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        BlockActionPreview.endPlacement(cir.getReturnValue().consumesAction());
        if (combatSync$placingCrystal && !player.isSpectator() && cir.getReturnValue().consumesAction()) {
            CombatSyncClient.predictPlacedCrystal(hit.getBlockPos().above());
        }
        combatSync$placingCrystal = false;
        if (combatSync$anchorPos != null && !player.isSpectator() && cir.getReturnValue().consumesAction()) {
            CombatSyncClient.predictAnchor(combatSync$anchorPos);
        }
        combatSync$anchorPos = null;
    }

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void combatSync$recordCrystalPlacement(LocalPlayer player, InteractionHand hand,
                                                  BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (minecraft.level != null && player.getItemInHand(hand).getItem() instanceof BlockItem) {
            BlockActionPreview.beginPlacement(minecraft.level);
        }
        combatSync$placingCrystal = player.getItemInHand(hand).is(Items.END_CRYSTAL);
        combatSync$anchorPos = null;
        if (minecraft.level == null || player.isSpectator()
            || minecraft.level.dimension() != Level.OVERWORLD && minecraft.level.dimension() != Level.END) return;
        BlockState state = minecraft.level.getBlockState(hit.getBlockPos());
        if (!(state.getBlock() instanceof RespawnAnchorBlock)) return;
        int charges = state.getValue(RespawnAnchorBlock.CHARGE);
        if (charges > 0 && !(charges < RespawnAnchorBlock.MAX_CHARGES
            && player.getItemInHand(hand).is(Items.GLOWSTONE))) {
            combatSync$anchorPos = hit.getBlockPos().immutable();
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void combatSync$beforeBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (minecraft.level != null) BlockActionPreview.beginBreak(minecraft.level, pos);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void combatSync$afterBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockActionPreview.endBreak(pos, cir.getReturnValue());
    }
}
