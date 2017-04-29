package com.boydti.fawe.util;

import java.util.Arrays;

public class CleanTextureUtil extends TextureUtil {
    public CleanTextureUtil(TextureUtil parent, int minPercent, int maxPercent) {
        super(parent.getFolder());
        int minIndex = (parent.distances.length * minPercent) / 100;
        int maxIndex = (parent.distances.length * maxPercent) / 100;
        long min = parent.distances[minIndex];
        long max = parent.distances[maxIndex];
        int num = maxIndex - minIndex + 1;

        this.blockColors = parent.blockColors;
        this.blockDistance = parent.blockDistance;
        this.distances = Arrays.copyOfRange(parent.blockDistance, minIndex, maxIndex + 1);
        this.validColors = new int[distances.length];
        this.validBlockIds = new char[distances.length];
        for (int i = 0, j = 0; i < parent.validBlockIds.length; i++) {
            char combined = parent.validBlockIds[i];
            long distance = parent.blockDistance[combined];
            if (distance >= min && distance <= max) {
                int color = parent.validColors[i];
                this.validColors[j] = color;
                this.validBlockIds[j++] = combined;
            }
        }
        this.calculateLayerArrays();
    }
}
