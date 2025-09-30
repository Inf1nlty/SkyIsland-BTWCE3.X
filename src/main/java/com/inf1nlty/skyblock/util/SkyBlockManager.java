package com.inf1nlty.skyblock.util;

import net.minecraft.src.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

public class SkyBlockManager {

    public static final int ISLAND_Y = 100;
    public static final double ISLAND_X_OFFSET = 0;
    public static final double ISLAND_Y_OFFSET = 0;
    public static final double ISLAND_Z_OFFSET = 0;
    public static final double SPAWN_X_OFFSET = 7.0;
    public static final double SPAWN_Y_OFFSET = 0;
    public static final double SPAWN_Z_OFFSET = 8.0;

    private static final java.util.Set<String> usedIslandPositions = new java.util.HashSet<>();

    public static final String SCHEMATIC_PATH = "assets/skyblock/island01.schematic";

    private static boolean islandPositionsDirty = false;

    public static final int MAX_RING = 100;

    private static class RegionId {

        public final int x, z;
        public RegionId(int x, int z) { this.x = x; this.z = z; }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RegionId other)) return false;
            return x == other.x && z == other.z;
        }

        @Override public int hashCode() {
            return 31 * x + z;
        }

        @Override public String toString() { return x + ":" + z; }

        public static RegionId fromString(String s) {
            String[] arr = s.split(":");
            return new RegionId(Integer.parseInt(arr[0]), Integer.parseInt(arr[1]));
        }

    }

    private static ArrayList<RegionId> getRegionIdsForRing(int ring) {

        ArrayList<RegionId> result = new ArrayList<>();
        int R = ring * 4;

        result.add(new RegionId(R, 0));
        result.add(new RegionId(-R, 0));
        result.add(new RegionId(0, R));
        result.add(new RegionId(0, -R));

        // Bevel and other points on the ring
        for (int i = 1; i < ring; i++) {
            int x = i * 4;
            int y = R - x;
            result.add(new RegionId(x, y));
            result.add(new RegionId(-x, y));
            result.add(new RegionId(x, -y));
            result.add(new RegionId(-x, -y));
            result.add(new RegionId(y, x));
            result.add(new RegionId(-y, x));
            result.add(new RegionId(y, -x));
            result.add(new RegionId(-y, -x));
        }
        return result;
    }

    public static SkyBlockPoint makeIsland(EntityPlayerMP player, WorldServer world) {
        for (int ring = 1; ring <= MAX_RING; ring++) {
            ArrayList<RegionId> regions = getRegionIdsForRing(ring);
            ArrayList<RegionId> candidates = new ArrayList<>();
            for (RegionId region : regions) {
                String key = world.provider.dimensionId + ":" + region.x + ":" + region.z;
                if (!usedIslandPositions.contains(key)) {
                    candidates.add(region);
                }
            }
            if (!candidates.isEmpty()) {
                Collections.shuffle(candidates);
                RegionId chosen = candidates.get(0);
                String key = world.provider.dimensionId + ":" + chosen.x + ":" + chosen.z;
                usedIslandPositions.add(key);
                islandPositionsDirty = true;
                writeGlobalIslandData(world.getWorldInfo().getNBTTagCompound());
                int x = chosen.x * 512;
                int z = chosen.z * 512;
                SkyBlockPoint ip = new SkyBlockPoint(player.username, x, ISLAND_Y, z, world.provider.dimensionId);
                SkyBlockDataManager.setIsland(player, ip);
                return ip;
            }
        }
        throw new RuntimeException("No island locations available!");
    }

    public static void freeIslandRegions(SkyBlockPoint island) {
        int rx = island.x / 512;
        int rz = island.z / 512;
        int dim = island.dim;
        usedIslandPositions.remove(dim + ":" + rx + ":" + rz);
        islandPositionsDirty = true;
    }

    public static void deleteIslandRegionFile(SkyBlockPoint island, File worldDir, WorldServer world) {
        int regionX = Math.floorDiv(island.x, 512);
        int regionZ = Math.floorDiv(island.z, 512);

        int minChunkX = regionX * 32;
        int maxChunkX = minChunkX + 31;
        int minChunkZ = regionZ * 32;
        int maxChunkZ = minChunkZ + 31;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (world.theChunkProviderServer.chunkExists(cx, cz)) {
                    world.theChunkProviderServer.forceAddToChunksToUnload(cx, cz);
                }
            }
        }

        ISaveHandler handler = world.getSaveHandler();
        if (handler instanceof AnvilSaveHandler) {
            handler.flush();
        }

        // Delete the center and its left/bottom/lower-left four regions
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                File regionFile = new File(worldDir, "region/r." + (regionX - dx) + "." + (regionZ - dz) + ".mca");
                RegionFileHelper.closeRegionFile(regionFile);
                if (regionFile.exists()) {
                    boolean deleted = regionFile.delete();
                    System.out.println("Delete region file: " + regionFile.getAbsolutePath() + " result: " + deleted);
                    if (!deleted) {
                        System.out.println("Warning: The file could not be deleted. It is recommended to clean it up manually after restarting!");
                    }
                }
            }
        }
    }

    public static SkyBlockPoint getIsland(EntityPlayer player) {
        return spIslands.get(player.username);
    }

    public static Iterable<SkyBlockPoint> getAllIslands() {
        return spIslands.values();
    }

    public static void setIsland(EntityPlayer player, SkyBlockPoint ip) {
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

    public static SkyBlockPoint getIslandForMember(EntityPlayer player) {
        for (SkyBlockPoint ip : spIslands.values()) {
            if (ip.members.contains(player.username)) {
                return ip;
            }
        }
        return null;
    }

    public static void writeGlobalIslandData(NBTTagCompound worldTag) {
        NBTTagCompound globalTag = new NBTTagCompound();
        globalTag.setInteger("count", usedIslandPositions.size());
        int i = 0;
        for (String pos : usedIslandPositions) {
            globalTag.setString("pos_" + i, pos);
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
            usedIslandPositions.add(pos);
        }
    }

    /**
     * Find the minimum Y coordinate of all water lilies in the specified schematic (as the island base layer)
     */
    public static int findLilyPadMinY(SchematicData schematic, int lilyPadBlockId) {
        int minY = schematic.height;
        for (int y = 0; y < schematic.height; y++) {
            for (int z = 0; z < schematic.length; z++) {
                for (int x = 0; x < schematic.width; x++) {
                    int idx = (y * schematic.length + z) * schematic.width + x;
                    int blockId = schematic.getBlockId(idx);
                    if (blockId == lilyPadBlockId) {
                        if (y < minY) minY = y;
                    }
                }
            }
        }
        return minY;
    }

    /**
     * Align the ISLAND_Y plane with the lily pad
     */
    public static void generateIsland(World world, SkyBlockPoint island) {
        int baseX = (int)(island.x + ISLAND_X_OFFSET);
        int baseZ = (int)(island.z + ISLAND_Z_OFFSET);

        SchematicData schematic = SchematicData.loadSchematic(SCHEMATIC_PATH);
        if (schematic == null) {
            throw new RuntimeException("Schematic file not found: " + SCHEMATIC_PATH);
        }

        int lilyPadBlockId = Block.waterlily.blockID;
        int lilyPadY = findLilyPadMinY(schematic, lilyPadBlockId);
        if (lilyPadY == schematic.height) {
            throw new RuntimeException("Schematic does not detect water lilies as a base layer!");
        }
        int baseY = ISLAND_Y - lilyPadY;

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

    public static void setSpawn(SkyBlockPoint island, double x, double y, double z) {
        island.spawnX = x;
        island.spawnY = y;
        island.spawnZ = z;
    }

    private static final java.util.Map<String, SkyBlockPoint> spIslands = new java.util.HashMap<>();

    public static void writeIslandToNBT(NBTTagCompound tag, SkyBlockPoint ip) {
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
        islandTag.setDouble("initSpawnX", ip.initSpawnX);
        islandTag.setDouble("initSpawnY", ip.initSpawnY);
        islandTag.setDouble("initSpawnZ", ip.initSpawnZ);
        islandTag.setDouble("warpX", ip.warpX);
        islandTag.setDouble("warpY", ip.warpY);
        islandTag.setDouble("warpZ", ip.warpZ);
        islandTag.setBoolean("tpaEnabled", ip.tpaEnabled);
        NBTTagList memberList = new NBTTagList();
        for (String member : ip.members) memberList.appendTag(new NBTTagString(member, member));
        islandTag.setTag("members", memberList);
        islandTag.setBoolean("protectEnabled", ip.protectEnabled);
        islandTag.setBoolean("kickEnabled", ip.kickEnabled);
        tag.setTag("SkyIsland", islandTag);
    }

    public static SkyBlockPoint readIslandFromNBT(EntityPlayer player, NBTTagCompound tag) {
        if (!tag.hasKey("SkyIsland")) return null;
        NBTTagCompound islandTag = tag.getCompoundTag("SkyIsland");
        if (!islandTag.getBoolean("exists")) return null;
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
        ip.initSpawnX = islandTag.getDouble("initSpawnX");
        ip.initSpawnY = islandTag.getDouble("initSpawnY");
        ip.initSpawnZ = islandTag.getDouble("initSpawnZ");
        ip.tpaEnabled = islandTag.getBoolean("tpaEnabled");
        ip.warpX = islandTag.getDouble("warpX");
        ip.warpY = islandTag.getDouble("warpY");
        ip.warpZ = islandTag.getDouble("warpZ");
        ip.protectEnabled = islandTag.getBoolean("protectEnabled");
        ip.kickEnabled = islandTag.getBoolean("kickEnabled");
        if (islandTag.hasKey("members")) {
            NBTTagList memberList = islandTag.getTagList("members");
            for (int i = 0; i < memberList.tagCount(); i++) {
                ip.members.add(((NBTTagString) memberList.tagAt(i)).data);
            }
        }
        return ip;
    }

    public static void trySyncIslandPositions(World world) {
        if (islandPositionsDirty) {
            writeGlobalIslandData(world.getWorldInfo().getNBTTagCompound());
            islandPositionsDirty = false;
        }
    }

    private static final Map<String, String> globalMemberMap = new HashMap<>();

    public static void setGlobalMember(String member, String owner) {
        if (owner == null) globalMemberMap.remove(member);
        else globalMemberMap.put(member, owner);
    }
    public static String getGlobalMemberOwner(String member) {
        return globalMemberMap.get(member);
    }
    public static void writeGlobalMembersToNBT(NBTTagCompound worldTag) {
        NBTTagCompound memberTag = new NBTTagCompound();
        for (Map.Entry<String, String> e : globalMemberMap.entrySet()) {
            memberTag.setString(e.getKey(), e.getValue());
        }
        worldTag.setTag("SkyBlockGlobalMembers", memberTag);
    }
    public static void readGlobalMembersFromNBT(NBTTagCompound worldTag) {
        globalMemberMap.clear();
        if (!worldTag.hasKey("SkyBlockGlobalMembers")) return;
        NBTTagCompound memberTag = worldTag.getCompoundTag("SkyBlockGlobalMembers");
        for (Object tagObj : memberTag.getTags()) {
            if (tagObj instanceof NBTTagString tag) {
                globalMemberMap.put(tag.getName(), tag.data);
            }
        }
    }

    public static void clearAll() {
        spIslands.clear();
        usedIslandPositions.clear();
        globalMemberMap.clear();
    }

}