package com.inf1nlty.skyisland.mixin;

import com.inf1nlty.skyisland.util.IslandManager;
import com.inf1nlty.skyisland.util.IslandDataManager;
import com.inf1nlty.skyisland.util.IslandPoint;
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
        IslandPoint ip;
        if (player instanceof EntityPlayerMP) {
            ip = IslandDataManager.getIsland((EntityPlayerMP) player);
        } else {
            ip = IslandManager.getIsland(player);
        }
        IslandManager.writeIslandToNBT(tag, ip);
    }

    /**
     * Read island data from player NBT after vanilla read.
     */
    @Inject(method = "readEntityFromNBT", at = @At("TAIL"))
    public void readIslandNBT(NBTTagCompound tag, CallbackInfo ci) {
        EntityPlayer player = (EntityPlayer) (Object) this;
        IslandPoint ip = IslandManager.readIslandFromNBT(player, tag);
        // Store to DataManager for multiplayer, fallback for singleplayer
        if (player instanceof EntityPlayerMP) {
            IslandDataManager.setIsland((EntityPlayerMP) player, ip);
        } else {
            IslandManager.setIsland(player, ip);
        }
    }
}