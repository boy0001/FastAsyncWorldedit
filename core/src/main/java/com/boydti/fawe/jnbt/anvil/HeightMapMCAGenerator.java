package com.boydti.fawe.jnbt.anvil;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.object.collection.LocalBlockVector2DSet;
import com.boydti.fawe.object.collection.SummedAreaTable;
import com.boydti.fawe.object.schematic.Schematic;
import com.boydti.fawe.util.CachedTextureUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.RandomTextureUtil;
import com.boydti.fawe.util.TextureUtil;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.registry.WorldData;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;

public class HeightMapMCAGenerator extends MCAWriter implements Extent {
    private final MutableBlockVector mutable = new MutableBlockVector();

    private final ThreadLocal<int[]> indexStore = new ThreadLocal<int[]>() {
        @Override
        protected int[] initialValue() {
            return new int[256];
        }
    };

    private final Int2ObjectOpenHashMap<char[][][]> blocks = new Int2ObjectOpenHashMap<>();

    public final byte[] heights;
    private final byte[] biomes;
    private final char[] floor;
    private final char[] main;
    private char[] overlay;
    private int waterHeight = 0;
    private TextureUtil textureUtil;
    private boolean randomVariation = true;
    private int biomePriority = 0;
    private byte waterId = BlockID.STATIONARY_WATER;
    private boolean modifiedMain = false;

    public HeightMapMCAGenerator(BufferedImage img, File regionFolder) {
        this(img.getWidth(), img.getHeight(), regionFolder);
        setHeight(img);
    }

    public HeightMapMCAGenerator(int width, int length, File regionFolder) {
        super(width, length, regionFolder);
        int area = getArea();
        heights = new byte[getArea()];
        biomes = new byte[getArea()];
        floor = new char[getArea()];
        main = new char[getArea()];
        char stone = (char) FaweCache.getCombined(1, 0);
        char grass = (char) FaweCache.getCombined(2, 0);
        Arrays.fill(main, stone);
        Arrays.fill(floor, grass);
    }

    public TextureUtil getTextureUtil() {
        if (textureUtil == null) {
            textureUtil = Fawe.get().getTextureUtil();
        }
        try {
            if (randomVariation) {
                return new RandomTextureUtil(textureUtil);
            } else if (textureUtil instanceof CachedTextureUtil) {
                return textureUtil;
            } else {
                return new CachedTextureUtil(textureUtil);
            }
        } catch (FileNotFoundException neverHappens) {
            neverHappens.printStackTrace();
            return null;
        }
    }

    public void setWaterHeight(int waterHeight) {
        this.waterHeight = waterHeight;
    }

    public void setWaterId(int waterId) {
        this.waterId = (byte) waterId;
    }

    public void setTextureRandomVariation(boolean randomVariation) {
        this.randomVariation = randomVariation;
    }

    public void setTextureUtil(TextureUtil textureUtil) {
        this.textureUtil = textureUtil;
    }

    public void smooth(BufferedImage img, boolean white, int radius, int iterations) {
        smooth(img, null, white, radius, iterations);
    }

    public void smooth(Mask mask, int radius, int iterations) {
        smooth(null, mask, false, radius, iterations);
    }

    private void smooth(BufferedImage img, Mask mask, boolean white, int radius, int iterations) {
        char snow = 78 << 4;
        long[] copy = new long[heights.length];
        char[] layers = new char[heights.length];
        int width = getWidth();
        int length = getLength();
        SummedAreaTable table = new SummedAreaTable(copy, layers, width, radius);
        for (int j = 0; j < iterations; j++) {
            for (int i = 0; i < heights.length; i++) {
                char combined = floor[i];
                int id = combined >> 4;
                if (id == 78 || id == 80) {
                    layers[i] = (char) (((heights[i] & 0xFF) << 3) + (floor[i] & 0x7) + 1);
                } else {
                    layers[i] = (char) (((heights[i] & 0xFF) << 3) + 8);
                }
            }
            int index = 0;
            table.processSummedAreaTable();
            if (img != null) {
                for (int z = 0; z < getLength(); z++) {
                    for (int x = 0; x < getWidth(); x++, index++) {
                        int height = img.getRGB(x, z) & 0xFF;
                        if (height == 255 || height > 0 && !white && PseudoRandom.random.nextInt(256) <= height) {
                            int newHeight = table.average(x, z, index);
                            int blockHeight = (newHeight - 1) >> 3;
                            int layerHeight = (newHeight - 1) & 0x7;
                            heights[index] = (byte) blockHeight;
                            int id = floor[index] >> 4;
                            if (id == 78 || id == 80) {
                                floor[index] = (char) (snow + layerHeight);
                            }
                        }
                    }
                }
            } else if (mask != null) {
                for (int z = 0; z < getLength(); z++) {
                    mutable.mutZ(z);
                    for (int x = 0; x < getWidth(); x++, index++) {
                        int y = heights[index] & 0xFF;
                        mutable.mutX(x);
                        mutable.mutY(y);
                        if (mask.test(mutable)) {
                            int height = table.average(x, z, index);
                            int blockHeight = (height - 1) >> 3;
                            int layerHeight = (height - 1) & 0x7;
                            heights[index] = (byte) blockHeight;
                            int id = floor[index] >> 4;
                            if (id == 78 || id == 80) {
                                floor[index] = (char) (snow + layerHeight);
                            }
                        }
                    }
                }
            }
        }
    }

