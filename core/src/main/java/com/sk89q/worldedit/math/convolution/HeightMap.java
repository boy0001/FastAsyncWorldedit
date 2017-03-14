package com.sk89q.worldedit.math.convolution;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.regions.Region;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Allows applications of Kernels onto the region's height map.
 *
 * <p>Currently only used for smoothing (with a GaussianKernel)</p>.
 */
public class HeightMap {

    private int[] data;
    private int width;
    private int height;

    private Region region;
    private EditSession session;

    /**
     * Constructs the HeightMap
     *
     * @param session an edit session
     * @param region the region
     */
    public HeightMap(EditSession session, Region region) {
        this(session, region, false);
    }

    public HeightMap(EditSession session, Region region, boolean naturalOnly) {
        checkNotNull(session);
        checkNotNull(region);

        this.session = session;
        this.region = region;

        this.width = region.getWidth();
        this.height = region.getLength();

        int minX = region.getMinimumPoint().getBlockX();
        int minY = region.getMinimumPoint().getBlockY();
        int minZ = region.getMinimumPoint().getBlockZ();
        int maxY = region.getMaximumPoint().getBlockY();

        // Store current heightmap data
        data = new int[width * height];
        for (int z = 0; z < height; ++z) {
            for (int x = 0; x < width; ++x) {
                data[z * width + x] = session.getHighestTerrainBlock(x + minX, z + minZ, minY, maxY, naturalOnly);
            }
        }
    }

    public HeightMap(EditSession session, Region region, int[] data) {
        this.session = session;
        this.region = region;

        this.width = region.getWidth();
        this.height = region.getLength();

        this.data = data;
    }

    /**
     * Apply the filter 'iterations' amount times.
     *
     * @param filter the filter
     * @param iterations the number of iterations
     * @return number of blocks affected
     * @throws MaxChangedBlocksException
     */

    public int applyFilter(HeightMapFilter filter, int iterations) throws WorldEditException {
        checkNotNull(filter);

        int[] newData = new int[data.length];
        System.arraycopy(data, 0, newData, 0, data.length);

        for (int i = 0; i < iterations; ++i) {
            newData = filter.filter(newData, width, height);
        }

        return apply(newData);
    }

    public int applyLayers(int[] data) throws WorldEditException {
        checkNotNull(data);

        Vector minY = region.getMinimumPoint();
        int originX = minY.getBlockX();
        int originY = minY.getBlockY();
        int originZ = minY.getBlockZ();

        int maxY = region.getMaximumPoint().getBlockY();
        BaseBlock fillerAir = EditSession.nullBlock;

        int blocksChanged = 0;

        // Apply heightmap
        int maxY4 = maxY << 4;
        int index = 0;
        for (int z = 0; z < height; ++z) {
            int zr = z + originZ;
            for (int x = 0; x < width; ++x) {
                int curHeight = this.data[index];
                int newHeight = Math.min(maxY4, data[index++]);
                int curBlock = (curHeight) >> 3;
                int newBlock = (newHeight + 7) >> 3;
                int xr = x + originX;

                // We are keeping the topmost blocks so take that in account for the scale
                double scale = (double) (curHeight - originY) / (double) (newHeight - originY);

                // Depending on growing or shrinking we need to start at the bottom or top
                if (newHeight > curHeight) {
                    // Set the top block of the column to be the same type (this might go wrong with rounding)
                    BaseBlock existing = session.getBlock(xr, curBlock, zr);

                    // Skip water/lava
                    if (!FaweCache.isLiquidOrGas(existing.getId())) {
                        // Grow -- start from 1 below top replacing airblocks
                        for (int y = newBlock - 1 - originY; y >= curBlock; --y) {
                            int copyFrom = (int) (y * scale);
                            session.setBlock(xr, originY + y, zr, session.getBlock(xr, originY + copyFrom, zr));
                            ++blocksChanged;
                        }
                        int setData = newHeight & 7;
                        if (setData != 0) {
                            existing = FaweCache.getBlock(existing.getId(), setData - 1);
                            session.setBlock(xr, newBlock, zr, existing);
                            ++blocksChanged;
                        } else {
                            existing = FaweCache.getBlock(existing.getId(), 7);
                            session.setBlock(xr, newBlock, zr, existing);
                            ++blocksChanged;
                        }
                    }
                } else if (curHeight > newHeight) {
                    // Fill rest with air
                    for (int y = newBlock + 1; y <= ((curHeight + 7) >> 3); ++y) {
                        session.setBlock(xr, y, zr, fillerAir);
                        ++blocksChanged;
                    }
                    // Set the top block of the column to be the same type
                    // (this could otherwise go wrong with rounding)
                    int setData = newHeight & 7;
                    BaseBlock existing = session.getBlock(xr, curBlock, zr);
                    if (setData != 0) {
                        existing = FaweCache.getBlock(existing.getId(), setData - 1);
                        session.setBlock(xr, newBlock, zr, existing);
                    } else {
                        existing = FaweCache.getBlock(existing.getId(), 7);
                        session.setBlock(xr, newBlock, zr, existing);
                    }
                    ++blocksChanged;
                }
            }
        }

        return blocksChanged;
    }

    public int apply(int[] data) throws WorldEditException {
        checkNotNull(data);

        Vector minY = region.getMinimumPoint();
        int originX = minY.getBlockX();
        int originY = minY.getBlockY();
        int originZ = minY.getBlockZ();

        int maxY = region.getMaximumPoint().getBlockY();
        BaseBlock fillerAir = EditSession.nullBlock;

        int blocksChanged = 0;

        // Apply heightmap
        int index = 0;
        for (int z = 0; z < height; ++z) {
            int zr = z + originZ;
            for (int x = 0; x < width; ++x) {
                int curHeight = this.data[index];
                int newHeight = Math.min(maxY, data[index++]);
                int xr = x + originX;

                // We are keeping the topmost blocks so take that in account for the scale
                double scale = (double) (curHeight - originY) / (double) (newHeight - originY);

                // Depending on growing or shrinking we need to start at the bottom or top
                if (newHeight > curHeight) {
                    // Set the top block of the column to be the same type (this might go wrong with rounding)
                    BaseBlock existing = session.getBlock(xr, curHeight, zr);

                    // Skip water/lava
                    if (!FaweCache.isLiquidOrGas(existing.getId())) {
                        session.setBlock(xr, newHeight, zr, existing);
                        ++blocksChanged;

                        // Grow -- start from 1 below top replacing airblocks
                        for (int y = newHeight - 1 - originY; y >= 0; --y) {
                            int copyFrom = (int) (y * scale);
                            session.setBlock(xr, originY + y, zr, session.getBlock(xr, originY + copyFrom, zr));
                            ++blocksChanged;
                        }
                    }
                } else if (curHeight > newHeight) {
                    // Shrink -- start from bottom
                    for (int y = 0; y < newHeight - originY; ++y) {
                        int copyFrom = (int) (y * scale);
                        session.setBlock(xr, originY + y, zr, session.getBlock(xr, originY + copyFrom, zr));
                        ++blocksChanged;
                    }

                    // Set the top block of the column to be the same type
                    // (this could otherwise go wrong with rounding)
                    session.setBlock(xr, newHeight, zr, session.getBlock(xr, curHeight, zr));
                    ++blocksChanged;

                    // Fill rest with air
                    for (int y = newHeight + 1; y <= curHeight; ++y) {
                        session.setBlock(xr, y, zr, fillerAir);
                        ++blocksChanged;
                    }
                }
            }
        }

        return blocksChanged;
    }

    public static Class<?> inject() {
        return HeightMap.class;
    }

}
