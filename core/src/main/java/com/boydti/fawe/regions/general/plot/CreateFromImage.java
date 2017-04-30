package com.boydti.fawe.regions.general.plot;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.jnbt.anvil.HeightMapMCAGenerator;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.CleanTextureUtil;
import com.boydti.fawe.util.FilteredTextureUtil;
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
import com.sk89q.worldedit.blocks.BaseBlock;
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
import java.util.Set;
import javax.imageio.ImageIO;

@CommandDeclaration(
        command = "cfi",
        permission = "plots.createfromimage",
        aliases = {"createfromheightmap", "createfromimage", "cfhm"},
        category = CommandCategory.APPEARANCE,
        requiredType = RequiredType.NONE,
        description = "Generate a world from an image heightmap: [More info](https://www.youtube.com/watch?v=cJZk1GTig7A)",
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
        if (manager instanceof SinglePlotAreaManager) TaskManager.IMP.async(new Runnable() {
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
                            image = com.boydti.fawe.util.MainUtil.toRGB(ImageIO.read(url));
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
                            Plot plot = TaskManager.IMP.sync(new RunnableVal<Plot>() {
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


                            // setGlassColor|setBiomeColor|setBlockAndBiomeColor|setColorPaletteComplexity|setColorPaletteRandomization|setColorPaletteBlocks|done|cancel|>");
                            fp.sendMessage(BBC.getPrefix() + "Initializing components... (2)");
                            fp.sendMessage(BBC.getPrefix() + "/2 cfi biome [url|mask] <biome> [white=false]");
                            fp.sendMessage(BBC.getPrefix() + "/2 cfi overlay [url|mask] <pattern> [white=false]");
                            fp.sendMessage(BBC.getPrefix() + "/2 cfi main [url|mask] <pattern> [white=false]");
                            fp.sendMessage(BBC.getPrefix() + "/2 cfi floor [url|mask] <pattern> [white=false]");
                            fp.sendMessage(BBC.getPrefix() + "/2 cfi column [url|mask] <pattern> [white=false]");
                            fp.sendMessage(BBC.getPrefix() + "/2 cfi caves");
                            fp.sendMessage(BBC.getPrefix() + "/2 cfi ore[s]");
                            fp.sendMessage(BBC.getPrefix() + "/2 cfi schem <mask> <schem> <rarity> <rotate>");
                            fp.sendMessage(BBC.getPrefix() + "/2 cfi height <image-url|height>");
                            fp.sendMessage(BBC.getPrefix() + "/2 cfi waterheight <height>");
                            fp.sendMessage(BBC.getPrefix() + "/2 cfi waterid <number-id>");
                            fp.sendMessage(BBC.getPrefix() + "/2 cfi color <image-url>");
                            fp.sendMessage(BBC.getPrefix() + "/2 cfi glass <image-url>");
                            fp.sendMessage(BBC.getPrefix() + "/2 cfi biomeColor <image-url>");
                            fp.sendMessage(BBC.getPrefix() + "/2 cfi blockBiomeColor <image-url>");
                            fp.sendMessage(BBC.getPrefix() + "/2 cfi paletteComplexity <min=0> <max=100>");
                            fp.sendMessage(BBC.getPrefix() + "/2 cfi paletteRandomization <true|false>");
                            fp.sendMessage(BBC.getPrefix() + "/2 cfi paletteBlocks <block-list>");
                            fp.sendMessage(BBC.getPrefix() + "/2 cfi paletteBiomePriority <percent=50>");
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
                        ParserContext context = new ParserContext();
                        context.setActor(fp.getPlayer());
                        context.setWorld(fp.getWorld());
                        context.setSession(fp.getSession());
                        context.setExtent(generator);
                        Request.request().setExtent(generator);
                        try {
                            switch (argList.get(0).toLowerCase()) {
                                case "schem":
                                case "schems":
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
                                case "palettecomplexity":
                                case "colorpalettecomplexity":
                                case "setcolorpalettecomplexity": {
                                    // roughness
                                    // blocks
                                    if (argList.size() != 3) {
                                        C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " <min-percent> <max-percent>");
                                        return;
                                    }
                                    int min = Integer.parseInt(argList.get(1));
                                    int max = Integer.parseInt(argList.get(2));
                                    generator.setTextureUtil(new CleanTextureUtil(Fawe.get().getTextureUtil(), min, max));
                                    player.sendMessage("Set color palette complexity, what's next?");
                                    return;
                                }
                                case "paletterandomization":
                                case "colorpaletterandomization":
                                case "setcolorpaletterandomization": {
                                    // roughness
                                    // blocks
                                    if (argList.size() != 2) {
                                        C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " <true|false>");
                                        return;
                                    }
                                    generator.setTextureRandomVariation(Boolean.parseBoolean(argList.get(1)));
                                    player.sendMessage("Set color palette randomization, what's next?");
                                    return;
                                }
                                case "paletteblocks":
                                case "colorpaletterblocks":
                                case "setcolorpaletteblocks": {
                                    // roughness
                                    // blocks
                                    if (argList.size() != 2) {
                                        C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " <pattern>");
                                        return;
                                    }
                                    context.setPreferringWildcard(true);
                                    Set<BaseBlock> blocks = we.getBlockFactory().parseFromListInput(argList.get(1), context);
                                    generator.setTextureUtil(new FilteredTextureUtil(Fawe.get().getTextureUtil(), blocks));
                                    player.sendMessage("Set color palette blocks, what's next?");
                                    return;
                                }
                                case "biomepriority":
                                case "palettebiomepriority":
                                case "setpalettebiomepriority": {
                                    // roughness
                                    // blocks
                                    if (argList.size() != 2) {
                                        C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " <percent=50>");
                                        return;
                                    }
                                    generator.setBiomePriority(Integer.parseInt(argList.get(1)));
                                    player.sendMessage("Set color palette biome priority, what's next?");
                                    return;
                                }
                                case "color":
                                case "setcolor": {
                                    if (argList.size() != 2) {
                                        C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " <url>");
                                        return;
                                    }
                                    BufferedImage image = getImgurImage(argList.get(1), fp);
                                    generator.setColor(image);
                                    player.sendMessage("Set color, what's next?");
                                    return;
                                }
                                case "biomecolor":
                                case "setbiomecolor": {
                                    if (argList.size() != 2) {
                                        C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " <url>");
                                        return;
                                    }
                                    BufferedImage image = getImgurImage(argList.get(1), fp);
                                    generator.setBiomeColor(image);
                                    player.sendMessage("Set color, what's next?");
                                    return;
                                }
                                case "blockbiomecolor":
                                case "setblockandbiomecolor": {
                                    if (argList.size() != 2) {
                                        C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " <url>");
                                        return;
                                    }
                                    BufferedImage image = getImgurImage(argList.get(1), fp);
                                    generator.setBlockAndBiomeColor(image);
                                    player.sendMessage("Set color, what's next?");
                                    return;
                                }
                                case "glass":
                                case "glasscolor":
                                case "setglasscolor": {
                                    if (argList.size() != 2) {
                                        C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " <url>");
                                        return;
                                    }
                                    BufferedImage image = getImgurImage(argList.get(1), fp);
                                    generator.setColorWithGlass(image);
                                    player.sendMessage("Set glass color, what's next?");
                                    return;
                                }
                                case "waterheight":
                                case "setwaterheight": {
                                    // roughness
                                    // blocks
                                    if (argList.size() != 2) {
                                        C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " <height>");
                                        return;
                                    }
                                    generator.setWaterHeight(Integer.parseInt(argList.get(1)));
                                    player.sendMessage("Set water height, what's next?");
                                    return;
                                }
                                case "waterid":
                                case "setwaterid": {
                                    // roughness
                                    // blocks
                                    if (argList.size() != 2) {
                                        C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " <id>");
                                        return;
                                    }
                                    generator.setWaterId(Integer.parseInt(argList.get(1)));
                                    player.sendMessage("Set water id, what's next?");
                                    return;
                                }
                                case "height":
                                case "setheight": {
                                    if (argList.size() != 2) {
                                        C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " <image-url|height>");
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
                                case "ores":
                                case "addores":
                                    if (argList.size() != 2) {
                                        C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " <mask>");
                                        return;
                                    }
                                    generator.addDefaultOres(we.getMaskFactory().parseFromInput(argList.get(1), context));
                                    player.sendMessage(BBC.getPrefix() + "Added ores, what's next?");
                                    return;
                                case "ore":
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
                                case "cave":
                                case "caves":
                                case "addcaves": {
                                    generator.addCaves();
                                    player.sendMessage(BBC.getPrefix() + "Added caves, what's next?");
                                    return;
                                }
                                case "biome":
                                case "setbiome": {
                                    int id;
                                    if (argList.size() < 2) {
                                        C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " [url|mask] <biome-id> [whiteonly]");
                                        return;
                                    }
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
                                case "overlay":
                                case "setoverlay": {
                                    Pattern id;
                                    if (argList.size() < 2) {
                                        C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " [url|mask] <pattern> [whiteonly]");
                                        return;
                                    }
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
                                case "main":
                                case "setmain": {
                                    Pattern id;
                                    if (argList.size() < 2) {
                                        C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " [url|mask] <pattern> [whiteonly]");
                                        return;
                                    }
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
                                case "floor":
                                case "setfloor": {
                                    Pattern id;
                                    if (argList.size() < 2) {
                                        C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " [url|mask] <pattern> [whiteonly]");
                                        return;
                                    }
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
                                case "column":
                                case "setcolumn": {
                                    Pattern id;
                                    if (argList.size() < 2) {
                                        C.COMMAND_SYNTAX.send(player, "/2 cfi " + argList.get(0) + " [url|mask] <pattern> [whiteonly]");
                                        return;
                                    }
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
                                case "create":
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
                                case "exit":
                                case "cancel":
                                    player.deleteMeta("HMGenerator");
                                    player.deleteMeta("HMGeneratorPlot");
                                    player.sendMessage(BBC.getPrefix() + "Cancelled!");
                                    return;
                                default:
                                    C.COMMAND_SYNTAX.send(player, "/2 cfi <setBiome|setOverlay|setMain|setFloor|setColumn|addCaves|addOre[s]|addSchems|setHeight|setColor|setGlassColor|setBiomeColor|setBlockAndBiomeColor|setColorPaletteComplexity|setColorPaletteRandomization|setColorPaletteBlocks|biomepriority|done|cancel|>");
                                    return;
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
        else {
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
            return com.boydti.fawe.util.MainUtil.toRGB(ImageIO.read(url));
        }
        return null;
    }
}