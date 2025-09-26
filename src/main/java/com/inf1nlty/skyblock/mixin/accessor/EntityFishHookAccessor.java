package com.inf1nlty.skyblock.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.src.EntityFishHook;

@Mixin(EntityFishHook.class)
public interface EntityFishHookAccessor {

    @Invoker("isBodyOfWaterLargeEnoughForFishing")
    boolean callIsBodyOfWaterLargeEnoughForFishing();
}