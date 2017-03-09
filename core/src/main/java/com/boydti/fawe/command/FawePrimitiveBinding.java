package com.boydti.fawe.command;

import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.internal.expression.runtime.EvaluationException;
import com.sk89q.worldedit.util.command.binding.Range;
import com.sk89q.worldedit.util.command.binding.Text;
import com.sk89q.worldedit.util.command.binding.Validate;
import com.sk89q.worldedit.util.command.parametric.ArgumentStack;
import com.sk89q.worldedit.util.command.parametric.BindingBehavior;
import com.sk89q.worldedit.util.command.parametric.BindingHelper;
import com.sk89q.worldedit.util.command.parametric.BindingMatch;
import com.sk89q.worldedit.util.command.parametric.ParameterException;
import java.lang.annotation.Annotation;
import javax.annotation.Nullable;

public class FawePrimitiveBinding extends BindingHelper {
    @BindingMatch(type = { Long.class, long.class },
            behavior = BindingBehavior.CONSUMES,
            consumedCount = 1,
            provideModifiers = true)
    public Long getLong(ArgumentStack context, Annotation[] modifiers) throws ParameterException {
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

    /**
     * Gets a type from a {@link ArgumentStack}.
     *
     * @param context the context
     * @param text the text annotation
     * @param modifiers a list of modifiers
     * @return the requested type
     * @throws ParameterException on error
     */
    @BindingMatch(classifier = Text.class,
            type = String.class,
            behavior = BindingBehavior.CONSUMES,
            consumedCount = -1,
            provideModifiers = true)
    public String getText(ArgumentStack context, Text text, Annotation[] modifiers)
            throws ParameterException {
        String v = context.remaining();
        validate(v, modifiers);
        return v;
    }

    /**
     * Gets a type from a {@link ArgumentStack}.
     *
     * @param context the context
     * @param modifiers a list of modifiers
     * @return the requested type
     * @throws ParameterException on error
     */
    @BindingMatch(type = String.class,
            behavior = BindingBehavior.CONSUMES,
            consumedCount = 1,
            provideModifiers = true)
    public String getString(ArgumentStack context, Annotation[] modifiers)
            throws ParameterException {
        String v = context.next();
        validate(v, modifiers);
        return v;
    }

    /**
     * Gets a type from a {@link ArgumentStack}.
     *
     * @param context the context
     * @return the requested type
     * @throws ParameterException on error
     */
    @BindingMatch(type = { Boolean.class, boolean.class },
            behavior = BindingBehavior.CONSUMES,
            consumedCount = 1)
    public Boolean getBoolean(ArgumentStack context) throws ParameterException {
        return context.nextBoolean();
    }

    /**
     * Try to parse numeric input as either a number or a mathematical expression.
     *
     * @param input input
     * @return a number
     * @throws ParameterException thrown on parse error
     */
    public static @Nullable
    Double parseNumericInput(@Nullable String input) throws ParameterException {
        if (input == null) {
            return null;
        }
        System.out.println("PARSE " + input);

        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e1) {
            System.out.println("NUMBER FORMAT EXCEPTION " + e1);
            try {
                Expression expression = Expression.compile(input);
                return expression.evaluate();
            } catch (EvaluationException e) {
                throw new ParameterException(String.format(
                        "Expected '%s' to be a valid number (or a valid mathematical expression)", input));
            } catch (ExpressionException e) {
                throw new ParameterException(String.format(
                        "Expected '%s' to be a number or valid math expression (error: %s)", input, e.getMessage()));
            }
        }
    }

    /**
     * Gets a type from a {@link ArgumentStack}.
     *
     * @param context the context
     * @param modifiers a list of modifiers
     * @return the requested type
     * @throws ParameterException on error
     */
    @BindingMatch(type = { Integer.class, int.class },
            behavior = BindingBehavior.CONSUMES,
            consumedCount = 1,
            provideModifiers = true)
    public Integer getInteger(ArgumentStack context, Annotation[] modifiers) throws ParameterException {
        Double v = parseNumericInput(context.next());
        if (v != null) {
            int intValue = v.intValue();
            validate(intValue, modifiers);
            return intValue;
        } else {
            return null;
        }
    }

    /**
     * Gets a type from a {@link ArgumentStack}.
     *
     * @param context the context
     * @param modifiers a list of modifiers
     * @return the requested type
     * @throws ParameterException on error
     */
    @BindingMatch(type = { Short.class, short.class },
            behavior = BindingBehavior.CONSUMES,
            consumedCount = 1,
            provideModifiers = true)
    public Short getShort(ArgumentStack context, Annotation[] modifiers) throws ParameterException {
        Integer v = getInteger(context, modifiers);
        if (v != null) {
            return v.shortValue();
        }
        return null;
    }

    /**
     * Gets a type from a {@link ArgumentStack}.
     *
     * @param context the context
     * @param modifiers a list of modifiers
     * @return the requested type
     * @throws ParameterException on error
     */
    @BindingMatch(type = { Double.class, double.class },
            behavior = BindingBehavior.CONSUMES,
            consumedCount = 1,
            provideModifiers = true)
    public Double getDouble(ArgumentStack context, Annotation[] modifiers) throws ParameterException {
        Double v = parseNumericInput(context.next());
        if (v != null) {
            validate(v, modifiers);
            return v;
        } else {
            return null;
        }
    }

    /**
     * Gets a type from a {@link ArgumentStack}.
     *
     * @param context the context
     * @param modifiers a list of modifiers
     * @return the requested type
     * @throws ParameterException on error
     */
    @BindingMatch(type = { Float.class, float.class },
            behavior = BindingBehavior.CONSUMES,
            consumedCount = 1,
            provideModifiers = true)
    public Float getFloat(ArgumentStack context, Annotation[] modifiers) throws ParameterException {
        Double v = getDouble(context, modifiers);
        if (v != null) {
            return v.floatValue();
        }
        return null;
    }

    /**
     * Validate a number value using relevant modifiers.
     *
     * @param number the number
     * @param modifiers the list of modifiers to scan
     * @throws ParameterException on a validation error
     */
    private static void validate(double number, Annotation[] modifiers)
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

    /**
     * Validate a number value using relevant modifiers.
     *
     * @param number the number
     * @param modifiers the list of modifiers to scan
     * @throws ParameterException on a validation error
     */
    private static void validate(int number, Annotation[] modifiers)
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

    /**
     * Validate a string value using relevant modifiers.
     *
     * @param string the string
     * @param modifiers the list of modifiers to scan
     * @throws ParameterException on a validation error
     */
    private static void validate(String string, Annotation[] modifiers)
            throws ParameterException {
        if (string == null) {
            return;
        }

        for (Annotation modifier : modifiers) {
            if (modifier instanceof Validate) {
                Validate validate = (Validate) modifier;

                if (!validate.regex().isEmpty()) {
                    if (!string.matches(validate.regex())) {
                        throw new ParameterException(
                                String.format(
                                        "The given text doesn't match the right " +
                                                "format (technically speaking, the 'format' is %s)",
                                        validate.regex()));
                    }
                }
            }
        }
    }
}
