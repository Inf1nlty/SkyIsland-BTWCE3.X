package com.inf1nlty.skyblock.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for automatically removing natural mobs in void world after a certain time.
 * - Mobs without a target are always removed after 1 minute.
 * - Mobs with a target are only removed after 3 minutes if their target player is AFK.
 */
public class VoidWorldMobCleaner {
    // Tracks mob spawn tick by UUID
    private static final Map<UUID, Integer> mobSpawnTickMap = new HashMap<>();
    private static final int MOB_CLEAN_WHEN_AFK_TICKS = 1200; // 1 minute
    private static final int MOB_CLEAN_WHEN_TARGET_AFK_TICKS = 3600; // 3 minutes

    private static boolean shouldClean(Object obj) {
        return (obj instanceof IMob
                || obj instanceof EntityWaterMob
                || obj instanceof EntityBat
                || (obj instanceof EntityOcelot && !((EntityOcelot)obj).isTamed()))
                && obj instanceof EntityLiving;
    }

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

        List<EntityLiving> toRemove = new ArrayList<>();

        for (Object obj : world.loadedEntityList) {
            if (shouldClean(obj) && obj instanceof EntityLiving mob) {
                if (mob instanceof EntityVillager) {
                    continue;
                }
                // Skip valuable mobs that should be preserved
                if (shouldPreserveMob(mob)) {
                    continue;
                }

                UUID uuid = mob.getUniqueID();
                mobSpawnTickMap.putIfAbsent(uuid, curTick);
                int spawnTick = mobSpawnTickMap.get(uuid);

                EntityLivingBase target = null;
                if (mob instanceof EntityMob entityMob) {
                    target = entityMob.getAttackTarget();
                }

                // Check distance to nearest player for enhanced cleaning logic
                EntityPlayer nearestPlayer = world.getClosestPlayerToEntity(mob, 64.0D);
                double distanceToPlayer = nearestPlayer != null ? mob.getDistanceToEntity(nearestPlayer) : Double.MAX_VALUE;

                if (target == null) {
                    // No target: enhanced cleaning based on distance
                    boolean shouldClean = false;

                    // Skip mobs outside the loaded chunk area (default view-distance: 8, so 128 blocks)
                    if (distanceToPlayer > 128.0D) {
                        // Do not process mobs outside the loaded area (not ticked)
                        continue;
                    }
                    // All mobs inside the loaded area: only clean if nearest player is AFK and mob has existed at least 1 minute
                    PlayerAFKChecker.updatePlayerMove(nearestPlayer, curTick);
                    if (PlayerAFKChecker.isAFK(nearestPlayer, curTick)) {
                        shouldClean = (curTick - spawnTick >= MOB_CLEAN_WHEN_AFK_TICKS);
                    }

                    if (shouldClean) {
                        toRemove.add(mob);
                        mobSpawnTickMap.remove(uuid);
                    }

                } else if (target instanceof EntityPlayer player) {
                    // Has player target: remove only if player is AFK
                    PlayerAFKChecker.updatePlayerMove(player, curTick);
                    if (PlayerAFKChecker.isAFK(player, curTick)) {
                        if (curTick - spawnTick >= MOB_CLEAN_WHEN_TARGET_AFK_TICKS) {
                            toRemove.add(mob);
                            mobSpawnTickMap.remove(uuid);
                        }
                    }
                    // If player is active, do not remove
                }
            }
        }

        for (EntityLiving mob : toRemove) {
            mob.setDead();
        }

        // Clean up map entries for mobs which no longer exist
        mobSpawnTickMap.keySet().removeIf(uuid -> {
            boolean stillLoaded = false;
            for (Object obj : world.loadedEntityList) {
                if (shouldClean(obj)
                        && obj instanceof EntityLiving
                        && ((EntityLiving) obj).getUniqueID().equals(uuid)) {
                    stillLoaded = true;
                    break;
                }
            }
            return !stillLoaded;
        });
    }

    private static boolean shouldPreserveMob(EntityLiving mob) {
        // Preserve mobs with custom names
        if (mob.hasCustomNameTag()) {
            return true;
        }

        // Preserve zombie villagers
        if (mob instanceof EntityZombie zombie && zombie.isVillager()) {
            return true;
        }

        // Preserve boss mobs (Dragon, Wither)
        if (mob instanceof EntityDragon || mob instanceof EntityWither) {
            return true;
        }

        // Preserve wither bosses by class name check
        String className = mob.getClass().getSimpleName().toLowerCase();
        if (className.contains("wither")) {
            return true;
        }

        // For EntityMob types, check equipment
        if (mob instanceof EntityMob entityMob) {
            // Preserve mobs holding items
            if (entityMob.getHeldItem() != null) {
                return true;
            }

            // Preserve mobs wearing armor
            for (int i = 1; i <= 4; i++) {
                if (entityMob.getCurrentItemOrArmor(i) != null) {
                    return true;
                }
            }
        }

        return false;
    }
}