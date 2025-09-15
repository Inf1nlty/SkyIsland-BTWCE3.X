package com.inf1nlty.skyblock.mixin.world.block;

import btw.block.BTWBlocks;
import com.inf1nlty.skyblock.util.SkyBlockProtectionUtil;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockMixin {

    /**
     * Intercept block interaction and deny action if player is not allowed in SkyBlock region.
     */
    @Inject(method = "onBlockActivated", at = @At("HEAD"), cancellable = true)
    private void onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {

        int blockId = world.getBlockId(x, y, z);

        // Exclude Crafting Table and WorkStumpBlock from protection
        if (blockId == BTWBlocks.workbench.blockID || blockId == BTWBlocks.workStump.blockID) {
            return;
        }

        if (SkyBlockProtectionUtil.denyInteractionIfProtected(player, x, z, world.provider.dimensionId)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

}