package com.boydti.fawe.bukkit.v1_10;

import com.boydti.fawe.bukkit.ABukkitMain;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.io.BufferedRandomAccessFile;
import com.boydti.fawe.object.io.FastByteArrayInputStream;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.worldedit.world.World;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import net.minecraft.server.v1_10_R1.RegionFile;
import net.minecraft.server.v1_10_R1.RegionFileCache;

public class BukkitMain_110 extends ABukkitMain {
    @Override
    public BukkitQueue_0 getQueue(World world) {
        return new BukkitQueue_1_10(world);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (Settings.EXPERIMENTAL.WORLD_COMPRESSION != -1) {
            try {
                ReflectionUtils.setFailsafeFieldValue(RegionFileCache.class.getDeclaredField("a"), null, new ConcurrentHashMap<File, RegionFile>(8, 0.9f, 1) {
                    @Override
                    public RegionFile get(Object key) {
                        RegionFile existing = super.get(key);
                        if (existing != null) {
                            return existing;
                        }
                        try {
                            File file = (File) key;
                            if (!file.exists()) {
                                file.getParentFile().mkdirs();
                            }
                            if (size() >= 256) {
                                RegionFileCache.a();
                            }
                            RegionFile regionFile = new RegionFile(file) {

                                private int[] d = ReflectionUtils.getField(RegionFile.class.getDeclaredField("d"), this);
                                private int[] e = ReflectionUtils.getField(RegionFile.class.getDeclaredField("e"), this);
                                private List<Boolean> f = ReflectionUtils.getField(RegionFile.class.getDeclaredField("f"), this);
                                public RandomAccessFile c = null;

                                @Override
                                public DataOutputStream b(final int i, final int j) {
                                    if (i < 0 || i >= 32 || j < 0 || j >= 32) {
                                        return null;
                                    }
//                                if (Settings.EXPERIMENTAL.FAST_WORLD_COMPRESSION) {
//                                    try {
//                                        return new DataOutputStream(new AsyncBufferedOutputStream(new LZ4OutputStream(new FastByteArrayOutputStream() {
//                                            @Override
//                                            public void close() {
//                                                try {
//                                                    super.close();
//                                                } catch (IOException e1) {
//                                                    e1.printStackTrace();
//                                                }
//                                                a(i, j, array, length);
//                                            }
//                                        }, 16000)));
//                                    } catch (Throwable e) {
//                                        e.printStackTrace();
//                                    }
//                                }
                                    return new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(new FastByteArrayOutputStream() {
                                        @Override
                                        public void close() throws IOException {
                                            super.close();
                                            a(i, j, this.array, length);
                                        }
                                    }, new Deflater(Settings.EXPERIMENTAL.WORLD_COMPRESSION))));
                                }

                                @Override
                                public synchronized DataInputStream a(int i, int j) {
                                    if ((i < 0) || (i >= 32) || (j < 0) || (j >= 32)) {
                                        return null;
                                    } else {
                                        try {
                                            int k = d[(i + j * 32)];
                                            if (k == 0) {
                                                return null;
                                            } else {
                                                int l = k >> 8;
                                                int i1 = k & 255;
                                                if (l + i1 > f.size()) {
                                                    return null;
                                                } else {
                                                    c.seek((long) (l * 4096));
                                                    int j1 = this.c.readInt();
                                                    if (j1 > 4096 * i1) {
                                                        return null;
                                                    } else if (j1 <= 0) {
                                                        return null;
                                                    } else {
                                                        byte b0 = c.readByte();
                                                        byte[] abyte;
                                                        if (b0 == 1) {
                                                            abyte = new byte[j1 - 1];
                                                            c.read(abyte);
                                                            return new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FastByteArrayInputStream(abyte))));
                                                        } else if (b0 == 2) {
                                                            abyte = new byte[j1 - 1];
                                                            c.read(abyte);
//                                                        if (Settings.EXPERIMENTAL.FAST_WORLD_COMPRESSION) {
//                                                            return new DataInputStream(new LZ4InputStream(new FastByteArrayInputStream(abyte)));
//                                                        }
                                                            return new DataInputStream(new BufferedInputStream(new InflaterInputStream(new FastByteArrayInputStream(abyte))));
                                                        } else {
                                                            return null;
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (IOException var9) {
                                            var9.printStackTrace();
                                            return null;
                                        }
                                    }
                                }
                            };
                            Field field = RegionFile.class.getDeclaredField("c");
                            field.setAccessible(true);
                            RandomAccessFile raf2 = (RandomAccessFile) field.get(regionFile);
                            raf2.close();
                            final BufferedRandomAccessFile raf = new BufferedRandomAccessFile(file, "rw");
                            ReflectionUtils.setFailsafeFieldValue(field, regionFile, raf);
                            put(file, regionFile);
                            regionFile.getClass().getDeclaredField("c").set(regionFile, raf);
                            return regionFile;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                });
                ;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}