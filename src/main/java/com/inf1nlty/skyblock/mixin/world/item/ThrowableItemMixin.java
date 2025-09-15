package com.inf1nlty.skyblock.mixin.world.item;

import com.inf1nlty.skyblock.util.SkyBlockProtectionUtil;
import net.minecraft.src.*;
import btw.item.items.ThrowableItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents using throwable items in protected SkyBlock regions.
 * Sends denial message and cancels the throw if in a protected area.
 */
@Mixin(ThrowableItem.class)
public abstract class ThrowableItemMixin {

    @Inject(method = "onItemRightClick", at = @At("HEAD"), cancellable = true)
    private void blockThrowableUse(ItemStack stack, World world, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir) {

        if (!world.isRemote && player instanceof EntityPlayerMP) {
            if (SkyBlockProtectionUtil.denyInteractionIfProtected(player, player.posX, player.posZ, world.provider.dimensionId)) {
                cir.setReturnValue(stack);
                cir.cancel();
            }
        }
    }

}