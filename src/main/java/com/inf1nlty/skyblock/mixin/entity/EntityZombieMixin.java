package com.inf1nlty.skyblock.mixin.entity;

import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
            // Randomly assign occupations (0~4), the original number of occupations is 5
            self.villagerClass = self.rand.nextInt(5);
            self.addPotionEffect(new PotionEffect(Potion.fireResistance.id, 999999 * 20, 0, true));
        }
    }
}