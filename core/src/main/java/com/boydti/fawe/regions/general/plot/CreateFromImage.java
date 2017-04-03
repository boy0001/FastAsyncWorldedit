package com.boydti.fawe.regions.general.plot;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.jnbt.anvil.HeightMapMCAGenerator;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.util.TaskManager;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.commands.Auto;
import com.intellectualcrafters.plot.commands.CommandCategory;
import com.intellectualcrafters.plot.commands.MainCommand;
import com.intellectualcrafters.plot.commands.RequiredType;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.config.Settings;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.RunnableVal2;
import com.intellectualcrafters.plot.object.RunnableVal3;
import com.intellectualcrafters.plot.object.worlds.PlotAreaManager;
import com.intellectualcrafters.plot.object.worlds.SinglePlotArea;
import com.intellectualcrafters.plot.object.worlds.SinglePlotAreaManager;
import com.intellectualcrafters.plot.util.MainUtil;
import com.plotsquared.general.commands.Command;
import com.plotsquared.general.commands.CommandDeclaration;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.biome.Biomes;
import com.sk89q.worldedit.world.registry.BiomeRegistry;
import com.sk89q.worldedit.world.registry.WorldData;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import javax.imageio.ImageIO;

@CommandDeclaration(
        command = "cfi",
        permission = "plots.createfromimage",
        aliases = {"createfromheightmap", "createfromimage", "cfhm"},
        category = CommandCategory.APPEARANCE,
        requiredType = RequiredType.NONE,
        description = "Generate a world from an image",
        usage = "/plots cfi [url or dimensions]"
)
public class CreateFromImage extends Command {
    private final WorldEdit we;

    public CreateFromImage() {
        super(MainCommand.getInstance(), true);
        this.we = WorldEdit.getInstance();
    }

