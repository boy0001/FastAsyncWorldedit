package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.config.Settings;
import java.lang.reflect.Method;

public class ChunkListener_9 extends ChunkListener {

    private Method methodDepth;
    private Method methodGetStackTraceElement;
    private Exception exception;
    private StackTraceElement[] elements;

    public ChunkListener_9() {
        if (Settings.IMP.TICK_LIMITER.ENABLED) {
            try {
                this.methodDepth = Throwable.class.getDeclaredMethod("getStackTraceDepth");
                this.methodDepth.setAccessible(true);
                this.methodGetStackTraceElement = Throwable.class.getDeclaredMethod("getStackTraceElement", int.class);
                this.methodGetStackTraceElement.setAccessible(true);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    private StackTraceElement[] getElements(Exception ex) {
        if (elements == null || ex != exception) {
            exception = ex;
            elements = ex.getStackTrace();
        }
        return elements;
    }

    @Override
    protected int getDepth(Exception ex) {
        if (methodDepth != null) {
            try {
                return (int) methodDepth.invoke(ex);
            } catch (Throwable t) {
                methodDepth = null;
                t.printStackTrace();
            }
        }
        return getElements(ex).length;
    }

    @Override
    protected StackTraceElement getElement(Exception ex, int i) {
        if (methodGetStackTraceElement != null) {
            try {
                return (StackTraceElement) methodGetStackTraceElement.invoke(ex, i);
            } catch (Throwable t) {
                methodGetStackTraceElement = null;
                t.printStackTrace();
            }
        }
        return getElements(ex)[i];
    }
}