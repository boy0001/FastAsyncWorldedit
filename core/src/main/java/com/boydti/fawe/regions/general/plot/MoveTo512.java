package com.boydti.fawe.regions.general.plot;

import com.boydti.fawe.example.NullFaweChunk;
import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAQueue;
import com.boydti.fawe.jnbt.anvil.MCAWriter;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.SetQueue;
import com.intellectualcrafters.configuration.ConfigurationSection;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.commands.CommandCategory;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.commands.RequiredType;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.generator.HybridPlotWorld;
import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RunnableVal2;
import com.intellectualcrafters.plot.object.RunnableVal3;
import com.intellectualcrafters.plot.object.SetupObject;
import com.intellectualcrafters.plot.util.SetupUtils;
import com.plotsquared.general.commands.Command;
import com.plotsquared.general.commands.CommandDeclaration;
import java.io.File;
import java.io.IOException;

@CommandDeclaration(
        command = "moveto512",
        permission = "plots.moveto512",
        category = CommandCategory.DEBUG,
        requiredType = RequiredType.CONSOLE,
        description = "Move plots to a 512 sized region",
        usage = "/plots moveto512 [world]"
)
public class MoveTo512 extends Command {
    public MoveTo512() {
        super(MainCommand.getInstance(), true);
    }

    public static void main(String[] args) {

    }

