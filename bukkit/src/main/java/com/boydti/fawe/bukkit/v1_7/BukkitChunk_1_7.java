package com.boydti.fawe.bukkit.v1_7;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.internal.Constants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.v1_7_R4.ChunkPosition;
import net.minecraft.server.v1_7_R4.ChunkSection;
import net.minecraft.server.v1_7_R4.Entity;
import net.minecraft.server.v1_7_R4.EntityPlayer;
import net.minecraft.server.v1_7_R4.EntityTypes;
import net.minecraft.server.v1_7_R4.NBTTagCompound;
import net.minecraft.server.v1_7_R4.NibbleArray;
import net.minecraft.server.v1_7_R4.TileEntity;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_7_R4.CraftChunk;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class BukkitChunk_1_7 extends CharFaweChunk<Chunk, BukkitQueue17> {

    @Override
    public Chunk getNewChunk() {
        return Bukkit.getWorld(getParent().getWorldName()).getChunkAt(getX(), getZ());
    }

    public final byte[][] byteIds;
    public final NibbleArray[] datas;

    public BukkitChunk_1_7(FaweQueue parent, int x, int z) {
        super(parent, x, z);
        this.byteIds = new byte[16][];
        this.datas = new NibbleArray[16];
    }

    public BukkitChunk_1_7(FaweQueue parent, int x, int z, char[][] ids, short[] count, short[] air, byte[] heightMap, byte[][] byteIds, NibbleArray[] datas) {
        super(parent, x, z, ids, count, air, heightMap);
        this.byteIds = byteIds;
        this.datas = datas;
    }

    @Override
    public CharFaweChunk copy(boolean shallow) {
        BukkitChunk_1_7 copy;
        if (shallow) {
            copy = new BukkitChunk_1_7(getParent(), getX(), getZ(), ids, count, air, heightMap, byteIds, datas);
            copy.biomes = biomes;
            copy.chunk = chunk;
        } else {
            copy = new BukkitChunk_1_7(getParent(), getX(), getZ(), (char[][]) MainUtil.copyNd(ids), count.clone(), air.clone(), heightMap.clone(), (byte[][]) MainUtil.copyNd(byteIds), datas.clone());
            copy.biomes = biomes;
            copy.chunk = chunk;
            copy.biomes = biomes.clone();
            copy.chunk = chunk;
        }
        return copy;
    }

    public byte[] getByteIdArray(int i) {
        return this.byteIds[i];
    }

    public NibbleArray getDataArray(int i) {
        return datas[i];
    }

    @Override
    public void setBlock(int x, int y, int z, int id) {
        this.setBlock(x, y, z, id, 0);
    }

    @Override
    public void setBlock(int x, int y, int z, int id, int data) {
        int i = FaweCache.CACHE_I[y][z][x];
        int j = FaweCache.CACHE_J[y][z][x];
        byte[] vs = this.byteIds[i];
        char[] vs2 = this.ids[i];
        if (vs2 == null) {
            vs2 = this.ids[i] = new char[4096];
        }
        if (vs == null) {
            vs = this.byteIds[i] = new byte[4096];
        }
        this.count[i]++;
        switch (id) {
            case 0:
                vs[j] = 0;
                vs2[j] = (char) 1;
                return;
            case 11:
            case 39:
            case 40:
            case 51:
            case 74:
            case 89:
            case 122:
            case 124:
            case 138:
            case 169:
            case 213:
            case 130:
            case 76:
            case 62:
            case 50:
            case 10:
            default:
                vs2[j] = (char) ((id << 4) + data);
                vs[j] = (byte) id;
                if (data != 0) {
                    NibbleArray dataArray = datas[i];
                    if (dataArray == null) {
                        datas[i] = dataArray = new NibbleArray(new byte[2048], 4);
                    }
                    dataArray.a(x, y & 15, z, data);
                }
                return;
        }
    }

    @Override
    public void start() {
        getChunk().load(true);
    }

    @Override
    public FaweChunk call() {
        CraftChunk chunk = (CraftChunk) this.getChunk();
        net.minecraft.server.v1_7_R4.Chunk nmsChunk = chunk.getHandle();
        nmsChunk.e(); // Modified
        nmsChunk.mustSave = true;
        net.minecraft.server.v1_7_R4.World nmsWorld = nmsChunk.world;
        try {
            final boolean flag = getParent().getWorld().getEnvironment() == World.Environment.NORMAL;
            // Sections
            ChunkSection[] sections = nmsChunk.getSections();
            Map<ChunkPosition, TileEntity> tiles = nmsChunk.tileEntities;
            Collection<Entity>[] entities = nmsChunk.entitySlices;
            // Set heightmap
            getParent().setHeightMap(this, heightMap);
            // Remove entities
            for (int i = 0; i < 16; i++) {
                int count = this.getCount(i);
                if (count == 0) {
                    continue;
                } else if (count >= 4096) {
                    Collection<Entity> ents = entities[i];
                    if (!ents.isEmpty()) {
                        synchronized (BukkitQueue_0.adapter) {
                            ents.clear();
                        }
                    }
                } else {
                    Collection<Entity> ents = entities[i];
                    if (!ents.isEmpty()) {
                        char[] array = this.getIdArray(i);
                        ents = new ArrayList<>(entities[i]);
                        synchronized (BukkitQueue_0.adapter) {
                            for (Entity entity : ents) {
                                if (entity instanceof EntityPlayer) {
                                    continue;
                                }
                                int x = ((int) Math.round(entity.locX) & 15);
                                int z = ((int) Math.round(entity.locZ) & 15);
                                int y = (int) Math.round(entity.locY);
                                if (array == null || y < 0 || y > 255) {
                                    continue;
                                }
                                if (y < 0 || y > 255 || array[FaweCache.CACHE_J[y][z][x]] != 0) {
                                    nmsWorld.removeEntity(entity);
                                }
                            }
                        }
                    }
                }
            }
            HashSet<UUID> entsToRemove = this.getEntityRemoves();
            if (!entsToRemove.isEmpty()) {
                synchronized (BukkitQueue_0.adapter) {
                    for (int i = 0; i < entities.length; i++) {
                        Collection<Entity> ents = new ArrayList<>(entities[i]);
                        for (Entity entity : ents) {
                            if (entsToRemove.contains(entity.getUniqueID())) {
                                nmsWorld.removeEntity(entity);
                            }
                        }
                    }
                }
            }
            // Set entities
            Set<UUID> createdEntities = new HashSet<>();
            Set<CompoundTag> entitiesToSpawn = this.getEntities();
            if (!entitiesToSpawn.isEmpty()) {
                synchronized (BukkitQueue_0.adapter) {
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
                                NBTTagCompound tag = (NBTTagCompound) BukkitQueue17.methodFromNative.invoke(BukkitQueue17.adapter, nativeTag);
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
            // Trim tiles
            Iterator<Map.Entry<ChunkPosition, TileEntity>> iterator = tiles.entrySet().iterator();
            HashMap<ChunkPosition, TileEntity> toRemove = null;
            while (iterator.hasNext()) {
                Map.Entry<ChunkPosition, TileEntity> tile = iterator.next();
                ChunkPosition pos = tile.getKey();
                int lx = pos.x & 15;
                int ly = pos.y;
                int lz = pos.z & 15;
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
                for (Map.Entry<ChunkPosition, TileEntity> entry : toRemove.entrySet()) {
                    ChunkPosition bp = entry.getKey();
                    TileEntity tile = entry.getValue();
                    tiles.remove(bp);
                    tile.s();
                    nmsWorld.p(bp.x, bp.y, bp.z);
                    tile.u();
                }

            }
            // Set blocks
            for (int j = 0; j < sections.length; j++) {
                if (this.getCount(j) == 0) {
                    continue;
                }
                byte[] newIdArray = this.getByteIdArray(j);
                if (newIdArray == null) {
                    continue;
                }
                NibbleArray newDataArray = this.getDataArray(j);
                ChunkSection section = sections[j];
                if ((section == null) || (this.getCount(j) >= 4096)) {
                    sections[j] = section = new ChunkSection(j << 4, flag);
                    section.setIdArray(newIdArray);
                    if (newDataArray != null) {
                        section.setDataArray(newDataArray);
                    }
                    continue;
                }
                byte[] currentIdArray = (byte[]) BukkitQueue17.fieldIds.get(section);
                NibbleArray currentDataArray = (NibbleArray) BukkitQueue17.fieldData.get(section);
                boolean data = currentDataArray != null && newDataArray != null;
                if (!data) {
                    section.setDataArray(newDataArray);
                }
                if (currentIdArray == null) {
                    section.setIdArray(newIdArray);
                    continue;
                }
                int solid = 0;
                char[] charArray = this.getIdArray(j);
                for (int k = 0; k < newIdArray.length; k++) {
                    char combined = charArray[k];
                    switch (combined) {
                        case 0:
                            if (currentIdArray[k] != 0) {
                                solid++;
                            }
                            continue;
                        case 1:
                            currentIdArray[k] = 0;
                            continue;
                        default:
                            solid++;
                            currentIdArray[k] = newIdArray[k];
                            if (data) {
                                int dataByte = FaweCache.getData(combined);
                                int x = FaweCache.CACHE_X[0][k];
                                int y = FaweCache.CACHE_Y[0][k];
                                int z = FaweCache.CACHE_Z[0][k];
                                int newData = newDataArray.a(x, y, z);
                                currentDataArray.a(x, y, z, newData);
                            }
                            continue;
                    }
                }
                getParent().setCount(0, solid, section);
            }

            // Set biomes
            int[][] biomes = this.biomes;
            if (biomes != null) {
                for (int x = 0; x < 16; x++) {
                    int[] array = biomes[x];
                    if (array == null) {
                        continue;
                    }
                    for (int z = 0; z < 16; z++) {
                        int biome = array[z];
                        if (biome == 0) {
                            continue;
                        }
                        nmsChunk.m()[((z & 0xF) << 4 | x & 0xF)] = (byte) biome; // Biome array
                    }
                }
            }
            // Set tiles
            Map<Short, CompoundTag> tilesToSpawn = this.getTiles();
            int bx = this.getX() << 4;
            int bz = this.getZ() << 4;

            for (Map.Entry<Short, CompoundTag> entry : tilesToSpawn.entrySet()) {
                CompoundTag nativeTag = entry.getValue();
                short blockHash = entry.getKey();
                int x = (blockHash >> 12 & 0xF) + bx;
                int y = (blockHash & 0xFF);
                int z = (blockHash >> 8 & 0xF) + bz;
                TileEntity tileEntity = nmsWorld.getTileEntity(x, y, z);
                if (tileEntity != null) {
                    NBTTagCompound tag = (NBTTagCompound) BukkitQueue17.methodFromNative.invoke(BukkitQueue17.adapter, nativeTag);
                    tileEntity.a(tag); // ReadTagIntoTile
                }
            }
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
        return this;
    }
}
