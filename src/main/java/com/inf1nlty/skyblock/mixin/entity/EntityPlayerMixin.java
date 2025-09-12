package com.inf1nlty.skyblock.mixin.entity;

import com.inf1nlty.skyblock.util.SkyBlockManager;
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
 * Supports only singleplayer (EntityPlayer, not EntityPlayerMP).
 * Multiplayer NBT handled by EntityPlayerMPMixin.
 */
@Mixin(EntityPlayer.class)
public class EntityPlayerMixin {
    /**
     * Write island data to player NBT after vanilla write.
     * Only for singleplayer (not EntityPlayerMP).
     */
    @Inject(method = "writeEntityToNBT", at = @At("TAIL"))
    public void writeIslandNBT(NBTTagCompound tag, CallbackInfo ci) {
        EntityPlayer player = (EntityPlayer) (Object) this;
        if (!(player instanceof EntityPlayerMP)) {
            SkyBlockPoint ip = SkyBlockManager.getIsland(player);
            SkyBlockManager.writeIslandToNBT(tag, ip);
        }
    }

    /**
     * Read island data from player NBT after vanilla read.
     * Only for singleplayer (not EntityPlayerMP).
     */
    @Inject(method = "readEntityFromNBT", at = @At("TAIL"))
    public void readIslandNBT(NBTTagCompound tag, CallbackInfo ci) {
        EntityPlayer player = (EntityPlayer) (Object) this;
        if (!(player instanceof EntityPlayerMP)) {
            SkyBlockPoint ip = SkyBlockManager.readIslandFromNBT(player, tag);
            SkyBlockManager.setIsland(player, ip);
        }
    }
}