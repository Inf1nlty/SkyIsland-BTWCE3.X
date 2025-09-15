package com.inf1nlty.skyblock.util;

import com.inf1nlty.skyblock.command.SkyBlockCommand;
import net.minecraft.src.*;

public class SkyBlockProtectionUtil {

    public static boolean isInRegion(EntityPlayer player, SkyBlockPoint island) {
        if (player == null) return false;
        if (player.dimension != island.dim) return false;

        double dx = Math.abs(player.posX - island.x);
        double dz = Math.abs(player.posZ - island.z);

        return dx <= 512.0 && dz <= 512.0;
    }

    public static boolean hasIslandPermission(EntityPlayer player, SkyBlockPoint island) {
        if (player == null) return false;

        if (player.capabilities.isCreativeMode) return true;

        if (island.owner.equals(player.username)) return true;

        if (island.members.contains(player.username)) return true;

        return false;
    }

    public static SkyBlockPoint getProtectedIslandAt(EntityPlayer player, double x, double z, int dim) {

        for (SkyBlockPoint island : SkyBlockDataManager.getAllIslands()) {

            if (!island.protectEnabled) continue;
            if (island.dim != dim) continue;

            double dx = Math.abs(x - island.x);
            double dz = Math.abs(z - island.z);

            if (dx <= 512.0 && dz <= 512.0 && !hasIslandPermission(player, island)) {
                return island;
            }
        }

        return null;
    }

    public static boolean isInBedrockRoom(double x, double z, int dim) {
        int centerX = 0, centerZ = 0, protection = 20, roomDim = 0;
        return dim == roomDim && Math.abs(x - centerX) <= protection && Math.abs(z - centerZ) <= protection;
    }

    public static boolean protectBedrockRoom(EntityPlayer player, double x, double z, int dim) {
        if (player == null || player.capabilities.isCreativeMode) return false;
        return isInBedrockRoom(x, z, dim);
    }

    public static boolean shouldDenyInteraction(EntityPlayer player, double x, double z, int dim) {
        if (protectBedrockRoom(player, x, z, dim)) return true;
        return getProtectedIslandAt(player, x, z, dim) != null;
    }

    public static boolean denyInteractionIfProtected(EntityPlayer player, double x, double z, int dim) {

        if (protectBedrockRoom(player, x, z, dim)) {
            sendBedrockRoomDenyMessage(player);
            return true;
        }

        if (getProtectedIslandAt(player, x, z, dim) != null) {
            sendProtectDenyMessage(player);
            return true;
        }

        return false;
    }

    public static void sendBedrockRoomDenyMessage(EntityPlayer player) {
        if (player != null) {
            player.sendChatToPlayer(SkyBlockCommand.createMessage("commands.protection.bedrock_room.deny", EnumChatFormatting.RED, false, false, false));
        }
    }

    public static void sendProtectDenyMessage(EntityPlayer player) {
        if (player != null) {
            player.sendChatToPlayer(SkyBlockCommand.createMessage("commands.island.protection.deny", EnumChatFormatting.RED, false, false, false));
        }
    }

    public static void checkSkyBlockProtection(World world) {

        for (Object obj : world.playerEntities) {
            EntityPlayerMP player = (EntityPlayerMP) obj;

            if (player.capabilities.isCreativeMode) continue;

            for (SkyBlockPoint island : SkyBlockDataManager.getAllIslands()) {

                if (!island.kickEnabled) continue;

                if (player.username.equals(island.owner) || island.members.contains(player.username)) continue;

                if (player.dimension != island.dim) continue;

                if (isInRegion(player, island)) {

                    player.setPositionAndUpdate(0.5, 101.0, 0.5);
                    player.sendChatToPlayer(SkyBlockCommand.createMessage("commands.island.protection.kicked", EnumChatFormatting.RED, false, false, false));

                    break;
                }
            }
        }
    }

}