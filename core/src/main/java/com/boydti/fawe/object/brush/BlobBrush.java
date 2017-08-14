package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.random.SimplexNoise;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import java.util.concurrent.ThreadLocalRandom;

public class BlobBrush implements Brush {
    private final double amplitude;
    private final double frequency;

    public BlobBrush(double frequency, double amplitude) {
        this.frequency = frequency;
        this.amplitude = amplitude;
    }

    @Override
    public void build(EditSession editSession, Vector position, Pattern pattern, double size) throws MaxChangedBlocksException {
        double seedX = ThreadLocalRandom.current().nextDouble();
        double seedY = ThreadLocalRandom.current().nextDouble();
        double seedZ = ThreadLocalRandom.current().nextDouble();

        int px = position.getBlockX();
        int py = position.getBlockY();
        int pz = position.getBlockZ();

        double distort = this.frequency / size;

        int radiusSqr = (int) (size * size);
        int sizeInt = (int) size * 2;
        for (int x = -sizeInt; x <= sizeInt; x++) {
            double nx = seedX + x * distort;
            int d1 = x * x;
            for (int y = -sizeInt; y <= sizeInt; y++) {
                int d2 = d1 + y * y;
                double ny = seedY + y * distort;
                for (int z = -sizeInt; z <= sizeInt; z++) {
                    double nz = seedZ + z * distort;
                    double distance = d2 + z * z;
                    double noise = this.amplitude * SimplexNoise.noise(nx, ny, nz);
                    if (distance + distance * noise < radiusSqr) {
                        editSession.setBlock(px + x, py + y, pz + z, pattern);
                    }
                }
            }
        }
    }
}
