package com.inf1nlty.skyblock;

import java.io.*;
import java.util.Properties;

public class SkyblockConfig {

    public static boolean ALLOW_ANIMAL_SPAWN_ON_GRASSSLAB = false;
    public static boolean OVERWORLD_NETHER_COORD_RATIO_1_1 = true;

    private static final String CONFIG_PATH = "config/skyblock.cfg";

    private SkyblockConfig() {}

    public static synchronized void reload() {
        File file = new File(CONFIG_PATH);
        if (!file.exists()) {
            generateDefault(file);
        }
        loadInternal(file);
    }

    private static void loadInternal(File file) {
        Properties prop = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            prop.load(fis);
        } catch (Exception ignored) {}

        ALLOW_ANIMAL_SPAWN_ON_GRASSSLAB = Boolean.parseBoolean(
                prop.getProperty("allow_animal_spawn_on_grassslab", "false")
        );
        OVERWORLD_NETHER_COORD_RATIO_1_1 = Boolean.parseBoolean(
                prop.getProperty("overworld_nether_coord_ratio_1_1", "true")
        );
    }

    private static void generateDefault(File file) {
        try {
            File dir = file.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            if (!file.exists()) file.createNewFile();
            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                w.write("# Skyblock 配置\n");
                w.write("# Skyblock configuration\n\n");

                w.write("# 是否允许在草半砖上自然生成动物 (true=允许, false=禁止)\n");
                w.write("# Whether to allow animals to spawn naturally on grass slabs (true=allow, false=disallow)\n");
                w.write("allow_animal_spawn_on_grassslab=false\n\n");

                w.write("# 是否将主世界-地狱坐标比例从8:1改为1:1，防止窜门 (true=1:1, false=原版8:1)\n");
                w.write("# Change the Overworld-Hell coordinate ratio from 8:1 to 1:1 to prevent cross-talk (true=1:1, false=original 8:1)\n");
                w.write("overworld_nether_coord_ratio_1_1=true\n");
            }
        } catch (Exception ignored) {}
    }
}