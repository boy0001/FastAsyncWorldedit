package com.boydti.fawe.jnbt.anvil.filters;

import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAFile;
import com.boydti.fawe.jnbt.anvil.MCAFilterCounter;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.object.RunnableVal4;
import com.boydti.fawe.object.exception.FaweException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * Deletes unvisited MCA files and Chunks<br>
 * - This a global filter and cannot be used a selection<br>
 */
public class DeleteUninhabitedFilter extends MCAFilterCounter {
    private final long inhabitedTicks;
    private final long fileAgeMillis;

    public DeleteUninhabitedFilter(long fileAgeMillis, long inhabitedTicks) {
        this.fileAgeMillis = fileAgeMillis;
        this.inhabitedTicks = inhabitedTicks;
    }

    public long getInhabitedTicks() {
        return inhabitedTicks;
    }

    public long getFileAgeMillis() {
        return fileAgeMillis;
    }

    @Override
    public MCAFile applyFile(MCAFile mca) {
        try {
            if (shouldDelete(mca)) {
                mca.setDeleted(true);
                get().add(512 * 512 * 256);
                return null;
            }
        } catch (IOException | UnsupportedOperationException ignore) {
        }
        try {
            ForkJoinPool pool = new ForkJoinPool();
            mca.init();
            filter(mca, pool);
            pool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            mca.close(pool);
            pool.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean shouldDelete(MCAFile mca) throws IOException {
        File file = mca.getFile();
        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        long creation = attr.creationTime().toMillis();
        long modified = attr.lastModifiedTime().toMillis();
        if ((modified - creation < fileAgeMillis && modified > creation) || file.length() < 12288) {
            return true;
        }
        return false;
    }

    public boolean shouldDeleteChunk(MCAFile mca, int cx, int cz) {
        return false;
    }

    public void filter(MCAFile mca, ForkJoinPool pool) throws IOException {
        mca.forEachSortedChunk(new RunnableVal4<Integer, Integer, Integer, Integer>() {
            @Override
            public void run(Integer x, Integer z, Integer offset, Integer size) {
                try {
                    int bx = mca.getX() << 5;
                    int bz = mca.getZ() << 5;
                    if (shouldDeleteChunk(mca, bx, bz)) {
                        MCAChunk chunk = new MCAChunk(null, x, z);
                        chunk.setDeleted(true);
                        synchronized (mca) {
                            mca.setChunk(chunk);
                        }
                        get().add(16 * 16 * 256);
                        return;
                    }
                    byte[] bytes = mca.getChunkCompressedBytes(offset);
                    if (bytes == null) return;
                    Runnable task = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mca.streamChunk(offset, new RunnableVal<NBTStreamer>() {
                                    @Override
                                    public void run(NBTStreamer value) {
                                        addReaders(mca, x, z, value);
                                    }
                                });
                            } catch (FaweException ignore) {
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    pool.submit(task);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void addReaders(MCAFile mca, int x, int z, NBTStreamer streamer) {
        streamer.addReader(".Level.InhabitedTime", new RunnableVal2<Integer, Long>() {
            @Override
            public void run(Integer index, Long value) {
                if (value <= inhabitedTicks) {
                    MCAChunk chunk = new MCAChunk(null, x, z);
                    chunk.setDeleted(true);
                    synchronized (mca) {
                        mca.setChunk(chunk);
                    }
                    get().add(16 * 16 * 256);
                }
            }
        });
    }
}
