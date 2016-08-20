package com.boydti.fawe.jnbt.anvil;

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

public class MCAQueue extends NMSMappedFaweQueue<FaweQueue, MCAChunk, MCAChunk, MCAChunk> {

    private final FaweQueue parent;

    public MCAQueue(FaweQueue parent) {
        super(parent.getWorldName(), new MCAQueueMap());
        this.parent = parent;
    }

    @Override
    public void relight(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void relightBlock(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void relightSky(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public boolean regenerateChunk(FaweQueue faweQueue, int x, int z) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public boolean setComponents(FaweChunk fc, RunnableVal<FaweChunk> changeTask) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public CharFaweChunk getPrevious(CharFaweChunk fs, MCAChunk sections, Map<?, ?> tiles, Collection<?>[] entities, Set<UUID> createdEntities, boolean all) throws Exception {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public MCAChunk getChunk(FaweQueue faweQueue, int x, int z) {
        return (MCAChunk) getFaweChunk(x, z);
    }

    @Override
    public FaweQueue getImpWorld() {
        return parent;
    }

    @Override
    public void setFullbright(MCAChunk sections) {
        sections.setFullbright();
    }

    @Override
    public boolean removeLighting(MCAChunk sections, RelightMode mode, boolean hasSky) {
        if (mode != RelightMode.NONE) {
            sections.removeLight();
            return true;
        }
        return false;
    }

    @Override
    public void setSkyLight(MCAChunk chars, int x, int y, int z, int value) {
        chars.setSkyLight(x, y, z, value);
    }

    @Override
    public void setBlockLight(MCAChunk chars, int x, int y, int z, int value) {
        chars.setBlockLight(x, y, z, value);
    }

    @Override
    public void refreshChunk(FaweChunk fs) {
        // Nothing
    }

    @Override
    public CompoundTag getTileEntity(MCAChunk mcaChunk, int x, int y, int z) {
        return mcaChunk.getTile(x, y, z);
    }

    @Override
    public boolean isChunkLoaded(FaweQueue faweQueue, int x, int z) {
        return true;
    }

    @Override
    public FaweChunk getFaweChunk(int x, int z) {
        return getFaweQueueMap().getFaweChunk(x, z);
    }

    @Override
    public File getSaveFolder() {
        return parent.getSaveFolder();
    }

    @Override
    public boolean hasSky() {
        return parent.hasSky();
    }

    @Override
    public boolean loadChunk(FaweQueue faweQueue, int x, int z, boolean generate) {
        return true;
    }

    @Override
    public MCAChunk getCachedSections(FaweQueue faweQueue, int cx, int cz) {
        return (MCAChunk) getFaweQueueMap().getFaweChunk(cx, cz);
    }

    @Override
    public MCAChunk getCachedSection(MCAChunk mcaChunk, int cy) {
        if (mcaChunk.doesSectionExist(cy)) {
            return mcaChunk;
        }
        return null;
    }

    @Override
    public int getCombinedId4Data(MCAChunk chars, int x, int y, int z) {
        return chars.getBlockCombinedId(x, y, z);
    }

    @Override
    public int getSkyLight(MCAChunk sections, int x, int y, int z) {
        return sections.getSkyLight(x, y, z);
    }

    @Override
    public int getEmmittedLight(MCAChunk sections, int x, int y, int z) {
        return sections.getBlockLight(x, y, z);
    }
}
