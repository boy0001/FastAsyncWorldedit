package com.boydti.fawe.command;

import com.boydti.fawe.util.StringMan;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.internal.registry.InputParser;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class FaweParser<T> extends InputParser<T> {
    protected FaweParser(WorldEdit worldEdit) {
        super(worldEdit);
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
        List<String> remainder = StringMan.split(input, ':');
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

    protected static class ParseEntry {
        public boolean and;
        public String input;

        public ParseEntry(String input, boolean type) {
            this.input = input;
            this.and = type;
        }

        @Override
        public String toString() {
            return input + " | " + and;
        }
    }

    public List<Map.Entry<ParseEntry, List<String>>> parse(String command) throws InputParseException {
        List<Map.Entry<ParseEntry, List<String>>> keys = new ArrayList<>();
        List<String> args = new ArrayList<>();
        int len = command.length();
        String current = null;
        int end = -1;
        for (int i = 0; i < len; i++) {
            boolean newEntry = i == 0;
            boolean prefix = false;
            boolean or = false;
            char c = command.charAt(i);
            if (i < end) continue;
            switch (c) {
                case '&':
                    or = true;
                case ',': {
                    prefix = true;
                    if (current == null) {
                        throw new InputParseException("Duplicate separator");
                    }
                    newEntry = true;
                    break;
                }
                case '[': {
                    int depth = 0;
                    end = len;
                    loop:
                    for (int j = i + 1; j < len; j++) {
                        char c2 = command.charAt(j);
                        switch (c2) {
                            case '[':
                                depth++;
                                continue;
                            case ']':
                                if (depth-- <= 0) {
                                    end = j;
                                    break loop;
                                }
                        }
                    }
                    String arg = command.substring(i + 1, end);
                    args.add(arg);
                    // start
                    break;
                }
            }
            if (newEntry) {
                int index = StringMan.indexOf(command, i + 1, '[', '&', ',');
                if (index < 0) index = len;
                end = index;
                current = command.substring(i + (prefix ? 1 : 0), end);
                args = new ArrayList<>();
                ParseEntry entry = new ParseEntry(current, or);
                keys.add(new AbstractMap.SimpleEntry<ParseEntry, List<String>>(entry, args));
            }
        }
        for (int i = 0; i < keys.size() - 1; i++) { // Apply greedy and
            if (keys.get(i + 1).getKey().and) {
                keys.get(i).getKey().and = true;
            }
        }
        return keys;
    }
}
