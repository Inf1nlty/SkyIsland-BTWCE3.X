package com.inf1nlty.skyblock.mixin.world;

import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.src.EntityZombie;
import net.minecraft.src.SpawnListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import btw.world.structure.NetherBridgeMapGen;

import java.util.List;

@Mixin(NetherBridgeMapGen.class)
public class NetherBridgeMapGenMixin {

    @SuppressWarnings("unchecked")
    @Inject(method = "<init>", at = @At("TAIL"))
    private void addZombieToNetherFortressSpawnList(CallbackInfo ci) {
        if (SkyBlockWorldUtil.isVoidWorldLoaded()) {
            List<SpawnListEntry> spawnList = ((NetherBridgeMapGen) (Object) this).getSpawnList();
            if (spawnList != null) {
                spawnList.add(new SpawnListEntry(EntityZombie.class, 3, 1, 1));
            }
        }
    }
}