package com.boydti.fawe.nukkit.core.converter;

import com.boydti.fawe.command.AnvilCommands;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAFile;
import com.boydti.fawe.jnbt.anvil.MCAFilter;
import com.boydti.fawe.jnbt.anvil.filters.DelegateMCAFilter;
import com.boydti.fawe.jnbt.anvil.filters.RemapFilter;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.clipboard.remap.ClipboardRemapper;
import com.boydti.fawe.object.number.MutableLong;
import com.boydti.fawe.util.SetQueue;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.MethodCommands;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.command.binding.Switch;
import java.io.IOException;

public class ConvertCommands extends MethodCommands {
    public ConvertCommands(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Command(
            aliases = {"anvil2leveldb"},
            usage = "<folder>",
            desc = "Convert the world between MCPE/PC values",
            help = "Convert the world between MCPE/PC values\n" +
                    "The -r filter will skip block remapping",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.anvil.anvil2leveldb")
    public void anvil2leveldb(Player player, String folder, @Switch('f') boolean force, @Switch('r') boolean skipRemap) throws WorldEditException {
        ClipboardRemapper mapper;
        RemapFilter filter = new RemapFilter(ClipboardRemapper.RemapPlatform.PC, ClipboardRemapper.RemapPlatform.PE);

        FaweQueue defaultQueue = SetQueue.IMP.getNewQueue(folder, true, false);
        try (MCAFile2LevelDB converter = new MCAFile2LevelDB(null, defaultQueue.getSaveFolder().getParentFile())) {

            DelegateMCAFilter<MutableLong> delegate = new DelegateMCAFilter<MutableLong>(filter) {
                @Override
                public void finishFile(MCAFile file, MutableLong cache) {
                    file.forEachChunk(new RunnableVal<MCAChunk>() {
                        @Override
                        public void run(MCAChunk value) {
                            try {
                                converter.write(value, !skipRemap, 0);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    file.clear();
                }
            };
            MCAFilter result = AnvilCommands.runWithWorld(player, folder, delegate, force);
            if (result != null) player.print(BBC.getPrefix() + BBC.VISITOR_BLOCK.format(filter.getTotal()));
        }
    }
}
