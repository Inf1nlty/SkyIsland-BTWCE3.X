package com.inf1nlty.skyblock.mixin.entity;

import btw.block.BTWBlocks;
import com.inf1nlty.skyblock.SkyblockConfig;
import com.inf1nlty.skyblock.mixin.accessor.EntityAccessor;
import com.inf1nlty.skyblock.util.SkyBlockWorldUtil;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(EntityAnimal.class)
public abstract class EntityAnimalMixin {

    /**
     * Only allow animals to spawn on grass slab blocks in Skyblock mode.
     * @author Inf1nlty
     * @reason Vanilla spawning does not allow slabs. This overwrites animal spawn logic for Skyblock.
     **/
    @Overwrite
    public boolean getCanSpawnHere() {
        EntityAccessor self = (EntityAccessor) this;
        double posX = self.getPosX();
        double posZ = self.getPosZ();
        World world = self.getWorldObj();
        AxisAlignedBB boundingBox = self.getBoundingBox();
        int x = MathHelper.floor_double(posX);
        int y = MathHelper.floor_double(boundingBox.minY);
        int z = MathHelper.floor_double(posZ);

        if (SkyblockConfig.ALLOW_ANIMAL_SPAWN_ON_GRASSSLAB && SkyBlockWorldUtil.isVoidWorldLoaded()) {
            // Only valid if the block below is a grass slab and light level is sufficient
            boolean validSlab = world.getBlockId(x, y - 1, z) == BTWBlocks.grassSlab.blockID;
            if (validSlab && world.getFullBlockLightValue(x, y, z) > 8) {
                // Check for collision and liquid
                return world.checkNoEntityCollision(boundingBox) && !world.isAnyLiquid(boundingBox);
            }
            return false;
        } else {
            // Vanilla logic: only allow if the block below is grass, light level >8, and basic collision/liquid checks
            boolean validGrass = world.getBlockId(x, y - 1, z) == Block.grass.blockID;
            boolean light = world.getFullBlockLightValue(x, y, z) > 8;
            return validGrass && light && world.checkNoEntityCollision(boundingBox) && !world.isAnyLiquid(boundingBox);
        }
    }
}