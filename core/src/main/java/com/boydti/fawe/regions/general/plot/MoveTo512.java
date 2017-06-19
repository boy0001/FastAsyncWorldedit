package com.boydti.fawe.regions.general.plot;

import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAQueue;
import com.boydti.fawe.jnbt.anvil.MCAWriter;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.SetQueue;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.commands.CommandCategory;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.commands.RequiredType;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RunnableVal2;
import com.intellectualcrafters.plot.object.RunnableVal3;
import com.plotsquared.general.commands.Command;
import com.plotsquared.general.commands.CommandDeclaration;
import java.io.File;

@CommandDeclaration(
        command = "moveto512",
        permission = "plots.moveto512",
        category = CommandCategory.ADMINISTRATION,
        requiredType = RequiredType.CONSOLE,
        description = "Move plots to a 512 sized region",
        usage = "/plots moveto512 [world]"
)
public class MoveTo512 extends Command {
    public MoveTo512() {
        super(MainCommand.getInstance(), true);
    }

    @Override
    public void execute(PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) throws CommandException {
        checkTrue(args.length == 1, C.COMMAND_SYNTAX, getUsage());
        PlotArea area = PS.get().getPlotAreaByString(args[0]);
        check(area, C.COMMAND_SYNTAX, getUsage());

        FaweQueue defaultQueue = SetQueue.IMP.getNewQueue(area.worldname, true, false);
        MCAQueue queueFrom = new MCAQueue(area.worldname, defaultQueue.getSaveFolder(), defaultQueue.hasSky());

        File folder = null; // TODO

        int minRoad = 5;
        PlotId nextId = new PlotId(0, 0);
        for (Plot plot : area.getPlots()) {
            Location bot = plot.getBottomAbs();
            Location top = plot.getTopAbs();

            int pLen = Math.max(top.getX() - bot.getX(), 512 - minRoad);
            int roadWidth = pLen - 512;
            int roadPosLower;
            if ((roadWidth & 1) == 0) {
                roadPosLower = (short) (Math.floor(roadWidth / 2) - 1);
            } else {
                roadPosLower = (short) Math.floor(roadWidth / 2);
            }
            int roadPosUpper = 512 - roadWidth + roadPosLower;
            MCAWriter writer = new MCAWriter(512, 512, folder) {

                @Override
                public boolean shouldWrite(int chunkX, int chunkZ) {
                    return true;
                }

                @Override
                public MCAChunk write(MCAChunk input, int bx, int tx, int bz, int tz) {
//                    int obx = bx - oX;
//                    int obz = bz - oZ;
//                    int otx = tx - oX;
//                    int otz = tz - oZ;
//                    int otherBCX = (obx) >> 4;
//                    int otherBCZ = (obz) >> 4;
//                    int otherTCX = (otx) >> 4;
//                    int otherTCZ = (otz) >> 4;
                    return input;
                }
            };
            writer.setMCAOffset(nextId.x, nextId.y);
            nextId = nextId.getNextId(1);
        }

    }
}
