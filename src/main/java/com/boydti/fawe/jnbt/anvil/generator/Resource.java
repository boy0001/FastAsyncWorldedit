package com.boydti.fawe.jnbt.anvil.generator;

import com.boydti.fawe.object.PseudoRandom;
import com.sk89q.worldedit.WorldEditException;

public abstract class Resource {
    public Resource() {
    }

    public abstract boolean spawn(PseudoRandom random, int x, int z) throws WorldEditException;
}
