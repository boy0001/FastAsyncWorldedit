package com.sk89q.worldedit.extension.factory;

import com.boydti.fawe.command.FaweParser;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.util.StringMan;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.command.MaskCommands;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.mask.MaskUnion;
import com.sk89q.worldedit.internal.command.ActorAuthorizer;
import com.sk89q.worldedit.internal.command.WorldEditBinding;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.command.Dispatcher;
import com.sk89q.worldedit.util.command.SimpleDispatcher;
import com.sk89q.worldedit.util.command.parametric.ParametricBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class DefaultMaskParser extends FaweParser<Mask> {
    private final Dispatcher dispatcher;
    private final Pattern INTERSECTION_PATTERN = Pattern.compile("[&|;]+(?![^\\[]*\\])");

    public DefaultMaskParser(WorldEdit worldEdit) {
        super(worldEdit);
        this.dispatcher = new SimpleDispatcher();
        this.register(new MaskCommands(worldEdit));
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void register(Object clazz) {
        ParametricBuilder builder = new ParametricBuilder();
        builder.setAuthorizer(new ActorAuthorizer());
        builder.addBinding(new WorldEditBinding(worldEdit));
        builder.registerMethodsAsCommands(dispatcher, clazz);
    }

    @Override
    public Mask parseFromInput(String input, ParserContext context) throws InputParseException {
        if (input.isEmpty()) return null;
        Extent extent = Request.request().getExtent();
        if (extent == null) extent = context.getExtent();
        HashSet<BaseBlock> blocks = new HashSet<BaseBlock>();
        List<Mask> intersection = new ArrayList<>();
        List<Mask> union = new ArrayList<>();
        final CommandLocals locals = new CommandLocals();
        Actor actor = context != null ? context.getActor() : null;
        if (actor != null) {
            locals.put(Actor.class, actor);
        }
        //
        try {
            List<Map.Entry<ParseEntry, List<String>>> parsed = parse(input);
            for (Map.Entry<ParseEntry, List<String>> entry : parsed) {
                ParseEntry pe = entry.getKey();
                String command = pe.input;
                Mask mask = null;
                if (command.isEmpty()) {
                    mask = parseFromInput(StringMan.join(entry.getValue(), ','), context);
                } else if (dispatcher.get(command) == null) {
                    // Legacy patterns
                    char char0 = command.charAt(0);
                    boolean charMask = input.length() > 1 && input.charAt(1) != '[';
                    if (charMask && input.charAt(0) == '=') {
                        return parseFromInput(char0 + "[" + input.substring(1) + "]", context);
                    }
                    if (mask == null) {
                        // Legacy syntax
                        if (charMask) {
                            switch (char0) {
                                case '\\': //
                                case '/': //
                                case '{': //
                                case '$': //
                                case '%': {
                                    command = command.substring(1);
                                    String value = command + ((entry.getValue().isEmpty()) ? "" : "[" + StringMan.join(entry.getValue(), "][") + "]");
                                    if (value.contains(":")) {
                                        if (value.charAt(0) == ':') value.replaceFirst(":", "");
                                        value = value.replaceAll(":", "][");
                                    }
                                    mask = parseFromInput(char0 + "[" + value + "]", context);
                                    break;
                                }
                                case '|':
                                case '~':
                                case '<':
                                case '>':
                                case '!':
                                    input = input.substring(input.indexOf(char0) + 1);
                                    mask = parseFromInput(char0 + "[" + input + "]", context);
                                    if (actor != null) {
                                        BBC.COMMAND_CLARIFYING_BRACKET.send(actor, char0 + "[" + input + "]");
                                    }
                                    return mask;
                            }
                        }
                        if (mask == null) {
                            try {
                                context.setPreferringWildcard(true);
                                context.setRestricted(false);
                                BaseBlock block = worldEdit.getBlockFactory().parseFromInput(command, context);
                                if (pe.and) {
                                    mask = new BlockMask(extent, block);
                                } else {
                                    blocks.add(block);
                                    continue;
                                }
                            } catch (NoMatchException e) {
                                throw new NoMatchException(e.getMessage() + " See: //masks");
                            }
                        }
                    }
                } else {
                    List<String> args = entry.getValue();
                    if (!args.isEmpty()) {
                        command += " " + StringMan.join(args, " ");
                    }
                    mask = (Mask) dispatcher.call(command, locals, new String[0]);
                }
                if (pe.and) { // &
                    intersection.add(mask);
                } else {
                    if (!intersection.isEmpty()) {
                        if (intersection.size() == 1) {
                            throw new InputParseException("Error, floating &");
                        }
                        union.add(new MaskIntersection(intersection));
                        intersection.clear();
                    }
                    union.add(mask);
                }
            }
        } catch (Throwable e) {
            throw new InputParseException(e.getMessage(), e);
        }
        if (!blocks.isEmpty()) {
            union.add(new BlockMask(extent, blocks));
        }
        if (!intersection.isEmpty()) {
            if (intersection.size() == 1) {
                throw new InputParseException("Error, floating &");
            }
            union.add(new MaskIntersection(intersection));
            intersection.clear();
        }
        if (union.isEmpty()) {
            return null;
        } else if (union.size() == 1) {
            return union.get(0);
        } else {
            return new MaskUnion(union);
        }
    }

    public static Class<?> inject() {
        return DefaultMaskParser.class;
    }
}
