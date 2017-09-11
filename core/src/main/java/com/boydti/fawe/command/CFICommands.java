package com.boydti.fawe.command;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Commands;
import com.boydti.fawe.jnbt.anvil.HeightMapMCAGenerator;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.CleanTextureUtil;
import com.boydti.fawe.util.FilteredTextureUtil;
import com.boydti.fawe.util.ImgurUtility;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.TextureUtil;
import com.boydti.fawe.util.chat.Message;
import com.boydti.fawe.util.image.ImageUtil;
import com.intellectualcrafters.plot.PS;
import com.intellectualcrafters.plot.commands.Auto;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.config.Settings;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotPlayer;
import com.intellectualcrafters.plot.object.worlds.PlotAreaManager;
import com.intellectualcrafters.plot.object.worlds.SinglePlotArea;
import com.intellectualcrafters.plot.object.worlds.SinglePlotAreaManager;
import com.intellectualcrafters.plot.util.MathMan;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.command.MethodCommands;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.util.command.parametric.ParameterException;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import com.sk89q.worldedit.world.registry.WorldData;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;

@Command(aliases = {"/cfi"}, desc = "Create a world from images: [More Info](https://git.io/v5iDy)")
public class CFICommands extends MethodCommands {

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public CFICommands(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Command(
            aliases = {"heightmap"},
            usage = "<url>",
            desc = "Start CFI with a height map as a base"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void heightmap(FawePlayer fp, BufferedImage image) {
        HeightMapMCAGenerator generator = new HeightMapMCAGenerator(image, null);
        setup(generator, fp);
    }

    @Command(
            aliases = {"empty"},
            usage = "<width> <length>",
            desc = "Start CFI with an empty map as a base"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void heightmap(FawePlayer fp, int width, int length) {
        HeightMapMCAGenerator generator = new HeightMapMCAGenerator(width, length, null);
        setup(generator, fp);
    }

    private void setup(HeightMapMCAGenerator generator, FawePlayer fp) {
        CFISettings settings = getSettings(fp);
        settings.remove().setGenerator(generator).bind();
        generator.setImageViewer(Fawe.imp().getImageViewer(fp));
        mainMenu(fp);
    }

    @Command(
            aliases = {"brush"},
            usage = "",
            desc = "Info about using brushes with CFI"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void brush(FawePlayer fp) throws ParameterException{
        CFISettings settings = assertSettings(fp);
        Message msg;
        if (settings.getGenerator().getImageViewer() != null) {
            msg = msg("CFI supports using brushes during creation").newline()
                    .text(" - Place the map on a wall of item frames").newline()
                    .text(" - Use any WorldEdit brush on the item frames").newline()
                    .text(" - Example: ").text("Video").linkTip("https://goo.gl/PK4DMG").newline();
        } else {
            msg = msg("This is not supported with your platform/version").newline();
        }
        msg.text("&8> &7[&aNext&7]").cmdTip(alias()).send(fp);
    }

    @Command(
            aliases = {"cancel", "exit"},
            usage = "",
            desc = "Cancel creation"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void cancel(FawePlayer fp) throws ParameterException, IOException {
        getSettings(fp).remove();
        fp.sendMessage(BBC.getPrefix() + "Cancelled!");
    }

    @Command(
            aliases = {"done", "create"},
            usage = "",
            desc = "Create the world"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void done(FawePlayer fp) throws ParameterException, IOException {
        CFISettings settings = assertSettings(fp);

        PlotAreaManager manager = PS.get().getPlotAreaManager();
        if (manager instanceof SinglePlotAreaManager) {
            SinglePlotAreaManager sManager = (SinglePlotAreaManager) manager;
            SinglePlotArea area = sManager.getArea();
            PlotPlayer player = PlotPlayer.wrap(fp.parent);

            fp.sendMessage(BBC.getPrefix() + "Claiming world");
            Plot plot = TaskManager.IMP.sync(new RunnableVal<Plot>() {
                @Override
                public void run(Plot o) {
                    int currentPlots = Settings.Limit.GLOBAL ? player.getPlotCount() : player.getPlotCount(area.worldname);
                    int diff = player.getAllowedPlots() - currentPlots;
                    if (diff < 1) {
                        C.CANT_CLAIM_MORE_PLOTS_NUM.send(player, -diff);
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
            if (plot == null) return;

            File folder = new File(PS.imp().getWorldContainer(), plot.getWorldName() + File.separator + "region");
            HeightMapMCAGenerator generator = settings.getGenerator();
            generator.setFolder(folder);

            fp.sendMessage(BBC.getPrefix() + "Generating");
            generator.generate();
            settings.remove();
            fp.sendMessage(BBC.getPrefix() + "Done!");
            TaskManager.IMP.sync(new RunnableVal<Object>() {
                @Override
                public void run(Object value) {
                    plot.teleportPlayer(player);
                }
            });
        } else {
            fp.sendMessage(BBC.getPrefix() + "Must have the `worlds` component enabled in the PlotSquared config.yml");
        }
    }

    @Command(
            aliases = {"column", "setcolumn"},
            usage = "<pattern> [url|mask]",
            desc = "Set the floor and main block"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void column(FawePlayer fp, Pattern pattern, @Optional BufferedImage image, @Optional Mask mask, @Switch('w') boolean disableWhiteOnly) throws ParameterException{
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        if (image != null) gen.setColumn(image, pattern, !disableWhiteOnly);
        else if (mask != null) gen.setColumn(mask, pattern);
        else gen.setColumn(pattern);
        fp.sendMessage("Set column!");
    }

    @Command(
            aliases = {"floor", "setfloor"},
            usage = "<pattern> [url|mask]",
            desc = "Set the floor (default: grass)"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void floor(FawePlayer fp, Pattern pattern, @Optional BufferedImage image, @Optional Mask mask, @Switch('w') boolean disableWhiteOnly) throws ParameterException{
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        if (image != null) gen.setFloor(image, pattern, !disableWhiteOnly);
        else if (mask != null) gen.setFloor(mask, pattern);
        else gen.setFloor(pattern);
        fp.sendMessage("Set floor!");
    }

    @Command(
            aliases = {"main", "setmain"},
            usage = "<pattern> [url|mask]",
            desc = "Set the main block (default: stone)"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void main(FawePlayer fp, Pattern pattern, @Optional BufferedImage image, @Optional Mask mask, @Switch('w') boolean disableWhiteOnly) throws ParameterException{
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        if (image != null) gen.setMain(image, pattern, !disableWhiteOnly);
        else if (mask != null) gen.setMain(mask, pattern);
        else gen.setMain(pattern);
        fp.sendMessage("Set main!");
    }

    @Command(
            aliases = {"overlay", "setoverlay"},
            usage = "<pattern> [url|mask]",
            desc = "Set the overlay block",
            help = "Change the block directly above the floor (default: air)\n" +
                    "e.g. Tallgrass"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void overlay(FawePlayer fp, Pattern pattern, @Optional BufferedImage image, @Optional Mask mask, @Switch('w') boolean disableWhiteOnly) throws ParameterException{
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        if (image != null) gen.setOverlay(image, pattern, !disableWhiteOnly);
        else if (mask != null) gen.setOverlay(mask, pattern);
        else gen.setOverlay(pattern);
        fp.sendMessage("Set overlay!");
    }

    @Command(
            aliases = {"smooth"},
            usage = "<radius> <iterations> [image|mask]",
            desc = "Smooth the terrain",
            help = "Smooth terrain within an image-mask, or worldedit mask\n" +
                    " - You can use !0 as the mask to smooth everything\n" +
                    " - This supports smoothing snow layers (set the floor to 78:7)\n" +
                    " - A good value for radius and iterations would be 1 8."
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void smooth(FawePlayer fp, int radius, int iterations, @Optional BufferedImage image, @Optional Mask mask, @Switch('w') boolean disableWhiteOnly) throws ParameterException{
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        if (image != null) gen.smooth(image, !disableWhiteOnly, radius, iterations);
        else gen.smooth(mask, radius, iterations);
        fp.sendMessage("Performed smooth!");
    }

    @Command(
            aliases = {"snow"},
            usage = "[image|mask]",
            desc = "Create some snow"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void snow(FawePlayer fp, @Optional BufferedImage image, @Optional Mask mask, @Switch('w') boolean disableWhiteOnly) throws ParameterException{
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        floor(fp, FaweCache.getBlock(78, 7), image, mask, disableWhiteOnly);
        smooth(fp, 1, 8, image, mask, disableWhiteOnly);
        msg("Added snow!").newline().text("&8> &7[&aNext&7]").cmdTip(alias()).send(fp);
    }

    @Command(
            aliases = {"biomepriority", "palettebiomepriority", "setpalettebiomepriority"},
            usage = "[percent=50]",
            desc = "Set the biome priority",
            help = "Increase or decrease biome priority when using blockBiomeColor.\n" +
                    "A value of 50 is the default\n" +
                    "Above 50 will prefer to color with biomes\n" +
                    "Below 50 will prefer to color with blocks"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void biomepriority(FawePlayer fp, int value) throws ParameterException{
        assertSettings(fp).getGenerator().setBiomePriority(value);
        coloring(fp);
    }

    @Command(
            aliases = {"paletteblocks", "colorpaletterblocks", "setcolorpaletteblocks"},
            usage = "<blocks|#clipboard|*>",
            desc = "Set the blocks used for coloring",
            help = "Allow only specific blocks to be used for coloring\n" +
                    "`blocks` is a list of blocks e.g. stone,bedrock,wool\n" +
                    "`#clipboard` will only use the blocks present in your clipboard."
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void paletteblocks(FawePlayer fp, @Optional String arg) throws ParameterException, EmptyClipboardException, InputParseException, FileNotFoundException {
        if (arg == null) {
            msg("What blocks do you want to color with?").newline()
            .text("&7[&aAll&7]").cmdTip(alias() + " PaletteBlocks *").text(" - All available blocks")
            .newline()
            .text("&7[&aClipboard&7]").cmdTip(alias() + " PaletteBlocks #clipboard").text(" - The blocks in your clipboard")
            .newline()
            .text("&7[&aList&7]").suggestTip(alias() + " PaletteBlocks stone,gravel").text(" - A comma separated list of blocks")
            .newline()
            .text("&7[&aComplexity&7]").cmdTip(alias() + " Complexity").text(" - Block textures within a complexity range")
            .newline()
            .text("&8< &7[&aBack&7]").cmdTip(alias() + " " + Commands.getAlias(CFICommands.class, "coloring"))
            .send(fp);
            return;
        }
        HeightMapMCAGenerator generator = assertSettings(fp).getGenerator();
        ParserContext context = new ParserContext();
        context.setActor(fp.getPlayer());
        context.setWorld(fp.getWorld());
        context.setSession(fp.getSession());
        context.setExtent(generator);
        Request.request().setExtent(generator);

        Set<BaseBlock> blocks;
        switch (arg.toLowerCase()) {
            case "*": {
                generator.setTextureUtil(Fawe.get().getTextureUtil());
                return;
            }
            case "#clipboard": {
                ClipboardHolder holder = fp.getSession().getClipboard();
                Clipboard clipboard = holder.getClipboard();
                boolean[] ids = new boolean[Character.MAX_VALUE + 1];
                for (Vector pt : clipboard.getRegion()) {
                    ids[clipboard.getBlock(pt).getCombined()] = true;
                }
                blocks = new HashSet<>();
                for (int combined = 0; combined < ids.length; combined++) {
                    if (ids[combined]) blocks.add(FaweCache.CACHE_BLOCK[combined]);
                }
                break;
            }
            default: {
                blocks = worldEdit.getBlockFactory().parseFromListInput(arg, context);
                break;
            }
        }
        generator.setTextureUtil(new FilteredTextureUtil(Fawe.get().getTextureUtil(), blocks));
        coloring(fp);
    }

    @Command(
            aliases = {"randomization", "paletterandomization"},
            usage = "<true|false>",
            desc = "Set whether randomization is enabled",
            help = "This is enabled by default, randomization will add some random variation in the blocks used to closer match the provided image.\n" +
                    "If disabled, the closest block to the color will always be used.\n" +
                    "Randomization will allow mixing biomes when coloring with biomes"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void randomization(FawePlayer fp, boolean enabled) throws ParameterException {
        assertSettings(fp).getGenerator().setTextureRandomVariation(enabled);
        coloring(fp);
    }

    @Command(
            aliases = {"complexity", "palettecomplexity"},
            usage = "<minPercent> <maxPercent>",
            desc = "Set the complexity for coloring",
            help = "Set the complexity for coloring\n" +
                    "Filter out blocks to use based on their complexity, which is a measurement of how much color variation there is in the texture for that block.\n" +
                    "Glazed terracotta is complex, and not very pleasant for terrain, whereas stone and wool are simpler textures.\n" +
                    "Using 0 73 for the min/max would use the simplest 73% of blocks for coloring, and is a reasonable value."
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void complexity(FawePlayer fp, int min, int max) throws ParameterException, FileNotFoundException {
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        if (min == 0 && max == 100) gen.setTextureUtil(Fawe.get().getTextureUtil());
        else gen.setTextureUtil(new CleanTextureUtil(Fawe.get().getTextureUtil(), min, max));
        coloring(fp);
    }

    @Command(
            aliases = {"schem", "schematic", "schems", "schematics", "addschems"},
            usage = "[url] <mask> <file|folder|url> <rarity> <distance> <rotate=true>",
            desc = "Populate schematics",
            help = "Populate a schematic on the terrain\n" +
                    " - Change the mask (e.g. angle mask) to only place the schematic in specific locations.\n" +
                    " - The rarity is a value between 0 and 100.\n" +
                    " - The distance is the spacing between each schematic"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void schem(FawePlayer fp, @Optional BufferedImage imageMask, Mask mask, String schematic, int rarity, int distance, boolean rotate) throws ParameterException, IOException, WorldEditException {
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();

        World world = fp.getWorld();
        WorldData wd = world.getWorldData();
        ClipboardHolder[] clipboards = ClipboardFormat.SCHEMATIC.loadAllFromInput(fp.getPlayer(), wd, schematic, true);
        if (clipboards == null) {
            return;
        }
        if (imageMask == null) {
            gen.addSchems(mask, wd, clipboards, rarity, distance, rotate);
        } else {
            gen.addSchems(imageMask, mask, wd, clipboards, rarity, distance, rotate);
        }
        msg("Added schematics!").newline().text("&8> &7[&aNext&7]").cmdTip(alias()).send(fp);
    }

    @Command(
            aliases = {"biome", "setbiome"},
            usage = "<biome> [image|mask]",
            desc = "Set the biome",
            help = "Set the biome in specific parts of the map.\n" +
                    " - If an image is used, the biome will have a chance to be set based on how white the pixel is (white #FFF = 100% chance)" +
                    " - The whiteOnly parameter determines if only white values on the image are set" +
                    " - If a mask is used, the biome will be set anywhere the mask applies"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void biome(FawePlayer fp, BaseBiome biome, @Optional BufferedImage image, @Optional Mask mask, @Switch('w') boolean disableWhiteOnly) throws ParameterException{
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        if (image != null) gen.setBiome(image, (byte) biome.getId(), !disableWhiteOnly);
        else if (mask != null) gen.setBiome(mask, (byte) biome.getId());
        else gen.setBiome((byte) biome.getId());
        msg("Set biome!").newline().text("&8> &7[&aNext&7]").cmdTip(alias()).send(fp);
    }

    @Command(
            aliases = {"caves", "addcaves"},
            desc = "Generate vanilla caves"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void caves(FawePlayer fp) throws ParameterException, WorldEditException {
        assertSettings(fp).getGenerator().addCaves();
        msg("Added caves!").newline().text("&8> &7[&aNext&7]").cmdTip(alias()).send(fp);
    }

    @Command(
            aliases = {"ore", "addore"},
            usage = "<mask=stone> <pattern> <size> <frequency> <rarity> <minY> <maxY>",
            desc = "Add an ore",
            help = "Use a specific pattern and settings to generate ore"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void ore(FawePlayer fp, Mask mask, Pattern pattern, int size, int frequency, int rariry, int minY, int maxY) throws ParameterException, WorldEditException {
        assertSettings(fp).getGenerator().addOre(mask, pattern, size, frequency, rariry, minY, maxY);
        msg("Added ore!").newline().text("&8> &7[&aNext&7]").cmdTip(alias()).send(fp);
    }

    @Command(
            aliases = {"ores", "addores"},
            usage = "<mask=stone>",
            desc = "Generate the vanilla ores"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void ores(FawePlayer fp, Mask mask) throws ParameterException, WorldEditException {
        assertSettings(fp).getGenerator().addDefaultOres(mask);
        msg("Added ores!").newline().text("&8> &7[&aNext&7]").cmdTip(alias()).send(fp);
    }

    @Command(
            aliases = {"height", "setheight"},
            usage = "<height|image>",
            desc = "Set the height",
            help = "Set the terrain height either based on an image heightmap, or a numeric value."
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void height(FawePlayer fp, String arg) throws ParameterException, WorldEditException {
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        if (!MathMan.isInteger(arg)) {
            gen.setHeight(ImageUtil.getImage(arg));
        } else {
            gen.setHeights(Integer.parseInt(arg));
        }
        msg("Set height!").newline().text("&8> &7[&aNext&7]").cmdTip(alias()).send(fp);
    }

    @Command(
            aliases = {"water", "waterid"},
            usage = "<block>",
            desc = "Change the block used for water\n" +
                    "e.g. Lava"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void waterId(FawePlayer fp, BaseBlock block) throws ParameterException, WorldEditException {
        assertSettings(fp).getGenerator().setWaterId(block.getId());
        msg("Set water id!").newline().text("&8> &7[&aNext&7]").cmdTip(alias()).send(fp);
    }

    @Command(
            aliases = {"waterheight", "sealevel", "setwaterheight"},
            usage = "<height>",
            desc = "Set the level water is generated at\n" +
                    "Set the level water is generated at\n" +
                    " - By default water is disabled (with a value of 0)"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void height(FawePlayer fp, int height) throws ParameterException, WorldEditException {
        assertSettings(fp).getGenerator().setWaterHeight(height);
        msg("Set height!").newline().text("&8> &7[&aNext&7]").cmdTip(alias()).send(fp);
    }

    @Command(
            aliases = {"glass", "glasscolor", "setglasscolor"},
            usage = "<url>",
            desc = "Color terrain using glass"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void glass(FawePlayer fp, BufferedImage image, @Optional BufferedImage imageMask, @Optional Mask mask, @Switch('w') boolean disableWhiteOnly) throws ParameterException, WorldEditException {
        assertSettings(fp).getGenerator().setColorWithGlass(image);
        msg("Set color with glass!").newline().text("&8> &7[&aNext&7]").cmdTip(alias()).send(fp);
    }

    @Command(
            aliases = {"color", "setcolor", "blockcolor", "blocks"},
            usage = "<url> [imageMask|mask]",
            desc = "Set the color with blocks and biomes",
            help = "Color the terrain using only blocks\n" +
                    "Provide an image, or worldedit mask for the 2nd argument to restrict what areas are colored\n" +
                    "The -w (disableWhiteOnly) will randomly apply depending on the pixel luminance"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void color(FawePlayer fp, BufferedImage image, @Optional BufferedImage imageMask, @Optional Mask mask, @Switch('w') boolean disableWhiteOnly) throws ParameterException, WorldEditException {
        HeightMapMCAGenerator gen = assertSettings(fp).getGenerator();
        if (imageMask != null) gen.setColor(image, imageMask, !disableWhiteOnly);
        else if (mask != null) gen.setColor(image, mask);
        else gen.setColor(image);
        msg("Set color with blocks!").newline().text("&8> &7[&aNext&7]").cmdTip(alias()).send(fp);
    }

    @Command(
            aliases = {"blockbiomecolor", "setblockandbiomecolor", "blockandbiome"},
            usage = "<url> [imageMask|mask]",
            desc = "Set the color with blocks and biomes",
            help = "Color the terrain using blocks and biomes.\n" +
                    "Provide an image, or worldedit mask to restrict what areas are colored\n" +
            "The -w (disableWhiteOnly) will randomly apply depending on the pixel luminance"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void blockbiome(FawePlayer fp, BufferedImage image, @Optional BufferedImage imageMask, @Optional Mask mask, @Switch('w') boolean disableWhiteOnly) throws ParameterException, WorldEditException {
        assertSettings(fp).getGenerator().setBlockAndBiomeColor(image, mask, imageMask, !disableWhiteOnly);
        msg("Set color with blocks and biomes!").newline().text("&8> &7[&aNext&7]").cmdTip(alias()).send(fp);
    }

    @Command(
            aliases = {"biomecolor", "setbiomecolor", "biomes"},
            usage = "<url> [imageMask|mask]",
            desc = "Color the terrain using biomes.\n" +
                    "Note: Biome coloring does not change blocks:\n" +
                    " - If you changed the block to something other than grass you will not see anything."
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void biomecolor(FawePlayer fp, BufferedImage image, @Optional BufferedImage imageMask, @Optional Mask mask, @Switch('w') boolean disableWhiteOnly) throws ParameterException, WorldEditException {
        assertSettings(fp).getGenerator().setBiomeColor(image);
        msg("Set color with biomes!").newline().text("&8> &7[&aNext&7]").cmdTip(alias()).send(fp);
    }


    @Command(
            aliases = {"coloring", "palette"},
            usage = "",
            desc = "Color the world using an image"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void coloring(FawePlayer fp) throws ParameterException{
        CFISettings settings = assertSettings(fp);
        HeightMapMCAGenerator gen = settings.getGenerator();
        boolean rand = gen.getTextureRandomVariation();
        String mask;
        if (settings.imageMask != null) {
            mask = "Mask Image";
        } else if (settings.mask != null) {
            mask = "WorldEdit Mask";
        } else {
            mask = "NONE";
        }
        TextureUtil tu = gen.getRawTextureUtil();
        String blocks;
        if (tu.getClass() == TextureUtil.class) {
            blocks = "All";
        } else if (tu.getClass() == CleanTextureUtil.class) {
            CleanTextureUtil clean = (CleanTextureUtil) tu;
            blocks = "Complexity(" + clean.getMin() + "," + clean.getMax() + ")";
        } else if (tu.getClass() == FilteredTextureUtil.class) {
            blocks = "Selected";
        } else {
            blocks = "Undefined";
        }

        Set<String> materials = new HashSet<>();
        char[] blockArray = tu.getValidBlockIds();
        for (char combined : blockArray) {
            int id = FaweCache.getId(combined);
            BundledBlockData.BlockEntry block = BundledBlockData.getInstance().findById(id);
            if (block != null) {
                if (block.id.contains(":"))
                materials.add(block.id.contains(":") ? block.id.substring(block.id.indexOf(":") + 1) : block.id);
            } else materials.add(Integer.toString(id));
        }
        String blockList = materials.size() > 100 ? materials.size() + " blocks" : StringMan.join(materials, ',');

        int biomePriority = gen.getBiomePriority();

        Message msg = msg("Current settings:").newline()
        .text("Randomization ").text("&7[&a" + (Boolean.toString(rand).toUpperCase()) + "&7]").cmdTip(alias() + " randomization " + (!rand))
        .newline()
        .text("Mask ").text("&7[&a" + mask + "&7]").cmdTip(alias() + " mask")
        .newline()
        .text("Blocks ").text("&7[&a" + blocks + "&7]").tooltip(blockList).command(alias() + " paletteBlocks")
        .newline()
        .text("BiomePriority ").text("&7[&a" + biomePriority + "&7]").cmdTip(alias() + " biomepriority")
        .newline();

        if (settings.image != null) {
            StringBuilder colorArgs = new StringBuilder();
            colorArgs.append(" " + settings.imageArg);
            if (settings.imageMask != null) colorArgs.append(" " + settings.imageMaskArg);
            if (settings.mask != null) colorArgs.append(" " + settings.maskArg);
            if (!settings.whiteOnly) colorArgs.append(" -w");

            msg.text("Image: ")
            .text("&7[&a" + settings.imageArg + "&7]").cmdTip(alias() + " " + Commands.getAlias(CFICommands.class, "image"))
            .newline().newline()
            .text("Let's Color: ")
            .cmdOptions(alias() + " ", colorArgs.toString(), "Biomes", "Blocks", "BlockAndBiome", "Glass")
            .newline();
        } else {
            msg.newline().text("You can color a world using an image like ")
            .text("&7[&aThis&7]").linkTip("http://i.imgur.com/vJYinIU.jpg").newline()
            .text("Please provide an image: ")
            .text("&7[&aNone&7]").cmdTip(alias() + " " + Commands.getAlias(Command.class, "image")).newline();
        }
        msg.text("&8< &7[&aBack&7]").cmdTip(alias()).send(fp);
    }

    @Command(
            aliases = {"mask"},
            usage = "<imageMask|mask>",
            desc = "Select a mask"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void mask(FawePlayer fp, @Optional BufferedImage imageMask, @Optional Mask mask, @Switch('w') boolean disableWhiteOnly, CommandContext context) throws ParameterException{
        CFISettings settings = assertSettings(fp);
        String[] split = getArguments(context).split(" ");
        int index = 2;
        settings.imageMask = imageMask;
        settings.imageMaskArg = imageMask != null ? split[index++] : null;
        settings.mask = mask;
        settings.maskArg = mask != null ? split[index++] : null;
        settings.whiteOnly = disableWhiteOnly;

        StringBuilder cmd = new StringBuilder(alias() + " mask ");
        if (!settings.whiteOnly) cmd.append("-w ");

        msg("Current settings:").newline()
                .text("Image Mask ").text("&7[&a" + settings.imageMaskArg + "&7]").suggestTip(cmd + "http://")
                .newline()
                .text("WorldEdit Mask ").text("&7[&a" + settings.maskArg + "&7]").suggestTip(cmd + "<mask>")
                .send(fp);
    }

    @Command(
            aliases = {"download"},
            desc = "Download the current image"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void download(FawePlayer fp) throws ParameterException, IOException {
        CFISettings settings = assertSettings(fp);
        BufferedImage image = settings.getGenerator().draw();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos );
        byte[] data = baos.toByteArray();
        fp.sendMessage(BBC.getPrefix() + "Please wait...");
        URL url = ImgurUtility.uploadImage(data);
        BBC.DOWNLOAD_LINK.send(fp, url);
    }

    @Command(
            aliases = {"image"},
            usage = "<image>",
            desc = "Select an image"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void image(FawePlayer fp, @Optional BufferedImage image, CommandContext context) throws ParameterException{
        CFISettings settings = getSettings(fp).bind();
        String[] split = getArguments(context).split(" ");
        int index = 2;

        settings.image = image;
        settings.imageArg = image != null ? split[index++] : null;
        String maskArg = settings.maskArg == null ? "Click Here" : settings.maskArg;

        StringBuilder cmd = new StringBuilder(alias() + " image ");
        Message msg;
        if (image == null) {
            msg = msg("Please provide an image:").newline()
            .text("From a URL: ").text("&7[&aClick Here&7]").suggestTip(cmd + "http://")
            .newline()
            .text("From a file: ").text("&7[&aClick Here&7]").suggestTip(cmd + "file://");
        } else {
            msg = msg("Current image: ")
            .text("&7[&a" + settings.imageArg + "&7]").suggestTip(cmd.toString())
            .newline();
            if (settings.hasGenerator()) {
                msg.text("&8< &7[&aBack&7]").cmdTip(alias() + " " + Commands.getAlias(CFICommands.class, "coloring"));
            } else {
                msg.text("&8> &7[&aNext&7]").cmdTip(alias() + " " + Commands.getAlias(CFICommands.class, "heightmap " + settings.imageArg));
            }
        }
        msg.send(fp);
    }

    @Command(
            aliases = {"populate"},
            usage = "",
            desc = ""
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void populate(FawePlayer fp) throws ParameterException{
        CFISettings settings = assertSettings(fp);

        msg("What would you like to populate?").newline()
        .cmdOptions(alias() + " ", "", "Ores", "Ore", "Caves", "Schematics", "Snow")
        .newline().text("&8< &7[&aBack&7]").cmdTip(alias())
        .send(fp);
    }

    @Command(
            aliases = {"component", "components"},
            usage = "",
            desc = "Components menu"
    )
    @CommandPermissions("worldedit.anvil.cfi")
    public void component(FawePlayer fp) throws ParameterException{
        CFISettings settings = assertSettings(fp);
        msg("What component would you like to change?").newline()
        .cmdOptions(alias() + " ", "", "WaterId", "WaterHeight", "Floor", "Main", "Column", "Overlay", "Height", "Smooth")
        .newline().text("&8< &7[&aBack&7]").cmdTip(alias())
        .send(fp);
    }



    private CFISettings assertSettings(FawePlayer fp) throws ParameterException {
        CFISettings settings = getSettings(fp);
        if (!settings.hasGenerator()) throw new ParameterException("Please use /" + alias());
        return settings;
    }


    protected CFISettings getSettings(FawePlayer fp) {
        CFISettings settings = fp.getMeta("CFISettings");
        return settings == null ? new CFISettings(fp) : settings;
    }

    public static class CFISettings {
        private final FawePlayer fp;

        private HeightMapMCAGenerator generator;

        protected BufferedImage image;
        protected String imageArg;
        protected Mask mask;
        protected BufferedImage imageMask;
        protected boolean whiteOnly = true;
        protected String maskArg;
        protected String imageMaskArg;

        public CFISettings(FawePlayer player) {
            this.fp = player;
        }

        public boolean hasGenerator() {
            return generator != null;
        }

        public HeightMapMCAGenerator getGenerator() {
            return generator;
        }

        public void setMask(Mask mask, String arg) {
            this.mask = mask;
            this.maskArg = arg;
        }

        public void setImage(BufferedImage image, String arg) {
            this.image = image;
        }

        public void setImageMask(BufferedImage imageMask, String arg) {
            this.imageMask = imageMask;
            this.imageMaskArg = arg;
        }

        public CFISettings setGenerator(HeightMapMCAGenerator generator) {
            this.generator = generator;
            return this;
        }

        public CFISettings bind() {
            fp.setMeta("CFISettings", this);
            return this;
        }

        public CFISettings remove() {
            fp.deleteMeta("CFISettings");
            generator = null;
            image = null;
            imageArg = null;
            mask = null;
            imageMask = null;
            whiteOnly = true;
            maskArg = null;
            imageMaskArg = null;
            return this;
        }
    }

    protected String alias() {
        return Commands.getAlias(CFICommand.class, "/cfi");
    }

    protected Message msg(String text) {
        return new Message(BBC.getPrefix())
                .text(text);
    }

    protected void mainMenu(FawePlayer fp) {
        msg("What do you want to do now?").newline()
                .cmdOptions(alias() + " ", "", "Coloring", "Populate", "Component", "Brush")
                .newline().text("&3<> &7[&aView&7]").command(alias() + " " + Commands.getAlias(CFICommands.class, "download")).tooltip("View full resolution image")
                .newline().text("&4>< &7[&aCancel&7]").cmdTip(alias() + " " + Commands.getAlias(CFICommands.class, "cancel"))
                .newline().text("&2>> &7[&aDone&7]").cmdTip(alias() + " " + Commands.getAlias(CFICommands.class, "done"))
                .send(fp);
    }
}