    @Override
    public void execute(final PlotPlayer player, String[] args, RunnableVal3<Command, Runnable, Runnable> confirm, RunnableVal2<Command, CommandResult> whenDone) throws CommandException {
        List<String> argList = StringMan.split(StringMan.join(args, " "), ' ');
        checkTrue(argList.size() >= 1, C.COMMAND_SYNTAX, getUsage());
        PlotAreaManager manager = PS.get().getPlotAreaManager();
        if (manager instanceof SinglePlotAreaManager) {
            TaskManager.IMP.async(new Runnable() {
                @Override
                public void run() {
                    FawePlayer<Object> fp = FawePlayer.wrap(player.getName());
                    HeightMapMCAGenerator generator = player.getMeta("HMGenerator");
                    Plot plot = player.getMeta("HMGeneratorPlot");
                    if (generator == null) {
                        final Vector2D dimensions;
                        final BufferedImage image;
                        if (argList.get(0).toLowerCase().startsWith("http")) {
                            try {
                                URL url = new URL(argList.get(0));
                                if (!url.getHost().equals("i.imgur.com")) {
                                    player.sendMessage("Images can only be loaded from i.imgur.com");
                                    return;
                                }
                                player.sendMessage(BBC.getPrefix() + "Loading image... (1)");
                                image = ImageIO.read(url);
                            } catch (IOException e) {
                                player.sendMessage(e.getMessage());
                                return;
                            }
                            dimensions = null;
                        } else {
                            image = null;
                            if (argList.size() != 2) {
                                C.COMMAND_SYNTAX.send(player, getUsage());
                                return;
                            }
                            dimensions = new Vector2D(Integer.parseInt(argList.get(0)), Integer.parseInt(argList.get(1)));
                        }
                        fp.runAction(new Runnable() {
                            @Override
                            public void run() {
                                SinglePlotAreaManager sManager = (SinglePlotAreaManager) manager;
                                SinglePlotArea area = sManager.getArea();
                                Plot plot = TaskManager.IMP.sync(new com.boydti.fawe.object.RunnableVal<Plot>() {
                                    @Override
                                    public void run(Plot o) {
                                        int currentPlots = Settings.Limit.GLOBAL ? player.getPlotCount() : player.getPlotCount(area.worldname);
                                        int diff = player.getAllowedPlots() - currentPlots;
                                        if (diff < 1) {
                                            MainUtil.sendMessage(player, C.CANT_CLAIM_MORE_PLOTS_NUM, -diff + "");
                                            return;
                                        }
                                        if (area.getMeta("lastPlot") == null) {
                                            area.setMeta("lastPlot", new PlotId(0, 0));
                                        }
                                        PlotId lastId = (PlotId) area.getMeta("lastPlot");
                                        while (true) {
                                            lastId = Auto.getNextPlotId(lastId, 1);
                                            if (area.canClaim(player, lastId, lastId)) {
                                                break;
                                            }
                                        }
                                        area.setMeta("lastPlot", lastId);
                                        this.value = area.getPlot(lastId);
                                        this.value.setOwner(player.getUUID());
                                    }
                                });
                                fp.sendMessage(BBC.getPrefix() + "Initializing components... (2)");
                                fp.sendMessage(BBC.getPrefix() + "/2 cfi setbiome");
                                fp.sendMessage(BBC.getPrefix() + "/2 cfi setoverlay");
                                fp.sendMessage(BBC.getPrefix() + "/2 cfi setmain");
                                fp.sendMessage(BBC.getPrefix() + "/2 cfi setfloor");
                                fp.sendMessage(BBC.getPrefix() + "/2 cfi setcolumn");
                                fp.sendMessage(BBC.getPrefix() + "/2 cfi addcaves");
                                fp.sendMessage(BBC.getPrefix() + "/2 cfi addore[s]");
                                fp.sendMessage(BBC.getPrefix() + "/2 cfi addschems");
                                fp.sendMessage(BBC.getPrefix() + "/2 cfi setheight");
                                fp.sendMessage(BBC.getPrefix() + "/2 cfi done");
                                fp.sendMessage(BBC.getPrefix() + "/2 cfi cancel");
                                File folder = new File(PS.imp().getWorldContainer(), plot.getWorldName() + File.separator + "region");
                                HeightMapMCAGenerator generator;
                                if (image != null) {
                                    generator = new HeightMapMCAGenerator(image, folder);
                                } else {
                                    generator = new HeightMapMCAGenerator(dimensions.getBlockX(), dimensions.getBlockZ(), folder);
                                }
                                player.setMeta("HMGenerator", generator);
                                player.setMeta("HMGeneratorPlot", plot);
                            }
                        }, true, false);
                        return;
                    }
                    fp.runAction(new Runnable() {
                        @Override
                        public void run() {
                            if (generator == null) {
                                C.COMMAND_SYNTAX.send(player, getUsage());
                                return;
                            }
                            if (argList.size() == 1) {
                                if (StringMan.isEqualIgnoreCaseToAny(argList.get(0), "setbiome", "setoverlay", "setmain", "setfloor", "setcolumn")) {
                                    C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " <image or mask> <value> [white-only]");
                                    C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " <value>");
                                    return;
                                } else if (!StringMan.isEqualIgnoreCaseToAny(argList.get(0), "done", "cancel", "addcaves", "addore", "addores", "addschems", "setheight")) {
                                    C.COMMAND_SYNTAX.send(player, "/2 cfi <setbiome|setoverlay|setmain|setfloor|setcolumn|done|cancel|addcaves|addore[s]|addschems|setheight>");
                                    return;
                                }
                            }
                            ParserContext context = new ParserContext();
                            context.setActor(fp.getPlayer());
                            context.setWorld(fp.getWorld());
                            context.setSession(fp.getSession());
                            context.setExtent(generator);
                            Request.request().setExtent(generator);
                            try {
                                switch (argList.get(0).toLowerCase()) {
                                    case "addschems": {
                                        if (argList.size() != 5) {
                                            C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " <mask> <file|folder|url> <rarity> <rotate>");
                                            return;
                                        }
                                        World world = fp.getWorld();
                                        WorldData wd = world.getWorldData();
                                        Mask mask = we.getMaskFactory().parseFromInput(argList.get(1), context);
                                        ClipboardHolder[] clipboards = ClipboardFormat.SCHEMATIC.loadAllFromInput(fp.getPlayer(), wd, argList.get(2), true);
                                        if (clipboards == null) {
                                            return;
                                        }
                                        int rarity = Integer.parseInt(argList.get(3));
                                        boolean rotate = Boolean.parseBoolean(argList.get(4));
                                        generator.addSchems(mask, wd, clipboards, rarity, rotate);
                                        player.sendMessage(BBC.getPrefix() + "Added schems, what's next?");
                                        return;
                                    }
                                    case "setheight": {
                                        if (argList.size() != 2) {
                                            C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " <height>");
                                            return;
                                        }
                                        if (argList.get(1).startsWith("http")) {
                                            player.sendMessage("Loading image (3)...");
                                            BufferedImage image = getImgurImage(argList.get(1), fp);
                                            generator.setHeight(image);
                                        } else {
                                            generator.setHeights(Integer.parseInt(args[1]));
                                        }
                                        player.sendMessage("Set height, what's next?");
                                        return;
                                    }
                                    case "addores":
                                        if (argList.size() != 2) {
                                            C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " <mask>");
                                            return;
                                        }
                                        generator.addDefaultOres(we.getMaskFactory().parseFromInput(argList.get(1), context));
                                        player.sendMessage(BBC.getPrefix() + "Added ores, what's next?");
                                        return;
                                    case "addore": {
                                        if (argList.size() != 8) {
                                            C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " <mask> <pattern> <size> <frequency> <rarity> <min-Y> <max-Y>");
                                            return;
                                        }
                                        // mask pattern size freq rarity miny maxy
                                        Mask mask = we.getMaskFactory().parseFromInput(argList.get(1), context);
                                        Pattern pattern = we.getPatternFactory().parseFromInput(argList.get(2), context);
                                        int size = Integer.parseInt(argList.get(3));
                                        int frequency = Integer.parseInt(argList.get(4));
                                        int rarity = Integer.parseInt(argList.get(5));
                                        int min = Integer.parseInt(argList.get(6));
                                        int max = Integer.parseInt(argList.get(7));
                                        generator.addOre(mask, pattern, size, frequency, rarity, min, max);
                                        player.sendMessage(BBC.getPrefix() + "Added ore, what's next?");
                                        return;
                                    }
                                    case "addcaves": {
                                        generator.addCaves();
                                        player.sendMessage(BBC.getPrefix() + "Added caves, what's next?");
                                        return;
                                    }
                                    case "setbiome": {
                                        int id;
                                        if (argList.size() == 2) {
                                            id = getBiome(argList.get(1), fp).getId();
                                            generator.setBiome(id);
                                        } else {
                                            id = getBiome(argList.get(2), fp).getId();
                                            BufferedImage img = getImgurImage(argList.get(1), fp);
                                            if (img != null) {
                                                boolean whiteOnly = argList.size() == 4 && Boolean.parseBoolean(argList.get(3));
                                                generator.setBiome(img, (byte) id, whiteOnly);
                                            } else {
                                                generator.setBiome(we.getMaskFactory().parseFromInput(argList.get(1), context), (byte) id);
                                            }
                                        }
                                        player.sendMessage(BBC.getPrefix() + "Set biome, what's next?");
                                        return;
                                    }
                                    case "setoverlay": {
                                        Pattern id;
                                        if (argList.size() == 2) {
                                            id = we.getPatternFactory().parseFromInput(argList.get(1), context);
                                            generator.setOverlay(id);
                                        } else {
                                            id = we.getPatternFactory().parseFromInput(argList.get(2), context);
                                            BufferedImage img = getImgurImage(argList.get(1), fp);
                                            if (img != null) {
                                                boolean whiteOnly = argList.size() == 4 && Boolean.parseBoolean(argList.get(3));
                                                generator.setOverlay(img, id, whiteOnly);
                                            } else {
                                                generator.setOverlay(we.getMaskFactory().parseFromInput(argList.get(1), context), id);
                                            }
                                        }
                                        player.sendMessage(BBC.getPrefix() + "Set overlay, what's next?");
                                        return;
                                    }
                                    case "setmain": {
                                        Pattern id;
                                        if (argList.size() == 2) {
                                            id = we.getPatternFactory().parseFromInput(argList.get(1), context);
                                            generator.setMain(id);
                                        } else {
                                            id = we.getPatternFactory().parseFromInput(argList.get(2), context);
                                            BufferedImage img = getImgurImage(argList.get(1), fp);
                                            if (img != null) {
                                                boolean whiteOnly = argList.size() == 4 && Boolean.parseBoolean(argList.get(3));
                                                generator.setMain(img, id, whiteOnly);
                                            } else {
                                                generator.setMain(we.getMaskFactory().parseFromInput(argList.get(1), context), id);
                                            }
                                        }
                                        player.sendMessage(BBC.getPrefix() + "Set main, what's next?");
                                        return;
                                    }
                                    case "setfloor": {
                                        Pattern id;
                                        if (argList.size() == 2) {
                                            id = we.getPatternFactory().parseFromInput(argList.get(1), context);
                                            generator.setFloor(id);
                                        } else {
                                            id = we.getPatternFactory().parseFromInput(argList.get(2), context);
                                            BufferedImage img = getImgurImage(argList.get(1), fp);
                                            if (img != null) {
                                                boolean whiteOnly = argList.size() == 4 && Boolean.parseBoolean(argList.get(3));
                                                generator.setFloor(img, id, whiteOnly);
                                            } else {
                                                generator.setFloor(we.getMaskFactory().parseFromInput(argList.get(1), context), id);
                                            }
                                        }
                                        player.sendMessage(BBC.getPrefix() + "Set floor, what's next?");
                                        return;
                                    }
                                    case "setcolumn": {
                                        Pattern id;
                                        if (argList.size() == 2) {
                                            id = we.getPatternFactory().parseFromInput(argList.get(1), context);
                                            generator.setColumn(id);
                                        } else {
                                            id = we.getPatternFactory().parseFromInput(argList.get(2), context);
                                            BufferedImage img = getImgurImage(argList.get(1), fp);
                                            if (img != null) {
                                                boolean whiteOnly = argList.size() == 4 && Boolean.parseBoolean(argList.get(3));
                                                generator.setColumn(img, id, whiteOnly);
                                            } else {
                                                generator.setColumn(we.getMaskFactory().parseFromInput(argList.get(1), context), id);
                                            }
                                        }
                                        player.sendMessage(BBC.getPrefix() + "Set columns, what's next?");
                                        return;
                                    }
                                    case "done":
                                        player.deleteMeta("HMGenerator");
                                        player.deleteMeta("HMGeneratorPlot");
                                        player.sendMessage("Generating... (4)");
                                        try {
                                            generator.generate();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            player.sendMessage(e.getMessage() + " (see console)");
                                            return;
                                        }
                                        player.sendMessage("Done!");
                                        TaskManager.IMP.sync(new RunnableVal<Object>() {
                                            @Override
                                            public void run(Object value) {
                                                plot.teleportPlayer(player);
                                            }
                                        });
                                        return;
                                    case "cancel":
                                        player.deleteMeta("HMGenerator");
                                        player.deleteMeta("HMGeneratorPlot");
                                        player.sendMessage(BBC.getPrefix() + "Cancelled!");
                                        return;
                                    default:
                                        C.COMMAND_SYNTAX.send(player, getUsage());
                                }
                            } catch (IOException e) {
                                player.sendMessage("Invalid url: " + e.getMessage());
                            } catch (InputParseException e) {
                                player.sendMessage("Invalid mask " + e.getMessage());
                            } catch (Throwable e) {
                                player.sendMessage("Error " + e.getMessage());
                            } finally {
                                Request.reset();
                            }
                        }
                    }, true, false);
                }
            });
        } else {
            player.sendMessage("Must have the `worlds` component enabled in the PlotSquared config.yml");
        }
    }

    private BaseBiome getBiome(String arg, FawePlayer fp) {
        World world = fp.getWorld();
        BiomeRegistry biomeRegistry = world.getWorldData().getBiomeRegistry();
        List<BaseBiome> knownBiomes = biomeRegistry.getBiomes();
        return Biomes.findBiomeByName(knownBiomes, arg, biomeRegistry);
    }

    private BufferedImage getImgurImage(String arg, FawePlayer fp) throws IOException {
        if (arg.startsWith("http")) {
            URL url = new URL(arg);
            if (!url.getHost().equalsIgnoreCase("i.imgur.com")) {
                throw new IOException("Only i.imgur.com links are allowed!");
            }
            fp.sendMessage(BBC.getPrefix() + "Downloading image... (3)");
            return ImageIO.read(url);
        }
        return null;
    }
}