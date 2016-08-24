package com.boydti.fawe.command;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAFilter;
import com.boydti.fawe.jnbt.anvil.MCAQueue;
import com.boydti.fawe.object.number.LongAdder;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.parametric.Optional;
import java.io.File;
import java.util.Set;


import static com.google.common.base.Preconditions.checkNotNull;

public class AnvilCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public AnvilCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
            aliases = { "/countall" },
            usage = "<folder> [hasSky] <id>",
            desc = "Count all blocks in a world",
            flags = "d",
            min = 2,
            max = 3
    )
    @CommandPermissions("worldedit.anvil.countallstone")
    public void countAll(Player player, EditSession editSession, String folder, @Optional("true") boolean hasSky, String arg, @Switch('d') boolean useData) throws WorldEditException {
        File root = new File(folder + File.separator + "region");
        MCAQueue queue = new MCAQueue(folder, root, hasSky);
        final LongAdder count = new LongAdder();
        if (arg.contains(":")) {
            useData = true; //override d flag, if they specified data they want it
        }
        Set<BaseBlock> searchBlocks = worldEdit.getBlocks(player, arg, true);
        final boolean[] allowedId = new boolean[FaweCache.getId(Character.MAX_VALUE)];
        for (BaseBlock block : searchBlocks) {
            allowedId[block.getId()] = true;
        }
        MCAFilter filter;
        if (useData) { // Optimize for both cases
            final boolean[] allowed = new boolean[Character.MAX_VALUE];
            for (BaseBlock block : searchBlocks) {
                allowed[FaweCache.getCombined(block)] = true;
            }
            filter = new MCAFilter() {
                @Override
                public MCAChunk applyChunk(MCAChunk chunk) {
                    for (int layer = 0; layer < chunk.ids.length; layer++) {
                        byte[] ids = chunk.ids[layer];
                        if (ids == null) {
                            continue;
                        }
                        byte[] datas = chunk.data[layer];
                        for (int i = 0; i < ids.length; i++) {
                            int id = ids[i] & 0xFF;
                            if (!allowedId[id]) {
                                continue;
                            }
                            int combined = (id) << 4;
                            if (FaweCache.hasData(id)) {
                                combined += chunk.getNibble(i, datas);
                            }
                            if (allowed[combined]) {
                                count.add(1);
                            }
                        }
                    }
                    return null;
                }
            };
        } else {
            filter = new MCAFilter() {
                @Override
                public MCAChunk applyChunk(MCAChunk chunk) {
                    for (int layer = 0; layer < chunk.ids.length; layer++) {
                        byte[] ids = chunk.ids[layer];
                        if (ids != null) {
                            for (byte i : ids) {
                                if (allowedId[i & 0xFF]) {
                                    count.add(1);
                                }
                            }
                        }
                    }
                    return null;
                }
            };
        }
        queue.filterWorld(filter);
        BBC.SELECTION_COUNT.send(player, count.longValue());
    }

}
