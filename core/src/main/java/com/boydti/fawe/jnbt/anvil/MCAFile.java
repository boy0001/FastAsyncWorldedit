package com.boydti.fawe.jnbt.anvil;

import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.RunnableVal4;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.io.BufferedRandomAccessFile;
import com.boydti.fawe.object.io.FastByteArrayInputStream;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Chunk format: http://minecraft.gamepedia.com/Chunk_format#Entity_format
 * e.g.: `.Level.Entities.#` (Starts with a . as the root tag is unnamed)
 */
public class MCAFile {

    private File file;
    private RandomAccessFile raf;
    private byte[] locations;
    private FaweQueue queue;
    private Field fieldBuf1;
    private Field fieldBuf2;
    private Field fieldBuf3;
    private Field fieldBuf4;
    private Field fieldBuf5;
    private Field fieldBuf6;

    private byte[] buffer1 = new byte[4096];
    private byte[] buffer2 = new byte[4096];
    private byte[] buffer3 = new byte[720];

    private final int X, Z;

    private Map<Integer, MCAChunk> chunks = new HashMap<>();

    public MCAFile(FaweQueue parent, File file) {
        this.queue = parent;
        this.file = file;
        if (!file.exists()) {
            throw new FaweException.FaweChunkLoadException();
        }
        String[] split = file.getName().split("\\.");
        X = Integer.parseInt(split[1]);
        Z = Integer.parseInt(split[2]);
    }

    public FaweQueue getParent() {
        return queue;
    }

