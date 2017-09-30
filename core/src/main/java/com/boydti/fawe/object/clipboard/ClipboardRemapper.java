package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.Region;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import static com.boydti.fawe.FaweCache.getBlock;

public class ClipboardRemapper {
    public enum RemapPlatform {
        PE,
        PC
    }

    public ClipboardRemapper() {

    }

    public ClipboardRemapper(RemapPlatform fromPlatform, RemapPlatform toPlatform) {
        if (fromPlatform == toPlatform) {
            return;
        }
        HashMap<BaseBlock, BaseBlock> mapPEtoPC = new HashMap<>();
        mapPEtoPC.put(new BaseBlock(95,-1), new BaseBlock(166,-1));
        mapPEtoPC.put(new BaseBlock(125,-1), new BaseBlock(158,-1));
        mapPEtoPC.put(new BaseBlock(126,-1), new BaseBlock(157,-1));
        mapPEtoPC.put(new BaseBlock(157,-1), new BaseBlock(125,-1));
        mapPEtoPC.put(new BaseBlock(158,-1), new BaseBlock(126,-1));
        mapPEtoPC.put(new BaseBlock(188,-1), new BaseBlock(210,-1));
        mapPEtoPC.put(new BaseBlock(189,-1), new BaseBlock(211,-1));
        mapPEtoPC.put(new BaseBlock(198,-1), new BaseBlock(208,-1));
        mapPEtoPC.put(new BaseBlock(207,-1), new BaseBlock(212,-1));
        mapPEtoPC.put(new BaseBlock(208,-1), new BaseBlock(198,-1));
        for (int data = 0; data < 16; data++) {
            mapPEtoPC.put(new BaseBlock(218, data), new BaseBlock(219 + data, -1));
        }

        mapPEtoPC.put(new BaseBlock(220,-1), new BaseBlock(235,-1));
        mapPEtoPC.put(new BaseBlock(220,-1), new BaseBlock(235,-1));
        mapPEtoPC.put(new BaseBlock(221,-1), new BaseBlock(236,-1));
        mapPEtoPC.put(new BaseBlock(222,-1), new BaseBlock(237,-1));
        mapPEtoPC.put(new BaseBlock(223,-1), new BaseBlock(238,-1));
        mapPEtoPC.put(new BaseBlock(224,-1), new BaseBlock(239,-1));
        mapPEtoPC.put(new BaseBlock(225,-1), new BaseBlock(240,-1));
        mapPEtoPC.put(new BaseBlock(226,-1), new BaseBlock(241,-1));
        mapPEtoPC.put(new BaseBlock(227,-1), new BaseBlock(242,-1));
        mapPEtoPC.put(new BaseBlock(228,-1), new BaseBlock(243,-1));
        mapPEtoPC.put(new BaseBlock(229,-1), new BaseBlock(244,-1));
        mapPEtoPC.put(new BaseBlock(231,-1), new BaseBlock(246,-1));
        mapPEtoPC.put(new BaseBlock(232,-1), new BaseBlock(247,-1));
        mapPEtoPC.put(new BaseBlock(233,-1), new BaseBlock(248,-1));
        mapPEtoPC.put(new BaseBlock(234,-1), new BaseBlock(249,-1));

        for (int id = 220; id <= 235; id++) {
            int pcId = id + 15;
            int peId = id == 230 ? 219 : id;
            mapPEtoPC.put(new BaseBlock(peId,3), new BaseBlock(pcId,0));
            mapPEtoPC.put(new BaseBlock(peId,4), new BaseBlock(pcId,1));
            mapPEtoPC.put(new BaseBlock(peId,2), new BaseBlock(pcId,2));
            mapPEtoPC.put(new BaseBlock(peId,5), new BaseBlock(pcId,3));
        }

        for (int id : new int[] {29, 33}) {
            addBoth(getBlock(id, 3), getBlock(id, 4));
            addBoth(getBlock(id, 10), getBlock(id, 11));
        }

        mapPEtoPC.put(new BaseBlock(236,-1), new BaseBlock(251,-1));
        mapPEtoPC.put(new BaseBlock(237,-1), new BaseBlock(252,-1));
        mapPEtoPC.put(new BaseBlock(240,-1), new BaseBlock(199,-1));
        mapPEtoPC.put(new BaseBlock(241,-1), new BaseBlock(95,-1));
        mapPEtoPC.put(new BaseBlock(243,0), new BaseBlock(3, 2));
        mapPEtoPC.put(new BaseBlock(244,-1), new BaseBlock(207,-1));

        mapPEtoPC.put(new BaseBlock(251,-1), new BaseBlock(218,-1));

        mapPEtoPC.put(new BaseBlock(168,2), new BaseBlock(168,1));
        mapPEtoPC.put(new BaseBlock(168,1), new BaseBlock(168,2));

        mapPEtoPC.put(new BaseBlock(44,7), new BaseBlock(44,6));
        mapPEtoPC.put(new BaseBlock(44,6), new BaseBlock(44,7));
        // Top variant
        mapPEtoPC.put(new BaseBlock(44,7 + 8), new BaseBlock(44,6 + 8));
        mapPEtoPC.put(new BaseBlock(44,6 + 8), new BaseBlock(44,7 + 8));

        mapPEtoPC.put(new BaseBlock(43,7), new BaseBlock(43,6));
        mapPEtoPC.put(new BaseBlock(43,6), new BaseBlock(43,7));

        mapPEtoPC.put(new BaseBlock(36,-1), new BaseBlock(34, 1));
        mapPEtoPC.put(new BaseBlock(85, 1), new BaseBlock(188,-1));
        mapPEtoPC.put(new BaseBlock(85,2), new BaseBlock(189,-1));
        mapPEtoPC.put(new BaseBlock(85,3), new BaseBlock(190,-1));
        mapPEtoPC.put(new BaseBlock(85,4), new BaseBlock(192,-1));
        mapPEtoPC.put(new BaseBlock(85, 5), new BaseBlock(191,-1));
        mapPEtoPC.put(new BaseBlock(202,-1), new BaseBlock(201,2));
        mapPEtoPC.put(new BaseBlock(182,1), new BaseBlock(205,-1));

        for (int id : new int[] {208}) { // end rod
            mapPEtoPC.put(new BaseBlock(id,4), new BaseBlock(id,5));
            mapPEtoPC.put(new BaseBlock(id,2), new BaseBlock(id,3));
            mapPEtoPC.put(new BaseBlock(id,5), new BaseBlock(id,4));
            mapPEtoPC.put(new BaseBlock(id,3), new BaseBlock(id,2));
        }

        for (int id : new int[] {77, 143}) { // button
            mapPEtoPC.put(new BaseBlock(id,4), new BaseBlock(id,2));
            mapPEtoPC.put(new BaseBlock(id,1), new BaseBlock(id,5));
            mapPEtoPC.put(new BaseBlock(id,2), new BaseBlock(id,4));
            mapPEtoPC.put(new BaseBlock(id,5), new BaseBlock(id,1));
        }

        // leaves
        for (int data = 4; data < 8; data++) mapPEtoPC.put(new BaseBlock(18,data + 4), new BaseBlock(18,data));
        for (int data = 4; data < 8; data++)  mapPEtoPC.put(new BaseBlock(18,data + 8), new BaseBlock(161,data));

        for (int id : new int[] {96, 167}) { // trapdoor
            for (int data = 0; data < 4; data++) mapPEtoPC.put(new BaseBlock(id, data), new BaseBlock(id, 3 - data));
            for (int data = 4; data < 12; data++) mapPEtoPC.put(new BaseBlock(id, data), new BaseBlock(id, 15 - data));
            for (int data = 12; data < 15; data++) mapPEtoPC.put(new BaseBlock(id, data), new BaseBlock(id, 27 - data));
        }

        // TODO any custom ids
        switch (fromPlatform) {
            case PE:
                break;
            case PC:
                break;
        }

        for (Map.Entry<BaseBlock, BaseBlock> entry : mapPEtoPC.entrySet()) {
            BaseBlock from = entry.getKey();
            BaseBlock to = entry.getValue();
            if (fromPlatform == RemapPlatform.PE) {
                add(from, to);
            } else {
                add(to, from);
            }
        }
    }

