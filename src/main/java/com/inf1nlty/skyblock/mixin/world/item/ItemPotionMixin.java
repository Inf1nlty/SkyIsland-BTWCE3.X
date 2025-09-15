package com.inf1nlty.skyblock.mixin.world.item;

import com.inf1nlty.skyblock.util.SkyBlockProtectionUtil;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents using splash potions in protected SkyBlock regions.
 * Only blocks splash potions, normal drinkable potions are still allowed.
 */
@Mixin(ItemPotion.class)
public abstract class ItemPotionMixin {

    @Inject(method = "onItemRightClick", at = @At("HEAD"), cancellable = true)
    private void blockSplashPotion(ItemStack stack, World world, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir) {

        if (ItemPotion.isSplash(stack.getItemDamage()) && SkyBlockProtectionUtil.denyInteractionIfProtected(player, player.posX, player.posZ, world.provider.dimensionId)) {
            cir.setReturnValue(stack);
            cir.cancel();
        }
    }

}