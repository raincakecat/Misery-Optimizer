package dev.tung.miseryoptimizer.mixin;

import dev.tung.miseryoptimizer.MiseryOptimizerClient;
import dev.tung.miseryoptimizer.BlockActionPreview;
import dev.tung.miseryoptimizer.FoodUsePreview;
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
    @Unique private boolean tung$usingPearl;
    @Unique private boolean tung$placingCrystal;
    @Unique private BlockPos tung$anchorPos;

    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void tung$syncSlotBeforeMining(BlockPos pos, net.minecraft.core.Direction direction,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if (minecraft.player == null || !MiseryOptimizerClient.slotSyncEnabled()) return;
        int selected = minecraft.player.getInventory().getSelectedSlot();
        if (selected != carriedIndex) {
            carriedIndex = selected;
            connection.send(new ServerboundSetCarriedItemPacket(selected));
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"))
    private void tung$recordPearl(Player player, InteractionHand hand,
                                        CallbackInfoReturnable<InteractionResult> cir) {
        tung$usingPearl = player.getItemInHand(hand).is(Items.ENDER_PEARL);
    }

    @Inject(method = "useItem", at = @At("RETURN"))
    private void tung$showPearl(Player player, InteractionHand hand,
                                      CallbackInfoReturnable<InteractionResult> cir) {
        if (tung$usingPearl && cir.getReturnValue().consumesAction()) {
            MiseryOptimizerClient.predictPearl();
        }
        if (player instanceof LocalPlayer localPlayer && cir.getReturnValue().consumesAction()) {
            FoodUsePreview.start(localPlayer, hand, player.getItemInHand(hand));
        }
        tung$usingPearl = false;
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void tung$attack(Player player, Entity target, CallbackInfo ci) {
        if (!player.isSpectator() && target instanceof EndCrystal crystal && !crystal.isRemoved()) {
            MiseryOptimizerClient.predictCrystal(crystal);
        }
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void tung$anchorUse(LocalPlayer player, InteractionHand hand,
                                      BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        BlockActionPreview.endPlacement(cir.getReturnValue().consumesAction());
        if (tung$placingCrystal && !player.isSpectator() && cir.getReturnValue().consumesAction()) {
            MiseryOptimizerClient.predictPlacedCrystal(hit.getBlockPos().above());
        }
        tung$placingCrystal = false;
        if (tung$anchorPos != null && !player.isSpectator() && cir.getReturnValue().consumesAction()) {
            MiseryOptimizerClient.predictAnchor(tung$anchorPos);
        }
        tung$anchorPos = null;
    }

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void tung$recordCrystalPlacement(LocalPlayer player, InteractionHand hand,
                                                  BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (minecraft.level != null && player.getItemInHand(hand).getItem() instanceof BlockItem) {
            BlockActionPreview.beginPlacement(minecraft.level);
        }
        tung$placingCrystal = player.getItemInHand(hand).is(Items.END_CRYSTAL);
        tung$anchorPos = null;
        if (minecraft.level == null || player.isSpectator()
            || minecraft.level.dimension() != Level.OVERWORLD && minecraft.level.dimension() != Level.END) return;
        BlockState state = minecraft.level.getBlockState(hit.getBlockPos());
        if (!(state.getBlock() instanceof RespawnAnchorBlock)) return;
        int charges = state.getValue(RespawnAnchorBlock.CHARGE);
        if (charges > 0 && !(charges < RespawnAnchorBlock.MAX_CHARGES
            && player.getItemInHand(hand).is(Items.GLOWSTONE))) {
            tung$anchorPos = hit.getBlockPos().immutable();
        }
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void tung$beforeBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (minecraft.level != null) BlockActionPreview.beginBreak(minecraft.level, pos);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void tung$afterBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BlockActionPreview.endBreak(pos, cir.getReturnValue());
    }
}
