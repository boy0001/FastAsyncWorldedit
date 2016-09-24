package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import java.util.List;

public abstract class CustomMask implements Mask {
    public CustomMask(List<Mask> masks, String component, ParserContext context) {
        try {
            this.getClass(). getConstructor ( List.class, String.class, ParserContext.class ) ;
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Constructor for " + getClass() + " must be <init>(List.class, String.class, ParserContext.class)");
        }
    }

    public abstract String description();

    public abstract boolean accepts(String input);
}
