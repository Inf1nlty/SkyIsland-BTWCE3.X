package com.inf1nlty.skyblock.util;

import net.minecraft.src.*;

import java.util.Arrays;

/**
 * Compatible with the vanilla/BWG/BTW void world generators
 * - Generates void islands when generatorOptions == "skyblock"
 * - Delegates all other types to the vanilla generator, ensuring normal generation of the Overworld/Nether/The End/BTW dimensions, etc.
 */
public class VoidWorldChunkProvider extends ChunkProviderGenerate {

    public VoidWorldChunkProvider(World world, long seed, boolean mapFeaturesEnabled) {
        super(world, seed, mapFeaturesEnabled);
    }

    // ==== Constantized room coordinates ====
    private static final int HALF_OUTER = 6; // Outer shell radius
    private static final int HALF_INNER = 5; // Inner space radius
    private static final int DESIRED_FLOOR_Y = 99; // Floor Y
    private static final int CENTER_Y = DESIRED_FLOOR_Y + HALF_INNER; // Room center Y
    private static final int TORCH_BLOCK_ID = 1057;
    private static final int SIGN_WALL_ID = Block.signWall.blockID;

    // Torches at the four corners of the room floor
    private static final int[][] ROOM_TORCH_POSITIONS = {
            {4, DESIRED_FLOOR_Y, 4},
            {-5, DESIRED_FLOOR_Y, 4},
            {4, DESIRED_FLOOR_Y, -5},
            {-5, DESIRED_FLOOR_Y, -5}
    };

    // Four torches outside the room
    private static final int TOP_Y = CENTER_Y + HALF_OUTER;
    private static final int[][] ROOF_TORCH_POSITIONS = {
            {HALF_OUTER - 1, TOP_Y, HALF_OUTER - 1},
            {HALF_OUTER - 1, TOP_Y, -HALF_OUTER},
            {-HALF_OUTER, TOP_Y, HALF_OUTER - 1},
            {-HALF_OUTER, TOP_Y, -HALF_OUTER}
    };

    // South wall sign (z=4, behind z=5, south meta=2)
    private static final int SIGN_Y = DESIRED_FLOOR_Y + 1;
    private static final int SIGN_SOUTH_WALL_Z = 4;
    private static final int SIGN_SOUTH_BEDROCK_Z = 5;
    private static final int SIGN_SOUTH_META = 2;

    // North wall sign (z=-5, back z=-6, north meta=3)
    private static final int SIGN_NORTH_WALL_Z = -5;
    private static final int SIGN_NORTH_BEDROCK_Z = -6;
    private static final int SIGN_NORTH_META = 3;

    // South,Right-Left
    private static final String[][] SIGN_SOUTH_TEXTS = {
            {"", "", "", ""},
            {"/is j '玩家名'申请加入他人岛屿", "/is r 玩家名移除成员", "/is a/deny 玩家名同意/拒绝", "均支持自动补全名字"},
            {"/is s 设置岛屿传送点", "X/Z各30格限制", "/is p on/off 切换保护", "非成员靠近将被弹出"},
            {"/is setyes/setno", "开关传送请求", "默认开启", ""},
            {"/is tp 请求传送", "到其他玩家空岛", "/is yes/no", "同意或拒绝请求"},
            {"/is i 查询信息", "/is d 删除空岛", "然后需要在60秒内输入", "/is d c 以确认"},
            {"/is n 创建岛屿", "/is 返回岛屿", "需已创建或加入", ""},
            {"本模组大部分指令可简写", "如 /is /is n /is j 等", "", ""},
            {"在虚空世界里","主世界全是空气", "不会生成建筑", "但拥有正常群系"},
            {"欢迎来到", "虚空世界", "请勿在出生点捣乱", "谢谢"}
    };

    // North,Left-Right
    private static final String[][] SIGN_NORTH_TEXTS = {
            {"Welcome to", "Void World", "please don’t mess up", "in the spawn area"},
            {"In the void world","the overworld is all air", "no structures will generate", "but biomes are normal"},
            {"Most commands support", "initial letter shortcuts", "like /is /is n /is j", ""},
            {"/is n to create", "/is to return", "must have created or joined", "an island"},
            {"/is i to view info", "/is d to delete", "/is d c to confirm", ""},
            {"/is tp to request", "teleport to another island", "/is yes/no", "to accept or decline"},
            {"/is setyes/setno to toggle", "teleport requests", "enabled by default", ""},
            {"/is s to set island tp-point", "limited to 30 blocks X/Z", "/is p on/off to toggle protection", "non-members will be ejected"},
            {"/is j 'name' to request to join", "/is r 'name' to remove member", "/is a/deny player to accept/deny", "name auto-completion supported"},
            {"", "", "", ""},
            {"", "", "", ""}
    };