    public void addBoth(BaseBlock from, BaseBlock to) {
        add(from, to);
        add(to, from);
    }

    public void apply(Clipboard clipboard) throws WorldEditException {
        if (clipboard instanceof BlockArrayClipboard) {
            BlockArrayClipboard bac = (BlockArrayClipboard) clipboard;
            bac.IMP = new RemappedClipboard(bac.IMP, this);
        } else {
            Region region = clipboard.getRegion();
            for (BlockVector pos : region) {
                BaseBlock block = clipboard.getBlock(pos);
                BaseBlock newBlock = remap(block);
                if (block != newBlock) {
                    clipboard.setBlock(pos, newBlock);
                }
            }
        }
    }

    private char[] remapCombined = new char[Character.MAX_VALUE + 1];
    private boolean[] remap = new boolean[Character.MAX_VALUE + 1];
    private boolean[] remapIds = new boolean[4096];
    private boolean[] remapAllIds = new boolean[4096];
    private boolean[] remapAnyIds = new boolean[4096];
    private boolean[] remapData = new boolean[16];

    public boolean hasRemapData(int data) {
        return remapData[data];
    }

    public boolean hasRemapId(int id) {
        return remapAnyIds[id];
    }

    public boolean hasRemap(int id) {
        return remapIds[id];
    }

    public int remapId(int id) {
        if (remapAllIds[id]) {
            return remapCombined[id << 4] >> 4;
        }
        return id;
    }


    public void add(BaseBlock from, BaseBlock to) {
        if (from.getData() != to.getData()) {
            if (from.getData() == -1) {
                Arrays.fill(remapData, true);
            } else {
                remapData[from.getData()] = true;
            }
        }
        if (from.getData() == -1) {
            for (int data = 0; data < 16; data++) {
                int combinedFrom = (from.getId() << 4) + data;
                int combinedTo = to.getData() == -1 ? (to.getId() << 4) + data : to.getCombined();
                remap[combinedFrom] = true;
                remapCombined[combinedFrom] = (char) combinedTo;
                remapIds[combinedFrom >> 4] = true;
                if (from.getId() != to.getId()) {
                    remapAnyIds[combinedFrom >> 4] = true;
                    remapAllIds[from.getId()] = true;
                }
            }
        } else {
            int data = from.getData();
            int combinedFrom = (from.getId() << 4) + data;
            int combinedTo = to.getData() == -1 ? (to.getId() << 4) + data : to.getCombined();
            remap[combinedFrom] = true;
            remapCombined[combinedFrom] = (char) combinedTo;
            remapIds[combinedFrom >> 4] = true;
            if (from.getId() != to.getId()) {
                remapAnyIds[combinedFrom >> 4] = true;
                remapAllIds[from.getId()] = false;
            }
        }
    }

    public BaseBlock remap(BaseBlock block) {
        int combined = block.getCombined();
        if (remap[combined]) {
            char value = remapCombined[combined];
            BaseBlock newBlock = FaweCache.CACHE_BLOCK[value];
            newBlock.setNbtData(block.getNbtData());
            return newBlock;
        }
        return block;
    }

}
