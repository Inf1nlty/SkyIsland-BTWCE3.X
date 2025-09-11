package com.inf1nlty.skyblock.util;

import net.minecraft.server.MinecraftServer;

public class SkyBlockWorldUtil {

    public static boolean isVoidWorldLoaded() {
        MinecraftServer srv = MinecraftServer.getServer();
        if (srv == null || srv.worldServers[0] == null) return false;
        return "voidworld".equals(srv.worldServers[0].getWorldInfo().getGeneratorOptions());
    }
}