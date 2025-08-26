package com.inf1nlty.skyblock.mixin;

import com.inf1nlty.skyblock.util.SkyBlockManager;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.WorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for persisting island allocation data in world NBT.
 * Ensures global island positions are loaded and saved with the world.
 */
@Mixin(WorldInfo.class)
public abstract class WorldInfoMixin {
    /**
     * Load global island allocation data when the world is loaded.
     */
    @Inject(method = "<init>(Lnet/minecraft/src/NBTTagCompound;)V", at = @At("TAIL"))
    private void readGlobalIslandData(NBTTagCompound tag, CallbackInfo ci) {
        SkyBlockManager.readGlobalIslandData(tag);
    }

    /**
     * Save global island allocation data when the world is saved.
     */
    @Inject(method = "updateTagCompound", at = @At("TAIL"))
    private void writeGlobalIslandData(NBTTagCompound tag, NBTTagCompound unused, CallbackInfo ci) {
        SkyBlockManager.writeGlobalIslandData(tag);
    }
}