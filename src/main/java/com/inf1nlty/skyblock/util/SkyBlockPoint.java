package com.inf1nlty.skyblock.util;

/**
 * Represents a sky island and its metadata.
 */
public class SkyBlockPoint {
    public final int x, y, z, dim;
    public final String owner;
    public double spawnX, spawnY, spawnZ;
    public boolean tpaEnabled = true;
    public boolean pendingDelete = false;
    public long pendingDeleteTime = 0;

    public SkyBlockPoint(String owner, int x, int y, int z, int dim) {
        this.owner = owner;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dim = dim;
        this.spawnX = x + SkyBlockManager.SPAWN_X_OFFSET;
        this.spawnY = y + SkyBlockManager.SPAWN_Y_OFFSET;
        this.spawnZ = z + SkyBlockManager.SPAWN_Z_OFFSET;
    }
}