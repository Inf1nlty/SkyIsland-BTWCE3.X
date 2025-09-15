package com.inf1nlty.skyblock.mixin.world.item;

import btw.item.items.PlaceAsBlockItem;
import com.inf1nlty.skyblock.util.SkyBlockProtectionUtil;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlaceAsBlockItem.class)
public abstract class PlaceAsBlockItemMixin {

    /**
     * Intercept block placement and deny action if player is not allowed in SkyBlock region.
     */
    @Inject(method = "onItemUse", at = @At("HEAD"), cancellable = true)
    private void blockOnItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {

        if (SkyBlockProtectionUtil.denyInteractionIfProtected(player, x, z, world.provider.dimensionId)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

}