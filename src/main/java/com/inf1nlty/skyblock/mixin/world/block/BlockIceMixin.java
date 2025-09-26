package com.inf1nlty.skyblock.mixin.world.block;

import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for BlockIce to enable vanilla-like ice melting in SkyBlock void worlds.
 * - In void worlds: Ice only melts into water source blocks via normal melting (tick/light), never instantly.
 * - In the Nether: Ice always disappears without producing water.
 * - In other worlds: Behavior is unchanged from the base mod (BTW/CE).
 * This allows SkyBlock progression by making ice a renewable water source in void worlds,
 * matching vanilla SkyBlock mechanics.
 */
@Mixin(BlockIce.class)
public abstract class BlockIceMixin {

    @Inject(method = "melt", at = @At("HEAD"), cancellable = true)
    private void skyblock$allowWaterSourceInVoidWorld(World world, int x, int y, int z, CallbackInfo ci) {

        if (world.provider.isHellWorld) {
            world.setBlockWithNotify(x, y, z, 0);
            ci.cancel();
            return;
        }

        if (SkyBlockWorldUtil.isVoidWorldLoaded()) {
            world.setBlockWithNotify(x, y, z, Block.waterStill.blockID);
            Block.waterStill.onNeighborBlockChange(world, x, y, z, 0);
            ci.cancel();
        }
    }

    @Inject(method = "harvestBlock", at = @At("HEAD"), cancellable = true)
    private void skyblock$voidWorldIceBreak(World world, EntityPlayer player, int x, int y, int z, int meta, CallbackInfo ci) {

        if (SkyBlockWorldUtil.isVoidWorldLoaded() && !world.provider.isHellWorld) {

            boolean silk = EnchantmentHelper.getSilkTouchModifier(player);

            if (!silk) {
                world.setBlockWithNotify(x, y, z, Block.waterStill.blockID);
                Block.waterStill.onNeighborBlockChange(world, x, y, z, 0);
                player.addStat(StatList.mineBlockStatArray[Block.ice.blockID], 1);
                player.addExhaustion(0.025F);
                ci.cancel();
            }
        }
    }

    @Inject(method = "onBlockAdded", at = @At("HEAD"), cancellable = true)
    private void skyblock$fixOnBlockAdded(World world, int x, int y, int z, CallbackInfo ci) {
        if (SkyBlockWorldUtil.isVoidWorld(world)) {
            ci.cancel();
        }
    }
}