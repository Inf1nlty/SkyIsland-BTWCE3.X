package com.inf1nlty.skyblock.mixin;

import net.minecraft.src.WorldServer;
import net.minecraft.src.WorldSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldServer.class)
public class WorldServerMixin {

    @Inject(method = "initialize", at = @At("TAIL"))
    private void forceVoidWorldOriginalSpawn(WorldSettings settings, CallbackInfo ci) {
        WorldServer ws = (WorldServer)(Object)this;
        if ("voidworld".equals(ws.getWorldInfo().getGeneratorOptions())) {
            ws.getWorldInfo().setSpawnPosition(0, 101, 0);
        }
    }
}