package com.sk89q.worldedit.extension.factory;

import com.boydti.fawe.command.SuggestInputParseException;
import com.boydti.fawe.object.pattern.*;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.StringMan;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.function.pattern.ClipboardPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.sk89q.worldedit.regions.shape.WorldEditExpressionEnvironment;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HashTagPatternParser extends InputParser<Pattern>{

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

    private List<String> split(String input, char delim) {
        List<String> result = new ArrayList<String>();
        int start = 0;
        boolean inQuotes = false;
        for (int current = 0; current < input.length(); current++) {
            if (input.charAt(current) == '\"') inQuotes = !inQuotes; // toggle state
            boolean atLastChar = (current == input.length() - 1);
            if(atLastChar) result.add(input.substring(start));
            else if (input.charAt(current) == delim && !inQuotes) {
                String toAdd = input.substring(start, current);
                if (toAdd.startsWith("\"")) {
                    toAdd = toAdd.substring(1, toAdd.length() - 1);
                }
                result.add(toAdd);
                start = current + 1;
            }
        }
        return result;
    }

    private Pattern catchSuggestion(String currentInput, String nextInput, ParserContext context) throws InputParseException{
        try {
            return parseFromInput(nextInput, context);
        } catch (SuggestInputParseException e) {
            e.prepend(currentInput.substring(0, currentInput.length() - nextInput.length()));
            throw e;
        }
    }

    public List<String> handleRemainder(String input, String... expected) throws InputParseException {
        List<String> remainder = split(input, ':');
        int len = remainder.size();
        if (len != expected.length - 1) {
            if (len <= expected.length - 1 && len != 0) {
                if (remainder.get(len - 1).endsWith(":")) {
                    throw new SuggestInputParseException(null, ALL_PATTERNS).prepend(expected[0] + ":" + input);
                }
                throw new SuggestInputParseException(null, expected[0] + ":" + input + ":" + StringMan.join(Arrays.copyOfRange(expected, len + 1, 3), ":"));
            } else {
                throw new SuggestInputParseException(null, StringMan.join(expected, ":"));
            }
        }
        return remainder;
    }

    @Override
    public Pattern parseFromInput(String input, ParserContext context) throws InputParseException {
        if (input.isEmpty()) {
            throw new SuggestInputParseException(input, ALL_PATTERNS);
        }
        switch (input.toLowerCase().charAt(0)) {
            case '#': {
                switch (input) {
                    case "#*":
                    case "#existing": {
                        return new ExistingPattern(Request.request().getEditSession());
                    }
                    case "#fullcopy": {
                        LocalSession session = context.requireSession();
                        if (session != null) {
                            try {
                                ClipboardHolder holder = session.getClipboard();
                                Clipboard clipboard = holder.getClipboard();
                                return new FullClipboardPattern(Request.request().getEditSession(), clipboard);
                            } catch (EmptyClipboardException e) {
                                throw new InputParseException("To use #fullcopy, please first copy something to your clipboard");
                            }
                        } else {
                            throw new InputParseException("No session is available, so no clipboard is available");
                        }
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
                }
                String[] split2 = input.split(":");
                if (split2.length > 1 || input.endsWith(":")) {
                    String rest = input.substring(split2[0].length() + 1);
                    switch (split2[0].toLowerCase()) {
                        case "#id": {
                            return new IdPattern(Request.request().getEditSession(), catchSuggestion(input, rest, context));
                        }
                        case "#data": {
                            return new DataPattern(Request.request().getEditSession(), catchSuggestion(input, rest, context));
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
                            List<String> split3 = handleRemainder(rest, "#mask", "<mask>", "<pattern-if>", "<pattern-else>");
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
                                List<String> split3 = handleRemainder(rest, "#offset", "<dx>", "<dy>", "<dz>", "<pattern>");
                                int x = (int) Math.abs(Expression.compile(split3.get(0)).evaluate());
                                int y = (int) Math.abs(Expression.compile(split3.get(1)).evaluate());
                                int z = (int) Math.abs(Expression.compile(split3.get(2)).evaluate());
                                rest = StringMan.join(split3.subList(3, split3.size() - 1), ":");
                                Pattern pattern = catchSuggestion(input, rest, context);
                                return new OffsetPattern(pattern, x, y, z);
                            } catch (NumberFormatException | ExpressionException | IndexOutOfBoundsException e) {
                                throw new SuggestInputParseException(null, "#offset:<dx>:<dy>:<dz>:<pattern>");
                            }
                        case "#surfacespread": {
                            try {
                                List<String> split3 = handleRemainder(rest, "#surfacespread", "<dx>", "<dy>", "<dz>", "<pattern>");
                                int x = (int) Math.abs(Expression.compile(split3.get(0)).evaluate());
                                int y = (int) Math.abs(Expression.compile(split3.get(1)).evaluate());
                                int z = (int) Math.abs(Expression.compile(split3.get(2)).evaluate());
                                rest = StringMan.join(split3.subList(3, split3.size() - 1), ":");
                                Pattern pattern = catchSuggestion(input, rest, context);
                                return new SurfaceRandomOffsetPattern(pattern, x, y, z);
                            } catch (NumberFormatException | ExpressionException | IndexOutOfBoundsException e) {
                                throw new SuggestInputParseException(null, "#surfacespread:<dx>:<dy>:<dz>:<pattern>");
                            }
                        }
                        case "#solidspread": {
                            try {
                                List<String> split3 = handleRemainder(rest, "#solidspread", "<dx>", "<dy>", "<dz>", "<pattern>");
                                int x = (int) Math.abs(Expression.compile(split3.get(0)).evaluate());
                                int y = (int) Math.abs(Expression.compile(split3.get(1)).evaluate());
                                int z = (int) Math.abs(Expression.compile(split3.get(2)).evaluate());
                                rest = StringMan.join(split3.subList(3, split3.size() - 1), ":");
                                Pattern pattern = catchSuggestion(input, rest, context);
                                return new SolidRandomOffsetPattern(pattern, x, y, z);
                            } catch (NumberFormatException | ExpressionException | IndexOutOfBoundsException e) {
                                throw new SuggestInputParseException(null, "#solidspread:<dx>:<dy>:<dz>:<pattern>");
                            }
                        }
                        case "#randomoffset":
                        case "#spread": {
                            try {
                                List<String> split3 = handleRemainder(rest, "#spread", "<dx>", "<dy>", "<dz>", "<pattern>");
                                int x = (int) Math.abs(Expression.compile(split3.get(0)).evaluate());
                                int y = (int) Math.abs(Expression.compile(split3.get(1)).evaluate());
                                int z = (int) Math.abs(Expression.compile(split3.get(2)).evaluate());
                                rest = StringMan.join(split3.subList(3, split3.size() - 1), ":");
                                Pattern pattern = catchSuggestion(input, rest, context);
                                return new RandomOffsetPattern(pattern, x, y, z);
                            } catch (NumberFormatException | ExpressionException | IndexOutOfBoundsException e) {
                                throw new SuggestInputParseException(null, "#spread:<dx>:<dy>:<dz>:<pattern>");
                            }
                        }
                        case "#l":
                        case "#linear": {
                            ArrayList<Pattern> patterns = new ArrayList<>();
                            for (String token : split(rest, ',')) {
                                patterns.add(catchSuggestion(input, token, context));
                            }
                            if (patterns.isEmpty()) {
                                throw new SuggestInputParseException(null, ALL_PATTERNS).prepend(input);
                            }
                            return new LinearBlockPattern(patterns.toArray(new Pattern[patterns.size()]));
                        }
                        case "#l3d":
                        case "#linear3d": {
                            ArrayList<Pattern> patterns = new ArrayList<>();
                            for (String token : split(rest, ',')) {
                                patterns.add(catchSuggestion(input, token, context));
                            }
                            if (patterns.isEmpty()) {
                                throw new SuggestInputParseException(null, ALL_PATTERNS).prepend(input);
                            }
                            return new Linear3DBlockPattern(patterns.toArray(new Pattern[patterns.size()]));
                        }
                        default:
                            throw new SuggestInputParseException(input, DELEGATE_PATTERNS);
                    }
                }
                throw new SuggestInputParseException(input, MainUtil.joinArrayGeneric(SIMPLE_PATTERNS, DELEGATE_PATTERNS));
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
                List<String> items = split(input, ',');
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