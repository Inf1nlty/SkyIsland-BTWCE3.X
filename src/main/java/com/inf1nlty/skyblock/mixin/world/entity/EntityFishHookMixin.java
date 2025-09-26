package com.inf1nlty.skyblock.mixin.world.entity;

import com.inf1nlty.skyblock.mixin.accessor.EntityFishHookAccessor;
import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.src.EntityFishHook;
import net.minecraft.src.MathHelper;
import net.minecraft.src.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityFishHook.class)
public class EntityFishHookMixin {

    @Inject(method = "checkForBite", at = @At(value = "INVOKE", target = "Lnet/minecraft/src/World;getMoonPhase()I"), cancellable = true)
    private void injectVoidWorldNight(CallbackInfoReturnable<Boolean> cir) {

        EntityFishHook self = (EntityFishHook)(Object)this;
        World world = self.worldObj;
        int iTimeOfDay = (int)(world.getWorldInfo().getWorldTime() % 24000L);

        if (SkyBlockWorldUtil.isVoidWorldLoaded() && iTimeOfDay > 14000 && iTimeOfDay < 22000) {
            int iBiteOdds = 1500 / 16;
            boolean bite = self.rand.nextInt(iBiteOdds) == 0
                    && world.canBlockSeeTheSky(
                    MathHelper.floor_double(self.posX),
                    MathHelper.floor_double(self.posY) + 1,
                    MathHelper.floor_double(self.posZ))
                    && ((EntityFishHookAccessor)self).callIsBodyOfWaterLargeEnoughForFishing();
            cir.setReturnValue(bite);
        }
    }
}