package com.boydti.fawe.command;

import com.sk89q.worldedit.util.command.binding.Range;
import com.sk89q.worldedit.util.command.parametric.ArgumentStack;
import com.sk89q.worldedit.util.command.parametric.BindingBehavior;
import com.sk89q.worldedit.util.command.parametric.BindingHelper;
import com.sk89q.worldedit.util.command.parametric.BindingMatch;
import com.sk89q.worldedit.util.command.parametric.ParameterException;
import java.lang.annotation.Annotation;

public class LongBinding extends BindingHelper {
    @BindingMatch(type = { Long.class, long.class },
            behavior = BindingBehavior.CONSUMES,
            consumedCount = 1,
            provideModifiers = true)
    public Long getInteger(ArgumentStack context, Annotation[] modifiers) throws ParameterException {
        try {
            Long v = Long.parseLong(context.next());
            validate(v, modifiers);
            return v;

        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    private static void validate(long number, Annotation[] modifiers)
            throws ParameterException {
        for (Annotation modifier : modifiers) {
            if (modifier instanceof Range) {
                Range range = (Range) modifier;
                if (number < range.min()) {
                    throw new ParameterException(
                            String.format(
                                    "A valid value is greater than or equal to %s " +
                                            "(you entered %s)", range.min(), number));
                } else if (number > range.max()) {
                    throw new ParameterException(
                            String.format(
                                    "A valid value is less than or equal to %s " +
                                            "(you entered %s)", range.max(), number));
                }
            }
        }
    }
}
