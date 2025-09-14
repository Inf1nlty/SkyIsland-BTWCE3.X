package com.inf1nlty.skyblock.util;

import net.minecraft.src.EntityPlayer;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a sky island and its metadata.
 */
public class SkyBlockPoint {
    public final int x, y, z, dim;
    public double initSpawnX, initSpawnY, initSpawnZ;
    public final String owner;
    public Set<String> members = new HashSet<>();
    public double spawnX, spawnY, spawnZ;
    public boolean tpaEnabled = true;
    public boolean protectEnabled;

    public SkyBlockPoint(String owner, int x, int y, int z, int dim) {
        this.owner = owner;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dim = dim;
        this.spawnX = x + SkyBlockManager.SPAWN_X_OFFSET;
        this.spawnY = y + SkyBlockManager.SPAWN_Y_OFFSET;
        this.spawnZ = z + SkyBlockManager.SPAWN_Z_OFFSET;
        this.initSpawnX = this.spawnX;
        this.initSpawnY = this.spawnY;
        this.initSpawnZ = this.spawnZ;
        this.protectEnabled = false;
    }

    public boolean isInProtectRegion(EntityPlayer player) {
        double dx = Math.abs(player.posX - this.initSpawnX);
        double dz = Math.abs(player.posZ - this.initSpawnZ);
        return dx <= 100.0 && dz <= 100.0;
    }
}