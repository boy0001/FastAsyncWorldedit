package com.sk89q.worldedit.extension.factory;

import com.boydti.fawe.command.FaweParser;
import com.boydti.fawe.command.SuggestInputParseException;
import com.boydti.fawe.object.mask.*;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.StringMan;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BiomeMask2D;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.ExpressionMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.mask.MaskUnion;
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

    public static final String[] BLOCK_MASK = new String[] { "<blocks>" };

    public static final String[] SIMPLE_MASK = new String[] {
            "#nolight", "#haslight", "#existing", "#solid", "#dregion", "#dselection", "#dsel", "#selection", "#region", "#sel", "#xaxis", "#yaxis", "#zaxis", "#id", "#data", "#wall", "#surface",
    };

    public static final String[] DELEGATE_MASKS = new String[] {
        "#offset:", "#light:", "#blocklight:", "#skylight:", "#brightness:", "#opacity:"
    };

    public static final String[] CHARACTER_MASKS= new String[] {
            "/", "{", "|", "~", ">", "<", "$", "%", "=", "!",
    };

    public static final String[] HASHTAG_MASKS = MainUtil.joinArrayGeneric(SIMPLE_MASK, DELEGATE_MASKS);

    public static final String[] ALL_MASKS = MainUtil.joinArrayGeneric(EXPRESSION_MASK, BLOCK_MASK, SIMPLE_MASK, DELEGATE_MASKS, CHARACTER_MASKS);

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

        for (String component : split(input, ' ')) {
            if (component.isEmpty()) {
                continue;
            }
            HashSet<BaseBlock> blocks = new HashSet<BaseBlock>();
            List<Mask> masksUnion = new ArrayList<Mask>();
            for (String elem : split(component, ',')) {
                ArrayList<Mask> list = new ArrayList<Mask>();
                list.add(catchSuggestion(input, list, elem, context));
                if (list.size() == 1) {
                    Mask mask = list.get(0);
                    if (mask.getClass() == BlockMask.class) {
                        blocks.addAll(((BlockMask) mask).getBlocks());
                    } else {
                        masksUnion.add(mask);
                    }
                } else {
                    masksUnion.add(new MaskIntersection(list));
                }
            }
            if (!blocks.isEmpty()) {
                masksUnion.add(new BlockMask(Request.request().getExtent(), blocks));
            }
            if (masksUnion.size() == 1) {
                masks.add(masksUnion.get(0));
            } else {
                masks.add(new MaskUnion(masksUnion));
            }
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

    public Mask catchSuggestion(String currentInput, List<Mask> masks, String nextInput, ParserContext context) throws InputParseException {
        try {
            return getBlockMaskComponent(masks, nextInput, context);
        } catch (SuggestInputParseException e) {
            e.prepend(currentInput.substring(0, currentInput.length() - nextInput.length()));
            throw e;
        }
    }

    private Mask getBlockMaskComponent(List<Mask> masks, String input, ParserContext context) throws InputParseException {
        Extent extent = Request.request().getExtent();
        final char firstChar = input.charAt(0);
        switch (firstChar) {
            case '#':
                int colon = input.indexOf(':');
                String component = input;
                if (colon != -1) {
                    component = component.substring(0, colon);
                    String rest = input.substring(colon + 1);
                    switch (component.toLowerCase()) {
                        case "#light":
                        case "#skylight":
                        case "#blocklight":
                        case "#emittedlight":
                        case "#opacity":
                        case "#brightness":
                            String[] split = rest.split(":");
                            if (split.length < 2) {
                                throw new SuggestInputParseException(input, component + ":<min>:<max>");
                            } else if (split.length > 2) {
                                masks.add(catchSuggestion(input, masks, StringMan.join(Arrays.copyOfRange(split, 2, split.length), ":"), context));
                            }
                            try {
                                int y1 = (int) Math.abs(Expression.compile(split[0]).evaluate());
                                int y2 = (int) Math.abs(Expression.compile(split[1]).evaluate());
                                switch (component.toLowerCase()) {
                                    case "#light":
                                        return new LightMask(extent, y1, y2);
                                    case "#skylight":
                                        return new SkyLightMask(extent, y1, y2);
                                    case "#blocklight":
                                    case "#emittedlight":
                                        return new BlockLightMask(extent, y1, y2);
                                    case "#opacity":
                                        return new OpacityMask(extent, y1, y2);
                                    case "#brightness":
                                        return new BrightnessMask(extent, y1, y2);
                                }
                            } catch (NumberFormatException | ExpressionException e) {
                                e.printStackTrace();
                                throw new SuggestInputParseException(input, component + ":<min>:<max>");
                            }
                        case "#~":
                        case "#rel":
                        case "#relative":
                        case "#offset":
                            try {
                                List<String> split3 = suggestRemaining(rest, "#offset", "<dx>", "<dy>", "<dz>", "<mask>");
                                int x = (int) Expression.compile(split3.get(0)).evaluate();
                                int y = (int) Expression.compile(split3.get(1)).evaluate();
                                int z = (int) Expression.compile(split3.get(2)).evaluate();
                                rest = StringMan.join(split3.subList(3, split3.size()), ":");
                                Mask mask = catchSuggestion(input, masks, rest, context);
                                return new OffsetMask(mask, new Vector(x, y, z));
                            } catch (NumberFormatException | ExpressionException | IndexOutOfBoundsException e) {
                                throw new SuggestInputParseException(null, "#offset:<dx>:<dy>:<dz>:<mask>");
                            }
                    }
                    Mask mask = catchSuggestion(input, masks, rest, context);
                    masks.add(mask);
                }
                switch (component.toLowerCase()) {
                    case "#haslight":
                        return new LightMask(extent, 1, Integer.MAX_VALUE);
                    case "#nolight":
                        return new LightMask(extent, 0, 0);
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
                        throw new SuggestInputParseException(input, HASHTAG_MASKS);
                }
            case '\\':
            case '/': {
                String[] split = input.substring(1).split(":");
                if (split.length != 2) {
                    throw new SuggestInputParseException(input, "/<min-angle>:<max-angle>");
                }
                try {
                    int y1 = (int) (Expression.compile(split[0]).evaluate());
                    int y2 = (int) (Expression.compile(split[1]).evaluate());
                    return new AngleMask(extent, y1, y2);
                } catch (NumberFormatException | ExpressionException e) {
                    throw new SuggestInputParseException(input, "/<min-angle>:<max-angle>");
                }
            }
            case '{': {
                String[] split = input.substring(1).split(":");
                if (split.length != 2) {
                    throw new SuggestInputParseException(input, "{<min-radius>:<max-radius>");
                }
                try {
                    int y1 = (int) Math.abs(Expression.compile(split[0]).evaluate());
                    int y2 = (int) Math.abs(Expression.compile(split[1]).evaluate());
                    return new RadiusMask(y1, y2);
                } catch (NumberFormatException | ExpressionException e) {
                    throw new SuggestInputParseException(input, "{<min-radius>:<max-radius>");
                }
            }
            case '|':
            case '~': {
                String[] split = input.substring(1).split("=");
                ParserContext tempContext = new ParserContext(context);
                tempContext.setRestricted(false);
                tempContext.setPreferringWildcard(true);
                try {
                    int requiredMin = 1;
                    int requiredMax = 8;
                    if (split.length == 2) {
                        String[] split2 = split[1].split(":");
                        requiredMin = (int) Math.abs(Expression.compile(split2[0]).evaluate());
                        if (split2.length == 2) {
                            requiredMax = (int) Math.abs(Expression.compile(split2[1]).evaluate());
                        }
                    }
                    if (firstChar == '~') {
                        return new AdjacentMask(extent, worldEdit.getBlockFactory().parseFromListInput(input.substring(1), tempContext), requiredMin, requiredMax);
                    } else {
                        return new WallMask(extent, worldEdit.getBlockFactory().parseFromListInput(input.substring(1), tempContext), requiredMin, requiredMax);
                    }
                } catch (NumberFormatException | ExpressionException e) {
                    throw new SuggestInputParseException(input, "~<blocks>=<amount>");
                }
            }
            case '>':
            case '<':
                Mask submask;
                if (input.length() > 1) {
                    submask = getBlockMaskComponent(masks, input.substring(1), context);
                } else {
                    submask = new ExistingBlockMask(extent);
                }
                OffsetMask offsetMask = new OffsetMask(submask, new Vector(0, firstChar == '>' ? -1 : 1, 0));
                return new MaskIntersection(offsetMask, Masks.negate(submask));

            case '$':
                Set<BaseBiome> biomes = new HashSet<BaseBiome>();
                String[] biomesList = input.substring(1).split(",");
                BiomeRegistry biomeRegistry = context.requireWorld().getWorldData().getBiomeRegistry();
                List<BaseBiome> knownBiomes = biomeRegistry.getBiomes();
                for (String biomeName : biomesList) {
                    BaseBiome biome = Biomes.findBiomeByName(knownBiomes, biomeName, biomeRegistry);
                    if (biome == null) {
                        throw new SuggestInputParseException(input, "$<biome>");
                    }
                    biomes.add(biome);
                }
                return Masks.asMask(new BiomeMask2D(context.requireExtent(), biomes));

            case '%':
                try {
                    double i = Math.abs(Expression.compile(input.substring(1)).evaluate());
                    return new NoiseFilter(new RandomNoise(), (i) / 100);
                } catch (NumberFormatException | ExpressionException e) {
                    throw new SuggestInputParseException(input, "%<percent>");
                }
            case '=':
                try {
                    Expression exp = Expression.compile(input.substring(1), "x", "y", "z");
                    WorldEditExpressionEnvironment env = new WorldEditExpressionEnvironment(
                            Request.request().getEditSession(), Vector.ONE, Vector.ZERO);
                    exp.setEnvironment(env);
                    return new ExpressionMask(exp);
                } catch (ExpressionException e) {
                    throw new SuggestInputParseException(input, "=<expression>");
                }

            case '!':
                if (input.length() > 1) {
                    return Masks.negate(getBlockMaskComponent(masks, input.substring(1), context));
                }
                throw new SuggestInputParseException(input, "!<mask>");
            default:
                for (CustomMask mask : customMasks) {
                    if (mask.accepts(input)) {
                        try {
                            Constructor<? extends CustomMask> constructor = mask.getClass().getDeclaredConstructor(List.class, String.class, ParserContext.class);
                            return constructor.newInstance(masks, input, context);
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                }
                ParserContext tempContext = new ParserContext(context);
                tempContext.setRestricted(false);
                tempContext.setPreferringWildcard(true);
                return new BlockMask(extent, worldEdit.getBlockFactory().parseFromListInput(input, tempContext));
        }
    }

    public static Class<?> inject() {
        return DefaultMaskParser.class;
    }
}