    public void init() {
        try {
            if (raf == null) {
                this.locations = new byte[4096];
                this.raf = new BufferedRandomAccessFile(file, "rw", (int) file.length());
                raf.readFully(locations);
                fieldBuf1 = BufferedInputStream.class.getDeclaredField("buf");
                fieldBuf1.setAccessible(true);
                fieldBuf2 = InflaterInputStream.class.getDeclaredField("buf");
                fieldBuf2.setAccessible(true);
                fieldBuf3 = NBTInputStream.class.getDeclaredField("buf");
                fieldBuf3.setAccessible(true);
                fieldBuf4 = FastByteArrayOutputStream.class.getDeclaredField("buffer");
                fieldBuf4.setAccessible(true);
                fieldBuf5 = DeflaterOutputStream.class.getDeclaredField("buf");
                fieldBuf5.setAccessible(true);
                fieldBuf6 = BufferedOutputStream.class.getDeclaredField("buf");
                fieldBuf6.setAccessible(true);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public MCAFile(FaweQueue parent, int mcrX, int mcrZ) throws Exception {
        this(parent, new File(parent.getSaveFolder(), "r." + mcrX + "." + mcrZ + ".mca"));
    }

    public int getX() {
        return X;
    }

    public int getZ() {
        return Z;
    }

    public File getFile() {
        return file;
    }

    public MCAChunk getCachedChunk(int cx, int cz) {
        int pair = MathMan.pair((short) (cx & 31), (short) (cz & 31));
        return chunks.get(pair);
    }

    public MCAChunk getChunk(int cx, int cz) throws IOException {
        MCAChunk cached = getCachedChunk(cx, cz);
        if (cached != null) {
            return cached;
        } else {
            return readChunk(cx, cz);
        }
    }

    public MCAChunk readChunk(int cx, int cz) throws IOException {
        int i = ((cx & 31) << 2) + ((cz & 31) << 7);
        int offset = (((locations[i] & 0xFF) << 16) + ((locations[i + 1] & 0xFF) << 8) + ((locations[i+ 2] & 0xFF))) << 12;
        int size = (locations[i + 3] & 0xFF) << 12;
        if (offset == 0) {
            return null;
        }
        NBTInputStream nis = getChunkIS(offset);
        MCAChunk chunk = new MCAChunk(nis, queue, cx, cz, size);
        nis.close();
        int pair = MathMan.pair((short) (cx & 31), (short) (cz & 31));
        chunks.put(pair, chunk);
        return chunk;
    }

    /**
     * @param onEach cx, cz, offset, size
     */
    public void forEachChunk(RunnableVal4<Integer, Integer, Integer, Integer> onEach) {
        int i = 0;
        for (int z = 0; z < 32; z++) {
            for (int x = 0; x < 32; x++, i += 4) {
                int offset = (((locations[i] & 0xFF) << 16) + ((locations[i + 1] & 0xFF) << 8) + ((locations[i+ 2] & 0xFF)));
                int size = locations[i + 3] & 0xFF;
                if (size != 0) {
                    onEach.run(x, z, offset << 12, size << 12);
                }
            }
        }
    }

    public void forEachChunk(RunnableVal<MCAChunk> onEach) {
        int i = 0;
        for (int z = 0; z < 32; z++) {
            for (int x = 0; x < 32; x++, i += 4) {
                int offset = (((locations[i] & 0xFF) << 16) + ((locations[i + 1] & 0xFF) << 8) + ((locations[i+ 2] & 0xFF)));
                int size = locations[i + 3] & 0xFF;
                if (size != 0) {
                    try {
                        onEach.run(getChunk(x, z));
                    } catch (Throwable ignore) {}
                }
            }
        }
    }

    public int getOffset(int cx, int cz) {
        int i = ((cx & 31) << 2) + ((cz & 31) << 7);
        int offset = (((locations[i] & 0xFF) << 16) + ((locations[i + 1] & 0xFF) << 8) + ((locations[i+ 2] & 0xFF)));
        return offset << 12;
    }

    public int getSize(int cx, int cz) {
        int i = ((cx & 31) << 2) + ((cz & 31) << 7);
        return (locations[i + 3] & 0xFF) << 12;
    }

    public List<Integer> getChunks() {
        final List<Integer> values = new ArrayList<>(chunks.size());
        for (int i = 0; i < locations.length; i+=4) {
            int offset = (((locations[i] & 0xFF) << 16) + ((locations[i + 1] & 0xFF) << 8) + ((locations[i+ 2] & 0xFF)));
            values.add(offset);
        }
        return values;
    }

    private byte[] getChunkCompressedBytes(int offset) throws IOException{
        raf.seek(offset);
        int size = raf.readInt();
        int compression = raf.read();
        byte[] data = new byte[size];
        raf.readFully(data);
        return data;
    }

    private void writeSafe(int offset, byte[] data) throws IOException {
        int len = data.length + 5;
        raf.seek(offset);
        if (raf.length() - offset < len) {
            raf.setLength(offset + len);
        }
        raf.writeInt(data.length);
        raf.write(2);
        raf.write(data);
    }

    private NBTInputStream getChunkIS(int offset) throws IOException {
        try {
            byte[] data = getChunkCompressedBytes(offset);
            FastByteArrayInputStream bais = new FastByteArrayInputStream(data);
            InflaterInputStream iis = new InflaterInputStream(bais, new Inflater(), 1);
            fieldBuf2.set(iis, buffer2);
            BufferedInputStream bis = new BufferedInputStream(iis, 1);
            fieldBuf1.set(bis, buffer1);
            NBTInputStream nis = new NBTInputStream(bis);
            fieldBuf3.set(nis, buffer3);
            return nis;
        } catch (IllegalAccessException unlikely) {
            unlikely.printStackTrace();
            return null;
        }
    }

    public void streamChunk(int cx, int cz, RunnableVal<NBTStreamer> addReaders) throws IOException {
        streamChunk(getOffset(cx, cz), addReaders);
    }

    public void streamChunk(int offset, RunnableVal<NBTStreamer> addReaders) throws IOException {
        if (offset == 0) {
            return;
        }
        NBTInputStream is = getChunkIS(offset);
        NBTStreamer ns = new NBTStreamer(is);
        addReaders.run(ns);
        ns.readFully();
        is.close();
    }

    /**
     * @param onEach chunk
     */
    public void forEachCachedChunk(RunnableVal<MCAChunk> onEach) {
        for (Map.Entry<Integer, MCAChunk> entry : chunks.entrySet()) {
            onEach.run(entry.getValue());
        }
    }

    public List<MCAChunk> getCachedChunks() {
        return new ArrayList<>(chunks.values());
    }

    public void uncache(int cx, int cz) {
        int pair = MathMan.pair((short) (cx & 31), (short) (cz & 31));
        chunks.remove(pair);
    }

    private byte[] toBytes(MCAChunk chunk) throws Exception {
        CompoundTag tag = chunk.toTag();
        if (tag == null || chunk.isDeleted()) {
            return null;
        }
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream(buffer3);
        DeflaterOutputStream deflater = new DeflaterOutputStream(baos, new Deflater(9), 1, true);
        fieldBuf5.set(deflater, buffer2);
        BufferedOutputStream bos = new BufferedOutputStream(deflater, 1);
        fieldBuf6.set(bos, buffer1);
        NBTOutputStream nos = new NBTOutputStream(bos);
        nos.writeNamedTag("", tag);
        bos.flush();
        bos.close();
        byte[] result = baos.toByteArray();
        baos.close();
        deflater.close();
        bos.close();
        nos.close();
        return result;
    }

    private byte[] getChunkBytes(int cx, int cz) throws  Exception{
        MCAChunk mca = getCachedChunk(cx, cz);
        if (mca == null) {
            int offset = getOffset(cx, cz);
            if (offset == 0) {
                return null;
            }
            return getChunkCompressedBytes(offset);
        }
        return toBytes(mca);
    }

    private void writeHeader(int cx, int cz, int offsetMedium, int sizeByte) throws IOException {
        int i = ((cx & 31) << 2) + ((cz & 31) << 7);
        raf.seek(i);
        raf.write((offsetMedium >> 16));
        raf.write((offsetMedium >> 8));
        raf.write((offsetMedium >> 0));
        raf.write(sizeByte);
        raf.seek(i + 4096);
        if (offsetMedium == 0 && sizeByte == 0) {
            raf.writeInt(0);
        } else {
            raf.writeInt((int) (System.currentTimeMillis() / 1000L));
        }
        int offset = (((locations[i] & 0xFF) << 16) + ((locations[i + 1] & 0xFF) << 8) + ((locations[i+ 2] & 0xFF))) << 12;
        int size = (locations[i + 3] & 0xFF) << 12;
    }

    public void close() {
        flush();
        if (raf != null) {
            try {
                raf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            file = null;
            raf = null;
            locations = null;
            queue = null;
            fieldBuf1 = null;
            fieldBuf2 = null;
            fieldBuf3 = null;
            fieldBuf4 = null;
            fieldBuf5 = null;
            fieldBuf6 = null;
            buffer1 = null;
            buffer2 = null;
            buffer3 = null;
            chunks = null;
        }
    }

    public void flush() {
        boolean modified = false;
        for (MCAChunk chunk : getCachedChunks()) {
            if (chunk.isModified()) {
                modified = true;
                break;
            }
        }
        if (!modified) {
            return;
        }
        final HashMap<Integer, Integer> offsetMap = new HashMap<>(); // Offset -> <byte cx, byte cz, short size>
        forEachChunk(new RunnableVal4<Integer, Integer, Integer, Integer>() {
            @Override
            public void run(Integer cx, Integer cz, Integer offset, Integer size) {
                short pair1 = MathMan.pairByte((byte) (cx & 31), (byte) (cz & 31));
                short pair2 = (short) (size >> 12);
                offsetMap.put(offset, MathMan.pair(pair1, pair2));
            }
        });

        HashMap<Integer, byte[]> relocate = new HashMap<Integer, byte[]>();
        int start = 8192;
        int written = start;
        int end = 8192;
        int nextOffset = 8192;
        try {
            for (int count = 0; count < offsetMap.size(); count++) {
                Integer loc = offsetMap.get(nextOffset);
                while (loc == null) {
                    nextOffset += 4096;
                    loc = offsetMap.get(nextOffset);
                }
                int offset = nextOffset;
                short cxz = MathMan.unpairX(loc);
                int cx = MathMan.unpairShortX(cxz);
                int cz = MathMan.unpairShortY(cxz);
                int size = MathMan.unpairY(loc) << 12;
                nextOffset += size;
                end += size;
                int pair = MathMan.pair((short) (cx & 31), (short) (cz & 31));
                byte[] newBytes = relocate.get(pair);
                if (newBytes == null) {
                    if (offset == start) {
                        MCAChunk cached = getCachedChunk(cx, cz);
                        if (cached == null || !cached.isModified()) {
                            start += size;
                            written = start;
                            continue;
                        } else {
                            newBytes = toBytes(cached);
                        }
                    } else {
                        newBytes = getChunkBytes(cx, cz);
                    }
                }
                if (newBytes == null) {
                    writeHeader(cx, cz, 0, 0);
                    continue;
                }
                int len = newBytes.length + 5;
                int oldSize = (size + 4095) >> 12;
                int newSize = (len + 4095) >> 12;
                int nextOffset2 = nextOffset;
                while (start + len > end) {
                    int nextLoc = offsetMap.get(nextOffset2);
                    short nextCXZ = MathMan.unpairX(nextLoc);
                    int nextCX = MathMan.unpairShortX(nextCXZ);
                    int nextCZ = MathMan.unpairShortY(nextCXZ);
                    if (getCachedChunk(nextCX, nextCZ) == null) {
                        byte[] nextBytes = getChunkCompressedBytes(nextOffset2);
                        relocate.put(pair, nextBytes);
                    }
//                    System.out.println("Relocating " + nextCX + "," + nextCZ);
                    int nextSize = MathMan.unpairY(nextLoc) << 12;
                    end += nextSize;
                    nextOffset2 += nextSize;
                }
//                System.out.println("Writing: " + cx + "," + cz);
                writeSafe(start, newBytes);
                if (offset != start || end != start + size || oldSize != newSize) {
//                    System.out.println("Header: " + cx + "," + cz + " | " + offset + "," + start + " | " + end + "," + (start + size) + " | " + size + " | " + start);
                    writeHeader(cx, cz, start >> 12, newSize);
                }
                written = start + newBytes.length + 6;
                start += newSize << 12;
            }
            raf.setLength(written);
            if (raf instanceof BufferedRandomAccessFile) {
                ((BufferedRandomAccessFile) raf).flush();
            }
            raf.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
