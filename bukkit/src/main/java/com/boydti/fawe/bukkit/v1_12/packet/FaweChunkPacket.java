package com.boydti.fawe.bukkit.v1_12.packet;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FaweChunkPacket {

    private final MCAChunk chunk;
    private final boolean full;
    private final boolean biomes;
    private final boolean sky;

    public FaweChunkPacket(MCAChunk fc, boolean replaceAllSections, boolean sendBiomeData, boolean hasSky) {
        this.chunk = fc;
        this.full = replaceAllSections;
        this.biomes = sendBiomeData;
        this.sky = hasSky;
    }

    public void write(PacketContainer packet) throws IOException {
        try {
            StructureModifier<Integer> ints = packet.getIntegers();
            StructureModifier<byte[]> byteArray = packet.getByteArrays();
            StructureModifier<Boolean> bools = packet.getBooleans();
            ints.write(0, this.chunk.getX());
            ints.write(1, this.chunk.getZ());

            bools.write(0, this.full);
            ints.write(2, this.chunk.getBitMask()); // writeVarInt

            FastByteArrayOutputStream fbaos = new FastByteArrayOutputStream();
            FaweOutputStream buffer = new FaweOutputStream(fbaos);
            byte[][] ids = chunk.ids;

            for (int layer = 0; layer < ids.length; layer++) {
                byte[] layerIds = ids[layer];
                if (layerIds == null) {
                    continue;
                }
                byte[] layerData = chunk.data[layer];
                int num = 9;
                buffer.write(num); // num blocks, anything > 8 - doesn't need to be accurate
                buffer.writeVarInt(0); // varint 0 - data palette global
                BitArray bits = new BitArray(num, 4096);
                for (int i = 0; i < 4096; i++) {
                    int id = layerIds[i];
                    if (id != 0) {
                        int data = FaweCache.hasData(id) ? chunk.getNibble(i, layerData) : 0;
                        int combined = FaweCache.getCombined(id, data);
                        bits.setAt(i, combined);
                    }
                }
                buffer.write(bits.getBackingLongArray());

                buffer.write(chunk.blockLight[layer]);
                if (sky) {
                    buffer.write(chunk.skyLight[layer]);
                }
            }

            if (this.biomes && chunk.biomes != null) {
                buffer.write(chunk.biomes);
            }

            byteArray.write(0, fbaos.toByteArray());
            // TODO - empty
            StructureModifier<List<NbtBase<?>>> list = packet.getListNbtModifier();
            list.write(0, new ArrayList<>());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
