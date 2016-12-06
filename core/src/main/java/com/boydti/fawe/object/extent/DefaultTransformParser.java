package com.boydti.fawe.object.extent;

import com.boydti.fawe.object.mask.CustomMask;
import com.boydti.fawe.util.ExtentTraverser;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.DefaultMaskParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Parses mask input strings.
 */
public class DefaultTransformParser extends InputParser<ResettableExtent> {

    public DefaultTransformParser(WorldEdit worldEdit) {
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
    public ResettableExtent parseFromInput(String input, ParserContext context) throws InputParseException {
        Extent extent = new NullExtent();
        for (String component : input.split(" ")) {
            if (component.isEmpty()) {
                continue;
            }
            extent = getTansformComponent(extent, component, context);
        }
        if (extent instanceof ResettableExtent) {
            return (ResettableExtent) extent;
        }
        return null;
    }

    private ResettableExtent getTansformComponent(Extent parent, String component, ParserContext context) throws InputParseException {
        final char firstChar = component.charAt(0);
        switch (firstChar) {
            case '#':
                int colon = component.indexOf(':');
                if (colon != -1) {
                    String rest = component.substring(colon + 1);
                    switch (component.substring(0, colon).toLowerCase()) {
                        case "#pattern": {
                            Pattern pattern = worldEdit.getPatternFactory().parseFromInput(rest, context);
                            return new PatternTransform(parent, pattern);
                        }
                        case "#scale": {
                            try {
                                String[] split2 = component.split(":");
                                double x = Math.abs(Expression.compile(split2[1]).evaluate());
                                double y = Math.abs(Expression.compile(split2[2]).evaluate());
                                double z = Math.abs(Expression.compile(split2[3]).evaluate());
                                rest = rest.substring(Math.min(rest.length(), split2[1].length() + split2[2].length() + split2[3].length() + 3));
                                if (!rest.isEmpty()) {
                                    parent = parseFromInput(rest, context);
                                }
                                return new ScaleTransform(parent, x, y, z);
                            } catch (NumberFormatException | ExpressionException e) {
                                throw new InputParseException("The correct format is #scale:<dx>:<dy>:<dz>");
                            }
                        }
                        case "#rotate": {
                            try {
                                String[] split2 = component.split(":");
                                double x = (Expression.compile(split2[1]).evaluate());
                                double y = (Expression.compile(split2[2]).evaluate());
                                double z = (Expression.compile(split2[3]).evaluate());
                                rest = rest.substring(Math.min(rest.length(), split2[1].length() + split2[2].length() + split2[3].length() + 3));
                                if (!rest.isEmpty()) {
                                    parent = parseFromInput(rest, context);
                                }
                                ExtentTraverser traverser = new ExtentTraverser(parent).find(BlockTransformExtent.class);
                                BlockTransformExtent affine = (BlockTransformExtent) (traverser != null ? traverser.get() : null);
                                if (affine == null) {
                                    parent = affine = new BlockTransformExtent(parent, context.requireWorld().getWorldData().getBlockRegistry());
                                }
                                AffineTransform transform = (AffineTransform) affine.getTransform();
                                transform = transform.rotateX(x);
                                transform = transform.rotateY(y);
                                transform = transform.rotateZ(z);
                                affine.setTransform(transform);
                                return (ResettableExtent) parent;
                            } catch (NumberFormatException | ExpressionException e) {
                                throw new InputParseException("The correct format is #rotate:<dx>:<dy>:<dz>");
                            }
                        }
                        default:
                            throw new NoMatchException("Unrecognized transform '" + component + "'");
                    }
                } else {
                    switch (component) {
                        case "#clipboard": {
                            LocalSession session = context.requireSession();
                            if (session != null) {
                                try {
                                    ClipboardHolder holder = session.getClipboard();
                                    Clipboard clipboard = holder.getClipboard();
                                    return new ClipboardExtent(parent, clipboard, true);
                                } catch (EmptyClipboardException e) {
                                    throw new InputParseException("To use #clipboard, please first copy something to your clipboard");
                                }
                            } else {
                                throw new InputParseException("No session is available, so no clipboard is available");
                            }
                        }
                    }
                }
            default:
                throw new NoMatchException("Unrecognized transform '" + component + "'");
        }
    }

    public static Class<?> inject() {
        return DefaultMaskParser.class;
    }
}