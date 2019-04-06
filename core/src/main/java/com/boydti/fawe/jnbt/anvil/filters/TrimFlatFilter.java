package com.boydti.fawe.jnbt.anvil.filters;

import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAFile;
import com.boydti.fawe.jnbt.anvil.MCAFilterCounter;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.number.MutableLong;

public class TrimFlatFilter extends MCAFilterCounter {
    @Override
    public MCAChunk applyChunk(MCAChunk chunk, MutableLong cache) {
        // Check other layers
        for (int layer = 1; layer < chunk.ids.length; layer++) {
            byte[] idLayer = chunk.ids[layer];
            if (idLayer == null) continue;
            for (int i = 0; i < 4096; i++) {
                if (idLayer[i] != 0) {
                    return null;
                }
            }
            { // Possibly dead code depending on the generator
                chunk.ids[layer] = null;
                chunk.data[layer] = null;
                chunk.setModified();
            }
        }
        byte[] layer0 = chunk.ids[0];
        if (layer0 == null) return null;
        int index = 0;
        for (int y = 0; y <= 3; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++, index++) {
                    int id = layer0[index] & 0xFF;
                    switch (id) {
                        case 2: // grass
                        case 3: // dirt
                        case 7: // bedrock
                            continue;
                        default:
                            return null;
                    }
                }
            }
        }
        for (int y = 4; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++, index++) {
                    if (layer0[index] != 0) return null;
                }
            }
        }

        // Check floor layers
        cache.add(Character.MAX_VALUE);
        chunk.setDeleted(true);
        return null;
    }

    @Override
    public void finishFile(MCAFile file, MutableLong cache) {
        boolean[] deleteFile = { true };
        file.forEachCachedChunk(new RunnableVal<MCAChunk>() {
            @Override
            public void run(MCAChunk value) {
                if (!value.isDeleted()) {
                    deleteFile[0] = false;
                }
            }
        });
        if (deleteFile[0]) {
            file.setDeleted(true);
        }
    }
}