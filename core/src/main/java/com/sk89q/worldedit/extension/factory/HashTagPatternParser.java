package com.sk89q.worldedit.extension.factory;

import com.boydti.fawe.object.pattern.ExistingPattern;
import com.boydti.fawe.object.pattern.LinearBlockPattern;
import com.boydti.fawe.object.pattern.NoXPattern;
import com.boydti.fawe.object.pattern.NoYPattern;
import com.boydti.fawe.object.pattern.NoZPattern;
import com.boydti.fawe.object.pattern.RelativePattern;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
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
                    switch (split2[0]) {
                        case "#relative":
                        case "#rel": {
                            String rest = input.substring(5);
                            return new RelativePattern(parseFromInput(rest, context));
                        }
                        case "#nox": {
                            String rest = input.substring(5);
                            return new NoXPattern(parseFromInput(rest, context));
                        }
                        case "#noy": {
                            String rest = input.substring(5);
                            return new NoYPattern(parseFromInput(rest, context));
                        }
                        case "#noz": {
                            String rest = input.substring(5);
                            return new NoZPattern(parseFromInput(rest, context));
                        }
                    }
                }
                throw new InputParseException("Invalid, see: https://github.com/boy0001/FastAsyncWorldedit/wiki/WorldEdit-and-FAWE-patterns");
            }
            case '[': {
                ArrayList<BaseBlock> blocks = new ArrayList<>();
                for (String token : input.substring(1).split(",")) {
                    BlockFactory blockRegistry = worldEdit.getBlockFactory();
                    BaseBlock block = blockRegistry.parseFromInput(token, context);
                    blocks.add(block);
                }
                if (blocks.isEmpty()) {
                    throw new InputParseException("No blocks provided for linear pattern e.g. [stone,wood");
                }
                return new LinearBlockPattern(blocks.toArray(new BaseBlock[blocks.size()]));
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