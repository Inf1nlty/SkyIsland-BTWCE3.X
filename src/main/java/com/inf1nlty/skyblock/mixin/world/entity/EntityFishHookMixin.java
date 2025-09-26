package com.inf1nlty.skyblock.mixin.world.entity;

import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.src.EntityFishHook;
import net.minecraft.src.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityFishHook.class)
public class EntityFishHookMixin {

    @ModifyVariable(method = "checkForBite", at = @At(value = "STORE", ordinal = 0), name = "iBiteOdds")
    private int injectVoidWorldBiteOdds(int iBiteOdds) {

        EntityFishHook self = (EntityFishHook)(Object)this;
        World world = self.worldObj;
        int iTimeOfDay = (int)(world.getWorldInfo().getWorldTime() % 24000L);

        if (SkyBlockWorldUtil.isVoidWorldLoaded() && iTimeOfDay > 14000 && iTimeOfDay < 22000) {
            return iBiteOdds / 16;
        }

        return iBiteOdds;
    }
}