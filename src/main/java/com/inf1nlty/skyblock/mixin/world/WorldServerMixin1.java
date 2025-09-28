package com.inf1nlty.skyblock.mixin.world;

import com.inf1nlty.skyblock.SkyblockConfig;
import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.src.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(WorldServer.class)
public abstract class WorldServerMixin1 {

    @ModifyArg(method= "tick", at=@At(value="INVOKE", target="Lnet/minecraft/src/SpawnerAnimals;findChunksForSpawning(Lnet/minecraft/src/WorldServer;ZZZ)I"), index=3)
    public boolean allowSpawnAnimal(boolean spawnAnimal) {
        return (SkyblockConfig.ALLOW_ANIMAL_SPAWN_ON_GRASSSLAB && SkyBlockWorldUtil.isVoidWorldLoaded()) || spawnAnimal;
    }
}