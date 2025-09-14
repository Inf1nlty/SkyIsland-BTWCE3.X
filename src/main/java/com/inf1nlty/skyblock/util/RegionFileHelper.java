package com.inf1nlty.skyblock.util;

import com.inf1nlty.skyblock.mixin.accessor.RegionFileCacheAccessor;
import net.minecraft.src.RegionFile;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RegionFileHelper {

    private static final Logger LOGGER = Logger.getLogger(RegionFileHelper.class.getName());

    public static void closeRegionFile(File regionFile) {

        Map<File, Object> map = RegionFileCacheAccessor.getRegionsByFilename();
        RegionFile rf = (RegionFile) map.get(regionFile);

        if (rf != null) {
            try {
                rf.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to close region file: " + regionFile.getAbsolutePath(), e);
            }
            map.remove(regionFile);
        }
    }
}