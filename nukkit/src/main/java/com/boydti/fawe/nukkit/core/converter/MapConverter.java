package com.boydti.fawe.nukkit.core.converter;

import java.io.Closeable;
import java.io.File;
import java.util.function.Consumer;

public abstract class MapConverter implements Consumer<ConverterFrame>, Closeable {

    protected final File folderTo;
    protected final File folderFrom;

    public MapConverter(File worldFrom, File worldTo) {
        this.folderFrom = worldFrom;
        this.folderTo = worldTo;
    }

    public static MapConverter get(File worldFrom, File worldTo) {
        if (new File(worldFrom, "db").exists()) {
            return new LevelDBToMCAFile(worldFrom, worldTo);
        } else {
            return new MCAFile2LevelDB(worldFrom, worldTo);
        }
    }

    public File getFolderTo() {
        return folderTo;
    }

    public File getFolderFrom() {
        return folderFrom;
    }

    private static Tag[] tags = new Tag[256];

    public enum Tag {
        Data2D(45),
        @Deprecated Data2DLegacy(46),
        SubChunkPrefix(47),
        @Deprecated LegacyTerrain(48),
        BlockEntity(49),
        Entity(50),
        PendingTicks(51),
        BlockExtraData(52),
        BiomeState(53),
        FinalizedState(54),
        Version(118),

        ;

        public final byte value;

        Tag(int value) {
            this.value = (byte) value;
            tags[value & 0xFF] = this;
        }

        public static Tag valueOf(byte key) {
            return tags[key & 0xFF];
        }

        public static Tag get(byte[] key) {
            if (key.length != 9 && key.length != 10) return null;
            return valueOf(key[8]);
        }

        public int getX(byte[] key) {
            return ((key[3] & 0xFF << 24) + (key[2] & 0xFF << 16) + (key[1] & 0xFF << 8) + (key[0] & 0xFF << 0));
        }

        public int getZ(byte[] key) {
            return ((key[7] & 0xFF << 24) + (key[6] & 0xFF << 16) + (key[5] & 0xFF << 8) + (key[4] & 0xFF << 0));
        }

        public byte[] fill(int chunkX, int chunkZ, byte[] key) {
            key[0] = (byte) (chunkX & 255);
            key[1] = (byte) (chunkX >>> 8 & 255);
            key[2] = (byte) (chunkX >>> 16 & 255);
            key[3] = (byte) (chunkX >>> 24 & 255);
            key[4] = (byte) (chunkZ & 255);
            key[5] = (byte) (chunkZ >>> 8 & 255);
            key[6] = (byte) (chunkZ >>> 16 & 255);
            key[7] = (byte) (chunkZ >>> 24 & 255);
            key[8] = value;
            return key;
        }
    }
}
