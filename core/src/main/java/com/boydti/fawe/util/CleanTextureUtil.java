package com.boydti.fawe.util;

import java.io.FileNotFoundException;
import java.util.Arrays;

public class CleanTextureUtil extends TextureUtil {
    public CleanTextureUtil(TextureUtil parent, int minPercent, int maxPercent) throws FileNotFoundException {
        super(parent.getFolder());
        int minIndex = ((parent.distances.length - 1) * minPercent) / 100;
        int maxIndex = ((parent.distances.length - 1) * maxPercent) / 100;
        long min = parent.distances[minIndex];
        long max = parent.distances[maxIndex];
        for (; minIndex > 0 && parent.distances[minIndex - 1] == min; minIndex--) ;
        for (; maxIndex < parent.distances.length - 1 && parent.distances[maxIndex + 1] == max; maxIndex++) ;
        int num = maxIndex - minIndex + 1;
        this.validBiomes = parent.validBiomes;
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
