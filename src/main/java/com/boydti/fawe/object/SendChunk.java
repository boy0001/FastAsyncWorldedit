package com.boydti.fawe.object;

import java.util.Collection;

public abstract class SendChunk {

    public abstract void fixLighting(final Collection<ChunkLoc> locs);

    public abstract void update(final Collection<ChunkLoc> locs);
}
