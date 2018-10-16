package com.boydti.fawe.nukkit.optimization.queue;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.network.protocol.UpdateBlockPacket;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.nukkit.core.NBTConverter;
import com.boydti.fawe.nukkit.optimization.FaweNukkit;
import com.boydti.fawe.nukkit.optimization.FaweNukkitPlayer;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.visitor.FaweChunkVisitor;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NukkitQueue extends NMSMappedFaweQueue<Level, BaseFullChunk, BaseFullChunk, BaseFullChunk> {
    private FaweNukkit faweNukkit;
    private Level world;

    public static int ALLOCATE;
    private static int LIGHT_MASK = 0x739C0;

    public NukkitQueue(FaweNukkit fn, World world) {
        super(world);
        init(fn);
    }

    public NukkitQueue(FaweNukkit fn, String world) {
        super(world);
        init(fn);
    }

    private void init(FaweNukkit fn) {
        this.faweNukkit = fn;
        this.world = faweNukkit.getPlugin().getServer().getLevelByName(getWorldName());
        if (Settings.IMP.QUEUE.EXTRA_TIME_MS != Integer.MIN_VALUE) {
            ALLOCATE = Settings.IMP.QUEUE.EXTRA_TIME_MS;
            Settings.IMP.QUEUE.EXTRA_TIME_MS = Integer.MIN_VALUE;
        }
    }

    @Override
    public void setHeightMap(FaweChunk chunk, byte[] heightMap) {
        BaseFullChunk forgeChunk = (BaseFullChunk) chunk.getChunk();
        if (forgeChunk != null) {
            byte[] otherMap = forgeChunk.getHeightMapArray();
            for (int i = 0; i < heightMap.length; i++) {
                int newHeight = heightMap[i] & 0xFF;
                int currentHeight = otherMap[i] & 0xFF;
                if (newHeight > currentHeight) {
                    otherMap[i] = (byte) newHeight;
                }
            }
        }
    }

    public FaweNukkit getFaweNukkit() {
        return faweNukkit;
    }

    @Override
    public int getOpacity(BaseFullChunk section, int x, int y, int z) {
        int id = section.getBlockId(x & 15, y, z & 15);
        return Block.lightFilter[id];
    }

    @Override
    public int getBrightness(BaseFullChunk section, int x, int y, int z) {
        int id = section.getBlockId(x & 15, y, z & 15);
        return Block.light[id];
    }

    @Override
    public int getOpacityBrightnessPair(BaseFullChunk section, int x, int y, int z) {
        int id = section.getBlockId(x & 15, y, z & 15);
        int opacity = Block.lightFilter[id];
        int brightness = Block.light[id];
        return MathMan.pair16(opacity, brightness);
    }

    @Override
    public void sendChunk(int x, int z, int bitMask) {
        Collection<Player> players = faweNukkit.getPlugin().getServer().getOnlinePlayers().values();
        int view = faweNukkit.getPlugin().getServer().getViewDistance();
        for (Player player : players) {
            Position pos = player.getPosition();
            int pcx = pos.getFloorX() >> 4;
            int pcz = pos.getFloorZ() >> 4;
            if (Math.abs(pcx - x) > view || Math.abs(pcz - z) > view) {
                continue;
            }
            world.requestChunk(x, z, player);
        }
    }

    @Override
    public void saveChunk(BaseFullChunk baseFullChunk) {
        baseFullChunk.setChanged();
    }

    @Override
    public void refreshChunk(FaweChunk fs) {
        sendChunk(fs.getX(), fs.getZ(), fs.getBitMask());
    }

    @Override
    public CharFaweChunk getPrevious(CharFaweChunk fs, BaseFullChunk sections, Map<?, ?> tiles, Collection<?>[] entities, Set<UUID> createdEntities, boolean all) throws Exception {
        return fs;
    }

    private int skip;

    @Override
    public File getSaveFolder() {
        if (world == null) {
            return new File("worlds" + File.separator + getWorldName() + File.separator + "region");
        }
        return new File("worlds" + File.separator + world.getFolderName() + File.separator + "region");
    }

    @Override
    public void sendBlockUpdate(FaweChunk chunk, FawePlayer... players) {
        try {
            boolean watching = true; // TODO check if player can see chunk
            if (!watching) return;
            final ArrayList<Block> blocks = new ArrayList<>();
            final int bx = chunk.getX() << 4;
            final int bz = chunk.getZ() << 4;
            chunk.forEachQueuedBlock(new FaweChunkVisitor() {
                @Override
                public void run(int localX, int y, int localZ, int combined) {
                    Block block = Block.get(FaweCache.getId(combined), FaweCache.getData(combined));
                    block.x = bx + localX;
                    block.y = y;
                    block.z = bz + localZ;
                    blocks.add(block);
                }
            });
            Map<Level, List<Player>> playerMap = new HashMap<>();
            for (FawePlayer player : players) {
                Player nukkitPlayer = ((FaweNukkitPlayer) player).parent;
                List<Player> list = playerMap.get(nukkitPlayer.getLevel());
                if (list == null) {
                    list = new ArrayList<>();
                    playerMap.put(nukkitPlayer.getLevel(), list);
                }
                list.add(nukkitPlayer);
            }
            Block[] blocksArray = blocks.toArray(new Block[blocks.size()]);
            for (Map.Entry<Level, List<Player>> levelListEntry : playerMap.entrySet()) {
                List<Player> playerList = levelListEntry.getValue();
                levelListEntry.getKey().sendBlocks(playerList.toArray(new Player[playerList.size()]), blocksArray, UpdateBlockPacket.FLAG_ALL_PRIORITY);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasSky() {
        if (world == null) return false;
        return world.getDimension() == 0;
    }

    @Override
    public void setFullbright(BaseFullChunk sections) {
        for (int y = 0; y < 256; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    sections.setBlockSkyLight(x, y, z, 15);
                }
            }
        }
    }

    @Override
    public boolean removeSectionLighting(BaseFullChunk section, int layer, boolean hasSky) {
        int minY = layer << 4;
        int maxY = minY + 15;
        for (int y = minY; y < maxY; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    section.setBlockSkyLight(x, y, z, 0);
                    section.setBlockLight(x, y, z, 0);
                }
            }
        }
        return true;
    }

    @Override
    public boolean removeLighting(BaseFullChunk sections, RelightMode mode, boolean hasSky) {
        for (int y = 0; y < 256; y++) {
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
        world.addLightUpdate(x, y, z);
    }

    @Override
    public void relightSky(int x, int y, int z) {
        world.updateBlockSkyLight(x, y, z);
    }

    @Override
    public void setSkyLight(BaseFullChunk chunkSection, int x, int y, int z, int value) {
        chunkSection.setBlockSkyLight(x & 15, y, z & 15, value);
    }

    @Override
    public void setBlockLight(BaseFullChunk chunkSection, int x, int y, int z, int value) {
        chunkSection.setBlockLight(x & 15, y, z & 15, value);
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
    public Level getImpWorld() {
        return world;
    }

    @Override
    public boolean regenerateChunk(Level level, int x, int z, BaseBiome biome, Long seed) {
        Map<Long, Entity> ents = level.getChunkEntities(x, z);
        if (!ents.isEmpty()) {
            Iterator<Map.Entry<Long, Entity>> iter = ents.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Long, Entity> entry = iter.next();
                Entity entity = entry.getValue();
                iter.remove();
                level.removeEntity(entity);
            }
        }
        level.regenerateChunk(x, z);
        return true;
    }

    @Override
    public FaweChunk getFaweChunk(int x, int z) {
        return new NukkitChunk(this, x, z);
    }

    @Override
    public BaseFullChunk loadChunk(Level level, int x, int z, boolean generate) {
        return level.getChunk(x, z, generate);
    }

    @Override
    public BaseFullChunk getSections(BaseFullChunk baseFullChunk) {
        return baseFullChunk;
    }

    @Override
    public BaseFullChunk getCachedChunk(Level level, int cx, int cz) {
        return level.getProvider().getLoadedChunk(cx, cz);
    }

    @Override
    public BaseFullChunk getCachedSections(Level level, int cx, int cz) {
        BaseFullChunk chunk = world.getChunk(cx, cz);
        return chunk;
    }

    @Override
    public int getBiome(BaseFullChunk baseFullChunk, int x, int z) {
        return baseFullChunk.getBiomeId(x & 15, z & 15);
    }

    @Override
    public BaseFullChunk getCachedSection(BaseFullChunk baseFullChunk, int cy) {
        return baseFullChunk;
    }

    @Override
    public int getCombinedId4Data(BaseFullChunk chunkSection, int x, int y, int z) {
        int id = chunkSection.getBlockId(x & 15, y, z & 15);
        if (FaweCache.hasData(id)) {
            int data = chunkSection.getBlockData(x & 15, y, z & 15);
            return (id << 4) + data;
        } else {
            return (id << 4);
        }
    }

    @Override
    public int getSkyLight(BaseFullChunk sections, int x, int y, int z) {
        return sections.getBlockSkyLight(x & 15, y, z & 15);
    }

    @Override
    public int getEmmittedLight(BaseFullChunk sections, int x, int y, int z) {
        return sections.getBlockLight(x & 15, y, z & 15);
    }
}
