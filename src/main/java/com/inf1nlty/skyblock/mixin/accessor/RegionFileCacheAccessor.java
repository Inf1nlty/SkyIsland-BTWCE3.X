package com.inf1nlty.skyblock.mixin.accessor;

import net.minecraft.src.RegionFileCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.io.File;
import java.util.Map;

@Mixin(RegionFileCache.class)
public interface RegionFileCacheAccessor {

    @Accessor("regionsByFilename")
    static Map<File, Object> getRegionsByFilename() {
        throw new AssertionError();
    }
}