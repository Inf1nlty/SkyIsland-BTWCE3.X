package com.inf1nlty.skyisland.util;

import net.minecraft.src.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Loads and stores schematic block/meta/tileentity data.
 */
public class SchematicData {
    public final int width, length, height;
    public final byte[] blocks;
    public final byte[] data;
    public final byte[] addBlocks;
    public final ArrayList<NBTTagCompound> tileEntities;

    public SchematicData(int width, int length, int height, byte[] blocks, byte[] data, byte[] addBlocks, ArrayList<NBTTagCompound> tileEntities) {
        this.width = width;
        this.length = length;
        this.height = height;
        this.blocks = blocks;
        this.data = data;
        this.addBlocks = addBlocks;
        this.tileEntities = tileEntities;
    }

    public int getBlockId(int idx) {
        int id = blocks[idx] & 0xFF;
        if (addBlocks != null && idx / 2 < addBlocks.length) {
            int add = (idx % 2 == 0)
                    ? (addBlocks[idx / 2] & 0x0F)
                    : ((addBlocks[idx / 2] >> 4) & 0x0F);
            id |= (add << 8);
        }
        return id;
    }

    public int getBlockMeta(int idx) {
        return data[idx] & 0xFF;
    }

    public static SchematicData loadSchematic(String path) {
        File file = new File(path);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(fis);
                return parseNBT(nbt);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            try (InputStream is = SchematicData.class.getClassLoader().getResourceAsStream(path)) {
                if (is == null) {
                    System.out.println("Schematic resource not found: " + path);
                    return null;
                }
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(is);
                return parseNBT(nbt);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    private static SchematicData parseNBT(NBTTagCompound nbt) {
        int width = nbt.getShort("Width");
        int length = nbt.getShort("Length");
        int height = nbt.getShort("Height");
        byte[] blocks = nbt.getByteArray("Blocks");
        byte[] data = nbt.getByteArray("Data");
        byte[] addBlocks = nbt.hasKey("AddBlocks") ? nbt.getByteArray("AddBlocks") : null;
        ArrayList<NBTTagCompound> tileEntities = new ArrayList<>();
        NBTTagList teList = nbt.getTagList("TileEntities");
        for (int i = 0; i < teList.tagCount(); ++i) {
            tileEntities.add((NBTTagCompound) teList.tagAt(i));
        }
        return new SchematicData(width, length, height, blocks, data, addBlocks, tileEntities);
    }
}