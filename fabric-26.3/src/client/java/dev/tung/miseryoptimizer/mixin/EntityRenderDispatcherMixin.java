package dev.tung.miseryoptimizer.mixin;

import dev.tung.miseryoptimizer.MiseryOptimizerClient;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void tung$hidePredictedCrystal(E entity, Frustum frustum,
            double x, double y, double z, float partialTick, CallbackInfoReturnable<Boolean> cir) {
        if (MiseryOptimizerClient.hideCrystal(entity) || MiseryOptimizerClient.hideServerPearl(entity)) {
            cir.setReturnValue(false);
        }
    }
}
