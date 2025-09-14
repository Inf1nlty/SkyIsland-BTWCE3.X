package com.inf1nlty.skyblock.mixin.world.item;

import btw.item.items.BucketItemLava;
import btw.util.MiscUtils;
import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BucketItemLava.class)
public abstract class BucketItemLavaMixin extends Item {

    public BucketItemLavaMixin(int id) {
        super(id);
    }

    /**
     * Allows lava buckets to place a lava source block in void worlds (SkyBlock logic).
     * Returns an empty bucket after use, or keeps the bucket if the player is in creative mode.
     * BucketItemLava has no onItemRightClick implementation in BTW.
     * This method adds right click behavior for legacy/creative usage.
     */
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        MovingObjectPosition mop = MiscUtils.getMovingObjectPositionFromPlayerHitWaterAndLava(world, player, false);
        if (mop != null && mop.typeOfHit == EnumMovingObjectType.TILE) {
            int x = mop.blockX, y = mop.blockY, z = mop.blockZ;
            int side = mop.sideHit;

            if (side == 0) y--;
            else if (side == 1) y++;
            else if (side == 2) z--;
            else if (side == 3) z++;
            else if (side == 4) x--;
            else if (side == 5) x++;

            if (!player.canPlayerEdit(x, y, z, side, stack)) return stack;

            if (SkyBlockWorldUtil.isVoidWorldLoaded()) {
                if (world.isAirBlock(x, y, z) || world.getBlockMaterial(x, y, z).isReplaceable()) {
                    world.setBlockWithNotify(x, y, z, Block.lavaMoving.blockID);
                    Block.lavaStill.onNeighborBlockChange(world, x, y, z, 0);
                    if (!player.capabilities.isCreativeMode) {
                        return new ItemStack(Item.bucketEmpty);
                    } else {
                        return stack;
                    }
                }
            }
        }
        return stack;
    }
}