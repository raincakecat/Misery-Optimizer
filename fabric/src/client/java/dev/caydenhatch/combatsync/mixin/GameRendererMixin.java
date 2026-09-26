package dev.caydenhatch.combatsync.mixin;

import dev.caydenhatch.combatsync.CombatSyncClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "pick", at = @At("TAIL"))
    private void combatSync$skipPredictedCrystalTarget(float partialTick, CallbackInfo ci) {
        if (minecraft.player != null && minecraft.hitResult instanceof EntityHitResult entityHit
            && CombatSyncClient.hideCrystal(entityHit.getEntity())) {
            minecraft.hitResult = minecraft.player.pick(minecraft.player.blockInteractionRange(), partialTick, false);
            minecraft.crosshairPickEntity = null;
        }
    }
}
