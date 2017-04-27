package com.boydti.fawe.util;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.blocks.BaseBlock;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import org.json.simple.parser.ParseException;

public class CachedTextureUtil extends TextureUtil {
    private final TextureUtil parent;
    private Int2ObjectOpenHashMap<Integer> colorBlockMap;


    public CachedTextureUtil(TextureUtil parent) {
        super(parent.getFolder());
        this.parent = parent;
        this.colorBlockMap = new Int2ObjectOpenHashMap<>();
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
