package com.inf1nlty.skyblock.mixin.world.item;

import btw.item.items.BucketItemFull;
import btw.util.MiscUtils;
import com.inf1nlty.skyblock.util.SkyBlockProtectionUtil;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItemFull.class)
public abstract class BucketItemFullMixin {

    /**
     * Disables all attempts to pour liquid from buckets by right-clicking within the protected area.
     */
    @Inject(method = "onItemRightClick", at = @At("HEAD"), cancellable = true)
    private void blockProtectedBucketUse(ItemStack stack, World world, EntityPlayer player, CallbackInfoReturnable<ItemStack> cir) {
        if (player != null && world != null) {

            MovingObjectPosition posClicked = MiscUtils.getMovingObjectPositionFromPlayerHitWaterAndLava(world, player, false);
            if (posClicked != null && posClicked.typeOfHit == EnumMovingObjectType.TILE) {
                int x = posClicked.blockX;
                int z = posClicked.blockZ;
                int dim = world.provider.dimensionId;
                if (SkyBlockProtectionUtil.denyInteractionIfProtected(player, x, z, dim)) {
                    cir.setReturnValue(stack);
                    cir.cancel();
                }
            }
        }
    }

}