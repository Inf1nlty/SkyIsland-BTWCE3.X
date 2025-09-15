package com.inf1nlty.skyblock.mixin.world.block;

import btw.block.BTWBlocks;
import btw.block.blocks.WorkStumpBlock;
import com.inf1nlty.skyblock.util.SkyBlockProtectionUtil;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin for ItemInWorldManager to enforce SkyBlock region protection.
 * Blocks right-click (activate/use) and block removal actions in protected regions.
 */
@Mixin(ItemInWorldManager.class)
public abstract class ItemInWorldManagerMixin {

    @Inject(method = "activateBlockOrUseItem", at = @At("HEAD"), cancellable = true)
    private void blockProtectedRightClick(EntityPlayer player, World world, ItemStack stack, int x, int y, int z, int side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
        if (player == null || world == null) return;

        int blockId = world.getBlockId(x, y, z);

        // Exclude Crafting Table and WorkStumpBlock from right-click protection
        if (blockId == BTWBlocks.workbench.blockID || blockId == BTWBlocks.workStump.blockID)
        {
            return;
        }

        if (SkyBlockProtectionUtil.denyInteractionIfProtected(player, x, z, world.provider.dimensionId)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Shadow public World theWorld;
    @Shadow public EntityPlayerMP thisPlayerMP;

    @Inject(method = "removeBlock", at = @At("HEAD"), cancellable = true)
    private void blockProtectedRemove(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {

        if (thisPlayerMP != null && theWorld != null && SkyBlockProtectionUtil.denyInteractionIfProtected(thisPlayerMP, x, z, theWorld.provider.dimensionId)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

}