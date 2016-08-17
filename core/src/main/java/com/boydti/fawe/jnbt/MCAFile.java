package com.boydti.fawe.jnbt;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.RunnableVal3;
import com.boydti.fawe.object.io.BufferedRandomAccessFile;
import com.sk89q.jnbt.NBTInputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class MCAFile {
    private final File file;
    private final BufferedRandomAccessFile raf;
    public final byte[] locations;
    private Field fieldBuf1;
    private Field fieldBuf2;
    private Field fieldBuf3;

    private byte[] buffer1 = new byte[Settings.HISTORY.BUFFER_SIZE];
    private byte[] buffer2 = new byte[Settings.HISTORY.BUFFER_SIZE];
    private byte[] buffer3 = new byte[720];


    public MCAFile(File file) throws Exception {
        this.file = file;
        if (!file.exists()) {
            throw new FileNotFoundException(file.toString());
        }
        this.locations = new byte[4096];
        this.raf = new BufferedRandomAccessFile(file, "rw", Settings.HISTORY.BUFFER_SIZE);
        raf.read(locations);
        fieldBuf1 = BufferedInputStream.class.getDeclaredField("buf");
        fieldBuf1.setAccessible(true);
        fieldBuf2 = InflaterInputStream.class.getDeclaredField("buf");
        fieldBuf2.setAccessible(true);
        fieldBuf3 = NBTInputStream.class.getDeclaredField("buf");
        fieldBuf3.setAccessible(true);
    }


    public MCAFile(File regionFolder, int mcrX, int mcrZ) throws Exception {
        this(new File(regionFolder, "r." + mcrX + "." + mcrZ + ".mca"));
    }

    /**
     * @param onEach cx, cz, offset
     */
    public void forEachChunk(RunnableVal3<Integer, Integer, Integer> onEach) {
        int i = 0;
        for (int z = 0; z < 32; z++) {
            for (int x = 0; x < 32; x++, i += 4) {
                int offset = (((locations[i] & 0xFF) << 16) + ((locations[i + 1] & 0xFF) << 8) + ((locations[i+ 2] & 0xFF)));
                int size = locations[i + 3] & 0xFF;
                if (size != 0) {
                    onEach.run(x, z, offset << 12);
                }
            }
        }
    }

    public int getOffset(int cx, int cz) {
        int i = (cx << 2) + (cz << 7);
        int offset = (((locations[i] & 0xFF) << 16) + ((locations[i + 1] & 0xFF) << 8) + ((locations[i+ 2] & 0xFF)));
        int size = locations[i + 3] & 0xFF;
        return offset << 12;
    }


    private NBTStreamer getChunkReader(int offset) throws Exception {
        raf.seek(offset);
        int size = raf.readInt();
        int compression = raf.readByte();
        byte[] data = new byte[size];
        raf.read(data);
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        InflaterInputStream iis = new InflaterInputStream(bais, new Inflater(), 1);
        fieldBuf2.set(iis, buffer2);
        BufferedInputStream bis = new BufferedInputStream(iis, 1);
        fieldBuf1.set(bis, buffer1);
        NBTInputStream nis = new NBTInputStream(bis);
        fieldBuf3.set(nis, buffer3);
        return new NBTStreamer(nis);
    }

    public int countId(int offset, final int id) throws Exception {
        try {
            NBTStreamer streamer = getChunkReader(offset);
            NBTStreamer.ByteReader reader = new NBTStreamer.ByteReader() {
                public int countId = id;
                public int count = 0;
                @Override
                public void run(int index, int byteValue) {
                    if (byteValue == countId) {
                        count++;
                    }
                }
            };
            streamer.addReader(".Level.Sections.#.Blocks.#", reader);
            streamer.readFully();
            return reader.getClass().getField("count").getInt(reader);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void main(String[] args) throws Exception {
        File folder = new File("../../mc/world/region");
        long start = System.nanoTime();
        final AtomicInteger count = new AtomicInteger();
        final int id = 1;
        for (File file : folder.listFiles()) {
//        {
//            File file = new File(folder, "r.0.0.mca");
            System.out.println(file);
            final MCAFile mca = new MCAFile(file);
            mca.forEachChunk(new RunnableVal3<Integer, Integer, Integer>() {
                @Override
                public void run(Integer cx, Integer cz, Integer offset) {
                    try {
                        count.addAndGet(mca.countId(offset, id));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

        }
        long diff = System.nanoTime() - start;
        System.out.println(diff / 1000000d);

        System.out.println("Count: " + count);

        // My results
        // 496,772,342 stone
        // 35,164 chunks
        // 17.175 seconds


    }
}
