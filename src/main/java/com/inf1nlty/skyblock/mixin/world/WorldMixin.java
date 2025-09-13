package com.inf1nlty.skyblock.mixin.world;

import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.src.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldMixin {
    @Inject(method = "getHorizon", at = @At("HEAD"), cancellable = true)
    private void patchGetHorizon(CallbackInfoReturnable<Double> cir) {
        if (SkyBlockWorldUtil.isVoidWorldLoaded()) {
            cir.setReturnValue(0.0D);
        }
    }
}