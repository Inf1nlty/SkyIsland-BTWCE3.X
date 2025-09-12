package com.inf1nlty.skyblock.mixin.world;

import com.inf1nlty.skyblock.SkyblockConfig;
import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.src.WorldServer;
import net.minecraft.src.WorldSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldServer.class)
public class WorldServerMixin {

    @Inject(method = "initialize", at = @At("TAIL"))
    private void forceVoidWorldOriginalSpawn(WorldSettings settings, CallbackInfo ci) {
        if (SkyBlockWorldUtil.isVoidWorldLoaded()) {
            ((WorldServer)(Object)this).getWorldInfo().setSpawnPosition(0, 101, 0);
        }
    }

    @ModifyArg(method= "tick", at=@At(value="INVOKE", target="Lnet/minecraft/src/SpawnerAnimals;findChunksForSpawning(Lnet/minecraft/src/WorldServer;ZZZ)I"), index=3)
    public boolean allowSpawnAnimal(boolean spawnAnimal) {
        return (SkyblockConfig.ALLOW_ANIMAL_SPAWN_ON_GRASSSLAB && SkyBlockWorldUtil.isVoidWorldLoaded()) || spawnAnimal;
    }

}