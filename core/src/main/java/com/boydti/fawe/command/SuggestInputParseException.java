package com.boydti.fawe.command;

import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.StringMan;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extension.input.InputParseException;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

public class SuggestInputParseException extends InputParseException {

    private final InputParseException cause;
    private final SuggestSupplier<List<String>> getSuggestions;
    private String prefix;

    public SuggestInputParseException(String msg, String prefix, SuggestSupplier<List<String>> getSuggestions) {
        this(new InputParseException(msg), prefix, getSuggestions);
    }

    public static SuggestInputParseException of(Throwable other, String prefix, SuggestSupplier<List<String>> getSuggestions) {
        InputParseException e = find(other);
        if (e != null) return of(e, prefix, getSuggestions);
        return of(new InputParseException(other.getMessage()), prefix, getSuggestions);
    }

    public static SuggestInputParseException of(InputParseException other, String prefix, SuggestSupplier<List<String>> getSuggestions) {
        if (other instanceof SuggestInputParseException) return (SuggestInputParseException) other;
        return new SuggestInputParseException(other, prefix, getSuggestions);
    }

    public SuggestInputParseException(InputParseException other, String prefix, SuggestSupplier<List<String>> getSuggestions) {
        super(other.getMessage());
        checkNotNull(getSuggestions);
        checkNotNull(other);
        this.cause = other;
        this.getSuggestions = getSuggestions;
        this.prefix = prefix;
    }

    public interface SuggestSupplier<T> {
        T get() throws InputParseException;
    }

    public static InputParseException find(Throwable e) {
        do {
            if (e instanceof InputParseException) return (InputParseException) e;
            e = e.getCause();
        }
        while (e != null);
        return null;
    }

    public static SuggestInputParseException get(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
            if (t instanceof SuggestInputParseException) return (SuggestInputParseException) t;
        }
        return null;
    }

    @Override
    public synchronized Throwable getCause() {
        return cause.getCause();
    }

    @Override
    public String getMessage() {
        return cause.getMessage();
    }


    public List<String> getSuggestions() throws InputParseException {
        return getSuggestions.get();
    }

    public SuggestInputParseException prepend(String input) {
        this.prefix = input + prefix;
        return this;
    }
}
