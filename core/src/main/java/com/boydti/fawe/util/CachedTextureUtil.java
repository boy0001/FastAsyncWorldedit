package com.boydti.fawe.util;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import org.json.simple.parser.ParseException;

public class CachedTextureUtil extends TextureUtil {
    private final TextureUtil parent;
    private Int2ObjectOpenHashMap<Integer> colorBlockMap;
    private Int2ObjectOpenHashMap<char[]> colorLayerMap;

    private int[] validLayerColors;
    private char[][] validLayerBlocks;

    public CachedTextureUtil(TextureUtil parent) {
        super(parent.getFolder());
        this.parent = parent;
        this.colorBlockMap = new Int2ObjectOpenHashMap<>();
        this.colorLayerMap = new Int2ObjectOpenHashMap<>();
        Int2ObjectOpenHashMap<char[]> colorLayerMap = new Int2ObjectOpenHashMap<>();
        for (int i = 0; i < parent.validBlockIds.length; i++) {
            int color = parent.validColors[i];
            int combined = parent.validBlockIds[i];
            if (hasAlpha(color)) {
                for (int j = 0; j < parent.validBlockIds.length; j++) {
                    int colorOther = parent.validColors[j];
                    if (!hasAlpha(colorOther)) {
                        int combinedOther = parent.validBlockIds[j];
                        int combinedColor = combine(color, colorOther);
                        colorLayerMap.put(combinedColor, new char[] {(char) combined, (char) combinedOther});
                    }
                }
            }
        }
        this.validLayerColors = new int[colorLayerMap.size()];
        this.validLayerBlocks = new char[colorLayerMap.size()][];
        int index = 0;
        for (Int2ObjectMap.Entry<char[]> entry : colorLayerMap.int2ObjectEntrySet()) {
            validLayerColors[index] = entry.getIntKey();
            validLayerBlocks[index++] = entry.getValue();
        }
    }

    public char[] getNearestLayer(int color) {
        char[] closest = colorLayerMap.get(color);
        if (closest != null) {
            return closest;
        }
        long min = Long.MAX_VALUE;
        int red1 = (color >> 16) & 0xFF;
        int green1 = (color >> 8) & 0xFF;
        int blue1 = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;
        for (int i = 0; i < validLayerColors.length; i++) {
            int other = validLayerColors[i];
            if (((other >> 24) & 0xFF) == alpha) {
                long distance = colorDistance(red1, green1, blue1, other);
                if (distance < min) {
                    min = distance;
                    closest = validLayerBlocks[i];
                }
            }
        }
        if (closest != null) {
            colorLayerMap.put(color, closest);
        }
        return closest;
    }

    @Override
    public BaseBlock getNearestBlock(int color) {
        Integer value = colorBlockMap.get(color);
        if (value != null) {
            return FaweCache.CACHE_BLOCK[value];
        }
        BaseBlock result = parent.getNearestBlock(color);
        if (result != null) {
            colorBlockMap.put((int) color, (Integer) result.getCombined());
        }
        return result;
    }

    @Override
    public BaseBlock getDarkerBlock(BaseBlock block) {
        return parent.getDarkerBlock(block);
    }

    @Override
    public int getColor(BaseBlock block) {
        return parent.getColor(block);
    }

    @Override
    public File getFolder() {
        return parent.getFolder();
    }

    @Override
    public void loadModTextures() throws IOException, ParseException {
        parent.loadModTextures();
    }

    @Override
    public BaseBlock getNearestBlock(BaseBlock block, boolean darker) {
        return parent.getNearestBlock(block, darker);
    }

    @Override
    public BaseBlock getNearestBlock(int color, boolean darker) {
        return parent.getNearestBlock(color, darker);
    }

    @Override
    public long colorDistance(int c1, int c2) {
        return parent.colorDistance(c1, c2);
    }

    @Override
    public int getColor(BufferedImage image) {
        return parent.getColor(image);
    }

    @Override
    public BaseBlock getLighterBlock(BaseBlock block) {
        return parent.getLighterBlock(block);
    }
}
