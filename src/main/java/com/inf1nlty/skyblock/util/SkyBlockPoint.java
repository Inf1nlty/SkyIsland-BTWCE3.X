package com.inf1nlty.skyblock.util;

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
    public Double warpX, warpY , warpZ;
    public boolean tpaEnabled;
    public boolean warpEnabled;
    public boolean protectEnabled;
    public boolean kickEnabled;

    public SkyBlockPoint(String owner, int x, int y, int z, int dim) {
        this.owner = owner;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dim = dim;
        this.spawnX = x + SkyBlockManager.SPAWN_X_OFFSET;
        this.spawnY = y + SkyBlockManager.SPAWN_Y_OFFSET;
        this.spawnZ = z + SkyBlockManager.SPAWN_Z_OFFSET;
        this.tpaEnabled = true;
        this.warpEnabled = false;
        this.initSpawnX = this.spawnX;
        this.initSpawnY = this.spawnY;
        this.initSpawnZ = this.spawnZ;
        this.warpX = this.initSpawnX;
        this.warpY = this.initSpawnY;
        this.warpZ = this.initSpawnZ;
        this.protectEnabled = true;
        this.kickEnabled = false;
    }

}