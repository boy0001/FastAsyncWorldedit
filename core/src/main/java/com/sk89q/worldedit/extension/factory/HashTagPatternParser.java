package com.sk89q.worldedit.extension.factory;

import com.boydti.fawe.object.pattern.ExistingPattern;
import com.boydti.fawe.object.pattern.Linear3DBlockPattern;
import com.boydti.fawe.object.pattern.LinearBlockPattern;
import com.boydti.fawe.object.pattern.NoXPattern;
import com.boydti.fawe.object.pattern.NoYPattern;
import com.boydti.fawe.object.pattern.NoZPattern;
import com.boydti.fawe.object.pattern.RelativePattern;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.function.pattern.ClipboardPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.sk89q.worldedit.session.ClipboardHolder;
import java.util.ArrayList;

public class HashTagPatternParser extends InputParser<Pattern> {

    public HashTagPatternParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Override
    public Pattern parseFromInput(String input, ParserContext context) throws InputParseException {
        switch (input.toLowerCase().charAt(0)) {
            case '#': {
                switch (input) {
                    case "#*":
                    case "#existing": {
                        return new ExistingPattern(context.requireExtent());
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
                        case "#l":
                        case "#linear": {
                            ArrayList<Pattern> patterns = new ArrayList<>();
                            for (String token : rest.split(",")) {
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
                            for (String token : rest.split(",")) {
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
            default:
                String[] items = input.split(",");
                if (items.length == 1) {
                    return new BlockPattern(worldEdit.getBlockFactory().parseFromInput(items[0], context));
                }
                BlockFactory blockRegistry = worldEdit.getBlockFactory();
                RandomPattern randomPattern = new RandomPattern();
                for (String token : input.split(",")) {
                    Pattern pattern;
                    double chance;
                    // Parse special percentage syntax
                    if (token.matches("[0-9]+(\\.[0-9]*)?%.*")) {
                        String[] p = token.split("%");
                        if (p.length < 2) {
                            throw new InputParseException("Missing the pattern after the % symbol for '" + input + "'");
                        } else {
                            chance = Double.parseDouble(p[0]);
                            pattern = parseFromInput(p[1], context);
                        }
                    } else {
                        chance = 1;
                        pattern = parseFromInput(token, context);
                    }
                    randomPattern.add(pattern, chance);
                }
                return randomPattern;

        }
    }

    public static Class<?> inject() {
        return HashTagPatternParser.class;
    }
}