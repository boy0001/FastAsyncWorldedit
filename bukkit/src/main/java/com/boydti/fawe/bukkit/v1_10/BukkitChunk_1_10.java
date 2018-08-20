
package com.boydti.fawe.bukkit.v1_10;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.*;
import com.sk89q.worldedit.internal.Constants;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import net.minecraft.server.v1_10_R1.*;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class BukkitChunk_1_10 extends CharFaweChunk<Chunk, BukkitQueue_1_10> {

    public DataPaletteBlock[] sectionPalettes;

    /**
     * A FaweSections object represents a chunk and the blocks that you wish to change in it.
     *
     * @param parent
     * @param x
     * @param z
     */
    public BukkitChunk_1_10(FaweQueue parent, int x, int z) {
        super(parent, x, z);
    }

    public BukkitChunk_1_10(FaweQueue parent, int x, int z, char[][] ids, short[] count, short[] air, byte[] heightMap) {
        super(parent, x, z, ids, count, air, heightMap);
    }

    @Override
    public CharFaweChunk copy(boolean shallow) {
        BukkitChunk_1_10 copy;
        if (shallow) {
            copy = new BukkitChunk_1_10(getParent(), getX(), getZ(), ids, count, air, heightMap);
            copy.biomes = biomes;
            copy.chunk = chunk;
        } else {
            copy = new BukkitChunk_1_10(getParent(), getX(), getZ(), (char[][]) MainUtil.copyNd(ids), count.clone(), air.clone(), heightMap.clone());
            copy.biomes = biomes != null ? biomes.clone() : null;
            copy.chunk = chunk;
        }
        if (sectionPalettes != null) {
            copy.sectionPalettes = new DataPaletteBlock[16];
            try {
                Field fieldBits = DataPaletteBlock.class.getDeclaredField("b");
                fieldBits.setAccessible(true);
                Field fieldPalette = DataPaletteBlock.class.getDeclaredField("c");
                fieldPalette.setAccessible(true);
                Field fieldSize = DataPaletteBlock.class.getDeclaredField("e");
                fieldSize.setAccessible(true);
                for (int i = 0; i < sectionPalettes.length; i++) {
                    DataPaletteBlock current = sectionPalettes[i];
                    if (current == null) {
                        continue;
                    }
                    // Clone palette
                    DataPalette currentPalette = (DataPalette) fieldPalette.get(current);
                    if (!(currentPalette instanceof DataPaletteGlobal)) {
                        current.a(128, null);
                    }
                    DataPaletteBlock paletteBlock = newDataPaletteBlock();
                    currentPalette = (DataPalette) fieldPalette.get(current);
                    if (!(currentPalette instanceof DataPaletteGlobal)) {
                        throw new RuntimeException("Palette must be global!");
                    }
                    fieldPalette.set(paletteBlock, currentPalette);
                    // Clone size
                    fieldSize.set(paletteBlock, fieldSize.get(current));
                    // Clone palette
                    DataBits currentBits = (DataBits) fieldBits.get(current);
                    DataBits newBits = new DataBits(1, 0);
                    for (Field field : DataBits.class.getDeclaredFields()) {
                        field.setAccessible(true);
                        Object currentValue = field.get(currentBits);
                        if (currentValue instanceof long[]) {
                            currentValue = ((long[]) currentValue).clone();
                        }
                        field.set(newBits, currentValue);
                    }
                    fieldBits.set(paletteBlock, newBits);
                    copy.sectionPalettes[i] = paletteBlock;
                }
            } catch (Throwable e) {
                MainUtil.handleError(e);
            }
        }
        return copy;
    }

    @Override
    public Chunk getNewChunk() {
        return ((com.boydti.fawe.bukkit.v1_10.BukkitQueue_1_10) getParent()).getWorld().getChunkAt(getX(), getZ());
    }

    public DataPaletteBlock newDataPaletteBlock() {
        try {
            return new DataPaletteBlock();
        } catch (Throwable e) {
            try {
                Constructor<DataPaletteBlock> constructor = DataPaletteBlock.class.getDeclaredConstructor(IBlockData[].class);
                return constructor.newInstance((Object) null);
            } catch (Throwable e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    public void optimize() {
        if (sectionPalettes != null) {
            return;
        }
        char[][] arrays = getCombinedIdArrays();
        IBlockData lastBlock = null;
        char lastChar = Character.MAX_VALUE;
        for (int layer = 0; layer < 16; layer++) {
            if (getCount(layer) > 0) {
                if (sectionPalettes == null) {
                    sectionPalettes = new DataPaletteBlock[16];
                }
                DataPaletteBlock palette = newDataPaletteBlock();
                char[] blocks = getIdArray(layer);
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            char combinedId = blocks[FaweCache.CACHE_J[y][z][x]];
                            if (combinedId > 1) {
                                palette.setBlock(x, y, z, Block.getById(combinedId >> 4).fromLegacyData(combinedId & 0xF));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void start() {
        getChunk().load(true);
    }

    private void removeEntity(Entity entity) {
        entity.b(false);
        entity.die();
        entity.valid = false;
    }

    public void storeBiomes(byte[] biomes) {
        this.biomes = Arrays.copyOf(biomes, biomes.length);
    }

    public boolean storeEntity(Entity ent) throws InvocationTargetException, IllegalAccessException {
        if (ent instanceof EntityPlayer) {
            return false;
        }
        int x = (MathMan.roundInt(ent.locX) & 15);
        int z = (MathMan.roundInt(ent.locZ) & 15);
        int y = (MathMan.roundInt(ent.locY) & 0xFF);
        int i = FaweCache.CACHE_I[y][z][x];
        int j = FaweCache.CACHE_J[y][z][x];
        String id = EntityTypes.b(ent);
        if (id != null) {
            NBTTagCompound tag = new NBTTagCompound();
            ent.e(tag); // readEntityIntoTag
            CompoundTag nativeTag = (CompoundTag) getParent().toNative(tag);
            Map<String, Tag> map = ReflectionUtils.getMap(nativeTag.getValue());
            map.put("Id", new StringTag(id));
            setEntity(nativeTag);
            return true;
        } else {
            return false;
        }
    }

    public boolean storeTile(TileEntity tile, BlockPosition pos) {
        NBTTagCompound tag = new NBTTagCompound();
        CompoundTag nativeTag = getParent().getTag(tile);
        setTile(pos.getX() & 15, pos.getY(), pos.getZ() & 15, nativeTag);
        return true;
    }

    @Override
    public FaweChunk call() {
        try {
            BukkitChunk_1_10_Copy copy = getParent().getChangeTask() != null ? new BukkitChunk_1_10_Copy(getParent(), getX(), getZ()) : null;
            final Chunk chunk = this.getChunk();
            final World world = chunk.getWorld();
            int bx = this.getX() << 4;
            int bz = this.getZ() << 4;
            final boolean flag = world.getEnvironment() == World.Environment.NORMAL;
            net.minecraft.server.v1_10_R1.Chunk nmsChunk = ((org.bukkit.craftbukkit.v1_10_R1.CraftChunk) chunk).getHandle();
            nmsChunk.f(true); // Set Modified
            nmsChunk.mustSave = true;
            net.minecraft.server.v1_10_R1.World nmsWorld = nmsChunk.world;
            net.minecraft.server.v1_10_R1.ChunkSection[] sections = nmsChunk.getSections();
            final Collection<net.minecraft.server.v1_10_R1.Entity>[] entities = (Collection<net.minecraft.server.v1_10_R1.Entity>[]) getParent().getEntitySlices.invoke(nmsChunk);
            Map<net.minecraft.server.v1_10_R1.BlockPosition, net.minecraft.server.v1_10_R1.TileEntity> tiles = nmsChunk.getTileEntities();
            // Set heightmap
            getParent().setHeightMap(this, heightMap);
            // Remove entities
            HashSet<UUID> entsToRemove = this.getEntityRemoves();
            if (!entsToRemove.isEmpty()) {
                for (int i = 0; i < entities.length; i++) {
                    Collection<net.minecraft.server.v1_10_R1.Entity> ents = entities[i];
                    if (!ents.isEmpty()) {
                        Iterator<net.minecraft.server.v1_10_R1.Entity> iter = ents.iterator();
                        while (iter.hasNext()) {
                            net.minecraft.server.v1_10_R1.Entity entity = iter.next();
                            if (entsToRemove.contains(entity.getUniqueID())) {
                                if (copy != null) {
                                    copy.storeEntity(entity);
                                }
                                removeEntity(entity);
                                iter.remove();
                            }
                        }
                    }
                }
            }
            for (int i = 0; i < entities.length; i++) {
                int count = this.getCount(i);
                if (count == 0 || getParent().getSettings().EXPERIMENTAL.KEEP_ENTITIES_IN_BLOCKS) {
                    continue;
                } else if (count >= 4096) {
                    Collection<net.minecraft.server.v1_10_R1.Entity> ents = entities[i];
                    if (!ents.isEmpty()) {
                        synchronized (BukkitQueue_0.class) {
                            Iterator<Entity> iter = ents.iterator();
                            while (iter.hasNext()) {
                                Entity entity = iter.next();
                                if (entity instanceof EntityPlayer) {
                                    continue;
                                }
                                iter.remove();
                                if (copy != null) {
                                    copy.storeEntity(entity);
                                }
                                removeEntity(entity);
                            }
                        }
                    }
                } else {
                    Collection<net.minecraft.server.v1_10_R1.Entity> ents = entities[i];
                    if (!ents.isEmpty()) {
                        char[] array = this.getIdArray(i);
                        if (array == null) continue;
                        Iterator<net.minecraft.server.v1_10_R1.Entity> iter = ents.iterator();
                        while (iter.hasNext()) {
                            net.minecraft.server.v1_10_R1.Entity entity = iter.next();
                            if (entity instanceof net.minecraft.server.v1_10_R1.EntityPlayer) {
                                continue;
                            }
                            int x = (MathMan.roundInt(entity.locX) & 15);
                            int z = (MathMan.roundInt(entity.locZ) & 15);
                            int y = MathMan.roundInt(entity.locY);
                            if (y < 0 || y > 255) continue;
                            if (array[FaweCache.CACHE_J[y][z][x]] != 0) {
                                if (copy != null) {
                                    copy.storeEntity(entity);
                                }
                                iter.remove();
                                removeEntity(entity);
                            }
                        }
                    }
                }
            }
            // Set entities
            Set<CompoundTag> entitiesToSpawn = this.getEntities();
            Set<UUID> createdEntities = new HashSet<>();
            if (!entitiesToSpawn.isEmpty()) {
                synchronized (BukkitQueue_0.class) {
                    for (CompoundTag nativeTag : entitiesToSpawn) {
                        Map<String, Tag> entityTagMap = ReflectionUtils.getMap(nativeTag.getValue());
                        StringTag idTag = (StringTag) entityTagMap.get("Id");
                        ListTag posTag = (ListTag) entityTagMap.get("Pos");
                        ListTag rotTag = (ListTag) entityTagMap.get("Rotation");
                        if (idTag == null || posTag == null || rotTag == null) {
                            Fawe.debug("Unknown entity tag: " + nativeTag);
                            continue;
                        }
                        double x = posTag.getDouble(0);
                        double y = posTag.getDouble(1);
                        double z = posTag.getDouble(2);
                        float yaw = rotTag.getFloat(0);
                        float pitch = rotTag.getFloat(1);
                        String id = idTag.getValue();
                        Entity entity = EntityTypes.createEntityByName(id, nmsWorld);
                        if (entity != null) {
                            UUID uuid = entity.getUniqueID();
                            entityTagMap.put("UUIDMost", new LongTag(uuid.getMostSignificantBits()));
                            entityTagMap.put("UUIDLeast", new LongTag(uuid.getLeastSignificantBits()));
                            if (nativeTag != null) {
                                NBTTagCompound tag = (NBTTagCompound) BukkitQueue_1_10.fromNative(nativeTag);
                                for (String name : Constants.NO_COPY_ENTITY_NBT_FIELDS) {
                                    tag.remove(name);
                                }
                                entity.f(tag);
                            }
                            entity.setLocation(x, y, z, yaw, pitch);
                            nmsWorld.addEntity(entity, CreatureSpawnEvent.SpawnReason.CUSTOM);
                            createdEntities.add(entity.getUniqueID());
                        }
                    }
                }
            }
            // Set blocks
            for (int j = 0; j < sections.length; j++) {
                int count = this.getCount(j);
                if (count == 0) {
                    continue;
                }
                int countAir = this.getAir(j);
                final char[] array = this.getIdArray(j);
                if (array == null) {
                    continue;
                }
                net.minecraft.server.v1_10_R1.ChunkSection section = sections[j];
                if (copy != null) {
                    copy.storeSection(section, j);
                }
                if (section == null) {
                    if (count == countAir) {
                        continue;
                    }
                    if (this.sectionPalettes != null && this.sectionPalettes[j] != null) {
                        section = sections[j] = getParent().newChunkSection(j << 4, flag, null);
                        getParent().setPalette(section, this.sectionPalettes[j]);
                        getParent().setCount(0, count - this.getAir(j), section);
                        continue;
                    } else {
                        sections[j] = getParent().newChunkSection(j << 4, flag, array);
                        continue;
                    }
                } else if (count >= 4096) {
                    if (countAir >= 4096) {
                        sections[j] = null;
                        continue;
                    }
                    if (this.sectionPalettes != null && this.sectionPalettes[j] != null) {
                        getParent().setPalette(section, this.sectionPalettes[j]);
                        getParent().setCount(0, count - this.getAir(j), section);
                        continue;
                    } else {
                        sections[j] = getParent().newChunkSection(j << 4, flag, array);
                        continue;
                    }
                }
                int by = j << 4;
                net.minecraft.server.v1_10_R1.DataPaletteBlock nibble = section.getBlocks();
                int nonEmptyBlockCount = 0;
                net.minecraft.server.v1_10_R1.IBlockData existing;
                for (int y = 0; y < 16; y++) {
                    short[][] i1 = FaweCache.CACHE_J[y];
                    for (int z = 0; z < 16; z++) {
                        short[] i2 = i1[z];
                        for (int x= 0; x < 16; x++) {
                            char combinedId = array[i2[x]];
                            switch (combinedId) {
                                case 0:
                                    continue;
                                case 1:
                                    existing = nibble.a(x, y, z);
                                    if (existing != BukkitQueue_1_10.air) {
                                        if (existing.d() > 0) {
                                            getParent().getRelighter().addLightUpdate(bx + x, by + y, bz + z);
                                        }
                                        nonEmptyBlockCount--;
                                    }
                                    nibble.setBlock(x, y, z, BukkitQueue_1_10.air);
                                    continue;
                                default:
                                    existing = nibble.a(x, y, z);
                                    if (existing != BukkitQueue_1_10.air) {
                                        if (existing.d() > 0) {
                                            getParent().getRelighter().addLightUpdate(bx + x, by + y, bz + z);
                                        }
                                    } else {
                                        nonEmptyBlockCount++;
                                    }
                                    nibble.setBlock(x, y, z, getParent().IBD_CACHE[(int) combinedId]);
                            }
                        }
                    }
                }
                getParent().setCount(0, getParent().getNonEmptyBlockCount(section) + nonEmptyBlockCount, section);
            }
            // Trim tiles
            Iterator<Map.Entry<net.minecraft.server.v1_10_R1.BlockPosition, net.minecraft.server.v1_10_R1.TileEntity>> iterator = tiles.entrySet().iterator();
            HashMap<net.minecraft.server.v1_10_R1.BlockPosition, net.minecraft.server.v1_10_R1.TileEntity> toRemove = null;
            while (iterator.hasNext()) {
                Map.Entry<net.minecraft.server.v1_10_R1.BlockPosition, net.minecraft.server.v1_10_R1.TileEntity> tile = iterator.next();
                net.minecraft.server.v1_10_R1.BlockPosition pos = tile.getKey();
                int lx = pos.getX() & 15;
                int ly = pos.getY();
                int lz = pos.getZ() & 15;
                int j = FaweCache.CACHE_I[ly][lz][lx];
                char[] array = this.getIdArray(j);
                if (array == null) {
                    continue;
                }
                int k = FaweCache.CACHE_J[ly][lz][lx];
                if (array[k] != 0) {
                    if (toRemove == null) {
                        toRemove = new HashMap<>();
                    }
                    if (copy != null) {
                        copy.storeTile(tile.getValue(), tile.getKey());
                    }
                    toRemove.put(tile.getKey(), tile.getValue());
                }
            }
            if (toRemove != null) {
                for (Map.Entry<net.minecraft.server.v1_10_R1.BlockPosition, net.minecraft.server.v1_10_R1.TileEntity> entry : toRemove.entrySet()) {
                    net.minecraft.server.v1_10_R1.BlockPosition bp = entry.getKey();
                    net.minecraft.server.v1_10_R1.TileEntity tile = entry.getValue();
                    tiles.remove(bp);
                    nmsWorld.s(bp);
                    tile.y();
                    tile.invalidateBlockCache();
                }

            }
            // Set biomes
            if (this.biomes != null) {
                if (copy != null) {
                    copy.storeBiomes(nmsChunk.getBiomeIndex());
                }
                byte[] currentBiomes = nmsChunk.getBiomeIndex();
                for (int i = 0 ; i < this.biomes.length; i++) {
                    byte biome = this.biomes[i];
                    if (biome != 0) {
                        if (biome == -1) biome = 0;
                        currentBiomes[i] = biome;
                    }
                }
            }
            // Set tiles
            Map<Short, CompoundTag> tilesToSpawn = this.getTiles();
            for (Map.Entry<Short, CompoundTag> entry : tilesToSpawn.entrySet()) {
                CompoundTag nativeTag = entry.getValue();
                short blockHash = entry.getKey();
                int x = (blockHash >> 12 & 0xF) + bx;
                int y = (blockHash & 0xFF);
                int z = (blockHash >> 8 & 0xF) + bz;
                net.minecraft.server.v1_10_R1.BlockPosition pos = new net.minecraft.server.v1_10_R1.BlockPosition(x, y, z); // Set pos
                net.minecraft.server.v1_10_R1.TileEntity tileEntity = nmsWorld.getTileEntity(pos);
                if (tileEntity != null) {
                    net.minecraft.server.v1_10_R1.NBTTagCompound tag = (net.minecraft.server.v1_10_R1.NBTTagCompound) com.boydti.fawe.bukkit.v1_10.BukkitQueue_1_10.fromNative(nativeTag);
                    tag.set("x", new NBTTagInt(x));
                    tag.set("y", new NBTTagInt(y));
                    tag.set("z", new NBTTagInt(z));
                    tileEntity.a(tag); // ReadTagIntoTile
                }
            }
            // Change task
            if (copy != null) {
                getParent().getChangeTask().run(copy, this);
            }
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
        return this;
    }
}
