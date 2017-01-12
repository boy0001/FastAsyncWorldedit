package com.boydti.fawe.object.brush.heightmap;

import com.boydti.fawe.object.IntegerPair;
import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldVector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.internal.LocalWorldAdapter;
import com.sk89q.worldedit.math.convolution.GaussianKernel;
import com.sk89q.worldedit.math.convolution.HeightMap;
import com.sk89q.worldedit.math.convolution.HeightMapFilter;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import javax.imageio.ImageIO;

public class ScalableHeightMap {
    public int size2;
    public int size;

    public ScalableHeightMap() {
        setSize(5);
    }

    public ScalableHeightMap(int size) {
        setSize(size);
    }

    public void setSize(int size) {
        this.size = size;
        this.size2 = size * size;
    }

    public double getHeight(int x, int z) {
        int dx = Math.abs(x);
        int dz = Math.abs(z);
        int d2 = dx * dx + dz * dz;
        if (d2 > size2) {
            return 0;
        }
        return size - MathMan.sqrtApprox(d2);
    }

    public static ScalableHeightMap fromClipboard(Clipboard clipboard) {
        Vector dim = clipboard.getDimensions();
        byte[][] heightArray = new byte[dim.getBlockX()][dim.getBlockZ()];
        int minX = clipboard.getMinimumPoint().getBlockX();
        int minZ = clipboard.getMinimumPoint().getBlockZ();
        int minY = clipboard.getMinimumPoint().getBlockY();
        int maxY = clipboard.getMaximumPoint().getBlockY();
        int clipHeight = maxY - minY + 1;
        HashSet<IntegerPair> visited = new HashSet<>();
        for (Vector pos : clipboard.getRegion()) {
            IntegerPair pair = new IntegerPair(pos.getBlockX(), pos.getBlockZ());
            if (visited.contains(pair)) {
                continue;
            }
            visited.add(pair);
            int xx = pos.getBlockX();
            int zz = pos.getBlockZ();
            int highestY = minY;
            for (int y = minY; y <= maxY; y++) {
                pos.mutY(y);
                BaseBlock block = clipboard.getBlock(pos);
                if (block != EditSession.nullBlock) {
                    highestY = y + 1;
                }
            }
            int pointHeight = Math.min(255, (256 * (highestY - minY)) / clipHeight);
            int x = xx - minX;
            int z = zz - minZ;
            heightArray[x][z] = (byte) pointHeight;
        }
        return new ArrayHeightMap(heightArray);
    }

    public static ScalableHeightMap fromPNG(File file) throws IOException {
        BufferedImage heightFile = ImageIO.read(file);
        int width = heightFile.getWidth();
        int length = heightFile.getHeight();
        Raster data = heightFile.getData();
        byte[][] array = new byte[width][length];
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                int pixel = heightFile.getRGB(x, z);
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = (pixel >> 0) & 0xFF;
                int intensity = (red + green + blue) / 3;
                array[x][z] = (byte) intensity;
            }
        }
        return new ArrayHeightMap(array);
    }

    public void apply(EditSession session, Mask mask, Vector pos, int size, int rotationMode, double yscale, boolean smooth) throws MaxChangedBlocksException {
        Vector top = session.getMaximumPoint();
        int maxY = top.getBlockY();
        int diameter = 2 * size + 1;
        int centerX = pos.getBlockX();
        int centerZ = pos.getBlockZ();
        int endY = pos.getBlockY() + size;
        int startY = pos.getBlockY() - size;
        int[] newData = new int[diameter * diameter];
        Vector mutablePos = new Vector(0, 0, 0);
        for (int x = -size; x <= size; x++) {
            int xx = centerX + x;
            mutablePos.mutX(xx);
            for (int z = -size; z <= size; z++) {
                int index = (z + size) * diameter + (x + size);
                int zz = centerZ + z;
                double raise;
                switch (rotationMode) {
                    default:
                        raise = getHeight(x, z);
                        break;
                    case 1:
                        raise = getHeight(z, x);
                        break;
                    case 2:
                        raise = getHeight(-x, -z);
                        break;
                    case 3:
                        raise = getHeight(-z, -x);
                        break;
                }
                raise = (yscale * raise);
                int random = PseudoRandom.random.random(maxY + 1) < (int) ((raise - (int) raise) * (maxY + 1)) ? 1 : 0;
                int height = session.getHighestTerrainBlock(xx, zz, 0, maxY, true) + (int) raise + random;
                newData[index] = height;
            }
        }
        int iterations = 1;
        WorldVector min = new WorldVector(LocalWorldAdapter.adapt(session.getWorld()), pos.subtract(size, maxY, size));
        Vector max = pos.add(size, maxY, size);
        Region region = new CuboidRegion(session.getWorld(), min, max);
        HeightMap heightMap = new HeightMap(session, region, true);
        if (smooth) {
            try {
                HeightMapFilter filter = (HeightMapFilter) HeightMapFilter.class.getConstructors()[0].newInstance(GaussianKernel.class.getConstructors()[0].newInstance(5, 1));
                newData = filter.filter(newData, diameter, diameter);
            } catch (Throwable e) {
                MainUtil.handleError(e);
            }
        }
        heightMap.apply(newData);
    }
}
