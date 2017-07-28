package com.boydti.fawe.nukkit.core.converter;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.clipboard.ClipboardRemapper;
import com.boydti.fawe.util.MemUtil;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

public class LevelDBToMCAFile implements Closeable, Runnable{

    private final DB db;
    private final ClipboardRemapper remapper;
    private final ForkJoinPool pool;

    public LevelDBToMCAFile(File folder) {
        try {
            this.pool = new ForkJoinPool();
            this.remapper = new ClipboardRemapper(ClipboardRemapper.RemapPlatform.PC, ClipboardRemapper.RemapPlatform.PE);
            BundledBlockData.getInstance().loadFromResource();
            int bufferSize = (int) Math.min(Integer.MAX_VALUE, Math.max((long) (MemUtil.getFreeBytes() * 0.8), 134217728));
            this.db = Iq80DBFactory.factory.open(new File(folder, "db"),
                    new Options()
                            .createIfMissing(false)
                            .verifyChecksums(false)
                            .blockSize(262144) // 256K
                            .cacheSize(bufferSize) // 8MB
            );
            try {
                this.db.suspendCompactions();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            pool.shutdown();
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            db.close();
            Fawe.debug("Done!");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        db.forEach(new Consumer<Map.Entry<byte[], byte[]>>() {
            @Override
            public void accept(Map.Entry<byte[], byte[]> entry) {
                byte[] key = entry.getKey();
                if (key.length != 10) {
                    return;
                }
//                byte[] value = entry.getValue();
            }
        });
        // TODO
    }
}
