package com.inf1nlty.skyblock.mixin.world.item;

import com.inf1nlty.skyblock.util.SkyBlockProtectionUtil;
import btw.item.items.DynamiteItem;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents using Dynamite in protected SkyBlock regions.
 */
@Mixin(DynamiteItem.class)
public abstract class DynamiteItemMixin {

    @Inject(method = "onItemRightClick", at = @At("HEAD"), cancellable = true)
    private void blockDynamiteUse(ItemStack stack, World world, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir) {

        if (SkyBlockProtectionUtil.denyInteractionIfProtected(player, player.posX, player.posZ, world.provider.dimensionId)) {
            cir.setReturnValue(stack);
            cir.cancel();
        }
    }

}