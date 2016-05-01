package com.boydti.fawe.object.brush;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.brush.heightmap.ArrayHeightMap;
import com.boydti.fawe.object.brush.heightmap.HeightMap;
import com.boydti.fawe.object.exception.FaweException;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.pattern.Pattern;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class HeightBrush implements Brush {

    public final HeightMap heightMap;
    private final int rotation;
    double yscale = 1;

    public HeightBrush(File file, int rotation, double yscale) {
        this.rotation = (rotation / 90) % 4;
        this.yscale = yscale;
        if (file == null || !file.exists()) {
            heightMap = new HeightMap();
        } else {
            try {
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
                heightMap = new ArrayHeightMap(array);
            } catch (IOException e) {
                throw new FaweException(BBC.BRUSH_HEIGHT_INVALID);
            }
        }
    }

    @Override
    public void build(EditSession editSession, Vector position, Pattern pattern, double sizeDouble) throws MaxChangedBlocksException {

        int size = (int) sizeDouble;
        heightMap.setSize(size);

        int size2 = size * size;
        int startY = position.getBlockY() + size;
        int endY = position.getBlockY() - size;
        int cx = position.getBlockX();
        int cz = position.getBlockZ();
        Vector mutablePos = new Vector(0, 0, 0);
        for (int x = -size; x <= size; x++) {
            int xx = cx + x;
            for (int z = -size; z <= size; z++) {
                int zz = cz + z;
                int raise;
                switch (rotation) {
                    default:
                        raise = heightMap.getHeight(x, z);
                        break;
                    case 1:
                        raise = heightMap.getHeight(z, x);
                        break;
                    case 2:
                        raise = heightMap.getHeight(-x, -z);
                        break;
                    case 3:
                        raise = heightMap.getHeight(-z, -x);
                        break;
                }
                raise = (int) (yscale * raise);
                if (raise == 0) {
                    continue;
                }
                int foundHeight = Integer.MAX_VALUE;
                BaseBlock block = null;
                for (int y = startY; y >= endY; y--) {
                    block = editSession.getLazyBlock(xx, y, zz);
                    if (block != EditSession.nullBlock) {
                        foundHeight = y;
                        break;
                    }
                }
                if (foundHeight  == Integer.MAX_VALUE) {
                    continue;
                }
                for (int y = foundHeight + 1; y <= foundHeight + raise; y++) {
                    mutablePos.x = xx;
                    mutablePos.y = y;
                    mutablePos.z = zz;
                    editSession.setBlock(mutablePos, block);
                }
            }
        }
    }
}
