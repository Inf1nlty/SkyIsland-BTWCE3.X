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
        if (SkyBlockProtectionUtil.denyInteractionIfProtected(player, entity.posX, entity.posZ, world.provider.dimensionId)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    /**
     * Prevents detonation of carried Blasting Oil by players in protected SkyBlock regions.
     * Cancels explosion caused by Blasting Oil if the player is in a protected area.
     */
    @Inject(method = "detonateCarriedBlastingOil", at = @At("HEAD"), cancellable = true)
    private void blockBlastingOilDetonation(CallbackInfo ci) {
        EntityPlayer self = (EntityPlayer)(Object)this;
        World world = self.worldObj;
        // Only block if in protected region
        if (!world.isRemote && SkyBlockProtectionUtil.denyInteractionIfProtected(self, self.posX, self.posZ, world.provider.dimensionId)) {
            ci.cancel();
        }
    }

    /**
     * Nether Air Soul Possession Simulation - triggers randomly in the Nether
     * to simulate soul possession effects for SoulMending mechanics.
     * Probability: 1/1000 per tick when player is in Nether (dimensionId == -1)
     */
    @Inject(method = "onLivingUpdate", at = @At("HEAD"))
    private void netherAirSoulPossession(CallbackInfo ci) {
        EntityPlayer self = (EntityPlayer)(Object)this;
        World world = self.worldObj;
        
        // Only process on server side and in Nether dimension
        if (!world.isRemote && world.provider.dimensionId == -1) {
            // 1/1000 probability per tick
            if (self.rand.nextInt(1000) == 0) {
                // Use reflection to call the SoulMending method to maintain compatibility
                try {
                    // Try to find and invoke the soulMending$onSoulPossession method
                    java.lang.reflect.Method method = self.getClass().getMethod("soulMending$onSoulPossession");
                    method.invoke(self);
                } catch (NoSuchMethodException e) {
                    // Method not found - this is expected if SoulMending isn't available
                    // Silently continue without error
                } catch (Exception e) {
                    // Other reflection errors - log but don't crash
                    System.err.println("Warning: Failed to invoke soulMending$onSoulPossession: " + e.getMessage());
                }
            }
        }
    }

}