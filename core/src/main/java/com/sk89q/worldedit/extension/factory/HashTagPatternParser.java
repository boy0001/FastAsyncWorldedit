package com.sk89q.worldedit.extension.factory;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.command.FaweParser;
import com.boydti.fawe.command.SuggestInputParseException;
import com.boydti.fawe.object.DataAngleMask;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.pattern.AngleColorPattern;
import com.boydti.fawe.object.pattern.AverageColorPattern;
import com.boydti.fawe.object.pattern.BiomePattern;
import com.boydti.fawe.object.pattern.BufferedPattern;
import com.boydti.fawe.object.pattern.DataPattern;
import com.boydti.fawe.object.pattern.DesaturatePattern;
import com.boydti.fawe.object.pattern.ExistingPattern;
import com.boydti.fawe.object.pattern.ExpressionPattern;
import com.boydti.fawe.object.pattern.FullClipboardPattern;
import com.boydti.fawe.object.pattern.IdDataMaskPattern;
import com.boydti.fawe.object.pattern.IdPattern;
import com.boydti.fawe.object.pattern.Linear3DBlockPattern;
import com.boydti.fawe.object.pattern.LinearBlockPattern;
import com.boydti.fawe.object.pattern.MaskedPattern;
import com.boydti.fawe.object.pattern.NoXPattern;
import com.boydti.fawe.object.pattern.NoYPattern;
import com.boydti.fawe.object.pattern.NoZPattern;
import com.boydti.fawe.object.pattern.OffsetPattern;
import com.boydti.fawe.object.pattern.PatternExtent;
import com.boydti.fawe.object.pattern.RandomFullClipboardPattern;
import com.boydti.fawe.object.pattern.RandomOffsetPattern;
import com.boydti.fawe.object.pattern.RelativePattern;
import com.boydti.fawe.object.pattern.SaturatePattern;
import com.boydti.fawe.object.pattern.ShadePattern;
import com.boydti.fawe.object.pattern.SolidRandomOffsetPattern;
import com.boydti.fawe.object.pattern.SurfaceRandomOffsetPattern;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.util.TextureUtil;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.function.pattern.ClipboardPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.regions.shape.WorldEditExpressionEnvironment;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.biome.Biomes;
import com.sk89q.worldedit.world.registry.BiomeRegistry;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;

public class HashTagPatternParser extends FaweParser<Pattern> {

    public static final String[] EXPRESSION_PATTERN = new String[] { "=<expression>" };

    public static final String[] BLOCK_PATTERN = new String[] { "<block>" };

    public static final String[] SIMPLE_PATTERNS = new String[] {
            "#existing", "#fullcopy", "#clipboard",
    };

    public static final String[] DELEGATE_PATTERNS = new String[] {
            "#linear3d:", "#linear:", "#spread:", "#solidspread:", "#surfacespread:", "#offset:", "#mask:", "#!x:", "#!y:", "#!z:", "#relative:", "#id:", "#data:",
    };

    public static final String[] MISC_PATTERNS = new String[] {
            "hand", "pos1",
    };

    public static final String[] ALL_PATTERNS = MainUtil.joinArrayGeneric(BLOCK_PATTERN, SIMPLE_PATTERNS, DELEGATE_PATTERNS, MISC_PATTERNS);

    public HashTagPatternParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Override
    public Pattern parseFromInput(String input, ParserContext context) throws InputParseException {
        if (input.isEmpty()) {
            throw new SuggestInputParseException(input, ALL_PATTERNS);
        }
        List<String> items = StringMan.split(input, ',');
        if (items.size() == 1) {
            return parseSinglePatternFromInput(items.get(0), context);
        }
        RandomPattern randomPattern = new RandomPattern();
        try {
            for (String token : items) {
                Pattern pattern;
                double chance;
                // Parse special percentage syntax
                if (token.matches("[0-9]+(\\.[0-9]*)?%.*")) {
                    String[] p = token.split("%");
                    if (p.length < 2) {
                        throw new InputParseException("Missing the pattern after the % symbol for '" + input + "'");
                    } else {
                        chance = Expression.compile(p[0]).evaluate();
                        pattern = catchSuggestion(input, p[1], context);
                    }
                } else {
                    chance = 1;
                    pattern = catchSuggestion(input, token, context);
                }
                randomPattern.add(pattern, chance);
            }
        } catch (NumberFormatException | ExpressionException e) {
            throw new InputParseException("Invalid, see: https://github.com/boy0001/FastAsyncWorldedit/wiki/WorldEdit-and-FAWE-patterns");
        }
        return randomPattern;
    }

