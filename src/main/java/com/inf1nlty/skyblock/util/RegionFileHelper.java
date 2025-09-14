package com.inf1nlty.skyblock.util;

import com.inf1nlty.skyblock.mixin.world.RegionFileCacheAccessor;
import net.minecraft.src.RegionFile;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class RegionFileHelper {
    public static void closeRegionFile(File regionFile) {
        Map<File, Object> map = RegionFileCacheAccessor.getRegionsByFilename();
        RegionFile rf = (RegionFile) map.get(regionFile);
        if (rf != null) {
            try {
                rf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            map.remove(regionFile);
        }
    }
}