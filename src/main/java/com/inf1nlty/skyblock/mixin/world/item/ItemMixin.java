package com.inf1nlty.skyblock.mixin.world.item;

import com.inf1nlty.skyblock.util.SkyBlockProtectionUtil;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts Item.onItemUse to enforce SkyBlock region protection.
 */
@Mixin(Item.class)
public abstract class ItemMixin {

    @Inject(method = "onItemUse", at = @At("HEAD"), cancellable = true)
    private void blockOnItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {

        if (stack != null && stack.getItem() instanceof ItemFood) return;

        if (SkyBlockProtectionUtil.denyInteractionIfProtected(player, x, z, world.provider.dimensionId)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "onItemRightClick", at = @At("HEAD"), cancellable = true)
    private void blockOnItemRightClick(ItemStack stack, World world, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir) {

        if (SkyBlockProtectionUtil.denyInteractionIfProtected(player, player.posX, player.posZ, world.provider.dimensionId)) {
            cir.setReturnValue(stack);
            cir.cancel();
        }
    }

}