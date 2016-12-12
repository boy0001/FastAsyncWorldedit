package com.sk89q.worldedit.extension.factory;

import com.boydti.fawe.command.FaweParser;
import com.boydti.fawe.object.mask.AdjacentMask;
import com.boydti.fawe.object.mask.AngleMask;
import com.boydti.fawe.object.mask.CustomMask;
import com.boydti.fawe.object.mask.DataMask;
import com.boydti.fawe.object.mask.IdDataMask;
import com.boydti.fawe.object.mask.IdMask;
import com.boydti.fawe.object.mask.RadiusMask;
import com.boydti.fawe.object.mask.WallMask;
import com.boydti.fawe.object.mask.XAxisMask;
import com.boydti.fawe.object.mask.YAxisMask;
import com.boydti.fawe.object.mask.ZAxisMask;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BiomeMask2D;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.ExpressionMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.mask.NoiseFilter;
import com.sk89q.worldedit.function.mask.OffsetMask;
import com.sk89q.worldedit.function.mask.RegionMask;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.math.noise.RandomNoise;
import com.sk89q.worldedit.regions.shape.WorldEditExpressionEnvironment;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.session.request.RequestSelection;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.biome.Biomes;
import com.sk89q.worldedit.world.registry.BiomeRegistry;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Parses mask input strings.
 */
public class DefaultMaskParser extends FaweParser<Mask> {

    public static final String[] EXPRESSION_MASK = new String[] { "=<expression>" };

    public static final String[] BLOCK_MASK = new String[] { "<block>" };

    public static final String[] SIMPLE_MASK = new String[] {
            "#existing", "#solid", "#dregion", "#dselection", "#dsel", "#selection", "#region", "#sel", "#xaxis", "#yaxis", "#zaxis", "#id", "#data", "#wall", "#surface",
    };

    public static final String[] MISC_PATTERNS = new String[] {
            "hand", "pos1",
    };

    public DefaultMaskParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    private static CustomMask[] customMasks = new CustomMask[0];

    public void addMask(CustomMask mask) {
        checkNotNull(mask);
        List<CustomMask> list = new ArrayList<>(Arrays.asList(customMasks));
        list.add(mask);
        customMasks = list.toArray(new CustomMask[list.size()]);
    }

    public List<CustomMask> getCustomMasks() {
        return Arrays.asList(customMasks);
    }

    @Override
    public Mask parseFromInput(String input, ParserContext context) throws InputParseException {
        List<Mask> masks = new ArrayList<Mask>();

        for (String component : input.split(" ")) {
            if (component.isEmpty()) {
                continue;
            }

            Mask current = getBlockMaskComponent(masks, component, context);

            masks.add(current);
        }

        switch (masks.size()) {
            case 0:
                return null;

            case 1:
                return masks.get(0);

            default:
                return new MaskIntersection(masks);
        }
    }

