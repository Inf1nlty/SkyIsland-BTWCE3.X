package com.inf1nlty.skyisland.util;

import net.minecraft.src.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class IslandManager {

    public static final int ISLAND_Y = 65;
    public static final double ISLAND_X_OFFSET = 0;
    public static final double ISLAND_Y_OFFSET = -19;
    public static final double ISLAND_Z_OFFSET = 0;
    public static final double SPAWN_X_OFFSET = 7.0;
    public static final double SPAWN_Y_OFFSET = -0.9;
    public static final double SPAWN_Z_OFFSET = 6.0;

    private static final java.util.Set<String> usedIslandPositions = new java.util.HashSet<>();

    public static final String SCHEMATIC_PATH = "assets/skyisland/Island01.schematic";

    private static boolean islandPositionsDirty = false;

    private static SchematicData parseNBT(NBTTagCompound nbt) {
        int width = nbt.getShort("Width");
        int length = nbt.getShort("Length");
        int height = nbt.getShort("Height");
        byte[] blocks = nbt.getByteArray("Blocks");
        byte[] data = nbt.getByteArray("Data");
        byte[] addBlocks = nbt.hasKey("AddBlocks") ? nbt.getByteArray("AddBlocks") : null;
        ArrayList<NBTTagCompound> tileEntities = new ArrayList<>();
        NBTTagList teList = nbt.getTagList("TileEntities");
        for (int i = 0; i < teList.tagCount(); ++i) {
            tileEntities.add((NBTTagCompound) teList.tagAt(i));
        }
        return new SchematicData(width, length, height, blocks, data, addBlocks, tileEntities);
    }

    /**
     * Allocates a new, unique island for a player (always assigns a new island).
     * Islands are distributed in concentric rings around (0,0), with each island spaced 2500 blocks apart.
     * Each island position is guaranteed to be unique and never reused.
     */
    public static final int ISLAND_DISTANCE = 2500;
    public static final int MAX_RING = 100;
    public static final int ISLANDS_PER_RING = 8;

    public static IslandPoint makeIsland(EntityPlayerMP player, WorldServer world) {
        IslandPoint existing = IslandDataManager.getIsland(player);
        if (existing != null) return existing;
        for (int ring = 0; ring < MAX_RING; ring++) {
            int radius = ISLAND_DISTANCE * (ring + 1);
            for (int idx = 0; idx < ISLANDS_PER_RING; idx++) {
                double angle = 2 * Math.PI * idx / ISLANDS_PER_RING;
                int x = (int) Math.round(radius * Math.cos(angle));
                int z = (int) Math.round(radius * Math.sin(angle));
                String posKey = world.provider.dimensionId + ":" + x + ":" + z;
                if (!usedIslandPositions.contains(posKey)) {
                    usedIslandPositions.add(posKey);
                    islandPositionsDirty = true;
                    writeGlobalIslandData(world.getWorldInfo().getNBTTagCompound());
                    IslandPoint ip = new IslandPoint(player.username, x, ISLAND_Y, z, world.provider.dimensionId);
                    IslandDataManager.setIsland(player, ip);
                    return ip;
                }
            }
        }
        throw new RuntimeException("No island locations available!");
    }

    public static IslandPoint getIsland(EntityPlayer player) {
        return spIslands.get(player.username);
    }

    public static void setIsland(EntityPlayer player, IslandPoint ip) {
        if (ip == null) {
            spIslands.remove(player.username);
        } else {
            spIslands.put(player.username, ip);
        }
    }

    public static boolean isUsedIslandPosition(String key) {
        return usedIslandPositions.contains(key);
    }

    public static void addUsedIslandPosition(String key) {
        usedIslandPositions.add(key);
        islandPositionsDirty = true;
    }

    public static void writeGlobalIslandData(NBTTagCompound worldTag) {
        NBTTagCompound globalTag = new NBTTagCompound();
        globalTag.setInteger("count", usedIslandPositions.size());
        int i = 0;
        for (String pos : usedIslandPositions) {
            globalTag.setString("pos_" + i, pos);
            i++;
        }
        worldTag.setTag("SkyIslandGlobal", globalTag);
    }

    public static void readGlobalIslandData(NBTTagCompound worldTag) {
        usedIslandPositions.clear();
        if (!worldTag.hasKey("SkyIslandGlobal")) return;
        NBTTagCompound globalTag = worldTag.getCompoundTag("SkyIslandGlobal");
        int count = globalTag.getInteger("count");
        for (int i = 0; i < count; i++) {
            String pos = globalTag.getString("pos_" + i);
            if (pos.indexOf(':') == pos.lastIndexOf(':')) {
                pos = "0:" + pos;
            }
            usedIslandPositions.add(pos);
        }
    }

    public static void generateIsland(World world, IslandPoint island) {
        int baseX = (int)(island.x + ISLAND_X_OFFSET);
        int baseY = (int)(island.y + ISLAND_Y_OFFSET);;
        int baseZ = (int)(island.z + ISLAND_Z_OFFSET);

        SchematicData schematic = SchematicData.loadSchematic(SCHEMATIC_PATH);
        if (schematic == null) {
            throw new RuntimeException("Schematic file not found: " + SCHEMATIC_PATH);
        }

        for (int y = 0; y < schematic.height; y++) {
            for (int z = 0; z < schematic.length; z++) {
                for (int x = 0; x < schematic.width; x++) {
                    int idx = (y * schematic.length + z) * schematic.width + x;
                    int blockId = schematic.getBlockId(idx);
                    int meta = schematic.getBlockMeta(idx);
                    if (blockId == 0) continue;
                    world.setBlock(baseX + x, baseY + y, baseZ + z, blockId, meta, 2);
                }
            }
        }

        if (schematic.tileEntities != null) {
            for (NBTTagCompound te : schematic.tileEntities) {
                int x = te.getInteger("x");
                int y = te.getInteger("y");
                int z = te.getInteger("z");
                te.setInteger("x", baseX + x);
                te.setInteger("y", baseY + y);
                te.setInteger("z", baseZ + z);
                world.setBlockTileEntity(baseX + x, baseY + y, baseZ + z, TileEntity.createAndLoadEntity(te));
            }
        }
    }

    public static void setSpawn(IslandPoint island, int x, int y, int z) {
        island.spawnX = x;
        island.spawnY = y;
        island.spawnZ = z;
    }

    private static final java.util.Map<String, IslandPoint> spIslands = new java.util.HashMap<>();

    public static void writeIslandToNBT(NBTTagCompound tag, IslandPoint ip) {
        if (ip == null) return;
        NBTTagCompound islandTag = new NBTTagCompound();
        islandTag.setBoolean("exists", true);
        islandTag.setInteger("x", ip.x);
        islandTag.setInteger("y", ip.y);
        islandTag.setInteger("z", ip.z);
        islandTag.setInteger("dim", ip.dim);
        islandTag.setInteger("spawnX", ip.spawnX);
        islandTag.setInteger("spawnY", ip.spawnY);
        islandTag.setInteger("spawnZ", ip.spawnZ);
        islandTag.setBoolean("tpaEnabled", ip.tpaEnabled);
        islandTag.setBoolean("pendingDelete", ip.pendingDelete);
        islandTag.setLong("pendingDeleteTime", ip.pendingDeleteTime);
        tag.setTag("SkyIsland", islandTag);
    }

    public static IslandPoint readIslandFromNBT(EntityPlayer player, NBTTagCompound tag) {
        if (!tag.hasKey("SkyIsland")) return null;
        NBTTagCompound islandTag = tag.getCompoundTag("SkyIsland");
        if (!islandTag.getBoolean("exists")) return null;
        IslandPoint ip = new IslandPoint(
                player.username,
                islandTag.getInteger("x"),
                islandTag.getInteger("y"),
                islandTag.getInteger("z"),
                islandTag.getInteger("dim")
        );
        ip.spawnX = islandTag.getInteger("spawnX");
        ip.spawnY = islandTag.getInteger("spawnY");
        ip.spawnZ = islandTag.getInteger("spawnZ");
        ip.tpaEnabled = islandTag.getBoolean("tpaEnabled");
        ip.pendingDelete = islandTag.getBoolean("pendingDelete");
        ip.pendingDeleteTime = islandTag.getLong("pendingDeleteTime");
        return ip;
    }

    public static void trySyncIslandPositions(World world) {
        if (islandPositionsDirty) {
            writeGlobalIslandData(world.getWorldInfo().getNBTTagCompound());
            islandPositionsDirty = false;
        }
    }
}