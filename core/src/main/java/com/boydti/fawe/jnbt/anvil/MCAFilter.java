package com.boydti.fawe.jnbt.anvil;

import com.sk89q.worldedit.blocks.BaseBlock;

public class MCAFilter {
    public boolean appliesFile(int mcaX, int mcaZ) {
        return true;
    }

    public MCAFile applyFile(MCAFile file) {
        return file;
    }

    public boolean appliesChunk(int cx, int cz) {
        return true;
    }

    public MCAChunk applyChunk(MCAChunk chunk) {
        return chunk;
    }

    public void applyBlock(int x, int y, int z, BaseBlock block) {}
}
