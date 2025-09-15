package com.inf1nlty.skyblock.mixin.world.entity;

import com.inf1nlty.skyblock.util.SkyBlockManager;
import com.inf1nlty.skyblock.util.SkyBlockPoint;
import com.inf1nlty.skyblock.util.SkyBlockProtectionUtil;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for persisting island data in EntityPlayer NBT.
 * Supports only singleplayer (EntityPlayer, not EntityPlayerMP).
 * Multiplayer NBT handled by EntityPlayerMPMixin.
 */
@Mixin(EntityPlayer.class)
public abstract class EntityPlayerMixin {
    /**
     * Write island data to player NBT after vanilla write.
     * Only for singleplayer (not EntityPlayerMP).
     */
    @Inject(method = "writeEntityToNBT", at = @At("TAIL"))
    public void writeIslandNBT(NBTTagCompound tag, CallbackInfo ci) {
        EntityPlayer player = (EntityPlayer) (Object) this;
        if (!(player instanceof EntityPlayerMP)) {
            SkyBlockPoint ip = SkyBlockManager.getIsland(player);
            if (ip != null && player.username.equals(ip.owner)) {
                SkyBlockManager.writeIslandToNBT(tag, ip);
            }
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

    @Inject(method = "attackTargetEntityWithCurrentItem", at = @At("HEAD"), cancellable = true)
    private void blockAttackTargetEntityWithCurrentItem(Entity target, CallbackInfo ci) {
        EntityPlayer player = (EntityPlayer) (Object) this;
        World world = player.worldObj;
        if (SkyBlockProtectionUtil.shouldDenyInteraction(player, target.posX, target.posZ, world.provider.dimensionId)) {
            SkyBlockProtectionUtil.sendProtectDenyMessage(player);
            ci.cancel();
        }
    }

    /**
     * Intercept right-click entity and deny if not allowed in SkyBlock region.
     */
    @Inject(method = "interactWith", at = @At("HEAD"), cancellable = true)
    private void interactWith(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        EntityPlayer player = (EntityPlayer) (Object) this;
        World world = player.worldObj;
        if (SkyBlockProtectionUtil.shouldDenyInteraction(player, entity.posX, entity.posZ, world.provider.dimensionId)) {
            SkyBlockProtectionUtil.sendProtectDenyMessage(player);
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

}