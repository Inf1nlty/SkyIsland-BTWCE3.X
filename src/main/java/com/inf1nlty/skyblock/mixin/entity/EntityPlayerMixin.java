package com.inf1nlty.skyblock.mixin.entity;

import com.inf1nlty.skyblock.util.SkyBlockManager;
import com.inf1nlty.skyblock.util.SkyBlockDataManager;
import com.inf1nlty.skyblock.util.SkyBlockPoint;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for persisting island data in EntityPlayer NBT.
 * Supports both singleplayer and multiplayer.
 */
@Mixin(EntityPlayer.class)
public class EntityPlayerMixin {
    /**
     * Write island data to player NBT after vanilla write.
     */
    @Inject(method = "writeEntityToNBT", at = @At("TAIL"))
    public void writeIslandNBT(NBTTagCompound tag, CallbackInfo ci) {
        EntityPlayer player = (EntityPlayer) (Object) this;
        // Use DataManager for multiplayer, fallback for singleplayer
        SkyBlockPoint ip;
        if (player instanceof EntityPlayerMP) {
            ip = SkyBlockDataManager.getIsland((EntityPlayerMP) player);
        } else {
            ip = SkyBlockManager.getIsland(player);
        }
        SkyBlockManager.writeIslandToNBT(tag, ip);
    }

    /**
     * Read island data from player NBT after vanilla read.
     */
    @Inject(method = "readEntityFromNBT", at = @At("TAIL"))
    public void readIslandNBT(NBTTagCompound tag, CallbackInfo ci) {
        EntityPlayer player = (EntityPlayer) (Object) this;
        SkyBlockPoint ip = SkyBlockManager.readIslandFromNBT(player, tag);
        // Store to DataManager for multiplayer, fallback for singleplayer
        if (player instanceof EntityPlayerMP) {
            SkyBlockDataManager.setIsland((EntityPlayerMP) player, ip);
        } else {
            SkyBlockManager.setIsland(player, ip);
        }
    }
}