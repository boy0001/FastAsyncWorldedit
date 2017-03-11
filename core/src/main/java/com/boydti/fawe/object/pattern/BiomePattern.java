package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.biome.BaseBiome;

public class BiomePattern extends ExistingPattern {
    private final BaseBiome biome;
    private BiomePatternException exception;

    public BiomePattern(Extent extent, BaseBiome biome) {
        super(extent);
        this.biome = biome;
        this.exception = new BiomePatternException();
    }

    @Override
    public BaseBlock apply(Vector position) {
        throw exception;
    }

    @Override
    public boolean apply(Extent extent, Vector position) throws WorldEditException {
        return extent.setBiome(new Vector2D(position.getBlockX(), position.getBlockZ()), biome);
    }

    public class BiomePatternException extends RuntimeException {

        public BiomePatternException() {
            super("Haha, you failed Empire92! Should've done things properly instead of some hacky AF biome pattern.\nHey, you! The one reading this stacktrace, can you do me a favor and report this on GitHub so I can get around to fixing it?");
        }

        public BiomePattern getPattern() {
            return BiomePattern.this;
        }

        public BaseBiome getBiome() {
            return biome;
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }
}
