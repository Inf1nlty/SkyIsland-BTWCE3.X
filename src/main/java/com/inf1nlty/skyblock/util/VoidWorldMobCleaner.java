package com.inf1nlty.skyblock.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityMob;
import net.minecraft.src.EntityLivingBase;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.WorldServer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility for automatically removing natural mobs in void world after a certain time.
 * - Mobs without a target are always removed after 3 minutes.
 * - Mobs with a target are only removed after 3 minutes if their target player is AFK.
 */
public class VoidWorldMobCleaner {
    // Tracks mob spawn tick by UUID
    private static final Map<UUID, Integer> mobSpawnTickMap = new HashMap<>();
    private static final int MAX_ALIVE_TICKS = 3600; // 3 minutes
    private static final int MOB_CLEAN_WHEN_AFK_TICKS = 3600; // 3 minutes

    /**
     * Called every server tick to check and remove mobs in void world if needed.
     */
    public static void onServerTick(WorldServer world) {
        // Only process if void world is loaded
        if (!SkyBlockWorldUtil.isVoidWorldLoaded()) {
            mobSpawnTickMap.clear();
            return;
        }

        int curTick = MinecraftServer.getServer().getTickCounter();

        for (Object obj : world.loadedEntityList) {
            if (obj instanceof EntityMob mob) {
                UUID uuid = mob.getUniqueID();
                mobSpawnTickMap.putIfAbsent(uuid, curTick);
                int spawnTick = mobSpawnTickMap.get(uuid);

                EntityLivingBase target = mob.getAttackTarget();
                if (target == null) {
                    // No target: remove if alive too long
                    if (curTick - spawnTick >= MAX_ALIVE_TICKS) {
                        mob.setDead();
                        mobSpawnTickMap.remove(uuid);
                    }
                } else if (target instanceof EntityPlayer player) {
                    // Has player target: remove only if player is AFK
                    PlayerAFKChecker.updatePlayerMove(player, curTick);
                    if (PlayerAFKChecker.isAFK(player, curTick)) {
                        if (curTick - spawnTick >= MOB_CLEAN_WHEN_AFK_TICKS) {
                            mob.setDead();
                            mobSpawnTickMap.remove(uuid);
                        }
                    }
                    // If player is active, do not remove
                }
            }
        }

        // Clean up map entries for mobs which no longer exist
        mobSpawnTickMap.keySet().removeIf(uuid -> {
            boolean stillLoaded = false;
            for (Object obj : world.loadedEntityList) {
                if (obj instanceof EntityMob && ((EntityMob) obj).getUniqueID().equals(uuid)) {
                    stillLoaded = true;
                    break;
                }
            }
            return !stillLoaded;
        });
    }
}