package com.boydti.fawe.util;

import com.boydti.fawe.object.PseudoRandom;
import com.sk89q.worldedit.blocks.BaseBlock;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class RandomTextureUtil extends CachedTextureUtil {

    public RandomTextureUtil(TextureUtil parent) {
        super(parent);
    }

    private Int2ObjectOpenHashMap<Integer> offsets = new Int2ObjectOpenHashMap<>();

    protected int addRandomColor(int c1, int c2) {
        int red1 = (c1 >> 16) & 0xFF;
        int green1 = (c1 >> 8) & 0xFF;
        int blue1 = (c1 >> 0) & 0xFF;
        byte red2 = (byte) ((c2 >> 16));
        byte green2 = (byte) ((c2 >> 8));
        byte blue2 = (byte) ((c2 >> 0));
        int red = MathMan.clamp(red1 + random(red2), 0, 255);
        int green = MathMan.clamp(green1 + random(green2), 0, 255);
        int blue = MathMan.clamp(blue1 + random(blue2), 0, 255);
        return (red << 16) + (green << 8) + (blue << 0) + (255 << 24);
    }


    private int random(int i) {
        if (i < 0) {
            return -PseudoRandom.random.nextInt(-i);
        } else {
            return PseudoRandom.random.nextInt(i);
        }
    }

    @Override
    public BaseBlock getNearestBlock(int color) {
        int offsetColor = offsets.getOrDefault(color, 0);
        if (offsetColor != 0) {
            offsetColor = addRandomColor(color, offsetColor);
        } else {
            offsetColor = color;
        }
        BaseBlock res = super.getNearestBlock(offsetColor);
        int newColor = getColor(res);
        {
            byte dr = (byte) (((color >> 16) & 0xFF) - ((newColor >> 16) & 0xFF));
            byte dg = (byte) (((color >> 8) & 0xFF) - ((newColor >> 8) & 0xFF));
            byte db = (byte) (((color >> 0) & 0xFF) - ((newColor >> 0) & 0xFF));
            offsets.put(color, (Integer) ((dr << 16) + (dg << 8) + (db << 0)));
        }
        return res;
    }
}
