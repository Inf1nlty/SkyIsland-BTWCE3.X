package com.inf1nlty.skyblock.mixin.entity;

import com.inf1nlty.skyblock.SkyblockConfig;
import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.Entity;
import net.minecraft.src.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "travelToDimension", at = @At("HEAD"), cancellable = true)
    private void forceSkyblockTravel(int dimension, CallbackInfo ci) {
        Entity entity = (Entity)(Object)this;
        if (SkyblockConfig.OVERWORLD_NETHER_COORD_RATIO_1_1 && SkyBlockWorldUtil.isVoidWorldLoaded()) {
            if ((entity.dimension == -1 && dimension == 0) || (entity.dimension == 0 && dimension == -1)) {
                MinecraftServer server = MinecraftServer.getServer();
                WorldServer newWorld = server.worldServerForDimension(dimension);
                entity.dimension = dimension;
                double x = entity.posX;
                double y = entity.posY;
                double z = entity.posZ;
                entity.setLocationAndAngles(x, y, z, entity.rotationYaw, entity.rotationPitch);
                newWorld.getDefaultTeleporter().placeInPortal(entity, x, y, z, entity.rotationYaw);
                newWorld.spawnEntityInWorld(entity);
                newWorld.updateEntityWithOptionalForce(entity, false);
                entity.setWorld(newWorld);
                ci.cancel();
            }
        }
    }
}