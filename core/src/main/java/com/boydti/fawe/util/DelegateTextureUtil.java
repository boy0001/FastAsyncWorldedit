package com.boydti.fawe.util;

import com.sk89q.worldedit.blocks.BaseBlock;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import org.json.simple.parser.ParseException;

public class DelegateTextureUtil extends TextureUtil {
    private final TextureUtil parent;

    public DelegateTextureUtil(TextureUtil parent) {
        super(parent.getFolder());
        this.parent = parent;
    }

    @Override
    public BaseBlock getNearestBlock(int color) {
        return parent.getNearestBlock(color);
    }

    @Override
    public char[] getNearestLayer(int color) {
        return parent.getNearestLayer(color);
    }

    @Override
    public BaseBlock getLighterBlock(BaseBlock block) {
        return parent.getLighterBlock(block);
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
    public int combineTransparency(int top, int bottom) {
        return parent.combineTransparency(top, bottom);
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
    public boolean hasAlpha(int color) {
        return parent.hasAlpha(color);
    }

    @Override
    public long colorDistance(int c1, int c2) {
        return parent.colorDistance(c1, c2);
    }

    @Override
    public long colorDistance(int red1, int green1, int blue1, int c2) {
        return parent.colorDistance(red1, green1, blue1, c2);
    }

    @Override
    public int getColor(BufferedImage image) {
        return parent.getColor(image);
    }
}
