package com.boydti.fawe.jnbt.anvil.filters;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAFile;
import com.boydti.fawe.jnbt.anvil.MCAFilterCounter;
import com.boydti.fawe.jnbt.anvil.MutableMCABackedBaseBlock;
import com.boydti.fawe.object.clipboard.ClipboardRemapper;
import com.boydti.fawe.object.number.MutableLong;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.ByteTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.blocks.BaseBlock;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RemapFilter extends MCAFilterCounter {
    private final ClipboardRemapper remapper;
    private final ClipboardRemapper.RemapPlatform from;
    private boolean skipRemap;

    public RemapFilter(ClipboardRemapper remapper) {
        this.remapper = remapper;
        this.from = null;
    }

    public RemapFilter(ClipboardRemapper.RemapPlatform from, ClipboardRemapper.RemapPlatform to) {
        this.remapper = new ClipboardRemapper(from, to);
        this.from = from;
    }

    @Override
    public MCAFile applyFile(MCAFile mca) {
        File file = mca.getFile();
        this.skipRemap = file.getName().endsWith(".mcapm");
        return super.applyFile(mca);
    }

    @Override
    public MCAChunk applyChunk(MCAChunk chunk, MutableLong cache) {
        if (skipRemap) return null;
        return super.applyChunk(chunk, cache);
    }

    @Override
    public void applyBlock(int x, int y, int z, BaseBlock block, MutableLong cache) {
        int id = block.getId();
        if (remapper.hasRemap(id)) {
            BaseBlock result = remapper.remap(block);
            if (result != block) {
                cache.add(1);
                block.setId(id = result.getId());
                if (id == 218) {
                    CompoundTag nbt = block.getNbtData();
                    if (nbt != null) {
                        Map<String, Tag> map = ReflectionUtils.getMap(nbt.getValue());
                        map.putIfAbsent("facing", new ByteTag((byte) block.getData()));
                    }
                }
                block.setData(result.getData());
            }
        }
        if (from != null) {
            outer:
            switch (from) {
                case PC: {
                    int newLight = 0;
                    switch (id) {
                        case 29:
                        case 33:
                            Map<String, Object> map = new HashMap<>();
                            map.put("Progress", 0f);
                            map.put("State", (byte) 0);
                            map.put("LastProgress", 0f);
                            map.put("NewState", (byte) 0);
                            map.put("isMoveable", (byte) 1);
                            map.put("id", "PistonArm");
                            map.put("AttachedBlocks", new ArrayList<>());
                            map.put("Sticky", (byte) (id == 29 ? 1 : 0));
                            map.put("x", x);
                            map.put("y", y);
                            map.put("z", z);
                            block.setNbtData(FaweCache.asTag(map));
                            break;
                        case 44:
                        case 182:
                        case 158:
                        case 53:
                        case 67:
                        case 108:
                        case 109:
                        case 114:
                        case 128:
                        case 134:
                        case 135:
                        case 136:
                        case 156:
                        case 163:
                        case 164:
                        case 180:
                        case 203:
                        case 198:
                            MutableMCABackedBaseBlock mcaBlock = (MutableMCABackedBaseBlock) block;
                            MCAChunk chunk = mcaBlock.getChunk();
                            int currentLight = chunk.getSkyLight(x, y, z);
                            if (currentLight >= 14) {
                                break;
                            }
                            newLight = chunk.getSkyLight(x, (y + 1) & 0xFF, z);
                            if (newLight > currentLight) break;
                            if (x > 0) {
                                if ((newLight = chunk.getSkyLight(x - 1, y, z)) > currentLight) break;
                            }
                            if (x < 16) {
                                if ((newLight = chunk.getSkyLight(x + 1, y, z)) > currentLight) break;
                            }
                            if (z > 0) {
                                if ((newLight = chunk.getSkyLight(x, y, z - 1)) > currentLight) break;
                            }
                            if (z < 16) {
                                if ((newLight = chunk.getSkyLight(x, y, z + 1)) > currentLight) break;
                            }
                        default:
                            break outer;
                    }
                    if (newLight != 0) {
                        ((MutableMCABackedBaseBlock) block).getChunk().setSkyLight(x, y, z, newLight);
                    }
                    break;
                }
            }
        }
    }
}
