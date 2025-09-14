package com.inf1nlty.skyblock.mixin.world;

import com.inf1nlty.skyblock.util.VoidWorldChunkProvider;
import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.src.World;
import net.minecraft.src.WorldProvider;
import net.minecraft.src.IChunkProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldProvider.class)
public class WorldProviderMixin {

    @Inject(method = "createChunkGenerator", at = @At("RETURN"), cancellable = true)
    private void onCreateChunkGenerator(CallbackInfoReturnable<IChunkProvider> cir) {
        WorldProvider self = (WorldProvider) (Object) this;
        World world = self.worldObj;
        if (SkyBlockWorldUtil.isVoidWorld(world)) {
            cir.setReturnValue(new VoidWorldChunkProvider(world, world.getSeed(), world.getWorldInfo().isMapFeaturesEnabled()));
        }
    }

    @Inject(method = "getWorldHasVoidParticles", at = @At("HEAD"), cancellable = true)
    private void patchVoidParticles(CallbackInfoReturnable<Boolean> cir) {
        if (SkyBlockWorldUtil.isVoidWorldLoaded()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getVoidFogYFactor", at = @At("HEAD"), cancellable = true)
    private void patchVoidFogYFactor(CallbackInfoReturnable<Double> cir) {
        if (SkyBlockWorldUtil.isVoidWorldLoaded()) {
            cir.setReturnValue(1.0D);
        }
    }

}