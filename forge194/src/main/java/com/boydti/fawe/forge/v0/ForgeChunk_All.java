package com.boydti.fawe.forge.v0;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MainUtil;
import java.lang.reflect.Field;
import net.minecraft.block.Block;
import net.minecraft.util.BitArray;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockStateContainer;
import net.minecraft.world.chunk.BlockStatePaletteRegistry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IBlockStatePalette;

public class ForgeChunk_All extends CharFaweChunk<Chunk> {

    public BlockStateContainer[] sectionPalettes;

    /**
     * A FaweSections object represents a chunk and the blocks that you wish to change in it.
     *
     * @param parent
     * @param x
     * @param z
     */
    public ForgeChunk_All(FaweQueue parent, int x, int z) {
        super(parent, x, z);
    }

    @Override
    public Chunk getNewChunk() {
        World world = ((ForgeQueue_All) getParent()).getWorld();
        return world.getChunkProvider().provideChunk(getX(), getZ());
    }

    @Override
    public CharFaweChunk<Chunk> copy(boolean shallow) {
        ForgeChunk_All value = (ForgeChunk_All) super.copy(shallow);
        if (sectionPalettes != null) {
            value.sectionPalettes = new BlockStateContainer[16];
            try {
                Field fieldBits = BlockStateContainer.class.getDeclaredField("storage");
                fieldBits.setAccessible(true);
                Field fieldPalette = BlockStateContainer.class.getDeclaredField("palette");
                fieldPalette.setAccessible(true);
                Field fieldSize = BlockStateContainer.class.getDeclaredField("bits");
                fieldSize.setAccessible(true);
                for (int i = 0; i < sectionPalettes.length; i++) {
                    BlockStateContainer current = sectionPalettes[i];
                    if (current == null) {
                        continue;
                    }
                    // Clone palette
                    IBlockStatePalette currentPalette = (IBlockStatePalette) fieldPalette.get(current);
                    if (!(currentPalette instanceof BlockStatePaletteRegistry)) {
                        current.onResize(128, null);
                    }
                    BlockStateContainer paletteBlock = new BlockStateContainer();
                    currentPalette = (IBlockStatePalette) fieldPalette.get(current);
                    if (!(currentPalette instanceof BlockStatePaletteRegistry)) {
                        throw new RuntimeException("Palette must be global!");
                    }
                    fieldPalette.set(paletteBlock, currentPalette);
                    // Clone size
                    fieldSize.set(paletteBlock, fieldSize.get(current));
                    // Clone palette
                    BitArray currentBits = (BitArray) fieldBits.get(current);
                    BitArray newBits = new BitArray(1, 0);
                    for (Field field : BitArray.class.getDeclaredFields()) {
                        field.setAccessible(true);
                        Object currentValue = field.get(currentBits);
                        if (currentValue instanceof long[]) {
                            currentValue = ((long[]) currentValue).clone();
                        }
                        field.set(newBits, currentValue);
                    }
                    fieldBits.set(paletteBlock, newBits);
                    value.sectionPalettes[i] = paletteBlock;
                }
            } catch (Throwable e) {
                MainUtil.handleError(e);
            }
        }
        return value;
    }

    public void optimize() {
        if (sectionPalettes != null) {
            return;
        }
        char[][] arrays = getCombinedIdArrays();
        char lastChar = Character.MAX_VALUE;
        for (int layer = 0; layer < 16; layer++) {
            if (getCount(layer) > 0) {
                if (sectionPalettes == null) {
                    sectionPalettes = new BlockStateContainer[16];
                }
                BlockStateContainer palette = new BlockStateContainer();
                char[] blocks = getIdArray(layer);
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            char combinedId = blocks[FaweCache.CACHE_J[y][z][x]];
                            if (combinedId > 1) {
                                palette.set(x, y, z, Block.getBlockById(combinedId >> 4).getStateFromMeta(combinedId & 0xF));
                            }
                        }
                    }
                }
            }
        }
    }
}
