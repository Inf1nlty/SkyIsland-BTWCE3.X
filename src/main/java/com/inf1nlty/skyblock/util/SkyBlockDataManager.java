package com.inf1nlty.skyblock.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles island data persistence and retrieval.
 */
public class SkyBlockDataManager {
    private static final Map<String, SkyBlockPoint> playerIslands = new HashMap<>();
    private static final Set<String> everCreatedIslanders = new HashSet<>();
    private static final Map<String, String> globalMemberMap = new HashMap<>();

    public static SkyBlockPoint getIsland(EntityPlayerMP player) {
        return playerIslands.get(player.username);
    }

    public static boolean hasEverCreatedIsland(String username) {
        return everCreatedIslanders.contains(username);
    }

    public static void setIsland(EntityPlayerMP player, SkyBlockPoint ip) {
        if (ip == null) {
            playerIslands.remove(player.username);
        } else {
            playerIslands.put(player.username, ip);
            everCreatedIslanders.add(player.username);
        }
        NBTTagCompound tag = new NBTTagCompound();
        writeIslandToNBT(player, tag);
        player.writeToNBT(tag);
        MinecraftServer.getServer().getConfigurationManager().writePlayerData(player);
    }

    public static void setIsland(String username, SkyBlockPoint ip) {
        if (ip == null) {
            playerIslands.remove(username);
        } else {
            playerIslands.put(username, ip);
            everCreatedIslanders.add(username);
        }
    }

    public static SkyBlockPoint getIslandForMember(EntityPlayerMP player) {
        for (SkyBlockPoint ip : playerIslands.values()) {
            if (ip.members.contains(player.username)) {
                return ip;
            }
        }
        return null;
    }

    public static SkyBlockPoint getIsland(String username) {
        return playerIslands.get(username);
    }

    public static Iterable<SkyBlockPoint> getAllIslands() {
        return playerIslands.values();
    }

    /**
     * Writes the player's island data to NBT.
     */
    public static void writeIslandToNBT(EntityPlayerMP player, NBTTagCompound tag) {
        SkyBlockPoint ip = getIsland(player);
        if (ip == null || !player.username.equals(ip.owner)) return;
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
        islandTag.setBoolean("tpaEnabled", ip.tpaEnabled);
        NBTTagList memberList = new NBTTagList();
        for (String member : ip.members) memberList.appendTag(new NBTTagString(member, member));
        islandTag.setTag("members", memberList);
        islandTag.setBoolean("protectEnabled", ip.protectEnabled);
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
        ip.initSpawnX = islandTag.getDouble("initSpawnX");
        ip.initSpawnY = islandTag.getDouble("initSpawnY");
        ip.initSpawnZ = islandTag.getDouble("initSpawnZ");
        ip.tpaEnabled = islandTag.getBoolean("tpaEnabled");
        ip.protectEnabled = islandTag.hasKey("protectEnabled") && islandTag.getBoolean("protectEnabled");
        if (islandTag.hasKey("members")) {
            NBTTagList memberList = islandTag.getTagList("members");
            for (int i = 0; i < memberList.tagCount(); i++) {
                ip.members.add(((NBTTagString)memberList.tagAt(i)).data);
            }
        }
        setIsland(player, ip);
        String newKey = ip.dim + ":" + ip.x + ":" + ip.z;
        if (!SkyBlockManager.isUsedIslandPosition(newKey)) {
            SkyBlockManager.addUsedIslandPosition(newKey);
        }
    }

    public static void writeHistoryToNBT(EntityPlayerMP player, NBTTagCompound tag) {
        tag.setBoolean("hasEverCreatedIsland", everCreatedIslanders.contains(player.username));
    }

    public static void readHistoryFromNBT(EntityPlayerMP player, NBTTagCompound tag) {
        if (tag.hasKey("hasEverCreatedIsland") && tag.getBoolean("hasEverCreatedIsland")) {
            everCreatedIslanders.add(player.username);
        }
    }

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
        playerIslands.clear();
        everCreatedIslanders.clear();
        globalMemberMap.clear();
    }

}