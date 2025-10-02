package com.inf1nlty.skyblock.mixin.world.entity;

import btw.util.hardcorespawn.HardcoreSpawnUtils;
import com.inf1nlty.skyblock.util.SkyBlockDataManager;
import com.inf1nlty.skyblock.util.SkyBlockPoint;
import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.WorldServer;
import net.minecraft.src.ChunkCoordinates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HardcoreSpawnUtils.class)
public abstract class HardcoreSpawnUtilsMixin {

    @Inject(method = "handleHardcoreSpawn", at = @At("HEAD"), cancellable = true)
    private static void forceVoidWorldRespawn(MinecraftServer server, EntityPlayerMP oldPlayer, EntityPlayerMP newPlayer, CallbackInfo ci) {

        WorldServer mainWorld = server.worldServerForDimension(0);
        if (!SkyBlockWorldUtil.isVoidWorldLoaded()) {
            return;
        }

        SkyBlockPoint island = SkyBlockDataManager.getIsland(newPlayer);
        if (island == null) {
            island = SkyBlockDataManager.getIslandForMember(newPlayer);
        }

        if (island != null) {
            if (newPlayer.dimension != island.dim) {
                newPlayer.mcServer.getConfigurationManager().transferPlayerToDimension(newPlayer, island.dim);
            }
            newPlayer.setLocationAndAngles(island.initSpawnX + 0.5, island.initSpawnY + 0.5, island.initSpawnZ + 0.5, 0.0f, 0.0f);

        } else
        {
            ChunkCoordinates spawn = mainWorld.getSpawnPoint();
            if (newPlayer.dimension != 0) {
                newPlayer.mcServer.getConfigurationManager().transferPlayerToDimension(newPlayer, 0);
            }
            newPlayer.setLocationAndAngles(spawn.posX + 0.5, spawn.posY + 0.5, spawn.posZ + 0.5, 0.0f, 0.0f);
        }

        newPlayer.setTimeOfLastSpawnAssignment(0L);
        newPlayer.hardcoreSpawnChunk = null;
        ci.cancel();
    }

}