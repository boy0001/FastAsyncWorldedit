/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.command;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.command.FawePrimitiveBinding;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.jnbt.anvil.generator.CavesGen;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.TextureUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.Logging;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.util.TreeGenerator.TreeType;
import com.sk89q.worldedit.util.command.binding.Range;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.binding.Text;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.util.command.parametric.ParameterException;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;


import static com.sk89q.minecraft.util.commands.Logging.LogMode.ALL;
import static com.sk89q.minecraft.util.commands.Logging.LogMode.PLACEMENT;
import static com.sk89q.minecraft.util.commands.Logging.LogMode.POSITION;

/**
 * Commands for the generation of shapes and other objects.
 */
@Command(aliases = {}, desc = "Create structures and features: [More Info](https://goo.gl/KuLFRW)")
public class GenerationCommands extends MethodCommands {

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public GenerationCommands(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Command(
            aliases = {"/caves"},
            usage = "[size=8] [freq=40] [rarity=7] [minY=8] [maxY=127] [sysFreq=1] [sysRarity=25] [pocketRarity=0] [pocketMin=0] [pocketMax=3]",
            desc = "Generates caves",
            help = "Generates a cave network"
    )
    @CommandPermissions("worldedit.generation.caves")
    @Logging(PLACEMENT)
    public void caves(FawePlayer fp, LocalSession session, EditSession editSession, @Selection Region region, @Optional("8") int size, @Optional("40") int frequency, @Optional("7") int rarity, @Optional("8") int minY, @Optional("127") int maxY, @Optional("1") int systemFrequency, @Optional("25") int individualRarity, @Optional("0") int pocketChance, @Optional("0") int pocketMin, @Optional("3") int pocketMax, CommandContext context) throws WorldEditException, ParameterException {
        fp.checkConfirmationRegion(getArguments(context), region);
        CavesGen gen = new CavesGen(size, frequency, rarity, minY, maxY, systemFrequency, individualRarity, pocketChance, pocketMin, pocketMax);
        editSession.generate(region, gen);
        BBC.VISITOR_BLOCK.send(fp, editSession.getBlockChangeCount());
    }

    // public void addOre(Mask mask, Pattern material, int size, int frequency, int rarity, int minY, int maxY) throws WorldEditException {

    @Command(
            aliases = {"/ores"},
            desc = "Generates ores",
            help = "Generates ores",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.generation.ore")
    @Logging(PLACEMENT)
    public void ores(FawePlayer player, LocalSession session, EditSession editSession, @Selection Region region, Mask mask, CommandContext context) throws WorldEditException, ParameterException {
        player.checkConfirmationRegion(getArguments(context), region);
        editSession.addOres(region, mask);
        BBC.VISITOR_BLOCK.send(player, editSession.getBlockChangeCount());
    }

    @Command(
            aliases = {"/image", "/img"},
            desc = "Generate an image",
            usage = "<imgur> [randomize=true] [complexity=100]",
            min = 1,
            max = 3
    )
    @CommandPermissions("worldedit.generation.image")
    @Logging(PLACEMENT)
    public void image(Player player, LocalSession session, EditSession editSession, String arg, @Optional("true") boolean randomize, @Optional("100") int threshold) throws WorldEditException, ParameterException, IOException {
        TextureUtil tu = Fawe.get().getCachedTextureUtil(randomize, 0, threshold);
        URL url = new URL(arg);
        if (!url.getHost().equalsIgnoreCase("i.imgur.com") && !url.getHost().equalsIgnoreCase("empcraft.com")) {
            throw new IOException("Only i.imgur.com or empcraft.com/ui links are allowed!");
        }
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        BufferedImage image = MainUtil.toRGB(ImageIO.read(url));
        MutableBlockVector pos1 = new MutableBlockVector(player.getPosition());
        MutableBlockVector pos2 = new MutableBlockVector(pos1.add(image.getWidth() - 1, 0, image.getHeight() - 1));
        CuboidRegion region = new CuboidRegion(pos1, pos2);
        int[] count = new int[1];
        RegionVisitor visitor = new RegionVisitor(region, new RegionFunction() {
            @Override
            public boolean apply(Vector pos) throws WorldEditException {
                try {
                    int x = pos.getBlockX() - pos1.getBlockX();
                    int z = pos.getBlockZ() - pos1.getBlockZ();
                    int color = image.getRGB(x, z);
                    BaseBlock block = tu.getNearestBlock(color);
                    count[0]++;
                    return editSession.setBlockFast(pos, block);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                return false;
            }
        }, editSession);
        Operations.completeBlindly(visitor);
        BBC.VISITOR_BLOCK.send(player, editSession.getBlockChangeCount());
    }

    @Command(
            aliases = {"/ore"},
            usage = "<mask> <pattern> <size> <freq> <rarity> <minY> <maxY>",
            desc = "Generates ores",
            help = "Generates ores",
            min = 7,
            max = 7
    )
    @CommandPermissions("worldedit.generation.ore")
    @Logging(PLACEMENT)
    public void ore(FawePlayer player, LocalSession session, EditSession editSession, @Selection Region region, Mask mask, Pattern material, int size, int freq, int rarity, int minY, int maxY, CommandContext context) throws WorldEditException, ParameterException {
        player.checkConfirmationRegion(getArguments(context), region);
        editSession.addOre(region, mask, material, size, freq, rarity, minY, maxY);
        BBC.VISITOR_BLOCK.send(player, editSession.getBlockChangeCount());
    }

    @Command(
            aliases = {"/hcyl"},
            usage = "<pattern> <radius>[,<radius>] [height]",
            desc = "Generates a hollow cylinder.",
            help =
                    "Generates a hollow cylinder.\n" +
                            "By specifying 2 radii, separated by a comma,\n" +
                            "you can generate elliptical cylinders.\n" +
                            "The 1st radius is north/south, the 2nd radius is east/west.",
            min = 2,
            max = 3
    )
    @CommandPermissions("worldedit.generation.cylinder")
    @Logging(PLACEMENT)
    public void hcyl(FawePlayer fp, Player player, LocalSession session, EditSession editSession, Pattern pattern, String radiusString, @Optional("1") int height, CommandContext context) throws WorldEditException, ParameterException {
        cyl(fp, player, session, editSession, pattern, radiusString, height, true, context);
    }

    @Command(
            aliases = {"/cyl"},
            usage = "<block> <radius>[,<radius>] [height]",
            flags = "h",
            desc = "Generates a cylinder.",
            help =
                    "Generates a cylinder.\n" +
                            "By specifying 2 radii, separated by a comma,\n" +
                            "you can generate elliptical cylinders.\n" +
                            "The 1st radius is north/south, the 2nd radius is east/west.",
            min = 2,
            max = 3
    )
    @CommandPermissions("worldedit.generation.cylinder")
    @Logging(PLACEMENT)
    public void cyl(FawePlayer fp, Player player, LocalSession session, EditSession editSession, Pattern pattern, String radiusString, @Optional("1") int height, @Switch('h') boolean hollow, CommandContext context) throws WorldEditException, ParameterException {
        String[] radii = radiusString.split(",");
        final double radiusX, radiusZ;
        switch (radii.length) {
            case 1:
                radiusX = radiusZ = Math.max(1, FawePrimitiveBinding.parseNumericInput(radii[0]));
                break;

            case 2:
                radiusX = Math.max(1, FawePrimitiveBinding.parseNumericInput(radii[0]));
                radiusZ = Math.max(1, FawePrimitiveBinding.parseNumericInput(radii[1]));
                break;

            default:
                fp.sendMessage(BBC.getPrefix() + "You must either specify 1 or 2 radius values.");
                return;
        }
        height = Math.min(256, height);
        worldEdit.checkMaxRadius(radiusX);
        worldEdit.checkMaxRadius(radiusZ);
        worldEdit.checkMaxRadius(height);

        double max = MathMan.max(radiusX, radiusZ, height);
        fp.checkConfirmationRadius(getArguments(context), (int) max);

        Vector pos = session.getPlacementPosition(player);
        int affected = editSession.makeCylinder(pos, pattern, radiusX, radiusZ, height, !hollow);
        BBC.VISITOR_BLOCK.send(fp, affected);
    }

    @Command(
            aliases = {"/hsphere"},
            usage = "<block> <radius>[,<radius>,<radius>] [raised?]",
            desc = "Generates a hollow sphere.",
            help =
                    "Generates a hollow sphere.\n" +
                            "By specifying 3 radii, separated by commas,\n" +
                            "you can generate an ellipsoid. The order of the ellipsoid radii\n" +
                            "is north/south, up/down, east/west.",
            min = 2,
            max = 3
    )
    @CommandPermissions("worldedit.generation.sphere")
    @Logging(PLACEMENT)
    public void hsphere(FawePlayer fp, Player player, LocalSession session, EditSession editSession, Pattern pattern, String radiusString, @Optional("false") boolean raised, CommandContext context) throws WorldEditException, ParameterException {
        sphere(fp, player, session, editSession, pattern, radiusString, raised, true, context);
    }

    @Command(
            aliases = {"/sphere"},
            usage = "<block> <radius>[,<radius>,<radius>] [raised?]",
            flags = "h",
            desc = "Generates a filled sphere.",
            help =
                    "Generates a filled sphere.\n" +
                            "By specifying 3 radii, separated by commas,\n" +
                            "you can generate an ellipsoid. The order of the ellipsoid radii\n" +
                            "is north/south, up/down, east/west.",
            min = 2,
            max = 3
    )
    @CommandPermissions("worldedit.generation.sphere")
    @Logging(PLACEMENT)
    public void sphere(FawePlayer fp, Player player, LocalSession session, EditSession editSession, Pattern pattern, String radiusString, @Optional("false") boolean raised, @Switch('h') boolean hollow, CommandContext context) throws WorldEditException, ParameterException {
        String[] radii = radiusString.split(",");
        final double radiusX, radiusY, radiusZ;
        switch (radii.length) {
            case 1:
                radiusX = radiusY = radiusZ = Math.max(1, FawePrimitiveBinding.parseNumericInput(radii[0]));
                break;

            case 3:
                radiusX = Math.max(1, FawePrimitiveBinding.parseNumericInput(radii[0]));
                radiusY = Math.max(1, FawePrimitiveBinding.parseNumericInput(radii[1]));
                radiusZ = Math.max(1, FawePrimitiveBinding.parseNumericInput(radii[2]));
                break;

            default:
                fp.sendMessage(BBC.getPrefix() + "You must either specify 1 or 3 radius values.");
                return;
        }
        worldEdit.checkMaxRadius(radiusX);
        worldEdit.checkMaxRadius(radiusY);
        worldEdit.checkMaxRadius(radiusZ);

        double max = MathMan.max(radiusX, radiusY, radiusZ);
        fp.checkConfirmationRadius(getArguments(context), (int) max);

        Vector pos = session.getPlacementPosition(player);
        if (raised) {
            pos = pos.add(0, radiusY, 0);
        }

        int affected = editSession.makeSphere(pos, pattern, radiusX, radiusY, radiusZ, !hollow);
        player.findFreePosition();
        BBC.VISITOR_BLOCK.send(fp, affected);
    }

    @Command(
            aliases = {"forestgen"},
            usage = "[size] [type] [density]",
            desc = "Generate a forest",
            min = 0,
            max = 3
    )
    @CommandPermissions("worldedit.generation.forest")
    @Logging(POSITION)
    @SuppressWarnings("deprecation")
    public void forestGen(Player player, LocalSession session, EditSession editSession, @Optional("10") int size, @Optional("tree") TreeType type, @Optional("5") double density) throws WorldEditException, ParameterException {
        density = density / 100;
        int affected = editSession.makeForest(session.getPlacementPosition(player), size, density, new TreeGenerator(type));
        BBC.COMMAND_TREE.send(player, affected);
    }

    @Command(
            aliases = {"pumpkins"},
            usage = "[size]",
            desc = "Generate pumpkin patches",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.generation.pumpkins")
    @Logging(POSITION)
    public void pumpkins(Player player, LocalSession session, EditSession editSession, @Optional("10") int apothem) throws WorldEditException, ParameterException {
        int affected = editSession.makePumpkinPatches(session.getPlacementPosition(player), apothem);
        BBC.COMMAND_PUMPKIN.send(player, affected);
    }

    @Command(
            aliases = {"/hpyramid"},
            usage = "<block> <size>",
            desc = "Generate a hollow pyramid",
            min = 2,
            max = 2
    )
    @CommandPermissions("worldedit.generation.pyramid")
    @Logging(PLACEMENT)
    public void hollowPyramid(FawePlayer fp, Player player, LocalSession session, EditSession editSession, Pattern pattern, @Range(min = 1) int size, CommandContext context) throws WorldEditException, ParameterException {
        pyramid(fp, player, session, editSession, pattern, size, true, context);
    }

    @Command(
            aliases = {"/pyramid"},
            usage = "<block> <size>",
            flags = "h",
            desc = "Generate a filled pyramid",
            min = 2,
            max = 2
    )
    @CommandPermissions("worldedit.generation.pyramid")
    @Logging(PLACEMENT)
    public void pyramid(FawePlayer fp, Player player, LocalSession session, EditSession editSession, Pattern pattern, @Range(min = 1) int size, @Switch('h') boolean hollow, CommandContext context) throws WorldEditException, ParameterException {
        fp.checkConfirmationRadius(getArguments(context), size);
        Vector pos = session.getPlacementPosition(player);
        worldEdit.checkMaxRadius(size);
        int affected = editSession.makePyramid(pos, pattern, size, !hollow);
        player.findFreePosition();
        BBC.VISITOR_BLOCK.send(fp, affected);
    }

    @Command(
            aliases = {"/generate", "/gen", "/g"},
            usage = "<block> <expression>",
            desc = "Generates a shape according to a formula.",
            help =
                    "Generates a shape according to a formula that is expected to\n" +
                            "return positive numbers (true) if the point is inside the shape\n" +
                            "Optionally set type/data to the desired block.\n" +
                            "Flags:\n" +
                            "  -h to generate a hollow shape\n" +
                            "  -r to use raw minecraft coordinates\n" +
                            "  -o is like -r, except offset from placement.\n" +
                            "  -c is like -r, except offset selection center.\n" +
                            "If neither -r nor -o is given, the selection is mapped to -1..1\n" +
                            "See also tinyurl.com/wesyntax.",
            flags = "hroc",
            min = 2,
            max = -1
    )
    @CommandPermissions("worldedit.generation.shape")
    @Logging(ALL)
    public void generate(FawePlayer fp, Player player, LocalSession session, EditSession editSession,
                         @Selection Region region,
                         Pattern pattern,
                         @Text String expression,
                         @Switch('h') boolean hollow,
                         @Switch('r') boolean useRawCoords,
                         @Switch('o') boolean offset,
                         @Switch('c') boolean offsetCenter,
                         CommandContext context) throws WorldEditException, ParameterException {
        fp.checkConfirmationRegion(getArguments(context), region);
        final Vector zero;
        Vector unit;

        if (useRawCoords) {
            zero = Vector.ZERO;
            unit = Vector.ONE;
        } else if (offset) {
            zero = session.getPlacementPosition(player);
            unit = Vector.ONE;
        } else if (offsetCenter) {
            final Vector min = region.getMinimumPoint();
            final Vector max = region.getMaximumPoint();

            zero = max.add(min).multiply(0.5);
            unit = Vector.ONE;
        } else {
            final Vector min = region.getMinimumPoint();
            final Vector max = region.getMaximumPoint();

            zero = max.add(min).multiply(0.5);
            unit = max.subtract(zero);

            if (unit.getX() == 0) unit.mutX(1);
            if (unit.getY() == 0) unit.mutY(1);
            if (unit.getZ() == 0) unit.mutZ(1);
        }

        try {
            final int affected = editSession.makeShape(region, zero, unit, pattern, expression, hollow);
            player.findFreePosition();
            BBC.VISITOR_BLOCK.send(fp, affected);
        } catch (ExpressionException e) {
            fp.sendMessage(BBC.getPrefix() + e.getMessage());
        }
    }

    @Command(
            aliases = {"/generatebiome", "/genbiome", "/gb"},
            usage = "<biome> <expression>",
            desc = "Sets biome according to a formula.",
            help =
                    "Generates a shape according to a formula that is expected to\n" +
                            "return positive numbers (true) if the point is inside the shape\n" +
                            "Sets the biome of blocks in that shape.\n" +
                            "Flags:\n" +
                            "  -h to generate a hollow shape\n" +
                            "  -r to use raw minecraft coordinates\n" +
                            "  -o is like -r, except offset from placement.\n" +
                            "  -c is like -r, except offset selection center.\n" +
                            "If neither -r nor -o is given, the selection is mapped to -1..1\n" +
                            "See also tinyurl.com/wesyntax.",
            flags = "hroc",
            min = 2,
            max = -1
    )
    @CommandPermissions({"worldedit.generation.shape", "worldedit.biome.set"})
    @Logging(ALL)
    public void generateBiome(FawePlayer fp, Player player, LocalSession session, EditSession editSession,
                              @Selection Region region,
                              BaseBiome target,
                              @Text String expression,
                              @Switch('h') boolean hollow,
                              @Switch('r') boolean useRawCoords,
                              @Switch('o') boolean offset,
                              @Switch('c') boolean offsetCenter,
                              CommandContext context) throws WorldEditException, ParameterException {
        fp.checkConfirmationRegion(getArguments(context), region);
        final Vector zero;
        Vector unit;

        if (useRawCoords) {
            zero = Vector.ZERO;
            unit = Vector.ONE;
        } else if (offset) {
            zero = session.getPlacementPosition(player);
            unit = Vector.ONE;
        } else if (offsetCenter) {
            final Vector min = region.getMinimumPoint();
            final Vector max = region.getMaximumPoint();

            zero = max.add(min).multiply(0.5);
            unit = Vector.ONE;
        } else {
            final Vector min = region.getMinimumPoint();
            final Vector max = region.getMaximumPoint();

            zero = max.add(min).multiply(0.5);
            unit = max.subtract(zero);

            if (unit.getX() == 0) unit.mutX(1);
            if (unit.getY() == 0) unit.mutY(1);
            if (unit.getZ() == 0) unit.mutZ(1);
        }

        try {
            final int affected = editSession.makeBiomeShape(region, zero, unit, target, expression, hollow);
            player.findFreePosition();
            BBC.VISITOR_FLAT.send(fp, affected);
        } catch (ExpressionException e) {
            fp.sendMessage(BBC.getPrefix() + e.getMessage());
        }
    }

    public static Class<GenerationCommands> inject() {
        return GenerationCommands.class;
    }
}