    private Mask getBlockMaskComponent(List<Mask> masks, String component, ParserContext context) throws InputParseException {
        Extent extent = Request.request().getExtent();

        final char firstChar = component.charAt(0);
        switch (firstChar) {
            case '#':
                int colon = component.indexOf(':');
                if (colon != -1) {
                    String rest = component.substring(colon + 1);
                    component = component.substring(0, colon);
                    masks.add(getBlockMaskComponent(masks, rest, context));
                }
                switch (component.toLowerCase()) {
                    case "#existing":
                        return new ExistingBlockMask(extent);
                    case "#solid":
                        return new SolidBlockMask(extent);
                    case "#dregion":
                    case "#dselection":
                    case "#dsel":
                        return new RegionMask(new RequestSelection());
                    case "#selection":
                    case "#region":
                    case "#sel":
                        try {
                            return new RegionMask(context.requireSession().getSelection(context.requireWorld()).clone());
                        } catch (IncompleteRegionException e) {
                            throw new InputParseException("Please make a selection first.");
                        }
                    case "#xaxis":
                        return new XAxisMask();
                    case "#yaxis":
                        return new YAxisMask();
                    case "#zaxis":
                        return new ZAxisMask();
                    case "#id":
                        return new IdMask(extent);
                    case "#data":
                        return new DataMask(extent);
                    case "#iddata":
                        return new IdDataMask(extent);
                    case "#wall":
                        masks.add(new ExistingBlockMask(extent));
                        BlockMask matchAir = new BlockMask(extent, EditSession.nullBlock);
                        return new WallMask(extent, Arrays.asList(new BaseBlock(0)), 1, 8);
                    case "#surface":
                        masks.add(new ExistingBlockMask(extent));
                        return new AdjacentMask(extent, Arrays.asList(new BaseBlock(0)), 1, 8);
                    default:
                        throw new NoMatchException("Unrecognized mask '" + component + "'");
                }
            case '\\':
            case '/': {
                String[] split = component.substring(1).split(",");
                if (split.length != 2) {
                    throw new InputParseException("Unknown angle '" + component + "' (not in form `/#,#`)");
                }
                try {
                    int y1 = (int) Math.abs(Expression.compile(split[0]).evaluate());
                    int y2 = (int) Math.abs(Expression.compile(split[1]).evaluate());
                    return new AngleMask(extent, y1, y2);
                } catch (NumberFormatException | ExpressionException e) {
                    throw new InputParseException("Unknown angle '" + component + "' (not in form `/#,#`)");
                }
            }
            case '{': {
                String[] split = component.substring(1).split(",");
                if (split.length != 2) {
                    throw new InputParseException("Unknown range '" + component + "' (not in form `{#,#`)");
                }
                try {
                    int y1 = (int) Math.abs(Expression.compile(split[0]).evaluate());
                    int y2 = (int) Math.abs(Expression.compile(split[1]).evaluate());
                    return new RadiusMask(y1, y2);
                } catch (NumberFormatException | ExpressionException e) {
                    throw new InputParseException("Unknown range '" + component + "' (not in form `{#,#`)");
                }
            }
            case '|':
            case '~': {
                String[] split = component.substring(1).split("=");
                ParserContext tempContext = new ParserContext(context);
                tempContext.setRestricted(false);
                tempContext.setPreferringWildcard(true);
                try {
                    int requiredMin = 1;
                    int requiredMax = 8;
                    if (split.length == 2) {
                        String[] split2 = split[1].split(",");
                        requiredMin = (int) Math.abs(Expression.compile(split2[0]).evaluate());
                        if (split2.length == 2) {
                            requiredMax = (int) Math.abs(Expression.compile(split2[1]).evaluate());
                        }
                    }
                    if (firstChar == '~') {
                        return new AdjacentMask(extent, worldEdit.getBlockFactory().parseFromListInput(component.substring(1), tempContext), requiredMin, requiredMax);
                    } else {
                        return new WallMask(extent, worldEdit.getBlockFactory().parseFromListInput(component.substring(1), tempContext), requiredMin, requiredMax);
                    }
                } catch (NumberFormatException | ExpressionException e) {
                    throw new InputParseException("Unknown adjacent mask '" + component + "' (not in form `~<ids>[=count]`)");
                }
            }
            case '>':
            case '<':
                Mask submask;
                if (component.length() > 1) {
                    submask = getBlockMaskComponent(masks, component.substring(1), context);
                } else {
                    submask = new ExistingBlockMask(extent);
                }
                OffsetMask offsetMask = new OffsetMask(submask, new Vector(0, firstChar == '>' ? -1 : 1, 0));
                return new MaskIntersection(offsetMask, Masks.negate(submask));

            case '$':
                Set<BaseBiome> biomes = new HashSet<BaseBiome>();
                String[] biomesList = component.substring(1).split(",");
                BiomeRegistry biomeRegistry = context.requireWorld().getWorldData().getBiomeRegistry();
                List<BaseBiome> knownBiomes = biomeRegistry.getBiomes();
                for (String biomeName : biomesList) {
                    BaseBiome biome = Biomes.findBiomeByName(knownBiomes, biomeName, biomeRegistry);
                    if (biome == null) {
                        throw new InputParseException("Unknown biome '" + biomeName + "'");
                    }
                    biomes.add(biome);
                }

                return Masks.asMask(new BiomeMask2D(context.requireExtent(), biomes));

            case '%':
                try {
                    double i = Math.abs(Expression.compile(component.substring(1)).evaluate());
                    return new NoiseFilter(new RandomNoise(), (i) / 100);
                } catch (NumberFormatException | ExpressionException e) {
                    throw new InputParseException("Unknown percentage '" + component.substring(1) + "'");
                }
            case '=':
                try {
                    Expression exp = Expression.compile(component.substring(1), "x", "y", "z");
                    WorldEditExpressionEnvironment env = new WorldEditExpressionEnvironment(
                            Request.request().getEditSession(), Vector.ONE, Vector.ZERO);
                    exp.setEnvironment(env);
                    return new ExpressionMask(exp);
                } catch (ExpressionException e) {
                    throw new InputParseException("Invalid expression: " + e.getMessage());
                }

            case '!':
                if (component.length() > 1) {
                    return Masks.negate(getBlockMaskComponent(masks, component.substring(1), context));
                }

            default:
                for (CustomMask mask : customMasks) {
                    if (mask.accepts(component)) {
                        try {
                            Constructor<? extends CustomMask> constructor = mask.getClass().getDeclaredConstructor(List.class, String.class, ParserContext.class);
                            return constructor.newInstance(masks, component, context);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                }

                ParserContext tempContext = new ParserContext(context);
                tempContext.setRestricted(false);
                tempContext.setPreferringWildcard(true);
                return new BlockMask(extent, worldEdit.getBlockFactory().parseFromListInput(component, tempContext));
        }
    }

    public static Class<?> inject() {
        return DefaultMaskParser.class;
    }
}