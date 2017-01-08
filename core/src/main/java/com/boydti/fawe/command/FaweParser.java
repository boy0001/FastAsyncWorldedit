package com.boydti.fawe.command;

import com.boydti.fawe.util.StringMan;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.internal.registry.InputParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class FaweParser<T> extends InputParser<T> {
    protected FaweParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    public List<String> split(String input, char delim) {
        List<String> result = new ArrayList<String>();
        int start = 0;
        int bracket = 0;
        boolean inQuotes = false;
        for (int current = 0; current < input.length(); current++) {
            char currentChar = input.charAt(current);
            boolean atLastChar = (current == input.length() - 1);
            if (!atLastChar && (bracket > 0 || (currentChar == '{' && ++bracket > 0) || (current == '}' && --bracket <= 0))) continue;
            if (currentChar == '\"') inQuotes = !inQuotes; // toggle state
            if(atLastChar) result.add(input.substring(start));
            else if (currentChar == delim && !inQuotes) {
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

    public T catchSuggestion(String currentInput, String nextInput, ParserContext context) throws InputParseException {
        try {
            return parseFromInput(nextInput, context);
        } catch (SuggestInputParseException e) {
            e.prepend(currentInput.substring(0, currentInput.length() - nextInput.length()));
            throw e;
        }
    }

    public List<String> suggestRemaining(String input, String... expected) throws InputParseException {
        List<String> remainder = split(input, ':');
        int len = remainder.size();
        if (len != expected.length - 1) {
            if (len <= expected.length - 1 && len != 0) {
                if (remainder.get(len - 1).endsWith(":")) {
                    throw new SuggestInputParseException(null, StringMan.join(expected, ":"));
                }
                throw new SuggestInputParseException(null, expected[0] + ":" + input + ":" + StringMan.join(Arrays.copyOfRange(expected, len + 1, 3), ":"));
            } else {
                throw new SuggestInputParseException(null, StringMan.join(expected, ":"));
            }
        }
        return remainder;
    }
}
