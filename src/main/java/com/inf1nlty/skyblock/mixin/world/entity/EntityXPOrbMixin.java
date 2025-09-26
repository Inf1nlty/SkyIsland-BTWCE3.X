package com.inf1nlty.skyblock.mixin.world.entity;

import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.src.EntityXPOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityXPOrb.class)
public abstract class EntityXPOrbMixin {

    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void mergeNearbyXPOrbs(CallbackInfo ci) {
        if (!SkyBlockWorldUtil.isVoidWorldLoaded()) return;

        EntityXPOrb self = (EntityXPOrb)(Object)this;

        if (self.worldObj.isRemote || self.isDead) return;

        if (self.xpOrbAge % 20 != 0) return;

        double mergeRadius = 0.5D;

        for (Object obj : self.worldObj.getEntitiesWithinAABB(EntityXPOrb.class, self.boundingBox.expand(mergeRadius, mergeRadius, mergeRadius))) {
            EntityXPOrb other = (EntityXPOrb)obj;
            if (other == self || other.isDead) continue;

            if (self.notPlayerOwned != other.notPlayerOwned) continue;

            if (other.xpValue + self.xpValue > 2047) continue;

            if (other.xpValue >= self.xpValue) {
                other.xpValue += self.xpValue;
                self.setDead();
            }
            else {
                self.xpValue += other.xpValue;
                other.setDead();
            }
            return;
        }
    }
}