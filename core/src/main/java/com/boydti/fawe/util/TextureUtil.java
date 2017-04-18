package com.boydti.fawe.util;

import com.sk89q.worldedit.world.registry.BundledBlockData;
import java.awt.Color;
import java.io.File;
import java.io.FilenameFilter;

public class TextureUtil {
    private final File folder;
    private Color[] colors;

    public TextureUtil(File folder) {
        this.folder = folder;
        BundledBlockData bundled = BundledBlockData.getInstance();
        for (File file : folder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".png");
            }
        })) {
            String name = file.getName().split("\\.")[0];

        }
    }
//
//    public Color getColor(BaseBlock block) {
//        long r;
//        long b;
//        long g;
//        long a;
//
//    }
}
