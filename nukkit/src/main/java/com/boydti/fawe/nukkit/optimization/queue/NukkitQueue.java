package com.boydti.fawe.nukkit.optimization.queue;

import cn.nukkit.Player;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.math.Vector3;
import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.nukkit.core.NBTConverter;
import com.boydti.fawe.nukkit.optimization.FaweNukkit;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.RunnableVal;
import com.sk89q.jnbt.CompoundTag;
import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NukkitQueue extends NMSMappedFaweQueue<Level, BaseFullChunk, BaseFullChunk, BaseFullChunk> {
    private final FaweNukkit faweNukkit;
    private final Level world;

    public static int ALLOCATE;
    public static double TPS_TARGET = 18.5;
    private static int LIGHT_MASK = 0x739C0;

    public NukkitQueue(FaweNukkit fn, String world) {
        super(world);
        this.faweNukkit = fn;
        this.world = faweNukkit.getPlugin().getServer().getLevelByName(world);
        if (Settings.QUEUE.EXTRA_TIME_MS != Integer.MIN_VALUE) {
            ALLOCATE = Settings.QUEUE.EXTRA_TIME_MS;
            Settings.QUEUE.EXTRA_TIME_MS = Integer.MIN_VALUE;
        }
    }

    public FaweNukkit getFaweNukkit() {
        return faweNukkit;
    }

    @Override
    public boolean execute(FaweChunk fc) {
        if (super.execute(fc)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void refreshChunk(FaweChunk fs) {
        NukkitChunk fc = (NukkitChunk) fs;
        Collection<Player> players = faweNukkit.getPlugin().getServer().getOnlinePlayers().values();
        int view = faweNukkit.getPlugin().getServer().getViewDistance();
        for (Player player : players) {
            Position pos = player.getPosition();
            int pcx = pos.getFloorX() >> 4;
            int pcz = pos.getFloorZ() >> 4;
            if (Math.abs(pcx - fs.getX()) > view || Math.abs(pcz - fs.getZ()) > view) {
                continue;
            }
            world.requestChunk(fs.getX(), fs.getZ(), player);
        }
    }

    @Override
    public CharFaweChunk getPrevious(CharFaweChunk fs, BaseFullChunk sections, Map<?, ?> tiles, Collection<?>[] entities, Set<UUID> createdEntities, boolean all) throws Exception {
        return fs;
    }

    private int skip;

    @Override
    public boolean setComponents(FaweChunk fc, RunnableVal<FaweChunk> changeTask) {
        if (skip > 0) {
            skip--;
            fc.addToQueue();
            return true;
        }
        long start = System.currentTimeMillis();
        ((NukkitChunk) fc).execute(start);
        if (System.currentTimeMillis() - start > 50 || Fawe.get().getTPS() < TPS_TARGET) {
            skip = 10;
        }
        return true;
    }

    @Override
    public File getSaveFolder() {
        return new File(world.getFolderName() + File.separator + "region");
    }

    @Override
    public boolean hasSky() {
        return world.getDimension() == 0;
    }

    @Override
    public void setFullbright(BaseFullChunk sections) {
        for (int y = 0; y < 128; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    sections.setBlockSkyLight(x, y, z, 15);
                }
            }
        }
    }

    @Override
    public boolean removeLighting(BaseFullChunk sections, RelightMode mode, boolean hasSky) {
        for (int y = 0; y < 128; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    sections.setBlockSkyLight(x, y, z, 0);
                    sections.setBlockLight(x, y, z, 0);
                }
            }
        }
        return true;
    }

    private Vector3 mutable = new Vector3();
    private Vector3 getMutable(int x, int y, int z) {
        mutable.x = x;
        mutable.y = y;
        mutable.z = z;
        return mutable;
    }

    @Override
    public void relight(int x, int y, int z) {
        world.updateAllLight(getMutable(x, y, z));
    }

    @Override
    public void relightBlock(int x, int y, int z) {
        world.updateBlockLight(x, y, z);
    }

    @Override
    public void relightSky(int x, int y, int z) {
        world.updateBlockSkyLight(x, y, z);
    }

    @Override
    public void setSkyLight(BaseFullChunk chunkSection, int x, int y, int z, int value) {
        chunkSection.setBlockSkyLight(x & 15, y & 15, z & 15, value);
    }

    @Override
    public void setBlockLight(BaseFullChunk chunkSection, int x, int y, int z, int value) {
        chunkSection.setBlockLight(x & 15, y & 15, z & 15, value);
    }

    @Override
    public CompoundTag getTileEntity(BaseFullChunk baseChunk, int x, int y, int z) {
        BlockEntity entity = baseChunk.getTile(x & 15, y, z & 15);
        if (entity == null) {
            return null;
        }
        cn.nukkit.nbt.tag.CompoundTag nbt = entity.namedTag;
        return NBTConverter.fromNative(nbt);
    }

    @Override
    public BaseFullChunk getChunk(Level level, int x, int z) {
        return (BaseFullChunk) level.getChunk(x, z);
    }

    @Override
    public Level getImpWorld() {
        return world;
    }

    @Override
    public boolean isChunkLoaded(Level level, int x, int z) {
        return level.isChunkLoaded(x, z);
    }

    @Override
    public boolean regenerateChunk(Level level, int x, int z) {
        level.regenerateChunk(x, z);
        return true;
    }

    @Override
    public FaweChunk getFaweChunk(int x, int z) {
        return new NukkitChunk(this, x, z);
    }

    @Override
    public boolean loadChunk(Level level, int x, int z, boolean generate) {
        return level.loadChunk(x, z, generate);
    }

    @Override
    public BaseFullChunk getCachedSections(Level level, int cx, int cz) {
        BaseFullChunk chunk = (BaseFullChunk) world.getChunk(cx, cz);
        return chunk;
    }

    @Override
    public int getCombinedId4Data(BaseFullChunk chunkSection, int x, int y, int z) {
        int id = chunkSection.getBlockId(x & 15, y & 15, z & 15);
        if (FaweCache.hasData(id)) {
            int data = chunkSection.getBlockData(x & 15, y & 15, z & 15);
            return (id << 4) + data;
        } else {
            return (id << 4);
        }
    }

    @Override
    public int getSkyLight(BaseFullChunk sections, int x, int y, int z) {
        return sections.getBlockSkyLight(x & 15, y & 15, z & 15);
    }

    @Override
    public int getEmmittedLight(BaseFullChunk sections, int x, int y, int z) {
        return sections.getBlockLight(x & 15, y & 15, z & 15);
    }
}
