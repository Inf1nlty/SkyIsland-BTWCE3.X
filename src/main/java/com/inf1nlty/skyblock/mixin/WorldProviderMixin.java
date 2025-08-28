package com.inf1nlty.skyblock.mixin;

import com.inf1nlty.skyblock.VoidWorldChunkProvider;
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
        if ("voidworld".equals(world.getWorldInfo().getGeneratorOptions())) {
            cir.setReturnValue(new VoidWorldChunkProvider(world, world.getSeed(), world.getWorldInfo().isMapFeaturesEnabled()));
        }
    }
}