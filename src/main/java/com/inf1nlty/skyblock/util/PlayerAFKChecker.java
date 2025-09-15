package com.inf1nlty.skyblock.util;

import net.minecraft.src.EntityPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility for detecting if a player is AFK (away from keyboard) in a void world.
 * - Tracks the last movement tick of each player by username.
 * - Only updates and checks AFK status when the void world is loaded.
 * - Considers a player AFK if they have not moved for at least 1 minute (1200 ticks).
 */
public class PlayerAFKChecker {
    // Tracks the last tick each player moved
    private static final Map<String, Integer> lastMoveTick = new HashMap<>();
    // Tracks the last known position of each player
    private static final Map<String, double[]> lastPos = new HashMap<>();
    private static final int AFK_TICKS = 1200; // 1 minute at 20 TPS

    /**
     * Updates the last movement tick of a player if they have changed position.
     * Only operates if the void world is currently loaded.
     */
    public static void updatePlayerMove(EntityPlayer player, int curTick) {
        if (!SkyBlockWorldUtil.isVoidWorldLoaded()) return;

        String id = player.username;
        double[] pos = lastPos.get(id);
        if (pos == null || pos[0] != player.posX || pos[1] != player.posY || pos[2] != player.posZ) {
            lastMoveTick.put(id, curTick);
            lastPos.put(id, new double[]{player.posX, player.posY, player.posZ});
        }
    }

    /**
     * Checks if a player is AFK (has not moved for at least AFK_TICKS).
     * Only operates if the void world is currently loaded.
     */
    public static boolean isAFK(EntityPlayer player, int curTick) {
        if (!SkyBlockWorldUtil.isVoidWorldLoaded()) return false;

        String id = player.username;
        Integer last = lastMoveTick.get(id);
        if (last == null) return false;
        return curTick - last >= AFK_TICKS;
    }
}