    public Pattern parseSinglePatternFromInput(String input, ParserContext context) throws InputParseException {
        if (input.isEmpty()) {
            throw new SuggestInputParseException(input, ALL_PATTERNS);
        }
        switch (input.toLowerCase().charAt(0)) {
            case '#': {
                String[] split2 = input.split(":");
                String rest = split2.length > 1 ? input.substring(split2[0].length() + 1) : "";
                String arg = split2[0].toLowerCase();
                switch (arg) {
                    case "#*":
                    case "#existing": {
                        return new ExistingPattern(Request.request().getExtent());
                    }
                    case "#clipboard":
                    case "#copy": {
                        LocalSession session = context.requireSession();
                        if (session != null) {
                            try {
                                ClipboardHolder holder = session.getClipboard();
                                Clipboard clipboard = holder.getClipboard();
                                return new ClipboardPattern(clipboard);
                            } catch (EmptyClipboardException e) {
                                throw new InputParseException("To use #clipboard, please first copy something to your clipboard");
                            }
                        } else {
                            throw new InputParseException("No session is available, so no clipboard is available");
                        }
                    }
                    case "#color": {
                        if (split2.length > 1) {
                            Color color = Color.web(split2[1]);
                            java.awt.Color awtColor = new java.awt.Color((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue(), (float) color.getOpacity());
                            BaseBlock block = Fawe.get().getTextureUtil().getNearestBlock(awtColor.getRGB());
                            return new BlockPattern(block);
                        } else {
                            throw new InputParseException("#color:<hex>");
                        }
                    }
                    case "#anglecolor": {
                        TextureUtil util = Fawe.get().getCachedTextureUtil(split2.length < 2 ? true : Boolean.parseBoolean(split2[1]), 0, split2.length < 3 ? 100 : Integer.parseInt(split2[2]));
                        return new AngleColorPattern(util, Request.request().getExtent());
                    }
                    case "#angledata": {
                        return new DataAngleMask(Request.request().getExtent());
                    }
                    case "#saturate":
                    case "#averagecolor": {
                        try {
                            TextureUtil util = Fawe.get().getCachedTextureUtil(split2.length < 3 ? true : Boolean.parseBoolean(split2[2]), 0, split2.length < 4 ? 100 : Integer.parseInt(split2[3]));
                            Color color = Color.web(split2[1]);
                            java.awt.Color awtColor = new java.awt.Color((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue(), (float) color.getOpacity());
                            if (arg.equals("#saturate"))
                                return new SaturatePattern(Request.request().getExtent(), util, awtColor.getRGB());
                            else return new AverageColorPattern(Request.request().getExtent(), util, awtColor.getRGB());
                        } catch (NumberFormatException | IndexOutOfBoundsException e) {
                            throw new SuggestInputParseException(null, "#" + arg + "[:<color>:<randomize=true>:<complexity=100>]");
                        }
                    }
                    case "#desaturate": {
                        try {
                            TextureUtil util = Fawe.get().getCachedTextureUtil(split2.length < 3 ? true : Boolean.parseBoolean(split2[2]), 0, split2.length < 4 ? 100 : Integer.parseInt(split2[3]));
                            double chance = split2.length < 2 ? 100 : Expression.compile(split2[1]).evaluate();
                            return new DesaturatePattern(Request.request().getExtent(), util, chance / 100d);
                        } catch (NumberFormatException | ExpressionException | IndexOutOfBoundsException e) {
                            throw new SuggestInputParseException(null, "#desaturate[:<percent=100>:<randomize=true>:<complexity=100>]");
                        }
                    }
                    case "#lighten":
                    case "#darken": {
                        TextureUtil util = Fawe.get().getCachedTextureUtil(split2.length < 2 ? true : Boolean.parseBoolean(split2[1]), 0, split2.length < 3 ? 100 : Integer.parseInt(split2[2]));
                        return new ShadePattern(Request.request().getExtent(), util, arg.equals("#darken"));
                    }
                    case "#fullcopy": {
                        LocalSession session = context.requireSession();
                        if (session != null) {
                            try {
                                if (split2.length > 1) {
                                    String location = split2[1];
                                    try {
                                        ClipboardHolder[] clipboards;
                                        switch (location.toLowerCase()) {
                                            case "#copy":
                                            case "#clipboard":
                                                ClipboardHolder clipboard = session.getExistingClipboard();
                                                if (clipboard == null) {
                                                    throw new InputParseException("To use #fullcopy, please first copy something to your clipboard");
                                                }
                                                clipboards = new ClipboardHolder[] {clipboard};
                                                break;
                                            default:
                                                clipboards = ClipboardFormat.SCHEMATIC.loadAllFromInput(context.getActor(), context.requireWorld().getWorldData(), location, true);
                                                break;
                                        }
                                        if (clipboards == null) {
                                            throw new InputParseException("#fullcopy:<source>");
                                        }
                                        boolean randomRotate = split2.length >= 3 && split2[2].equalsIgnoreCase("true");
                                        boolean randomFlip = split2.length >= 4 && split2[3].equalsIgnoreCase("true");
                                        return new RandomFullClipboardPattern(Request.request().getExtent(), context.requireWorld().getWorldData(), clipboards, randomRotate, randomFlip);

                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                                ClipboardHolder holder = session.getClipboard();
                                Clipboard clipboard = holder.getClipboard();
                                return new FullClipboardPattern(Request.request().getExtent(), clipboard);
                            } catch (EmptyClipboardException e) {
                                throw new InputParseException("To use #fullcopy, please first copy something to your clipboard");
                            }
                        } else {
                            throw new InputParseException("No session is available, so no clipboard is available");
                        }
                    }
                    case "#buffer": {
                        return new BufferedPattern(FawePlayer.wrap(context.requireActor()), catchSuggestion(input, rest, context));
                    }
                    case "#iddatamask": {
                        String[] split = rest.split(":", 1);
                        if (split.length != 2) {
                            throw new InputParseException("#iddatamask:<mask>:<pattern>");
                        }
                        int mask = Integer.parseInt(split[0]);
                        return new IdDataMaskPattern(Request.request().getExtent(), catchSuggestion(input, split[1], context), mask);
                    }
                    case "#id": {
                        return new IdPattern(Request.request().getExtent(), catchSuggestion(input, rest, context));
                    }
                    case "#data": {
                        return new DataPattern(Request.request().getExtent(), catchSuggestion(input, rest, context));
                    }
                    case "#biome": {
                        if (MathMan.isInteger(rest)) {
                            return new BiomePattern(Request.request().getExtent(), new BaseBiome(Integer.parseInt(rest)));
                        }
                        World world = context.requireWorld();
                        BiomeRegistry biomeRegistry = world.getWorldData().getBiomeRegistry();
                        List<BaseBiome> knownBiomes = biomeRegistry.getBiomes();
                        BaseBiome biome = Biomes.findBiomeByName(knownBiomes, rest, biomeRegistry);
                        return new BiomePattern(Request.request().getExtent(), biome);
                    }
                    case "#~":
                    case "#r":
                    case "#relative":
                    case "#rel": {
                        return new RelativePattern(catchSuggestion(input, rest, context));
                    }
                    case "#!x":
                    case "#nx":
                    case "#nox": {
                        return new NoXPattern(catchSuggestion(input, rest, context));
                    }
                    case "#!y":
                    case "#ny":
                    case "#noy": {
                        return new NoYPattern(catchSuggestion(input, rest, context));
                    }
                    case "#!z":
                    case "#nz":
                    case "#noz": {
                        return new NoZPattern(catchSuggestion(input, rest, context));
                    }
                    case "#mask": {
                        List<String> split3 = suggestRemaining(rest, "#mask", "<mask>", "<pattern-if>", "<pattern-else>");
                        Pattern primary = catchSuggestion(input, split3.get(1), context);
                        Pattern secondary = catchSuggestion(input, split3.get(2), context);
                        PatternExtent extent = new PatternExtent(primary);
                        Request request = Request.request();
                        request.setExtent(extent);
                        request.setSession(context.getSession());
                        request.setWorld(context.getWorld());
                        context.setExtent(extent);
                        MaskFactory factory = worldEdit.getMaskFactory();
                        Mask mask = factory.parseFromInput(split3.get(0), context);
                        if (mask == null | primary == null || secondary == null) {
                            throw new SuggestInputParseException(null, "#mask:<mask>:<pattern-if>:<pattern-else>");
                        }
                        return new MaskedPattern(mask, extent, secondary);
                    }
                    case "#offset":
                        try {
                            List<String> split3 = suggestRemaining(rest, "#offset", "<dx>", "<dy>", "<dz>", "<pattern>");
                            int x = (int) Expression.compile(split3.get(0)).evaluate();
                            int y = (int) Expression.compile(split3.get(1)).evaluate();
                            int z = (int) Expression.compile(split3.get(2)).evaluate();
                            rest = StringMan.join(split3.subList(3, split3.size()), ":");
                            Pattern pattern = catchSuggestion(input, rest, context);
                            return new OffsetPattern(pattern, x, y, z);
                        } catch (NumberFormatException | ExpressionException | IndexOutOfBoundsException e) {
                            throw new SuggestInputParseException(null, "#offset:<dx>:<dy>:<dz>:<pattern>");
                        }
                    case "#surfacespread": {
                        try {
                            List<String> split3 = suggestRemaining(rest, "#surfacespread", "<distance>", "<pattern>");
                            int dist = (int) Math.abs(Expression.compile(split3.get(0)).evaluate());
                            rest = StringMan.join(split3.subList(1, split3.size()), ":");
                            Pattern pattern = catchSuggestion(input, rest, context);
                            return new SurfaceRandomOffsetPattern(Request.request().getExtent(), pattern, dist);
                        } catch (NumberFormatException | ExpressionException | IndexOutOfBoundsException e) {
                            throw new SuggestInputParseException(null, "#surfacespread:<distance>:<pattern>");
                        }
                    }
                    case "#solidspread": {
                        try {
                            List<String> split3 = suggestRemaining(rest, "#solidspread", "<dx>", "<dy>", "<dz>", "<pattern>");
                            int x = (int) Math.abs(Expression.compile(split3.get(0)).evaluate());
                            int y = (int) Math.abs(Expression.compile(split3.get(1)).evaluate());
                            int z = (int) Math.abs(Expression.compile(split3.get(2)).evaluate());
                            rest = StringMan.join(split3.subList(3, split3.size()), ":");
                            Pattern pattern = catchSuggestion(input, rest, context);
                            return new SolidRandomOffsetPattern(pattern, x, y, z);
                        } catch (NumberFormatException | ExpressionException | IndexOutOfBoundsException e) {
                            throw new SuggestInputParseException(null, "#solidspread:<dx>:<dy>:<dz>:<pattern>");
                        }
                    }
                    case "#randomoffset":
                    case "#spread": {
                        try {
                            List<String> split3 = suggestRemaining(rest, "#spread", "<dx>", "<dy>", "<dz>", "<pattern>");
                            int x = (int) Math.abs(Expression.compile(split3.get(0)).evaluate());
                            int y = (int) Math.abs(Expression.compile(split3.get(1)).evaluate());
                            int z = (int) Math.abs(Expression.compile(split3.get(2)).evaluate());
                            rest = StringMan.join(split3.subList(3, split3.size()), ":");
                            Pattern pattern = catchSuggestion(input, rest, context);
                            return new RandomOffsetPattern(pattern, x, y, z);
                        } catch (NumberFormatException | ExpressionException | IndexOutOfBoundsException e) {
                            throw new SuggestInputParseException(null, "#spread:<dx>:<dy>:<dz>:<pattern>");
                        }
                    }
                    case "#l":
                    case "#linear": {
                        if (rest.startsWith("\"") && rest.endsWith("\"")) {
                            rest = rest.substring(1, rest.length() - 1);
                        }
                        ArrayList<Pattern> patterns = new ArrayList<>();
                        for (String token : StringMan.split(rest, ',')) {
                            patterns.add(catchSuggestion(input, token, context));
                        }
                        if (patterns.isEmpty()) {
                            throw new SuggestInputParseException(null, ALL_PATTERNS).prepend(input);
                        }
                        return new LinearBlockPattern(patterns.toArray(new Pattern[patterns.size()]));
                    }
                    case "#l3d":
                    case "#linear3d": {
                        if (rest.startsWith("\"") && rest.endsWith("\"")) {
                            rest = rest.substring(1, rest.length() - 1);
                        }
                        ArrayList<Pattern> patterns = new ArrayList<>();
                        for (String token : StringMan.split(rest, ',')) {
                            patterns.add(catchSuggestion(input, token, context));
                        }
                        if (patterns.isEmpty()) {
                            throw new SuggestInputParseException(null, ALL_PATTERNS).prepend(input);
                        }
                        return new Linear3DBlockPattern(patterns.toArray(new Pattern[patterns.size()]));
                    }
                    default:
                        throw new SuggestInputParseException(input, MainUtil.joinArrayGeneric(SIMPLE_PATTERNS, DELEGATE_PATTERNS));
                }
            }
            case '=': {
                try {
                    Expression exp = Expression.compile(input.substring(1), "x", "y", "z");
                    EditSession editSession = Request.request().getEditSession();
                    if (editSession == null) {
                        editSession = context.requireSession().createEditSession((Player) context.getActor());
                    }
                    WorldEditExpressionEnvironment env = new WorldEditExpressionEnvironment(
                            editSession, Vector.ONE, Vector.ZERO);
                    exp.setEnvironment(env);
                    return new ExpressionPattern(exp);
                } catch (ExpressionException e) {
                    throw new SuggestInputParseException("=http://wiki.sk89q.com/wiki/WorldEdit/Expression_syntax");
                }
            }
            default:
                switch (input) {
                    case "<dx>":
                    case "<dy>":
                    case "<dz>":
                        throw new SuggestInputParseException(input, "0", "-3", "7");
                    case "<pattern>":
                    case "<pattern-if>":
                    case "<pattern-else>":
                        throw new SuggestInputParseException(input, ALL_PATTERNS);
                    case "<block>":
                        throw new SuggestInputParseException(input, BundledBlockData.getInstance().getBlockNames());
                }
                List<String> items = StringMan.split(input, ',');
                if (items.size() == 1) {
                    return new BlockPattern(worldEdit.getBlockFactory().parseFromInput(items.get(0), context));
                }
                BlockFactory blockRegistry = worldEdit.getBlockFactory();
                RandomPattern randomPattern = new RandomPattern();
                try {
                    for (String token : items) {
                        Pattern pattern;
                        double chance;
                        // Parse special percentage syntax
                        if (token.matches("[0-9]+(\\.[0-9]*)?%.*")) {
                            String[] p = token.split("%");
                            if (p.length < 2) {
                                throw new InputParseException("Missing the pattern after the % symbol for '" + input + "'");
                            } else {
                                chance = Expression.compile(p[0]).evaluate();
                                pattern = catchSuggestion(input, p[1], context);
                            }
                        } else {
                            chance = 1;
                            pattern = catchSuggestion(input, token, context);
                        }
                        randomPattern.add(pattern, chance);
                    }
                } catch (NumberFormatException | ExpressionException e) {
                    throw new InputParseException("Invalid, see: https://github.com/boy0001/FastAsyncWorldedit/wiki/WorldEdit-and-FAWE-patterns");
                }
                return randomPattern;
        }
    }

    public static Class<?> inject() {
        return HashTagPatternParser.class;
    }
}