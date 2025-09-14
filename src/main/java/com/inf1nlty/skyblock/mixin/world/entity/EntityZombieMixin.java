package com.inf1nlty.skyblock.mixin.world.entity;

import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityZombie.class)
public class EntityZombieMixin {

    @Inject(method = "onSpawnWithEgg", at = @At("RETURN"))
    public void netherZombieToVillager(EntityLivingData data, CallbackInfoReturnable<EntityLivingData> cir) {
        EntityZombie self = (EntityZombie)(Object)this;
        if (SkyBlockWorldUtil.isVoidWorldLoaded()
                && self.worldObj != null
                && self.worldObj.provider.dimensionId == -1) {
            self.setVillager(true);

            // 6:1:1:1:1
            int n = self.rand.nextInt(10);
            if (n < 6) {
                self.villagerClass = 0;
            } else {
                self.villagerClass = n - 5;
            }

            self.addPotionEffect(new PotionEffect(Potion.fireResistance.id, 9999 * 20, 0, true));
        }
    }

    @Inject(method = "startConversion", at = @At("HEAD"))
    private void makePersistentWhenConverting(int time, CallbackInfo ci) {
        EntityZombie self = (EntityZombie)(Object)this;
        self.setPersistent(true);
    }

}