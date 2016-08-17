package com.boydti.fawe.jnbt;

import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RunnableVal;
import com.sk89q.jnbt.CompoundTag;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MCAQueue extends NMSMappedFaweQueue<FaweQueue, MCAChunk, MCAChunk, char[]> {

    private final FaweQueue parent;

    public MCAQueue(FaweQueue parent) {
        super(parent.getWorldName());
        this.parent = parent;
    }

    @Override
    public void setFullbright(MCAChunk sections) {

    }

    @Override
    public boolean removeLighting(MCAChunk sections, RelightMode mode, boolean hasSky) {
        return false;
    }

    @Override
    public void relight(int x, int y, int z) {

    }

    @Override
    public void relightBlock(int x, int y, int z) {

    }

    @Override
    public void relightSky(int x, int y, int z) {

    }

    @Override
    public void setSkyLight(char[] chars, int x, int y, int z, int value) {

    }

    @Override
    public void setBlockLight(char[] chars, int x, int y, int z, int value) {

    }

    @Override
    public void refreshChunk(FaweChunk fs) {

    }

    @Override
    public CharFaweChunk getPrevious(CharFaweChunk fs, MCAChunk sections, Map<?, ?> tiles, Collection<?>[] entities, Set<UUID> createdEntities, boolean all) throws Exception {
        return null;
    }

    @Override
    public CompoundTag getTileEntity(MCAChunk mcaChunk, int x, int y, int z) {
        return null;
    }

    @Override
    public MCAChunk getChunk(FaweQueue faweQueue, int x, int z) {
        return null;
    }

    @Override
    public FaweQueue getImpWorld() {
        return null;
    }

    @Override
    public boolean isChunkLoaded(FaweQueue faweQueue, int x, int z) {
        return false;
    }

    @Override
    public boolean regenerateChunk(FaweQueue faweQueue, int x, int z) {
        return false;
    }

    @Override
    public boolean setComponents(FaweChunk fc, RunnableVal<FaweChunk> changeTask) {
        return false;
    }

    @Override
    public FaweChunk getFaweChunk(int x, int z) {
        return null;
    }

    @Override
    public File getSaveFolder() {
        return null;
    }

    @Override
    public boolean hasSky() {
        return false;
    }

    @Override
    public boolean loadChunk(FaweQueue faweQueue, int x, int z, boolean generate) {
        return false;
    }

    @Override
    public MCAChunk getCachedSections(FaweQueue faweQueue, int cx, int cz) {
        return null;
    }

    @Override
    public int getCombinedId4Data(char[] chars, int x, int y, int z) {
        return 0;
    }

    @Override
    public int getSkyLight(char[] sections, int x, int y, int z) {
        return 0;
    }

    @Override
    public int getEmmittedLight(char[] sections, int x, int y, int z) {
        return 0;
    }
}
