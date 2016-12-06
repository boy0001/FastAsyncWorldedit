package com.sk89q.worldedit.extension.factory;

import com.boydti.fawe.object.pattern.*;
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
import java.util.ArrayList;
import java.util.List;

public class HashTagPatternParser extends InputParser<Pattern> {

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

    @Override
    public Pattern parseFromInput(String input, ParserContext context) throws InputParseException {
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
                if (split2.length > 1) {
                    String rest = input.substring(split2[0].length() + 1);
                    switch (split2[0].toLowerCase()) {
                        case "#id": {
                            return new IdPattern(Request.request().getEditSession(), parseFromInput(rest, context));
                        }
                        case "#data": {
                            return new DataPattern(Request.request().getEditSession(), parseFromInput(rest, context));
                        }
                        case "#~":
                        case "#r":
                        case "#relative":
                        case "#rel": {
                            return new RelativePattern(parseFromInput(rest, context));
                        }
                        case "#!x":
                        case "#nx":
                        case "#nox": {
                            return new NoXPattern(parseFromInput(rest, context));
                        }
                        case "#!y":
                        case "#ny":
                        case "#noy": {
                            return new NoYPattern(parseFromInput(rest, context));
                        }
                        case "#!z":
                        case "#nz":
                        case "#noz": {
                            return new NoZPattern(parseFromInput(rest, context));
                        }
                        case "#mask": {
                            List<String> split3 = split(rest, ':');
                            if (split3.size() != 3) {
                                throw new InputParseException("The correct format is #mask:<mask>:<pattern-if>:<pattern-else>");
                            }
                            Pattern primary = parseFromInput(split3.get(1), context);
                            Pattern secondary = parseFromInput(split3.get(2), context);
                            PatternExtent extent = new PatternExtent(primary);
                            Request request = Request.request();
                            request.setExtent(extent);
                            request.setSession(context.getSession());
                            request.setWorld(context.getWorld());
                            context.setExtent(extent);
                            MaskFactory factory = worldEdit.getMaskFactory();
                            Mask mask = factory.parseFromInput(split3.get(0), context);
                            if (mask == null | primary == null || secondary == null) {
                                throw new InputParseException("The correct format is #mask:<mask>;<pattern-if>;<pattern-else> (null provided)");
                            }
                            return new MaskedPattern(mask, extent, secondary);
                        }
                        case "#offset":
                            try {
                                int x = (int) Math.abs(Expression.compile(split2[1]).evaluate());
                                int y = (int) Math.abs(Expression.compile(split2[2]).evaluate());
                                int z = (int) Math.abs(Expression.compile(split2[3]).evaluate());
                                rest = rest.substring(split2[1].length() + split2[2].length() + split2[3].length() + 3);
                                Pattern pattern = parseFromInput(rest, context);
                                return new OffsetPattern(pattern, x, y, z);
                            } catch (NumberFormatException | ExpressionException e) {
                                throw new InputParseException("The correct format is #offset:<dx>:<dy>:<dz>:<pattern>");
                            }
                        case "#surfacespread": {
                            try {
                                int x = (int) Math.abs(Expression.compile(split2[1]).evaluate());
                                int y = (int) Math.abs(Expression.compile(split2[2]).evaluate());
                                int z = (int) Math.abs(Expression.compile(split2[3]).evaluate());
                                rest = rest.substring(split2[1].length() + split2[2].length() + split2[3].length() + 3);
                                Pattern pattern = parseFromInput(rest, context);
                                return new SurfaceRandomOffsetPattern(pattern, x, y, z);
                            } catch (NumberFormatException | ExpressionException e) {
                                throw new InputParseException("The correct format is #spread:<dx>:<dy>:<dz>:<pattern>");
                            }
                        }
                        case "#solidspread": {
                            try {
                                int x = (int) Math.abs(Expression.compile(split2[1]).evaluate());
                                int y = (int) Math.abs(Expression.compile(split2[2]).evaluate());
                                int z = (int) Math.abs(Expression.compile(split2[3]).evaluate());
                                rest = rest.substring(split2[1].length() + split2[2].length() + split2[3].length() + 3);
                                Pattern pattern = parseFromInput(rest, context);
                                return new SolidRandomOffsetPattern(pattern, x, y, z);
                            } catch (NumberFormatException | ExpressionException e) {
                                throw new InputParseException("The correct format is #spread:<dx>:<dy>:<dz>:<pattern>");
                            }
                        }
                        case "#randomoffset":
                        case "#spread": {
                            try {
                                int x = (int) Math.abs(Expression.compile(split2[1]).evaluate());
                                int y = (int) Math.abs(Expression.compile(split2[2]).evaluate());
                                int z = (int) Math.abs(Expression.compile(split2[3]).evaluate());
                                rest = rest.substring(split2[1].length() + split2[2].length() + split2[3].length() + 3);
                                Pattern pattern = parseFromInput(rest, context);
                                return new RandomOffsetPattern(pattern, x, y, z);
                            } catch (NumberFormatException | ExpressionException e) {
                                throw new InputParseException("The correct format is #spread:<dx>:<dy>:<dz>:<pattern>");
                            }
                        }
                        case "#l":
                        case "#linear": {
                            ArrayList<Pattern> patterns = new ArrayList<>();
                            for (String token : split(rest, ',')) {
                                patterns.add(parseFromInput(token, context));
                            }
                            if (patterns.isEmpty()) {
                                throw new InputParseException("No blocks provided for linear pattern e.g. [stone,wood");
                            }
                            return new LinearBlockPattern(patterns.toArray(new Pattern[patterns.size()]));
                        }
                        case "#l3d":
                        case "#linear3D": {
                            ArrayList<Pattern> patterns = new ArrayList<>();
                            for (String token : split(rest, ',')) {
                                patterns.add(parseFromInput(token, context));
                            }
                            if (patterns.isEmpty()) {
                                throw new InputParseException("No blocks provided for linear pattern e.g. [stone,wood");
                            }
                            return new Linear3DBlockPattern(patterns.toArray(new Pattern[patterns.size()]));
                        }
                    }
                }
                throw new InputParseException("Invalid, see: https://github.com/boy0001/FastAsyncWorldedit/wiki/WorldEdit-and-FAWE-patterns");
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
                    throw new InputParseException("Invalid expression: " + e.getMessage());
                }
            }
            default:
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
                                pattern = parseFromInput(p[1], context);
                            }
                        } else {
                            chance = 1;
                            pattern = parseFromInput(token, context);
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