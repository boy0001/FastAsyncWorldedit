package com.boydti.fawe.bukkit.v1_12.packet;

import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import java.util.function.Function;

public class FaweChunkPacket implements Function<byte[], byte[]> {

    private final FaweChunk chunk;
    private final boolean full;
    private final boolean biomes;
    private final boolean sky;

    public FaweChunkPacket(FaweChunk fc, boolean replaceAllSections, boolean sendBiomeData, boolean hasSky) {
        this.chunk = fc;
        this.full = replaceAllSections;
        this.biomes = sendBiomeData;
        this.sky = hasSky;
    }

    @Override
    public byte[] apply(byte[] buffer) {
        try {
            FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
            FaweOutputStream fos = new FaweOutputStream(baos);

            fos.writeInt(this.chunk.getX());
            fos.writeInt(this.chunk.getZ());

            fos.writeBoolean(this.full);
            fos.writeVarInt(this.chunk.getBitMask()); // writeVarInt

            FastByteArrayOutputStream sectionByteArray = new FastByteArrayOutputStream(buffer);
            FaweOutputStream sectionWriter = new FaweOutputStream(sectionByteArray);

            char[][] ids = chunk.getCombinedIdArrays();
            byte[][] blockLight = chunk.getBlockLightArray();
            byte[][] skyLight = chunk.getSkyLightArray();

            for (int layer = 0; layer < ids.length; layer++) {
                char[] layerIds = ids[layer];
                if (layerIds == null) {
                    continue;
                }
                int num = 13;
                sectionWriter.write(num); // num blocks, anything > 8 - doesn't need to be accurate
                sectionWriter.writeVarInt(0); // varint 0 - data palette global
                BitArray bits = new BitArray(num, 4096);
                for (int i = 0; i < 4096; i++) {
                    char combinedId = layerIds[i];
                    if (combinedId != 0) {
                        bits.setAt(i, combinedId);
                    }
                }
                sectionWriter.write(bits.getBackingLongArray());

                if (blockLight != null && blockLight[layer] != null) {
                    sectionWriter.write(blockLight[layer]);
                } else {
                    sectionWriter.write(0, 2048);
                }
                if (sky) {
                    if (skyLight != null && skyLight[layer] != null) {
                        sectionWriter.write(skyLight[layer]);
                    } else {
                        sectionWriter.write((byte) 255, 2048);
                    }
                }
            }

            if (this.biomes) {
                byte[] biomeArr = chunk.getBiomeArray();
                if (biomeArr != null) {
                    sectionWriter.write(biomeArr);
                }
            }

            fos.writeVarInt(sectionByteArray.getSize());
            for (byte[] arr : sectionByteArray.toByteArrays()) {
                fos.write(arr);
            }
            fos.writeVarInt(0);

            fos.close();
            sectionWriter.close();
            return baos.toByteArray();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
