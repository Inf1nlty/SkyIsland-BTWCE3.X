package com.inf1nlty.skyblock.util;

import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.NBTTagCompound;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles island data persistence and retrieval.
 */
public class SkyBlockDataManager {
    private static final Map<String, SkyBlockPoint> playerIslands = new HashMap<>();

    public static SkyBlockPoint getIsland(EntityPlayerMP player) {
        return playerIslands.get(player.username);
    }

    public static void setIsland(EntityPlayerMP player, SkyBlockPoint ip) {
        if (ip == null) {
            playerIslands.remove(player.username);
        } else {
            playerIslands.put(player.username, ip);
        }
    }

    /**
     * Writes the player's island data to NBT.
     */
    public static void writeIslandToNBT(EntityPlayerMP player, NBTTagCompound tag) {
        SkyBlockPoint ip = getIsland(player);
        if (ip == null) return;
        NBTTagCompound islandTag = new NBTTagCompound();
        islandTag.setBoolean("exists", true);
        islandTag.setInteger("x", ip.x);
        islandTag.setInteger("y", ip.y);
        islandTag.setInteger("z", ip.z);
        islandTag.setInteger("dim", ip.dim);
        islandTag.setDouble("spawnX", ip.spawnX);
        islandTag.setDouble("spawnY", ip.spawnY);
        islandTag.setDouble("spawnZ", ip.spawnZ);
        islandTag.setBoolean("tpaEnabled", ip.tpaEnabled);
        islandTag.setBoolean("pendingDelete", ip.pendingDelete);
        islandTag.setLong("pendingDeleteTime", ip.pendingDeleteTime);
        tag.setTag("SkyIsland", islandTag);
    }

    /**
     * Reads the player's island data from NBT.
     */
    public static void readIslandFromNBT(EntityPlayerMP player, NBTTagCompound tag) {
        if (!tag.hasKey("SkyIsland")) {
            setIsland(player, null);
            return;
        }
        NBTTagCompound islandTag = tag.getCompoundTag("SkyIsland");
        if (!islandTag.getBoolean("exists")) {
            setIsland(player, null);
            return;
        }
        SkyBlockPoint ip = new SkyBlockPoint(
                player.username,
                islandTag.getInteger("x"),
                islandTag.getInteger("y"),
                islandTag.getInteger("z"),
                islandTag.getInteger("dim")
        );
        ip.spawnX = islandTag.getDouble("spawnX");
        ip.spawnY = islandTag.getDouble("spawnY");
        ip.spawnZ = islandTag.getDouble("spawnZ");
        ip.tpaEnabled = islandTag.getBoolean("tpaEnabled");
        ip.pendingDelete = islandTag.getBoolean("pendingDelete");
        ip.pendingDeleteTime = islandTag.getLong("pendingDeleteTime");
        setIsland(player, ip);
        String newKey = ip.dim + ":" + ip.x + ":" + ip.z;
        if (!SkyBlockManager.isUsedIslandPosition(newKey)) {
            SkyBlockManager.addUsedIslandPosition(newKey);
        }
    }
}