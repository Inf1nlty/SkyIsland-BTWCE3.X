package com.inf1nlty.skyisland.util;

/**
 * Represents a sky island and its metadata.
 */
public class IslandPoint {
    public final int x, y, z, dim;
    public final String owner;
    public int spawnX, spawnY, spawnZ;
    public boolean tpaEnabled = false;
    public boolean pendingDelete = false;
    public long pendingDeleteTime = 0;

    public IslandPoint(String owner, int x, int y, int z, int dim) {
        this.owner = owner;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dim = dim;
        this.spawnX = (int)(x + IslandManager.SPAWN_X_OFFSET);
        this.spawnY = (int)(y + IslandManager.SPAWN_Y_OFFSET);
        this.spawnZ = (int)(z + IslandManager.SPAWN_Z_OFFSET);
    }
}