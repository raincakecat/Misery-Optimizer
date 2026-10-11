package dev.tung.miseryoptimizer.mixin;

import dev.tung.miseryoptimizer.MiseryOptimizerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class GameRendererMixin {
    @Inject(method = "pick", at = @At("TAIL"))
    private void tung$skipPredictedCrystalTarget(float partialTick, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.hitResult instanceof EntityHitResult entityHit
            && MiseryOptimizerClient.hideCrystal(entityHit.getEntity())) {
            minecraft.hitResult = minecraft.player.pick(minecraft.player.blockInteractionRange(), partialTick, false);
            minecraft.crosshairPickEntity = null;
        }
    }
}
