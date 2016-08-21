package com.boydti.fawe.bukkit.v1_10;

import com.boydti.fawe.bukkit.ABukkitMain;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.ReflectionUtils;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import net.minecraft.server.v1_10_R1.RegionFile;
import net.minecraft.server.v1_10_R1.RegionFileCache;

public class BukkitMain_110 extends ABukkitMain {
    @Override
    public BukkitQueue_0 getQueue(String world) {
        return new BukkitQueue_1_10(world);
    }

    public BukkitMain_110() {
        try {
            ReflectionUtils.setFailsafeFieldValue(RegionFileCache.class.getDeclaredField("a"), null, new ConcurrentHashMap<File, RegionFile>() {
                @Override
                public RegionFile get(Object key) {
                    RegionFile existing = super.get(key);
                    if (existing != null) {
                        return existing;
                    }
                    File file = (File) key;
                    if (!file.exists()) {
                        file.getParentFile().mkdirs();
                    }
                    if (size() >= 256) {
                        RegionFileCache.a();
                    }
                    RegionFile regionFile = new RegionFile(file) {
                        @Override
                        public DataOutputStream b(final int i, final int j) {
                            if (i < 0 || i >= 32 || j < 0 || j >= 32) {
                                return null;
                            }
                            return new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(new ByteArrayOutputStream() {
                                @Override
                                public void close() throws IOException {
                                    a(i, j, this.buf, this.count);
                                }
                            }, new Deflater(Settings.EXPERIMENTAL.WORLD_COMPRESSION))));
                        }
                    };
                    put(file, regionFile);
                    return regionFile;
                }
            });
            ;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}