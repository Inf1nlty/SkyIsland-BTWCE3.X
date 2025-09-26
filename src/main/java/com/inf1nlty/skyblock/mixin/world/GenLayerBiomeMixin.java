package com.inf1nlty.skyblock.mixin.world;

import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.src.GenLayerBiome;
import net.minecraft.src.BiomeGenBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GenLayerBiome.class)
public class GenLayerBiomeMixin {

    @Inject(method = "getInts", at = @At("RETURN"), cancellable = true)
    private void injectVoidWorldBiomes(int x, int z, int width, int depth, CallbackInfoReturnable<int[]> cir) {
        if (!SkyBlockWorldUtil.isVoidWorldLoaded()) return;

        int[] biomes = cir.getReturnValue();

        for (int i = 0; i < biomes.length; i++) {

            int id = biomes[i];

            if (id == BiomeGenBase.icePlains.biomeID
                    || id == BiomeGenBase.iceMountains.biomeID
                    || id == BiomeGenBase.extremeHills.biomeID
                    || id == BiomeGenBase.extremeHillsEdge.biomeID)
            {
                biomes[i] = BiomeGenBase.jungle.biomeID;
            }
            else if (id == BiomeGenBase.ocean.biomeID
                    || id == BiomeGenBase.river.biomeID
                    || id == BiomeGenBase.frozenRiver.biomeID
                    || id == BiomeGenBase.frozenOcean.biomeID)
            {
                biomes[i] = BiomeGenBase.swampland.biomeID;
            }
        }
        cir.setReturnValue(biomes);
    }
}