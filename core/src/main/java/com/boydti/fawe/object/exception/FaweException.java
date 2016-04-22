package com.boydti.fawe.object.exception;

import com.boydti.fawe.config.BBC;

public class FaweException extends RuntimeException {
    private final String message;

    public FaweException(BBC reason) {
        this.message = reason.format();
    }

    @Override
    public String getMessage() {
        return message;
    }

    public static FaweException get(Throwable e) {
        if (e instanceof FaweException) {
            return (FaweException) e;
        }
        Throwable cause = e.getCause();
        if (cause == null) {
            return null;
        }
        return get(cause);
    }

    public static class FaweChunkLoadException extends FaweException {
        public FaweChunkLoadException() {
            super(BBC.WORLDEDIT_FAILED_LOAD_CHUNK);
        }
    }
}
