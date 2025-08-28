package com.inf1nlty.skyblock.mixin;

import com.inf1nlty.skyblock.util.SkyBlockDataManager;
import com.inf1nlty.skyblock.util.SkyBlockPoint;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.ServerConfigurationManager;
import net.minecraft.src.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerConfigurationManager.class)
public class ServerConfigurationManagerMixin {

    @Inject(method = "respawnPlayer", at = @At("RETURN"))
    private void forceVoidWorldRespawn(EntityPlayerMP oldPlayer, int iDefaultDimension, boolean playerLeavingTheEnd, CallbackInfoReturnable<EntityPlayerMP> cir) {
        EntityPlayerMP newPlayer = cir.getReturnValue();
        WorldServer world = newPlayer.mcServer.worldServerForDimension(newPlayer.dimension);
        if ("voidworld".equals(world.getWorldInfo().getGeneratorOptions()) && newPlayer.dimension == 0) {
            SkyBlockPoint island = SkyBlockDataManager.getIsland(newPlayer);
            if (island == null) {
                island = SkyBlockDataManager.getIslandForMember(newPlayer);
            }
            if (island != null) {
                newPlayer.setLocationAndAngles(island.initSpawnX + 0.5, island.initSpawnY + 0.5, island.initSpawnZ + 0.5, 0.0F, 0.0F);
            } else {
                newPlayer.setLocationAndAngles(0.5, 101.0, 0.5, 0.0F, 0.0F);
            }
            newPlayer.playerNetServerHandler.setPlayerLocation(newPlayer.posX, newPlayer.posY, newPlayer.posZ, newPlayer.rotationYaw, newPlayer.rotationPitch);
        }
    }
}