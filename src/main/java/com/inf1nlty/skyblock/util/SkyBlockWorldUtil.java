package com.inf1nlty.skyblock.util;

import com.inf1nlty.skyblock.network.VoidWorldSyncNet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.World;

public class SkyBlockWorldUtil {

    public static boolean isVoidWorld(World world) {
        return world != null && "voidworld".equals(world.getWorldInfo().getGeneratorOptions());
    }
    public static boolean isVoidOverworld(World world) {
        return isVoidWorld(world) && world.provider.dimensionId == 0;
    }

    public static boolean isVoidWorldLoaded() {
        MinecraftServer srv = MinecraftServer.getServer();
        if (srv == null || srv.worldServers[0] == null) return false;
        return "voidworld".equals(srv.worldServers[0].getWorldInfo().getGeneratorOptions());
    }

    public static boolean isVoidWorldClientSynced() {
        return "voidworld".equals(VoidWorldSyncNet.clientGeneratorOptions);
    }
}