    @Override
    public void generateTerrain(int chunkX, int chunkZ, short[] blockIDs, byte[] metadata) {
        if (SkyBlockWorldUtil.isVoidOverworld(this.worldObj)) {
            Arrays.fill(blockIDs, (short) 0);
            if (metadata != null) {
                Arrays.fill(metadata, (byte) 0);
            }
        } else {
            super.generateTerrain(chunkX, chunkZ, blockIDs, metadata);
        }
    }

    @Override
    public void replaceBlocksForBiome(int chunkX, int chunkZ, short[] blockIDs, byte[] metadata, BiomeGenBase[] biomes) {
        if (SkyBlockWorldUtil.isVoidOverworld(this.worldObj)) return;
        super.replaceBlocksForBiome(chunkX, chunkZ, blockIDs, metadata, biomes);
    }

    @Override
    public Chunk provideChunk(int chunkX, int chunkZ) {
        if (SkyBlockWorldUtil.isVoidOverworld(this.worldObj)) {
            short[] blockIDs = new short[16 * 16 * 128];
            byte[] metadata = new byte[16 * 16 * 128];
            generateTerrain(chunkX, chunkZ, blockIDs, metadata);

            this.biomesForGeneration = this.worldObj.getWorldChunkManager().loadBlockGeneratorData(
                    this.biomesForGeneration, chunkX * 16, chunkZ * 16, 16, 16);

            replaceBlocksForBiome(chunkX, chunkZ, blockIDs, metadata, this.biomesForGeneration);

            Chunk chunk = new Chunk(this.worldObj, chunkX, chunkZ);
            byte[] biomeArr = chunk.getBiomeArray();
            for (int i = 0; i < biomeArr.length; ++i) {
                biomeArr[i] = (byte) this.biomesForGeneration[i].biomeID;
            }
            chunk.generateSkylightMap();

            // Generate bedrock room
            for (int bx = -HALF_OUTER; bx <= HALF_OUTER - 1; bx++) {
                for (int by = -HALF_OUTER; by <= HALF_OUTER - 1; by++) {
                    for (int bz = -HALF_OUTER; bz <= HALF_OUTER - 1; bz++) {
                        boolean isOuter =
                                bx == -HALF_OUTER || bx == HALF_INNER ||
                                        by == -HALF_OUTER || by == HALF_INNER ||
                                        bz == -HALF_OUTER || bz == HALF_INNER;
                        if (isOuter) {
                            int worldX = bx;
                            int worldY = CENTER_Y + by;
                            int worldZ = bz;
                            if (((worldX) >> 4) == chunkX && ((worldZ) >> 4) == chunkZ) {
                                int localX = worldX & 15;
                                int localY = worldY;
                                int localZ = worldZ & 15;
                                int storageIdx = localY >> 4;
                                int yInStorage = localY & 15;
                                if (chunk.getBlockStorageArray()[storageIdx] == null) {
                                    chunk.getBlockStorageArray()[storageIdx] = new ExtendedBlockStorage(storageIdx << 4, !worldObj.provider.hasNoSky);
                                }
                                chunk.getBlockStorageArray()[storageIdx].setExtBlockID(localX, yInStorage, localZ, Block.bedrock.blockID);
                            }
                        }
                    }
                }
            }

            // Torches at the four corners of the room floor
            for (int[] pos : ROOM_TORCH_POSITIONS) {
                int tx = pos[0], ty = pos[1], tz = pos[2];
                if ((tx >> 4) == chunkX && (tz >> 4) == chunkZ) {
                    int localX = tx & 15;
                    int localY = ty;
                    int localZ = tz & 15;
                    int storageIdx = localY >> 4;
                    int yInStorage = localY & 15;
                    if (chunk.getBlockStorageArray()[storageIdx] == null) {
                        chunk.getBlockStorageArray()[storageIdx] = new ExtendedBlockStorage(storageIdx << 4, !worldObj.provider.hasNoSky);
                    }
                    chunk.getBlockStorageArray()[storageIdx].setExtBlockID(localX, yInStorage, localZ, TORCH_BLOCK_ID);
                }
            }

            // Four torches outside the room
            for (int[] pos : ROOF_TORCH_POSITIONS) {
                int tx = pos[0], ty = pos[1], tz = pos[2];
                if ((tx >> 4) == chunkX && (tz >> 4) == chunkZ) {
                    int localX = tx & 15;
                    int localY = ty;
                    int localZ = tz & 15;
                    int storageIdx = localY >> 4;
                    int yInStorage = localY & 15;
                    if (chunk.getBlockStorageArray()[storageIdx] == null) {
                        chunk.getBlockStorageArray()[storageIdx] = new ExtendedBlockStorage(storageIdx << 4, !worldObj.provider.hasNoSky);
                    }
                    chunk.getBlockStorageArray()[storageIdx].setExtBlockID(localX, yInStorage, localZ, TORCH_BLOCK_ID);
                }
            }

            // A row of notice boards on the south wall
            for (int signX = -5; signX <= 4; signX++) {
                int idx = signX + 5; // x:-5~4 => idx:0~9
                if ((signX >> 4) == chunkX && (SIGN_SOUTH_WALL_Z >> 4) == chunkZ) {
                    int localX = signX & 15;
                    int localZ = SIGN_SOUTH_WALL_Z & 15;
                    int bedrockLocalZ = SIGN_SOUTH_BEDROCK_Z & 15;
                    int localY = SIGN_Y + (idx % 2);
                    int storageIdx = localY >> 4;
                    int yInStorage = localY & 15;
                    if (chunk.getBlockStorageArray()[storageIdx] == null) {
                        chunk.getBlockStorageArray()[storageIdx] = new ExtendedBlockStorage(storageIdx << 4, !worldObj.provider.hasNoSky);
                    }
                    if (chunk.getBlockStorageArray()[storageIdx].getExtBlockID(localX, yInStorage, bedrockLocalZ) != Block.bedrock.blockID) {
                        chunk.getBlockStorageArray()[storageIdx].setExtBlockID(localX, yInStorage, bedrockLocalZ, Block.bedrock.blockID);
                    }
                    if (chunk.getBlockStorageArray()[storageIdx].getExtBlockID(localX, yInStorage, localZ) == 0) {
                        chunk.getBlockStorageArray()[storageIdx].setExtBlockID(localX, yInStorage, localZ, SIGN_WALL_ID);
                        chunk.getBlockStorageArray()[storageIdx].setExtBlockMetadata(localX, yInStorage, localZ, SIGN_SOUTH_META);

                        TileEntitySign sign = new TileEntitySign();
                        sign.setWorldObj(worldObj);
                        sign.xCoord = signX;
                        sign.yCoord = SIGN_Y + (idx % 2);
                        sign.zCoord = SIGN_SOUTH_WALL_Z;
                        sign.signText[0] = SIGN_SOUTH_TEXTS[idx][0];
                        sign.signText[1] = SIGN_SOUTH_TEXTS[idx][1];
                        sign.signText[2] = SIGN_SOUTH_TEXTS[idx][2];
                        sign.signText[3] = SIGN_SOUTH_TEXTS[idx][3];
                        chunk.addTileEntity(sign);
                    }
                }
            }

            // A row of signs on the north wall
            for (int signX = -5; signX <= 4; signX++) {
                int idx = signX + 5; // x:-5~4 => idx:0~9
                if ((signX >> 4) == chunkX && (SIGN_NORTH_WALL_Z >> 4) == chunkZ) {
                    int localX = signX & 15;
                    int localZ = SIGN_NORTH_WALL_Z & 15;
                    int bedrockLocalZ = SIGN_NORTH_BEDROCK_Z & 15;
                    int localY = SIGN_Y + (idx % 2);
                    int storageIdx = localY >> 4;
                    int yInStorage = localY & 15;
                    if (chunk.getBlockStorageArray()[storageIdx] == null) {
                        chunk.getBlockStorageArray()[storageIdx] = new ExtendedBlockStorage(storageIdx << 4, !worldObj.provider.hasNoSky);
                    }
                    if (chunk.getBlockStorageArray()[storageIdx].getExtBlockID(localX, yInStorage, bedrockLocalZ) != Block.bedrock.blockID) {
                        chunk.getBlockStorageArray()[storageIdx].setExtBlockID(localX, yInStorage, bedrockLocalZ, Block.bedrock.blockID);
                    }
                    if (chunk.getBlockStorageArray()[storageIdx].getExtBlockID(localX, yInStorage, localZ) == 0) {
                        chunk.getBlockStorageArray()[storageIdx].setExtBlockID(localX, yInStorage, localZ, SIGN_WALL_ID);
                        chunk.getBlockStorageArray()[storageIdx].setExtBlockMetadata(localX, yInStorage, localZ, SIGN_NORTH_META);

                        TileEntitySign sign = new TileEntitySign();
                        sign.setWorldObj(worldObj);
                        sign.xCoord = signX;
                        sign.yCoord = SIGN_Y + (idx % 2);
                        sign.zCoord = SIGN_NORTH_WALL_Z;
                        sign.signText[0] = SIGN_NORTH_TEXTS[idx][0];
                        sign.signText[1] = SIGN_NORTH_TEXTS[idx][1];
                        sign.signText[2] = SIGN_NORTH_TEXTS[idx][2];
                        sign.signText[3] = SIGN_NORTH_TEXTS[idx][3];
                        chunk.addTileEntity(sign);
                    }
                }
            }
            return chunk;
        } else {
            return super.provideChunk(chunkX, chunkZ);
        }
    }

    @Override
    public void populate(IChunkProvider provider, int chunkX, int chunkZ) {
        if (SkyBlockWorldUtil.isVoidOverworld(this.worldObj)) return;
        super.populate(provider, chunkX, chunkZ);
    }

    public void btwPostProcessChunk(World worldObj, int iChunkX, int iChunkZ) {}
}