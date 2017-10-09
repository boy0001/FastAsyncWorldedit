package com.boydti.fawe.object.clipboard.remap;

import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.object.clipboard.AbstractDelegateFaweClipboard;
import com.boydti.fawe.object.clipboard.FaweClipboard;
import com.sk89q.worldedit.blocks.BaseBlock;

public class RemappedClipboard extends AbstractDelegateFaweClipboard {
    private final ClipboardRemapper remapper;

    public RemappedClipboard(FaweClipboard parent, ClipboardRemapper remapper) {
        super(parent);
        this.remapper = remapper;
    }

    @Override
    public BaseBlock getBlock(int x, int y, int z) {
        return remapper.remap(super.getBlock(x, y, z));
    }

    @Override
    public BaseBlock getBlock(int index) {
        return remapper.remap(super.getBlock(index));
    }

    @Override
    public void forEach(BlockReader task, boolean air) {
        super.forEach(new BlockReader() {
            @Override
            public void run(int x, int y, int z, BaseBlock block) {
                task.run(x, y, z, remapper.remap(block));
            }
        }, air);
    }

    @Override
    public void streamIds(NBTStreamer.ByteReader task) {
        super.streamIds(new NBTStreamer.ByteReader() {
            @Override
            public void run(int index, int byteValue) {
                if (remapper.hasRemapId(byteValue)) {
                    int result = remapper.remapId(byteValue);
                    if (result != byteValue) {
                        task.run(index, result);
                    } else {
                        task.run(index, getBlock(index).getId());
                    }
                }
            }
        });
    }

    @Override
    public void streamDatas(NBTStreamer.ByteReader task) {
        super.streamDatas(new NBTStreamer.ByteReader() {
            @Override
            public void run(int index, int byteValue) {
                if (remapper.hasRemapData(byteValue)) {
                    task.run(index, getBlock(index).getData());
                }
            }
        });
    }
}
