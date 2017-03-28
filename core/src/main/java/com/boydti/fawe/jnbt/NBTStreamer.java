package com.boydti.fawe.jnbt;

import com.boydti.fawe.object.RunnableVal2;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import java.io.IOException;
import java.util.HashMap;

public class NBTStreamer {
    private final NBTInputStream is;
    private final HashMap<String, RunnableVal2> readers;

    public NBTStreamer(NBTInputStream stream) {
        this.is = stream;
        readers = new HashMap<>();
    }

    public void readFully() throws IOException {
        is.readNamedTagLazy(new RunnableVal2<String, RunnableVal2>() {
            @Override
            public void run(String node, RunnableVal2 result) {
                this.value2 = readers.get(node);
            }
        });
        is.close();
    }

    public <T, V> void addReader(String node, RunnableVal2<T, V> run) {
        if (run instanceof NBTStreamReader) {
            ((NBTStreamReader) run).init(node);
        }
        readers.put(node, run);
    }

    public <T, V> void addReader(RunnableVal2<T, V> run, String... nodes) {
        for (String node : nodes) {
            addReader(node, run);
        }
    }

    public static  abstract class NBTStreamReader<T, V> extends RunnableVal2<T, V> {
        private String node;
        public void init(String node) {
            this.node = node;
        }

        public String getNode() {
            return node;
        }
    }

    public static abstract class ByteReader extends RunnableVal2<Integer, Integer> {
        @Override
        public void run(Integer index, Integer value) {
            run(index, value);
        }

        public abstract void run(int index, int byteValue);
    }
}
