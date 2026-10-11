package dev.tung.miseryoptimizer.mixin;

import dev.tung.miseryoptimizer.MiseryOptimizerClient;
import dev.tung.miseryoptimizer.FoodUsePreview;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Unique private boolean tung$suppressAnchorEffect;
    @Unique private boolean tung$suppressCrystalEffect;

    @Inject(method = "handleRemoveEntities", at = @At("TAIL"))
    private void tung$confirmedRemoval(ClientboundRemoveEntitiesPacket packet, CallbackInfo ci) {
        packet.getEntityIds().forEach(MiseryOptimizerClient::confirmCrystal);
    }

    @Inject(method = "handleAddEntity", at = @At("TAIL"))
    private void tung$entityIdReused(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        MiseryOptimizerClient.confirmCrystal(packet.getId());
        MiseryOptimizerClient.confirmPearl(packet);
        MiseryOptimizerClient.confirmPlacedCrystal(packet);
    }

    @Inject(method = "handleBlockUpdate", at = @At("TAIL"))
    private void tung$confirmBlock(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        MiseryOptimizerClient.confirmBlock(packet.getPos());
    }

    @Inject(method = "handleChunkBlocksUpdate", at = @At("TAIL"))
    private void tung$confirmSection(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
        packet.runUpdates((pos, state) -> MiseryOptimizerClient.confirmBlock(pos));
    }

    @Inject(method = "handleLevelChunkWithLight", at = @At("TAIL"))
    private void tung$confirmChunk(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
        MiseryOptimizerClient.confirmChunk(packet.getX(), packet.getZ());
    }

    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void tung$confirmFood(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (packet.getEventId() == 9 && client.level != null
                && packet.getEntity(client.level) == client.player) {
            FoodUsePreview.confirm();
        }
    }

    @Inject(method = "handleExplosion", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"))
    private void tung$matchAnchor(ClientboundExplodePacket packet, CallbackInfo ci) {
        tung$suppressAnchorEffect = packet.radius() == 5.0F
            && MiseryOptimizerClient.predictedAnchorExplosion(packet.center());
        tung$suppressCrystalEffect = packet.radius() == 6.0F
            && MiseryOptimizerClient.predictedCrystalExplosion(packet.center());
    }

    @WrapOperation(method = "handleExplosion", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"))
    private void tung$avoidDuplicateSound(ClientLevel level, double x, double y, double z,
            SoundEvent sound, SoundSource source, float volume, float pitch, boolean distanceDelay,
            Operation<Void> original) {
        if (!tung$suppressAnchorEffect && !tung$suppressCrystalEffect) {
            original.call(level, x, y, z, sound, source, volume, pitch, distanceDelay);
        }
    }

    @WrapOperation(method = "handleExplosion", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void tung$avoidDuplicateParticle(ClientLevel level, ParticleOptions particle,
            double x, double y, double z, double dx, double dy, double dz,
            Operation<Void> original) {
        if (!tung$suppressAnchorEffect && !tung$suppressCrystalEffect) {
            original.call(level, particle, x, y, z, dx, dy, dz);
        }
    }

    @Inject(method = "handleExplosion", at = @At("TAIL"))
    private void tung$confirmAnchor(ClientboundExplodePacket packet, CallbackInfo ci) {
        if (tung$suppressAnchorEffect) MiseryOptimizerClient.confirmAnchorExplosion(packet.center());
        if (tung$suppressCrystalEffect) MiseryOptimizerClient.confirmCrystalExplosion(packet.center());
        tung$suppressAnchorEffect = false;
        tung$suppressCrystalEffect = false;
    }
}
