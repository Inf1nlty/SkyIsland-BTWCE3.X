package com.inf1nlty.skyisland.mixin;

import com.inf1nlty.skyisland.util.IslandDataManager;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for multiplayer player NBT persistence.
 */
@Mixin(EntityPlayerMP.class)
public class EntityPlayerMPMixin {
    /**
     * Write multiplayer island data to NBT after vanilla write.
     */
    @Inject(method = "writeEntityToNBT", at = @At("TAIL"))
    public void writeIslandData(NBTTagCompound tag, CallbackInfo ci) {
        IslandDataManager.writeIslandToNBT((EntityPlayerMP)(Object)this, tag);
    }

    /**
     * Read multiplayer island data from NBT after vanilla read.
     */
    @Inject(method = "readEntityFromNBT", at = @At("TAIL"))
    public void readIslandData(NBTTagCompound tag, CallbackInfo ci) {
        IslandDataManager.readIslandFromNBT((EntityPlayerMP)(Object)this, tag);
    }
}