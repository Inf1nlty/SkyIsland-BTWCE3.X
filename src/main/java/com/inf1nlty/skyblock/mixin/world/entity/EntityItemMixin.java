package com.inf1nlty.skyblock.mixin.world.entity;

import com.inf1nlty.skyblock.util.SkyBlockProtectionUtil;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents detonation of Blasting Oil item entities in protected SkyBlock regions.
 * Cancels explosion caused by dropped Blasting Oil if in a protected area.
 */
@Mixin(EntityItem.class)
public abstract class EntityItemMixin {

    @Inject(method = "detonateBlastingOil", at = @At("HEAD"), cancellable = true)
    private void blockBlastingOilEntityDetonation(CallbackInfo ci) {

        EntityItem self = (EntityItem)(Object)this;
        World world = self.worldObj;

        if (!world.isRemote && SkyBlockProtectionUtil.denyInteractionIfProtected(null, self.posX, self.posZ, world.provider.dimensionId)) {
            ci.cancel();
        }
    }

}