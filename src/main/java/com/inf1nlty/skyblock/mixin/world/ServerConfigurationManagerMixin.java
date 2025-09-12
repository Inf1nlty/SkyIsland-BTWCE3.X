package com.inf1nlty.skyblock.mixin.world;

import com.inf1nlty.skyblock.SkyblockConfig;
import com.inf1nlty.skyblock.util.SkyBlockDataManager;
import com.inf1nlty.skyblock.util.SkyBlockPoint;
import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerConfigurationManager.class)
public abstract class ServerConfigurationManagerMixin {

    @Inject(method = "respawnPlayer", at = @At("RETURN"))
    private void forceVoidWorldRespawn(EntityPlayerMP oldPlayer, int iDefaultDimension, boolean playerLeavingTheEnd, CallbackInfoReturnable<EntityPlayerMP> cir) {
        EntityPlayerMP newPlayer = cir.getReturnValue();
        WorldServer world = newPlayer.mcServer.worldServerForDimension(newPlayer.dimension);
        if (SkyBlockWorldUtil.isVoidWorld(world) && newPlayer.dimension == 0) {
            SkyBlockPoint island = SkyBlockDataManager.getIsland(newPlayer);
            if (island == null) {
                island = SkyBlockDataManager.getIslandForMember(newPlayer);
            }
            if (island != null) {
                newPlayer.setLocationAndAngles(island.initSpawnX + 0.5, island.initSpawnY + 0.5, island.initSpawnZ + 0.5, 0.0F, 0.0F);
            } else {
                newPlayer.setLocationAndAngles(0.5, 101.0, 0.5, 0.0F, 0.0F);
            }
            newPlayer.setHealth(10.0F);
            newPlayer.foodStats.setFoodLevel(30);
            newPlayer.playerNetServerHandler.setPlayerLocation(newPlayer.posX, newPlayer.posY, newPlayer.posZ, newPlayer.rotationYaw, newPlayer.rotationPitch);
        }
    }

    @Invoker("flagChunksAroundTeleportingEntityForCheckForUnload")

    public abstract void invokeFlagChunksAroundTeleportingEntityForCheckForUnload(WorldServer world, Entity entity);

    /**
     * Makes Nether portals use a 1:1 coordinate ratio instead of 8:1.
     */
    @Inject(method = "transferEntityToWorld", at = @At("HEAD"), cancellable = true)
    private void makePortal1to1(Entity entity, int fromDim, WorldServer oldWorld, WorldServer newWorld, CallbackInfo ci) {
        if (SkyblockConfig.OVERWORLD_NETHER_COORD_RATIO_1_1 && SkyBlockWorldUtil.isVoidWorldLoaded()) {
            if ((entity.dimension == -1 && fromDim == 0) || (entity.dimension == 0 && fromDim == -1)) {
                oldWorld.theProfiler.startSection("moving");
                if (entity.isEntityAlive()) {
                    oldWorld.updateEntityWithOptionalForce(entity, false);
                }
                oldWorld.theProfiler.endSection();
                oldWorld.theProfiler.startSection("placing");
                double x = MathHelper.clamp_int((int)entity.posX, -29999872, 29999872);
                double z = MathHelper.clamp_int((int)entity.posZ, -29999872, 29999872);
                if (entity.isEntityAlive()) {
                    entity.setLocationAndAngles(x, entity.posY, z, entity.rotationYaw, entity.rotationPitch);
                    this.invokeFlagChunksAroundTeleportingEntityForCheckForUnload(newWorld, entity);
                    newWorld.getDefaultTeleporter().placeInPortal(entity, entity.posX, entity.posY, entity.posZ, entity.rotationYaw);
                    newWorld.spawnEntityInWorld(entity);
                    newWorld.updateEntityWithOptionalForce(entity, false);
                }
                oldWorld.theProfiler.endSection();
                entity.setWorld(newWorld);
                ci.cancel();
            }
        }
    }

}