    public void setHeight(BufferedImage img) {
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            for (int x = 0; x < getWidth(); x++, index++) {
                heights[index] = (byte) (img.getRGB(x, z) >> 8);
            }
        }
    }

    public void addCaves() throws WorldEditException {
        CuboidRegion region = new CuboidRegion(new Vector(0, 0, 0), new Vector(getWidth(), 255, getLength()));
        addCaves(region);
    }

    @Deprecated
    public void addSchems(Mask mask, WorldData worldData, ClipboardHolder[] clipboards, int rarity, boolean rotate) throws WorldEditException {
        CuboidRegion region = new CuboidRegion(new Vector(0, 0, 0), new Vector(getWidth(), 255, getLength()));
        addSchems(region, mask, worldData, clipboards, rarity, rotate);
    }

    public void addSchems(BufferedImage img, Mask mask, WorldData worldData, ClipboardHolder[] clipboards, int rarity, int distance, boolean randomRotate) throws WorldEditException {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        double doubleRarity = rarity / 100d;
        int index = 0;
        AffineTransform identity = new AffineTransform();
        LocalBlockVector2DSet placed = new LocalBlockVector2DSet();
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights[index];
                int height = img.getRGB(x, z) & 0xFF;
                if (height == 0 || PseudoRandom.random.nextInt(256) > height * doubleRarity) {
                    continue;
                }
                mutable.mutX(x);
                mutable.mutY(y);
                if (!mask.test(mutable)) {
                    continue;
                }
                if (placed.containsRadius(x, z, distance)) {
                    continue;
                }
                placed.add(x, z);
                ClipboardHolder holder = clipboards[PseudoRandom.random.random(clipboards.length)];
                if (randomRotate) {
                    int rotate = PseudoRandom.random.random(4) * 90;
                    if (rotate != 0) {
                        holder.setTransform(new AffineTransform().rotateY(PseudoRandom.random.random(4) * 90));
                    } else {
                        holder.setTransform(identity);
                    }
                }
                Clipboard clipboard = holder.getClipboard();
                Schematic schematic = new Schematic(clipboard);
                Transform transform = holder.getTransform();
                if (transform.isIdentity()) {
                    schematic.paste(this, mutable, false);
                } else {
                    schematic.paste(this, worldData, mutable, false, transform);
                }
                if (x + distance < getWidth()) {
                    x += distance;
                    index += distance;
                } else {
                    break;
                }
            }
        }
    }

    public void addSchems(Mask mask, WorldData worldData, ClipboardHolder[] clipboards, int rarity, int distance, boolean randomRotate) throws WorldEditException {
        int scaledRarity = (256 * rarity) / 100;
        int index = 0;
        AffineTransform identity = new AffineTransform();
        LocalBlockVector2DSet placed = new LocalBlockVector2DSet();
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights[index];
                if (PseudoRandom.random.nextInt(256) > scaledRarity) {
                    continue;
                }
                mutable.mutX(x);
                mutable.mutY(y);
                if (!mask.test(mutable)) {
                    continue;
                }
                if (placed.containsRadius(x, z, distance)) {
                    continue;
                }
                mutable.mutY(y + 1);
                placed.add(x, z);
                ClipboardHolder holder = clipboards[PseudoRandom.random.random(clipboards.length)];
                if (randomRotate) {
                    int rotate = PseudoRandom.random.random(4) * 90;
                    if (rotate != 0) {
                        holder.setTransform(new AffineTransform().rotateY(PseudoRandom.random.random(4) * 90));
                    } else {
                        holder.setTransform(identity);
                    }
                }
                Clipboard clipboard = holder.getClipboard();
                Schematic schematic = new Schematic(clipboard);
                Transform transform = holder.getTransform();
                if (transform.isIdentity()) {
                    schematic.paste(this, mutable, false);
                } else {
                    schematic.paste(this, worldData, mutable, false, transform);
                }
                if (x + distance < getWidth()) {
                    x += distance;
                    index += distance;
                } else {
                    break;
                }
            }
        }
    }

    public void addOre(Mask mask, Pattern material, int size, int frequency, int rarity, int minY, int maxY) throws WorldEditException {
        CuboidRegion region = new CuboidRegion(new Vector(0, 0, 0), new Vector(getWidth(), 255, getLength()));
        addOre(region, mask, material, size, frequency, rarity, minY, maxY);
    }

    public void addDefaultOres(Mask mask) throws WorldEditException {
        addOres(new CuboidRegion(new Vector(0, 0, 0), new Vector(getWidth(), 255, getLength())), mask);
    }

    @Override
    public Vector getMinimumPoint() {
        return new Vector(0, 0, 0);
    }

    @Override
    public Vector getMaximumPoint() {
        return new Vector(getWidth() - 1, 255, getLength() - 1);
    }

    @Override
    public boolean setBlock(Vector position, BaseBlock block) throws WorldEditException {
        return setBlock(position.getBlockX(), position.getBlockY(), position.getBlockZ(), block);
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        int index = position.getBlockZ() * getWidth() + position.getBlockX();
        if (index < 0 || index >= heights.length) return false;
        biomes[index] = (byte) biome.getId();
        return true;
    }

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) throws WorldEditException {
        int index = z * getWidth() + x;
        if (index < 0 || index >= heights.length) return false;
        int height = heights[index] & 0xFF;
        char combined = (char) FaweCache.getCombined(block);
        if (y > height) {
            if (y == height + 1) {
                if (overlay == null) {
                    overlay = new char[getArea()];
                }
                overlay[index] = combined;
                return true;
            }
        } else if (y == height) {
            floor[index] = combined;
            return true;
        }
        short chunkX = (short) (x >> 4);
        short chunkZ = (short) (z >> 4);
        int pair = MathMan.pair(chunkX, chunkZ);
        char[][][] map = blocks.get(pair);
        if (map == null) {
            map = new char[256][][];
            blocks.put(pair, map);
        }
        char[][] yMap = map[y];
        if (yMap == null) {
            map[y] = yMap = new char[16][];
        }
        z = z & 15;
        char[] zMap = yMap[z];
        if (zMap == null) {
            yMap[z] = zMap = new char[16];
        }
        zMap[x & 15] = combined != 0 ? combined : 1;
        return true;
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        int index = position.getBlockZ() * getWidth() + position.getBlockX();
        if (index < 0 || index >= heights.length) return EditSession.nullBiome;
        return FaweCache.CACHE_BIOME[biomes[index] & 0xFF];
    }

    @Override
    public BaseBlock getBlock(Vector position) {
        return getLazyBlock(position);
    }

    @Override
    public BaseBlock getLazyBlock(Vector position) {
        return getLazyBlock(position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }

    @Override
    public BaseBlock getLazyBlock(int x, int y, int z) {
        int index = z * getWidth() + x;
        if (index < 0 || index >= heights.length) return EditSession.nullBlock;
        int height = heights[index] & 0xFF;
        if (y > height) {
            if (y == height + 1) {
                return FaweCache.CACHE_BLOCK[overlay != null ? overlay[index] : 0];
            }
            if (!blocks.isEmpty()) {
                short chunkX = (short) (x >> 4);
                short chunkZ = (short) (z >> 4);
                int pair = MathMan.pair(chunkX, chunkZ);
                char[][][] map = blocks.get(pair);
                if (map != null) {
                    char combined = get(map, x, y, z);
                    if (combined != 0) {
                        return FaweCache.CACHE_BLOCK[combined];
                    }
                }
            }
            if (y > waterHeight) {
                return FaweCache.CACHE_BLOCK[waterId << 4];
            }
            return FaweCache.CACHE_BLOCK[0];
        } else if (y == height) {
            return FaweCache.CACHE_BLOCK[floor[index]];
        } else {
            if (!blocks.isEmpty()) {
                short chunkX = (short) (x >> 4);
                short chunkZ = (short) (z >> 4);
                int pair = MathMan.pair(chunkX, chunkZ);
                char[][][] map = blocks.get(pair);
                if (map != null) {
                    char combined = get(map, x, y, z);
                    if (combined != 0) {
                        return FaweCache.CACHE_BLOCK[combined];
                    }
                }
            }
            return FaweCache.CACHE_BLOCK[main[index]];
        }
    }

    @Override
    public int getNearestSurfaceLayer(int x, int z, int y, int minY, int maxY) {
        int index = z * getWidth() + x;
        if (index < 0 || index >= heights.length) return y;
        return ((heights[index] & 0xFF) << 3) + (floor[index] & 0xFF) + 1;
    }

    @Override
    public int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY) {
        int index = z * getWidth() + x;
        if (index < 0 || index >= heights.length) return y;
        return heights[index] & 0xFF;
    }

    public void setBiome(BufferedImage img, byte biome, boolean white) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            for (int x = 0; x < getWidth(); x++, index++) {
                int height = img.getRGB(x, z) & 0xFF;
                if (height == 255 || height > 0 && !white && PseudoRandom.random.nextInt(256) <= height) {
                    biomes[index] = biome;
                }
            }
        }
    }

    public void setBiomePriority(int value) {
        this.biomePriority = ((value * 65536) / 100) - 32768;
    }

    public void setBlockAndBiomeColor(BufferedImage img) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        TextureUtil textureUtil = getTextureUtil();
        int index = 0;
        int widthIndex = img.getWidth() - 1;
        int heightIndex = img.getHeight() - 1;
        int maxIndex = biomes.length - 1;
        for (int y = 0; y < img.getHeight(); y++) {
            boolean yBiome = y > 0 && y < heightIndex;
            for (int x = 0; x < img.getWidth(); x++) {
                int color = img.getRGB(x, y);
                BaseBlock block = textureUtil.getNearestBlock(color);
                TextureUtil.BiomeColor biome = textureUtil.getNearestBiome(color);
                int blockColor = textureUtil.getColor(block);
                biomes[index] = (byte) biome.id;
                if (textureUtil.colorDistance(biome.grass, color) - biomePriority > textureUtil.colorDistance(blockColor, color)) {
                    char combined = (char) block.getCombined();
                    main[index] = combined;
                    floor[index] = combined;
                }
                index++;
            }
        }
    }

    private void setBiomeIfZero(int index, byte value) {
        if (biomes[index] == 0) {
            biomes[index] = value;
        }
    }

    public void setBiomeColor(BufferedImage img) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        TextureUtil textureUtil = getTextureUtil();
        int index = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int color = img.getRGB(x, y);
                TextureUtil.BiomeColor biome = textureUtil.getNearestBiome(color);
                if (biome != null) {
                    biomes[index] = (byte) biome.id;
                }
                index++;
            }
        }
    }

    public void setColor(BufferedImage img) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        TextureUtil textureUtil = getTextureUtil();
        int index = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int color = img.getRGB(x, y);
                BaseBlock block = textureUtil.getNearestBlock(color);
                if (block != null) {
                    char combined = (char) block.getCombined();
                    main[index] = combined;
                    floor[index] = combined;
                }
                index++;
            }
        }
    }

    public void setColorWithGlass(BufferedImage img) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        TextureUtil textureUtil = getTextureUtil();
        int index = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int color = img.getRGB(x, y);
                char[] layer = textureUtil.getNearestLayer(color);
                if (layer != null) {
                    floor[index] = layer[0];
                    main[index] = layer[1];
                }
                index++;
            }
        }
    }

    public void setBiome(Mask mask, byte biome) {
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights[index] & 0xFF;
                mutable.mutX(x);
                mutable.mutY(y);
                if (mask.test(mutable)) {
                    biomes[index] = biome;
                }
            }
        }
    }

    public void setOverlay(BufferedImage img, Pattern pattern, boolean white) {
        if (pattern instanceof BlockPattern) {
            setOverlay(img, (char) ((BlockPattern) pattern).getBlock().getCombined(), white);
            return;
        }
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        if (overlay == null) overlay = new char[getArea()];
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int height = img.getRGB(x, z) & 0xFF;
                if (height == 255 || height > 0 && !white && PseudoRandom.random.nextInt(256) <= height) {
                    mutable.mutX(x);
                    mutable.mutY(height);
                    overlay[index] = (char) pattern.apply(mutable).getCombined();
                }
            }
        }
    }

    public void setMain(BufferedImage img, Pattern pattern, boolean white) {
        if (pattern instanceof BlockPattern) {
            setMain(img, (char) ((BlockPattern) pattern).getBlock().getCombined(), white);
            return;
        }
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        modifiedMain = true;
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int height = img.getRGB(x, z) & 0xFF;
                if (height == 255 || height > 0 && !white && PseudoRandom.random.nextInt(256) <= height) {
                    mutable.mutX(x);
                    mutable.mutY(height);
                    main[index] = (char) pattern.apply(mutable).getCombined();
                }
            }
        }
    }

    public void setFloor(BufferedImage img, Pattern pattern, boolean white) {
        if (pattern instanceof BlockPattern) {
            setFloor(img, (char) ((BlockPattern) pattern).getBlock().getCombined(), white);
            return;
        }
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int height = img.getRGB(x, z) & 0xFF;
                if (height == 255 || height > 0 && !white && PseudoRandom.random.nextInt(256) <= height) {
                    mutable.mutX(x);
                    mutable.mutY(height);
                    floor[index] = (char) pattern.apply(mutable).getCombined();
                }
            }
        }
    }

    public void setColumn(BufferedImage img, Pattern pattern, boolean white) {
        if (pattern instanceof BlockPattern) {
            setColumn(img, (char) ((BlockPattern) pattern).getBlock().getCombined(), white);
            return;
        }
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        modifiedMain = true;
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int height = img.getRGB(x, z) & 0xFF;
                if (height == 255 || height > 0 && !white && PseudoRandom.random.nextInt(256) <= height) {
                    mutable.mutX(x);
                    mutable.mutY(height);
                    char combined = (char) pattern.apply(mutable).getCombined();
                    main[index] = combined;
                    floor[index] = combined;
                }
            }
        }
    }

    public void setOverlay(Mask mask, Pattern pattern) {
        if (pattern instanceof BlockPattern) {
            setOverlay(mask, (char) ((BlockPattern) pattern).getBlock().getCombined());
            return;
        }
        int index = 0;
        if (overlay == null) overlay = new char[getArea()];
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights[index] & 0xFF;
                mutable.mutX(x);
                mutable.mutY(y);
                if (mask.test(mutable)) {
                    overlay[index] = (char) pattern.apply(mutable).getCombined();
                }
            }
        }
    }

    public void setFloor(Mask mask, Pattern pattern) {
        if (pattern instanceof BlockPattern) {
            setFloor(mask, (char) ((BlockPattern) pattern).getBlock().getCombined());
            return;
        }
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights[index] & 0xFF;
                mutable.mutX(x);
                mutable.mutY(y);
                if (mask.test(mutable)) {
                    floor[index] = (char) pattern.apply(mutable).getCombined();
                }
            }
        }
    }

    public void setMain(Mask mask, Pattern pattern) {
        if (pattern instanceof BlockPattern) {
            setMain(mask, (char) ((BlockPattern) pattern).getBlock().getCombined());
            return;
        }
        modifiedMain = true;
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights[index] & 0xFF;
                mutable.mutX(x);
                mutable.mutY(y);
                if (mask.test(mutable)) {
                    main[index] = (char) pattern.apply(mutable).getCombined();
                }
            }
        }
    }

    public void setColumn(Mask mask, Pattern pattern) {
        if (pattern instanceof BlockPattern) {
            setColumn(mask, (char) ((BlockPattern) pattern).getBlock().getCombined());
            return;
        }
        modifiedMain = true;
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights[index] & 0xFF;
                mutable.mutX(x);
                mutable.mutY(y);
                if (mask.test(mutable)) {
                    char combined = (char) pattern.apply(mutable).getCombined();
                    floor[index] = combined;
                    main[index] = combined;
                }
            }
        }
    }

    public void setBiome(int biome) {
        Arrays.fill(biomes, (byte) biome);
    }

    public void setFloor(Pattern value) {
        if (value instanceof BlockPattern) {
            setFloor(((BlockPattern) value).getBlock().getCombined());
            return;
        }
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights[index] & 0xFF;
                mutable.mutX(x);
                mutable.mutY(y);
                floor[index] = (char) value.apply(mutable).getCombined();
            }
        }
    }

    public void setColumn(Pattern value) {
        if (value instanceof BlockPattern) {
            setColumn(((BlockPattern) value).getBlock().getCombined());
            return;
        }
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights[index] & 0xFF;
                mutable.mutX(x);
                mutable.mutY(y);
                char combined = (char) value.apply(mutable).getCombined();
                main[index] = combined;
                floor[index] = combined;
            }
        }
    }

    public void setMain(Pattern value) {
        if (value instanceof BlockPattern) {
            setMain(((BlockPattern) value).getBlock().getCombined());
            return;
        }
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights[index] & 0xFF;
                mutable.mutX(x);
                mutable.mutY(y);
                main[index] = (char) value.apply(mutable).getCombined();
            }
        }
    }

    public void setOverlay(Pattern value) {
        if (overlay == null) overlay = new char[getArea()];
        if (value instanceof BlockPattern) {
            setOverlay(((BlockPattern) value).getBlock().getCombined());
            return;
        }
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights[index] & 0xFF;
                mutable.mutX(x);
                mutable.mutY(y);
                overlay[index] = (char) value.apply(mutable).getCombined();
            }
        }
    }

    public void setHeights(int value) {
        Arrays.fill(heights, (byte) value);
    }

    @Override
    public boolean shouldWrite(int chunkX, int chunkZ) {
        return true;
    }

    @Override
    public MCAChunk write(MCAChunk chunk, int csx, int cex, int csz, int cez) {
        try {
            int[] indexes = indexStore.get();
            for (int i = 0; i < chunk.ids.length; i++) {
                byte[] idsArray = chunk.ids[i];
                if (idsArray != null) {
                    Arrays.fill(idsArray, (byte) 0);
                    Arrays.fill(chunk.data[i], (byte) 0);
                }
            }
            int index;
            int maxY = 0;
            int minY = Integer.MAX_VALUE;
            int[] heightMap = chunk.getHeightMapArray();
            int globalIndex;
            for (int z = csz; z <= cez; z++) {
                globalIndex = z * getWidth() + csx;
                index = (z & 15) << 4;
                for (int x = csx; x <= cex; x++, index++, globalIndex++) {
                    indexes[index] = globalIndex;
                    chunk.biomes[index] = biomes[globalIndex];
                    int height = heights[globalIndex] & 0xFF;
                    heightMap[index] = height;
                    maxY = Math.max(maxY, height);
                    minY = Math.min(minY, height);
                }
            }
            boolean hasOverlay = this.overlay != null;
            if (hasOverlay) {
                maxY++;
            }
            int maxLayer = maxY >> 4;
            int fillLayers = Math.max(0, (minY - 1)) >> 4;
            for (int layer = 0; layer <= maxLayer; layer++) {
                if (chunk.ids[layer] == null) {
                    chunk.ids[layer] = new byte[4096];
                    chunk.data[layer] = new byte[2048];
                    chunk.skyLight[layer] = new byte[2048];
                    chunk.blockLight[layer] = new byte[2048];
                }
            }
            if (waterHeight != 0) {
                maxY = Math.max(maxY, waterHeight);
                int maxWaterLayer = ((waterHeight + 15) >> 4);
                for (int layer = 0; layer < maxWaterLayer; layer++) {
                    boolean fillAll = (layer << 4) + 15 <= waterHeight;
                    byte[] ids = chunk.ids[layer];
                    if (ids == null) {
                        chunk.ids[layer] = ids = new byte[4096];
                        chunk.data[layer] = new byte[2048];
                        chunk.skyLight[layer] = new byte[2048];
                        chunk.blockLight[layer] = new byte[2048];
                        Arrays.fill(chunk.skyLight[layer], (byte) 255);
                    }
                    if (fillAll) {
                        Arrays.fill(ids, waterId);
                    } else {
                        int maxIndex = maxWaterLayer << 8;
                        Arrays.fill(ids, 0, maxIndex, waterId);
                    }
                }
            }
            if (modifiedMain) { // If the main block is modified, we can't short circuit this
                for (int layer = 0; layer < fillLayers; layer++) {
                    byte[] layerIds = chunk.ids[layer];
                    byte[] layerDatas = chunk.data[layer];
                    for (int z = csz; z <= cez; z++) {
                        index = (z & 15) << 4;
                        for (int x = csx; x <= cex; x++, index++) {
                            globalIndex = indexes[index];
                            char mainCombined = main[globalIndex];
                            byte id = (byte) FaweCache.getId(mainCombined);
                            int data = FaweCache.getData(mainCombined);
                            if (data != 0) {
                                for (int y = 0; y < 16; y++) {
                                    int mainIndex = index + (y << 8);
                                    chunk.setNibble(mainIndex, layerDatas, data);
                                }
                            }
                            for (int y = 0; y < 16; y++) {
                                layerIds[index + (y << 8)] = id;
                            }
                        }
                    }
                }
            } else {
                for (int layer = 0; layer < fillLayers; layer++) {
                    Arrays.fill(chunk.ids[layer], (byte) 1);
                }
            }
            for (int layer = fillLayers; layer <= maxLayer; layer++) {
                Arrays.fill(chunk.skyLight[layer], (byte) 255);
                byte[] layerIds = chunk.ids[layer];
                byte[] layerDatas = chunk.data[layer];
                int startY = layer << 4;
                int endY = startY + 15;
                for (int z = csz; z <= cez; z++) {
                    index = (z & 15) << 4;
                    for (int x = csx; x <= cex; x++, index++) {
                        globalIndex = indexes[index];
                        int height = heightMap[index];
                        int diff;
                        if (height > endY) {
                            diff = 16;
                        } else if (height >= startY) {
                            diff = height - startY;
                            char floorCombined = floor[globalIndex];
                            int id = FaweCache.getId(floorCombined);
                            int floorIndex = index + ((height & 15) << 8);
                            layerIds[floorIndex] = (byte) id;
                            int data = FaweCache.getData(floorCombined);
                            if (data != 0) {
                                chunk.setNibble(floorIndex, layerDatas, data);
                            }
                            if (hasOverlay && height >= startY - 1 && height < endY) {
                                char overlayCombined = overlay[globalIndex];
                                id = FaweCache.getId(overlayCombined);
                                int overlayIndex = index + (((height + 1) & 15) << 8);
                                layerIds[overlayIndex] = (byte) id;
                                data = FaweCache.getData(overlayCombined);
                                if (data != 0) {
                                    chunk.setNibble(overlayIndex, layerDatas, data);
                                }
                            }
                        } else if (hasOverlay && height == startY - 1) {
                            char overlayCombined = overlay[globalIndex];
                            int id = FaweCache.getId(overlayCombined);
                            int overlayIndex = index + (((height + 1) & 15) << 8);
                            layerIds[overlayIndex] = (byte) id;
                            int data = FaweCache.getData(overlayCombined);
                            if (data != 0) {
                                chunk.setNibble(overlayIndex, layerDatas, data);
                            }
                            continue;
                        } else {
                            continue;
                        }
                        char mainCombined = main[globalIndex];
                        byte id = (byte) FaweCache.getId(mainCombined);
                        int data = FaweCache.getData(mainCombined);
                        if (data != 0) {
                            for (int y = 0; y < diff; y++) {
                                int mainIndex = index + (y << 8);
                                chunk.setNibble(mainIndex, layerDatas, data);
                            }
                        }
                        for (int y = 0; y < diff; y++) {
                            layerIds[index + (y << 8)] = id;
                        }
                    }
                }
            }
            int maxYMod = 15 + (maxLayer << 4);
            for (int layer = (maxY >> 4) + 1; layer < 16; layer++) {
                chunk.ids[layer] = null;
                chunk.data[layer] = null;
            }
            { // Bedrock
                byte[] layerIds = chunk.ids[0];
                for (int z = csz; z <= cez; z++) {
                    index = (z & 15) << 4;
                    for (int x = csx; x <= cex; x++) {
                        layerIds[index++] = (byte) 7;
                    }
                }
            }
            int chunkPair = MathMan.pair((short) chunk.getX(), (short) chunk.getZ());
            char[][][] localBlocks = blocks.get(chunkPair);
            if (localBlocks != null) {
                for (int layer = 0; layer < 16; layer++) {
                    int by = layer << 4;
                    int ty = by + 15;
                    index = 0;
                    for (int y = by; y <= ty; y++, index += 256) {
                        char[][] yBlocks = localBlocks[y];
                        if (yBlocks != null) {
                            if (chunk.ids[layer] == null) {
                                chunk.ids[layer] = new byte[4096];
                                chunk.data[layer] = new byte[2048];
                                chunk.skyLight[layer] = new byte[2048];
                                chunk.blockLight[layer] = new byte[2048];
                            }
                            byte[] idsLayer = chunk.ids[layer];
                            byte[] dataLayer = chunk.data[layer];
                            for (int z = 0; z < yBlocks.length; z++) {
                                char[] zBlocks = yBlocks[z];
                                if (zBlocks != null) {
                                    int zIndex = index + (z << 4);
                                    for (int x = 0; x < zBlocks.length; x++, zIndex++) {
                                        char combined = zBlocks[x];
                                        if (combined == 0) continue;
                                        int id = FaweCache.getId(combined);
                                        if (!FaweCache.hasData(id)) {
                                            chunk.setIdUnsafe(idsLayer, zIndex, (byte) id);
                                        } else {
                                            chunk.setBlockUnsafe(idsLayer, dataLayer, zIndex, (byte) id, FaweCache.getData(combined));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return chunk;
    }

    private char get(char[][][] map, int x, int y, int z) {
        char[][] yMap = map[y];
        if (yMap == null) {
            return 0;
        }
        char[] zMap = yMap[z & 15];
        if (zMap == null) {
            return 0;
        }
        return zMap[x & 15];
    }

    private void setOverlay(Mask mask, char combined) {
        int index = 0;
        if (overlay == null) overlay = new char[getArea()];
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights[index] & 0xFF;
                mutable.mutX(x);
                mutable.mutY(y);
                if (mask.test(mutable)) {
                    overlay[index] = combined;
                }
            }
        }
    }

    private void setFloor(Mask mask, char combined) {
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights[index] & 0xFF;
                mutable.mutX(x);
                mutable.mutY(y);
                if (mask.test(mutable)) {
                    floor[index] = combined;
                }
            }
        }
    }

    private void setMain(Mask mask, char combined) {
        modifiedMain = true;
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights[index] & 0xFF;
                mutable.mutX(x);
                mutable.mutY(y);
                if (mask.test(mutable)) {
                    main[index] = combined;
                }
            }
        }
    }

    private void setColumn(Mask mask, char combined) {
        modifiedMain = true;
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            mutable.mutZ(z);
            for (int x = 0; x < getWidth(); x++, index++) {
                int y = heights[index] & 0xFF;
                mutable.mutX(x);
                mutable.mutY(y);
                if (mask.test(mutable)) {
                    floor[index] = combined;
                    main[index] = combined;
                }
            }
        }
    }

    private void setFloor(int value) {
        Arrays.fill(floor, (char) value);
    }

    private void setColumn(int value) {
        setFloor(value);
        setMain(value);
    }

    private void setMain(int value) {
        modifiedMain = true;
        Arrays.fill(main, (char) value);
    }

    private void setOverlay(int value) {
        if (overlay == null) overlay = new char[getArea()];
        Arrays.fill(overlay, (char) value);
    }

    private void setOverlay(BufferedImage img, char combined, boolean white) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        if (overlay == null) overlay = new char[getArea()];
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            for (int x = 0; x < getWidth(); x++, index++) {
                int height = img.getRGB(x, z) & 0xFF;
                if (height == 255 || height > 0 && white && PseudoRandom.random.nextInt(256) <= height) {
                    overlay[index] = combined;
                }
            }
        }
    }

    private void setMain(BufferedImage img, char combined, boolean white) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        modifiedMain = true;
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            for (int x = 0; x < getWidth(); x++, index++) {
                int height = img.getRGB(x, z) & 0xFF;
                if (height == 255 || height > 0 && !white && PseudoRandom.random.nextInt(256) <= height) {
                    main[index] = combined;
                }
            }
        }
    }

    private void setFloor(BufferedImage img, char combined, boolean white) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            for (int x = 0; x < getWidth(); x++, index++) {
                int height = img.getRGB(x, z) & 0xFF;
                if (height == 255 || height > 0 && !white && PseudoRandom.random.nextInt(256) <= height) {
                    floor[index] = combined;
                }
            }
        }
    }

    private void setColumn(BufferedImage img, char combined, boolean white) {
        if (img.getWidth() != getWidth() || img.getHeight() != getLength())
            throw new IllegalArgumentException("Input image dimensions do not match the current height map!");
        modifiedMain = true;
        int index = 0;
        for (int z = 0; z < getLength(); z++) {
            for (int x = 0; x < getWidth(); x++, index++) {
                int height = img.getRGB(x, z) & 0xFF;
                if (height == 255 || height > 0 && !white && PseudoRandom.random.nextInt(256) <= height) {
                    main[index] = combined;
                    floor[index] = combined;
                }
            }
        }
    }
}
