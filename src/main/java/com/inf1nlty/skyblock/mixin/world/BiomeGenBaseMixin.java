package com.inf1nlty.skyblock.mixin.world;

import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.src.BiomeGenBase;
import net.minecraft.src.EntityBat;
import net.minecraft.src.SpawnListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BiomeGenBase.class)
public class BiomeGenBaseMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(int par1, CallbackInfo ci) {
        if (!SkyBlockWorldUtil.isVoidWorldLoaded()) return;

        @SuppressWarnings("unchecked")
        List<SpawnListEntry> caveList = (List<SpawnListEntry>) ((BiomeGenBase)(Object)this).spawnableCaveCreatureList;
        caveList.removeIf(entry -> entry.entityClass == EntityBat.class);
        caveList.add(new SpawnListEntry(EntityBat.class, 2, 2, 2));
    }
}