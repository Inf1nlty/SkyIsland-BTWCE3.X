package com.inf1nlty.skyblock.mixin;

import com.inf1nlty.skyblock.util.SkyBlockProtectionUtil;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerControllerMP.class)
public abstract class PlayerControllerMPMixin {

    @Final @Shadow private Minecraft mc;

    /**
     * Intercept block breaking and deny if not allowed in SkyBlock region.
     */
    @Inject(method = "onPlayerDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onPlayerDestroyBlock(int x, int y, int z, int side, CallbackInfoReturnable<Boolean> cir) {

        EntityPlayer player = this.mc.thePlayer;
        World world = this.mc.theWorld;

        if (SkyBlockProtectionUtil.shouldDenyInteraction(player, x, z, world.provider.dimensionId)) {
            SkyBlockProtectionUtil.sendProtectDenyMessage(player);
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

}