    @Override
    public void execute(PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) throws CommandException {
        checkTrue(args.length == 1, C.COMMAND_SYNTAX, getUsage());
        PlotArea area = player.getPlotAreaAbs();
        check(area, C.COMMAND_SYNTAX, getUsage());
        checkTrue(area instanceof HybridPlotWorld, C.NOT_VALID_HYBRID_PLOT_WORLD);

        FaweQueue defaultQueue = SetQueue.IMP.getNewQueue(area.worldname, true, false);
        MCAQueue queueFrom = new MCAQueue(area.worldname, defaultQueue.getSaveFolder(), defaultQueue.hasSky());

        String world = args[0];
        File folder = new File(PS.imp().getWorldContainer(), world);
        checkTrue(!folder.exists(), C.SETUP_WORLD_TAKEN, world);

        HybridPlotWorld hpw = (HybridPlotWorld) area;
        int minRoad = 5;
        int pLen = Math.max(hpw.PLOT_WIDTH, 512 - minRoad);
        int roadWidth = pLen - 512;
        int roadPosLower;
        if ((roadWidth & 1) == 0) {
            roadPosLower = (short) (Math.floor(roadWidth / 2) - 1);
        } else {
            roadPosLower = (short) Math.floor(roadWidth / 2);
        }
        int roadPosUpper = 512 - roadWidth + roadPosLower;

        PlotId nextId = new PlotId(0, 0);
        for (Plot plot : area.getPlots()) {
            Location bot = plot.getBottomAbs();
            Location top = plot.getTopAbs();

            int oX = roadPosLower - bot.getX();
            int oZ = roadPosLower - bot.getZ();

            MCAWriter writer = new MCAWriter(512, 512, folder) {

                @Override
                public boolean shouldWrite(int chunkX, int chunkZ) {
                    return true;
                }

                @Override
                public MCAChunk write(MCAChunk newChunk, int bx, int tx, int bz, int tz) {
                    if (tx < roadPosLower || tz < roadPosLower || bx > roadPosUpper || bz > roadPosUpper) {
                        newChunk.fillCuboid(0, 15, 0, hpw.ROAD_HEIGHT, 0, 15, hpw.ROAD_BLOCK.id, hpw.ROAD_BLOCK.data);
                        newChunk.fillCuboid(0, 15, 0, 0, 0, 15, 7, (byte) 0);
                    } else {
                        int obx = bx - oX;
                        int obz = bz - oZ;
                        int otx = tx - oX;
                        int otz = tz - oZ;
                        int otherBCX = (obx) >> 4;
                        int otherBCZ = (obz) >> 4;
                        int otherTCX = (otx) >> 4;
                        int otherTCZ = (otz) >> 4;
                        int cx = newChunk.getX();
                        int cz = newChunk.getZ();

                        int cbx = (cx << 4) - oX;
                        int cbz = (cz << 4) - oZ;
                        for (int otherCZ = otherBCZ; otherCZ <= otherTCZ; otherCZ++) {
                            for (int otherCX = otherBCX; otherCX <= otherTCX; otherCX++) {
                                FaweChunk chunk = queueFrom.getFaweChunk(otherCX, otherCZ);
                                if (!(chunk instanceof NullFaweChunk)) {
                                    MCAChunk other = (MCAChunk) chunk;
                                    int ocbx = otherCX << 4;
                                    int ocbz = otherCZ << 4;
                                    int octx = ocbx + 15;
                                    int octz = ocbz + 15;
                                    int offsetY = 0;
                                    int minX = obx > ocbx ? (obx - ocbx) & 15 : 0;
                                    int maxX = otx < octx ? (otx - ocbx) : 15;
                                    int minZ = obz > ocbz ? (obz - ocbz) & 15 : 0;
                                    int maxZ = otz < octz ? (otz - ocbz) : 15;
                                    int offsetX = ocbx - cbx;
                                    int offsetZ = ocbz - cbz;
                                    newChunk.copyFrom(other, minX, maxX, 0, 255, minZ, maxZ, offsetX, offsetY, offsetZ);
                                }
                            }
                        }
                        if (bx < roadPosLower || bz < roadPosLower || tx > roadPosUpper || tz > roadPosUpper) {
                            boolean[] gx = new boolean[16];
                            boolean[] wx = new boolean[16];
                            boolean[] gz = new boolean[16];
                            boolean[] wz = new boolean[16];
                            for (short i = 0; i < 16; i++) {
                                int vx = bx + i;
                                int vz = bz + i;
                                gz[i] = vz < roadPosLower || vz > roadPosUpper;
                                wz[i] = vz == roadPosLower || vz == roadPosUpper;
                                gx[i] = vx < roadPosLower || vx > roadPosUpper;
                                wx[i] = vx == roadPosLower || vx == roadPosUpper;
                            }
                            for (int z = 0; z < 16; z++) {
                                for (int x = 0; x < 16; x++) {
                                    if (gx[x] || gz[z]) {
                                        for (int y = 1; y < hpw.ROAD_HEIGHT; y++) {
                                            newChunk.setBlock(x, y, z, hpw.ROAD_BLOCK.id, hpw.ROAD_BLOCK.data);
                                        }
                                    } else if (wx[x] || wz[z]) {
                                        for (int y = 1; y < hpw.WALL_HEIGHT; y++) {
                                            newChunk.setBlock(x, y, z, hpw.WALL_FILLING.id, hpw.WALL_FILLING.data);
                                        }
                                        newChunk.setBlock(x, hpw.WALL_HEIGHT, z, hpw.CLAIMED_WALL_BLOCK.id, hpw.CLAIMED_WALL_BLOCK.data);
                                    }
                                }
                            }
                            newChunk.fillCuboid(0, 15, 0, 0, 0, 15, 7, (byte) 0);
                        }
                    }

                    return newChunk;
                }
            };
            writer.setMCAOffset(nextId.x, nextId.y);
            try {
                writer.generate();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            queueFrom.clear();
            nextId = nextId.getNextId(1);
        }
        ConfigurationSection section = PS.get().worlds.getConfigurationSection("worlds." + world);
        area.saveConfiguration(section);
        section.set("plot.size", pLen);
        section.set("road.width", roadWidth);
        try {
            PS.get().worlds.save(PS.get().worldsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final SetupObject object = new SetupObject();
        object.world = world;
        object.plotManager = PS.imp().getPluginName();
        object.setupGenerator = PS.imp().getPluginName();
        String created = SetupUtils.manager.setupWorld(object);
    }
}
