package com.boydti.fawe.bukkit.v1_8;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.internal.Constants;
import java.util.*;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class BukkitChunk_1_8 extends CharFaweChunk<Chunk, BukkitQueue18R3> {
    /**
     * A FaweSections object represents a chunk and the blocks that you wish to change in it.
     *
     * @param parent
     * @param x
     * @param z
     */
    public BukkitChunk_1_8(FaweQueue parent, int x, int z) {
        super(parent, x, z);
    }

    public BukkitChunk_1_8(FaweQueue parent, int x, int z, char[][] ids, short[] count, short[] air, byte[] heightMap) {
        super(parent, x, z, ids, count, air, heightMap);
    }

    @Override
    public CharFaweChunk copy(boolean shallow) {
        BukkitChunk_1_8 copy;
        if (shallow) {
            copy = new BukkitChunk_1_8(getParent(), getX(), getZ(), ids, count, air, heightMap);
            copy.biomes = biomes;
            copy.chunk = chunk;
        } else {
            copy = new BukkitChunk_1_8(getParent(), getX(), getZ(), (char[][]) MainUtil.copyNd(ids), count.clone(), air.clone(), heightMap.clone());
            copy.biomes = biomes != null ? biomes.clone() : null;
            copy.chunk = chunk;
        }
        return copy;
    }

    @Override
    public Chunk getNewChunk() {
        return Bukkit.getWorld(getParent().getWorldName()).getChunkAt(getX(), getZ());
    }

    @Override
    public void start() {
        getChunk().load(true);
    }

    @Override
    public FaweChunk call() {
        CraftChunk chunk = (CraftChunk) this.getChunk();
        int bx = this.getX() << 4;
        int bz = this.getZ() << 4;
        net.minecraft.server.v1_8_R3.Chunk nmsChunk = chunk.getHandle();
        nmsChunk.f(true); // Modified
        nmsChunk.mustSave = true;
        net.minecraft.server.v1_8_R3.World nmsWorld = nmsChunk.getWorld();
        try {
            final boolean flag = getParent().getWorld().getEnvironment() == World.Environment.NORMAL;
            // Sections
            ChunkSection[] sections = nmsChunk.getSections();
            Map<BlockPosition, TileEntity> tiles = nmsChunk.getTileEntities();
            Collection<Entity>[] entities = nmsChunk.getEntitySlices();
            // Set heightmap
            getParent().setHeightMap(this, heightMap);
            // Remove entities
            for (int i = 0; i < 16; i++) {
                int count = this.getCount(i);
                if (count == 0 || getParent().getSettings().EXPERIMENTAL.KEEP_ENTITIES_IN_BLOCKS) {
                    continue;
                } else if (count >= 4096) {
                    Collection<Entity> ents = entities[i];
                    if (!ents.isEmpty()) {
                        synchronized (BukkitQueue_0.class) {
                            Iterator<Entity> iter = ents.iterator();
                            while (iter.hasNext()) {
                                Entity entity = iter.next();
                                if (entity instanceof EntityPlayer) {
                                    continue;
                                }
                                iter.remove();
                                nmsWorld.removeEntity(entity);
                            }
                        }
                    }
                } else {
                    Collection<Entity> ents = entities[i];
                    if (!ents.isEmpty()) {
                        char[] array = this.getIdArray(i);
                        if (array == null || entities[i] == null || entities[i].isEmpty()) continue;
                        Entity[] entsArr = ents.toArray(new Entity[ents.size()]);
                        synchronized (BukkitQueue_0.class) {
                            for (Entity entity : entsArr) {
                                if (entity instanceof EntityPlayer) {
                                    continue;
                                }
                                int x = (MathMan.roundInt(entity.locX) & 15);
                                int z = (MathMan.roundInt(entity.locZ) & 15);
                                int y = MathMan.roundInt(entity.locY);
                                if (y < 0 || y > 255) continue;
                                if (array[FaweCache.CACHE_J[y][z][x]] != 0) {
                                    nmsWorld.removeEntity(entity);
                                }
                            }
                        }
                    }
                }
            }
            HashSet<UUID> entsToRemove = this.getEntityRemoves();
            if (!entsToRemove.isEmpty()) {
                synchronized (BukkitQueue_0.class) {
                    for (int i = 0; i < entities.length; i++) {
                        Collection<Entity> ents = entities[i];
                        if (ents.isEmpty()) {
                            Entity[] entsArr = ents.toArray(new Entity[ents.size()]);
                            for (Entity entity : entsArr) {
                                if (entsToRemove.contains(entity.getUniqueID())) {
                                    nmsWorld.removeEntity(entity);
                                }
                            }
                        }
                    }
                }
            }
            // Set entities
            Set<UUID> createdEntities = new HashSet<>();
            Set<CompoundTag> entitiesToSpawn = this.getEntities();
            if (!entitiesToSpawn.isEmpty()) {
                synchronized (BukkitQueue_0.class) {
                    for (CompoundTag nativeTag : entitiesToSpawn) {
                        Map<String, Tag> entityTagMap = nativeTag.getValue();
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
                            if (nativeTag != null) {
                                NBTTagCompound tag = (NBTTagCompound) BukkitQueue18R3.fromNative(nativeTag);
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
            // Run change task if applicable
            if (getParent().getChangeTask() != null) {
                CharFaweChunk previous = getParent().getPrevious(this, sections, tiles, entities, createdEntities, false);
                getParent().getChangeTask().run(previous, this);
            }
            // Set blocks
            for (int j = 0; j < sections.length; j++) {
                int count = this.getCount(j);
                if (count == 0) {
                    continue;
                }
                char[] newArray = this.getIdArray(j);
                if (newArray == null) {
                    continue;
                }
                int countAir = this.getAir(j);
                ChunkSection section = sections[j];
                if (section != null && BukkitQueue18R3.isDirty != null) {
                    BukkitQueue18R3.isDirty.set(section, true);
                }
                if (section == null) {
                    if (count == countAir) {
                        continue;
                    }
                    sections[j] = section = new ChunkSection(j << 4, flag, newArray);
                    continue;
                }
                if (count >= 4096) {
                    if (count == countAir) {
                        sections[j] = null;
                        continue;
                    }
                    sections[j] = section = new ChunkSection(j << 4, flag, newArray);
                    continue;
                }
                int by = j << 4;
                char[] currentArray = section.getIdArray();
                int solid = 0;
                int existingId;
                for (int k = 0; k < newArray.length; k++) {
                    char n = newArray[k];
                    switch (n) {
                        case 0:
                            continue;
                        case 1:
                            existingId = currentArray[k];
                            if (existingId > 1) {
                                if (FaweCache.hasLight(existingId)) {
                                    int x = FaweCache.CACHE_X[j][k];
                                    int y = FaweCache.CACHE_Y[j][k];
                                    int z = FaweCache.CACHE_Z[j][k];
                                    getParent().getRelighter().addLightUpdate(bx + x, y, bz + z);
                                }
                                solid--;
                                currentArray[k] = 0;
                            }
                            continue;
                        default:
                            existingId = currentArray[k];
                            if (existingId <= 1) {
                                solid++;
                            } else if (FaweCache.hasLight(existingId)) {
                                int x = FaweCache.CACHE_X[j][k];
                                int y = FaweCache.CACHE_Y[j][k];
                                int z = FaweCache.CACHE_Z[j][k];
                                getParent().getRelighter().addLightUpdate(bx + x, y, bz + z);
                            }
                            currentArray[k] = n;
                            continue;
                    }
                }
                getParent().setCount(0, getParent().getNonEmptyBlockCount(section) + solid, section);
            }
            // Trim tiles
            Iterator<Map.Entry<BlockPosition, TileEntity>> iterator = tiles.entrySet().iterator();
            HashMap<BlockPosition, TileEntity> toRemove = null;
            while (iterator.hasNext()) {
                Map.Entry<BlockPosition, TileEntity> tile = iterator.next();
                BlockPosition pos = tile.getKey();
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
                    toRemove.put(tile.getKey(), tile.getValue());
                }
            }
            if (toRemove != null) {
                for (Map.Entry<BlockPosition, TileEntity> entry : toRemove.entrySet()) {
                    BlockPosition bp = entry.getKey();
                    TileEntity tile = entry.getValue();
                    tiles.remove(bp);
                    nmsWorld.t(bp);
                    tile.y();
                    tile.E();
                }

            }
            // Set biomes
            if (this.biomes != null) {
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
                BlockPosition pos = new BlockPosition(x, y, z); // Set pos
                TileEntity tileEntity = nmsWorld.getTileEntity(pos);
                if (tileEntity != null) {
                    NBTTagCompound tag = (NBTTagCompound) BukkitQueue18R3.fromNative(nativeTag);
                    tag.set("x", new NBTTagInt(x));
                    tag.set("y", new NBTTagInt(y));
                    tag.set("z", new NBTTagInt(z));
                    tileEntity.a(tag); // ReadTagIntoTile
                }
            }
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
        return this;
    }
}
