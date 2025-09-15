package com.inf1nlty.skyblock.util;

import net.minecraft.src.*;

public class SkyBlockProtectionUtil {

    public static boolean isInRegion(EntityPlayer player, SkyBlockPoint island) {
        if (player.dimension != island.dim) return false;
        double dx = Math.abs(player.posX - island.initSpawnX);
        double dz = Math.abs(player.posZ - island.initSpawnZ);
        return dx <= 512.0 && dz <= 512.0;
    }

    public static boolean hasIslandPermission(EntityPlayer player, SkyBlockPoint island) {
        if (player.capabilities.isCreativeMode) return true;
        if (island.owner.equals(player.username)) return true;
        if (island.members.contains(player.username)) return true;
        return false;
    }

    public static SkyBlockPoint getProtectedIslandAt(EntityPlayer player, double x, double z, int dim) {
        for (SkyBlockPoint island : SkyBlockManager.getAllIslands()) {
            if (island.dim != dim) continue;
            double dx = Math.abs(x - island.initSpawnX);
            double dz = Math.abs(z - island.initSpawnZ);
            if (dx <= 512.0 && dz <= 512.0 && !hasIslandPermission(player, island)) {
                return island;
            }
        }
        return null;
    }

    public static boolean shouldDenyInteraction(EntityPlayer player, double x, double z, int dim) {
        return getProtectedIslandAt(player, x, z, dim) != null;
    }

    public static void sendProtectDenyMessage(EntityPlayer player) {
        player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.protection.deny").setColor(EnumChatFormatting.RED));
    }

    public static void checkSkyBlockProtection(World world) {
        for (Object obj : world.playerEntities) {
            EntityPlayerMP player = (EntityPlayerMP) obj;
            if (player.capabilities.isCreativeMode) continue;
            for (SkyBlockPoint island : SkyBlockManager.getAllIslands()) {
                if (!island.protectEnabled) continue;
                if (player.username.equals(island.owner) || island.members.contains(player.username)) continue;
                if (player.dimension != island.dim) continue;
                if (isInRegion(player, island)) {
                    player.setPositionAndUpdate(0.5, 101.0, 0.5);
                    player.sendChatToPlayer(ChatMessageComponent.createFromText("commands.island.protection.kicked").setColor(EnumChatFormatting.RED));
                    break;
                }
            }
        }
    }
}