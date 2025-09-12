package com.inf1nlty.skyblock.util;

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

    public static Iterable<SkyBlockPoint> getAllIslands() {
        return playerIslands.values();
    }

    public static void checkSkyBlockProtection(World world) {
        for (Object obj : world.playerEntities) {
            EntityPlayerMP player = (EntityPlayerMP) obj;
             if (player.capabilities.isCreativeMode) continue;
            for (SkyBlockPoint island : SkyBlockDataManager.getAllIslands()) {
                if (!island.protectEnabled) continue;
                if (player.username.equals(island.owner) || island.members.contains(player.username)) continue;
                if (player.dimension != island.dim) continue;
                if (island.isInProtectRegion(player)) {
                    double kickRadius = 33.0;
                    double angle = Math.atan2(player.posZ - island.initSpawnZ, player.posX - island.initSpawnX);
                    double newX = island.initSpawnX + Math.cos(angle) * kickRadius;
                    double newZ = island.initSpawnZ + Math.sin(angle) * kickRadius;
                    player.setPositionAndUpdate(newX, player.posY, newZ);
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.protection.kicked").setColor(EnumChatFormatting.RED));
                    break;
                }
            }
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
        islandTag.setDouble("initSpawnX", ip.initSpawnX);
        islandTag.setDouble("initSpawnY", ip.initSpawnY);
        islandTag.setDouble("initSpawnZ", ip.initSpawnZ);
        islandTag.setBoolean("tpaEnabled", ip.tpaEnabled);
        NBTTagList memberList = new NBTTagList();
        for (String member : ip.members) memberList.appendTag(new NBTTagString(member));
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
}