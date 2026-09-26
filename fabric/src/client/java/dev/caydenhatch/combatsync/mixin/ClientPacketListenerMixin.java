package dev.caydenhatch.combatsync.mixin;

import dev.caydenhatch.combatsync.CombatSyncClient;
import dev.caydenhatch.combatsync.FoodUsePreview;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Unique private boolean combatSync$suppressAnchorEffect;
    @Unique private boolean combatSync$suppressCrystalEffect;

    @Inject(method = "handleRemoveEntities", at = @At("TAIL"))
    private void combatSync$confirmedRemoval(ClientboundRemoveEntitiesPacket packet, CallbackInfo ci) {
        packet.getEntityIds().forEach(CombatSyncClient::confirmCrystal);
    }

    @Inject(method = "handleAddEntity", at = @At("TAIL"))
    private void combatSync$entityIdReused(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        CombatSyncClient.confirmCrystal(packet.getId());
        CombatSyncClient.confirmPearl(packet);
        CombatSyncClient.confirmPlacedCrystal(packet);
    }

    @Inject(method = "handleBlockUpdate", at = @At("TAIL"))
    private void combatSync$confirmBlock(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        CombatSyncClient.confirmBlock(packet.getPos());
    }

    @Inject(method = "handleChunkBlocksUpdate", at = @At("TAIL"))
    private void combatSync$confirmSection(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
        packet.runUpdates((pos, state) -> CombatSyncClient.confirmBlock(pos));
    }

    @Inject(method = "handleLevelChunkWithLight", at = @At("TAIL"))
    private void combatSync$confirmChunk(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
        CombatSyncClient.confirmChunk(packet.getX(), packet.getZ());
    }

    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void combatSync$confirmFood(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (packet.getEventId() == 9 && client.level != null
                && packet.getEntity(client.level) == client.player) {
            FoodUsePreview.confirm();
        }
    }

    @Inject(method = "handleExplosion", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"))
    private void combatSync$matchAnchor(ClientboundExplodePacket packet, CallbackInfo ci) {
        combatSync$suppressAnchorEffect = packet.radius() == 5.0F
            && CombatSyncClient.predictedAnchorExplosion(packet.center());
        combatSync$suppressCrystalEffect = packet.radius() == 6.0F
            && CombatSyncClient.predictedCrystalExplosion(packet.center());
    }

    @Redirect(method = "handleExplosion", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"))
    private void combatSync$avoidDuplicateSound(ClientLevel level, double x, double y, double z,
            SoundEvent sound, SoundSource source, float volume, float pitch, boolean distanceDelay) {
        if (!combatSync$suppressAnchorEffect && !combatSync$suppressCrystalEffect) {
            level.playLocalSound(x, y, z, sound, source, volume, pitch, distanceDelay);
        }
    }

    @Redirect(method = "handleExplosion", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void combatSync$avoidDuplicateParticle(ClientLevel level, ParticleOptions particle,
            double x, double y, double z, double dx, double dy, double dz) {
        if (!combatSync$suppressAnchorEffect && !combatSync$suppressCrystalEffect) {
            level.addParticle(particle, x, y, z, dx, dy, dz);
        }
    }

    @Inject(method = "handleExplosion", at = @At("TAIL"))
    private void combatSync$confirmAnchor(ClientboundExplodePacket packet, CallbackInfo ci) {
        if (combatSync$suppressAnchorEffect) CombatSyncClient.confirmAnchorExplosion(packet.center());
        if (combatSync$suppressCrystalEffect) CombatSyncClient.confirmCrystalExplosion(packet.center());
        combatSync$suppressAnchorEffect = false;
        combatSync$suppressCrystalEffect = false;
    }
}
