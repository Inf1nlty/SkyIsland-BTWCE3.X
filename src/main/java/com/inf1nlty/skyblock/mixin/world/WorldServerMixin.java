package com.inf1nlty.skyblock.mixin.world;

import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.src.WorldServer;
import net.minecraft.src.WorldSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldServer.class)
public abstract class WorldServerMixin {

    @Inject(method = "initialize", at = @At("TAIL"))
    private void forceVoidWorldOriginalSpawn(WorldSettings settings, CallbackInfo ci) {
        if (SkyBlockWorldUtil.isVoidWorldLoaded()) {
            ((WorldServer)(Object)this).getWorldInfo().setSpawnPosition(0, 101, 0);
        }
    }
}