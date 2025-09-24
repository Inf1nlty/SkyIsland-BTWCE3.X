package com.inf1nlty.skyblock.network;

import btw.BTWAddon;
import net.minecraft.src.*;

import java.io.*;

/**
 * Channel for synchronizing voidworld generatorOptions from server to client.
 */
public final class VoidWorldSyncNet {

    public static String CHANNEL;
    private static final int OPCODE_SYNC_GEN_OPTIONS = 1;

    public static String clientGeneratorOptions = "";

    private VoidWorldSyncNet() {}

    public static void register(BTWAddon addon) {
        CHANNEL = addon.getModID() + "|VW";

        addon.registerPacketHandler(CHANNEL, (packet, player) -> handleClientPacket(packet));
    }

    public static void sendGeneratorOptionsTo(EntityPlayerMP player) {
        if (player == null || player.worldObj == null) return;

        String options = player.worldObj.getWorldInfo().getGeneratorOptions();

        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeByte(OPCODE_SYNC_GEN_OPTIONS);
            dos.writeUTF(options != null ? options : "");
            dos.close();
            Packet250CustomPayload pkt = new Packet250CustomPayload();
            pkt.channel = CHANNEL;
            pkt.data = bos.toByteArray();
            pkt.length = pkt.data.length;
            player.playerNetServerHandler.sendPacketToPlayer(pkt);
        } catch (Exception ignored) {}
    }

    public static void handleClientPacket(Packet250CustomPayload packet) {
        if (packet == null || packet.data == null) return;

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(packet.data))) {
            int op = in.readUnsignedByte();
            if (op == OPCODE_SYNC_GEN_OPTIONS) {
                clientGeneratorOptions = in.readUTF();
            }
        } catch (Exception ignored) {}
    }
}