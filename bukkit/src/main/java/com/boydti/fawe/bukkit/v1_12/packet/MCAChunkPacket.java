package com.boydti.fawe.bukkit.v1_12.packet;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import java.util.function.Function;

public class MCAChunkPacket implements Function<byte[], byte[]> {

    private final MCAChunk chunk;
    private final boolean full;
    private final boolean biomes;
    private final boolean sky;

    public MCAChunkPacket(MCAChunk fc, boolean replaceAllSections, boolean sendBiomeData, boolean hasSky) {
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
            byte[][] ids = chunk.ids;

            for (int layer = 0; layer < ids.length; layer++) {
                byte[] layerIds = ids[layer];
                if (layerIds == null) {
                    continue;
                }
                byte[] layerData = chunk.data[layer];
                int num = 13;
                sectionWriter.write(num); // num blocks, anything > 8 - doesn't need to be accurate
                sectionWriter.writeVarInt(0); // varint 0 - data palette global
                BitArray bits = new BitArray(num, 4096);
                bits.setAt(0, 0);
                for (int i = 0; i < 4096; i++) {
                    int id = layerIds[i] & 0xFF;
                    if (id != 0) {
                        int data = FaweCache.hasData(id) ? chunk.getNibble(i, layerData) : 0;
                        int combined = FaweCache.getCombined(id, data);
                        bits.setAt(i, combined);
                    }
                }
                sectionWriter.write(bits.getBackingLongArray());

                sectionWriter.write(chunk.blockLight[layer]);
                if (sky) {
                    sectionWriter.write(chunk.skyLight[layer]);
                }
            }

            if (this.biomes && chunk.biomes != null) {
                sectionWriter.write(chunk.biomes);
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
