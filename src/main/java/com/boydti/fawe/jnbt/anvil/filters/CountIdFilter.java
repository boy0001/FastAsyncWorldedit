package com.boydti.fawe.jnbt.anvil.filters;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAFilterCounter;
import com.boydti.fawe.object.number.MutableLong;
import com.sk89q.worldedit.blocks.BaseBlock;

public class CountIdFilter extends MCAFilterCounter {
    private final boolean[] allowedId = new boolean[FaweCache.getId(Character.MAX_VALUE)];

    public CountIdFilter() {
    }

    public CountIdFilter addBlock(int id) {
        allowedId[id] = true;
        return this;
    }

    public CountIdFilter addBlock(BaseBlock block) {
        allowedId[block.getId()] = true;
        return this;
    }

    @Override
    public MCAChunk applyChunk(MCAChunk chunk, MutableLong count) {
        for (int layer = 0; layer < chunk.ids.length; layer++) {
            byte[] ids = chunk.ids[layer];
            if (ids != null) {
                for (byte i : ids) {
                    if (allowedId[i & 0xFF]) {
                        count.increment();
                    }
                }
            }
        }
        return null;
    }
}
