package com.sk89q.worldedit.math.convolution;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.visitor.Fast2DIterator;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.Regions;
import java.util.Iterator;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Allows applications of Kernels onto the region's height map.
 * <p>
 * <p>Currently only used for smoothing (with a GaussianKernel)</p>.
 */
public class HeightMap {

    private final boolean layers;
    private int[] data;
    private int width;
    private int height;

    private Region region;
    private EditSession session;

    /**
     * Constructs the HeightMap
     *
     * @param session an edit session
     * @param region  the region
     */
    public HeightMap(EditSession session, Region region) {
        this(session, region, false);
    }

    public HeightMap(EditSession session, Region region, boolean naturalOnly) {
        this(session, region, naturalOnly, false);
    }

    public HeightMap(EditSession session, Region region, boolean naturalOnly, boolean layers) {
        checkNotNull(session);
        checkNotNull(region);

        this.session = session;
        this.region = region;

        this.width = region.getWidth();
        this.height = region.getLength();

        this.layers = layers;

        int minX = region.getMinimumPoint().getBlockX();
        int minY = region.getMinimumPoint().getBlockY();
        int minZ = region.getMinimumPoint().getBlockZ();
        int maxY = region.getMaximumPoint().getBlockY();

        if (layers) {
            Vector min = region.getMinimumPoint();
            Vector max = region.getMaximumPoint();
            int width = region.getWidth();
            int height = region.getLength();
            data = new int[width * height];
            int bx = min.getBlockX();
            int bz = min.getBlockZ();
            Iterable<Vector2D> flat = Regions.asFlatRegion(region).asFlatRegion();
            Iterator<Vector2D> iter = new Fast2DIterator(flat, session).iterator();
            int y = 0;
            MutableBlockVector mutable = new MutableBlockVector();
            while (iter.hasNext()) {
                Vector2D pos = iter.next();
                int x = pos.getBlockX();
                int z = pos.getBlockZ();
                y = session.getNearestSurfaceLayer(x, z, y, 0, maxY);
                data[(z - bz) * width + (x - bx)] = y;
            }
        } else {
            // Store current heightmap data
            data = new int[width * height];
            for (int z = 0; z < height; ++z) {
                for (int x = 0; x < width; ++x) {
                    data[z * width + x] = session.getHighestTerrainBlock(x + minX, z + minZ, minY, maxY, naturalOnly);
                }
            }
        }
    }

    public HeightMap(EditSession session, Region region, int[] data, boolean layers) {
        this.session = session;
        this.region = region;

        this.width = region.getWidth();
        this.height = region.getLength();

        this.data = data;

        this.layers = layers;
    }

    /**
     * Apply the filter 'iterations' amount times.
     *
     * @param filter     the filter
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

        return layers ? applyLayers(newData) : apply(newData);
    }

//    TODO
//    public int averageFilter(int iterations) throws WorldEditException {
//        Vector min = region.getMinimumPoint();
//        Vector max = region.getMaximumPoint();
//        int shift = layers ? 3 : 0;
//        AverageHeightMapFilter filter = new AverageHeightMapFilter(data, width, height, min.getBlockY() << shift, max.getBlockY() << shift);
//        int[] newData = filter.filter(iterations);
//        return layers ? applyLayers(newData) : apply(newData);
//    }

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
                        for (int y = newHeight - 1 - originY; y >= curHeight; --y) {
                            int copyFrom = (int) (y * scale);
                            session.setBlock(xr, originY + y, zr, session.getBlock(xr, originY + copyFrom, zr));
                            ++blocksChanged;
                        }
                    }
                } else if (curHeight > newHeight) {
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
