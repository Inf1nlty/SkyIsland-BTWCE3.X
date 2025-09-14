package com.inf1nlty.skyblock.mixin.world.entity;

import com.inf1nlty.skyblock.util.SkyBlockDataManager;
import com.inf1nlty.skyblock.util.SkyBlockPoint;
import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.ItemInWorldManager;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for multiplayer player NBT persistence.
 */
@Mixin(EntityPlayerMP.class)
public class EntityPlayerMPMixin {

    /**
     * Write multiplayer island data to NBT after vanilla write.
     */
    @Inject(method = "writeEntityToNBT", at = @At("TAIL"))
    public void writeIslandData(NBTTagCompound tag, CallbackInfo ci) {
        EntityPlayerMP player = (EntityPlayerMP)(Object)this;
        SkyBlockDataManager.writeIslandToNBT(player, tag);
        SkyBlockDataManager.writeHistoryToNBT(player, tag);
    }

    /**
     * Read multiplayer island data from NBT after vanilla read.
     */
    @Inject(method = "readEntityFromNBT", at = @At("TAIL"))
    public void readIslandData(NBTTagCompound tag, CallbackInfo ci) {
        EntityPlayerMP player = (EntityPlayerMP)(Object)this;
        String owner = SkyBlockDataManager.getGlobalMemberOwner(player.username);
        if (owner != null && !owner.equals(player.username)) {
            SkyBlockPoint ip = SkyBlockDataManager.getIsland(owner);
            if (ip != null) {
                ip.members.add(player.username);
            }
            SkyBlockDataManager.setIsland(player, ip);
        } else {
            SkyBlockDataManager.readIslandFromNBT(player, tag);
        }
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void forceVoidWorldPlayerSpawn(MinecraftServer par1MinecraftServer, World par2World, String par3Str, ItemInWorldManager par4ItemInWorldManager, CallbackInfo ci) {
        if (SkyBlockWorldUtil.isVoidWorldLoaded()) {
            ((EntityPlayerMP)(Object)this).setLocationAndAngles(0.5, 101.0, 0.5, 0.0F, 0.0F);
        }
    }

    @Inject(method = "dropMysteryMeat", at = @At("HEAD"), cancellable = true)
    private void cancelMysteryMeatInVoidWorld(int iLootingModifier, CallbackInfo ci) {
        if (SkyBlockWorldUtil.isVoidWorldLoaded()) {
            ci.cancel();
        }
    }

}