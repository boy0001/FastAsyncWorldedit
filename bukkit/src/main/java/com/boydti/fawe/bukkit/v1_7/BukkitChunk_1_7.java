package com.boydti.fawe.bukkit.v1_7;

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
import net.minecraft.server.v1_7_R4.*;
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
            copy.biomes = biomes != null ? biomes.clone() : null;
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
            this.count[i]++;
        } else {
            switch (vs2[j]) {
                case 0:
                    this.count[i]++;
                    break;
                case 1:
                    this.air[i]--;
                    break;
            }
        }
        if (vs == null) {
            vs = this.byteIds[i] = new byte[4096];
        }
        switch (id) {
            case 0:
                this.air[i]++;
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
                                NBTTagCompound tag = (NBTTagCompound) BukkitQueue17.fromNative(nativeTag);
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
                byte[] newIdArray = this.getByteIdArray(j);
                if (newIdArray == null) {
                    continue;
                }
                int countAir = this.getAir(j);
                NibbleArray newDataArray = this.getDataArray(j);
                ChunkSection section = sections[j];
                if (section == null) {
                    if (count == countAir) {
                        continue;
                    }
                    sections[j] = section = new ChunkSection(j << 4, flag);
                    BukkitQueue17.fieldCompactId.set(section, 0);
                    BukkitQueue17.fieldIds.set(section, newIdArray);
                    getParent().setCount(0, count - this.getAir(j), section);
                    BukkitQueue17.fieldCompactData.set(section, (byte) 0);
                    BukkitQueue17.fieldData.set(section, newDataArray);
                    continue;
                }
                if (count >= 4096) {
                    if (countAir >= 4096) {
                        sections[j] = null;
                        continue;
                    }
                    BukkitQueue17.fieldCompactId.set(section, 0);
                    BukkitQueue17.fieldIds.set(section, newIdArray);
                    getParent().setCount(0, count - this.getAir(j), section);
                    BukkitQueue17.fieldCompactData.set(section, (byte) 0);
                    BukkitQueue17.fieldData.set(section, newDataArray);
                    continue;
                }
                char[] charArray = this.getIdArray(j);
                byte[] currentIdArray = (byte[]) BukkitQueue17.fieldIds.get(section);
                NibbleArray currentDataArray = (NibbleArray) BukkitQueue17.fieldData.get(section);
                boolean data = currentDataArray != null && newDataArray != null;
                if (currentDataArray == null) {
                    int compactData = ((byte) BukkitQueue17.fieldCompactData.get(section)) & 0xFF;
                    if (compactData != 0 && newDataArray == null) {
                        newDataArray = new NibbleArray(new byte[2048], 4);
                        byte full = (byte) ((compactData << 4) + compactData);
                        Arrays.fill(newDataArray.a, full);
                        for (int i = 0; i < newDataArray.a.length; i++) {
                            int i2 = i << 1;
                            int i3 = i2 + 1;
                            byte val = newDataArray.a[i];
                            if (FaweCache.hasData(charArray[i3] >> 4)) {
                                newDataArray.a[i] = (byte) (val & 15);
                            }
                            if (FaweCache.hasData(charArray[i2] >> 4)) {
                                newDataArray.a[i] = (byte) (val & 240);
                            }
                        }
                    }
                    else if (newDataArray != null) {
                        if (compactData != 0) {
                            byte full = (byte) ((compactData << 4) + compactData);
                            for (int i = 0; i < newDataArray.a.length; i++) {
                                int i2 = i << 1;
                                int i3 = i2 + 1;
                                byte val = newDataArray.a[i];
                                if (charArray[i3] != 0) {
                                    if (charArray[i2] != 0) continue;
                                    newDataArray.a[i] = (byte) (val & 240 | compactData);
                                    continue;
                                }
                                if (charArray[i2] != 0) {
                                    if (charArray[i3] != 0) continue;
                                    newDataArray.a[i] = (byte) (val & 15 | (compactData) << 4);
                                    continue;
                                }
                                newDataArray.a[i] = full;
                            }
                        }
                        BukkitQueue17.fieldCompactData.set(section, (byte) 0);
                        BukkitQueue17.fieldData.set(section, newDataArray);
                    }
                } else if (newDataArray == null) {
                    for (int i = 0; i < currentDataArray.a.length; i++) {
                        int i2 = i << 1;
                        int i3 = i2 + 1;
                        byte val = currentDataArray.a[i];
                        if (newIdArray[i3] == 0) {
                            if (newIdArray[i2] != 0) currentDataArray.a[i] = (byte) (val & 240);
                            continue;
                        }
                        if (newIdArray[i2] == 0) {
                            if (newIdArray[i3] != 0) currentDataArray.a[i] = (byte) (val & 15);
                            continue;
                        }
                        currentDataArray.a[i] = 0;
                    }
                }
                int solid = 0;
                if (currentIdArray == null) {
                    byte id = (byte) ((int) BukkitQueue17.fieldCompactId.get(section));
                    if (id != 0) {
                        solid = 4096;
                        for (int i = 0; i < 4096; i++) {
                            if (charArray[i] == 0) newIdArray[i] = id;
                            else if (newIdArray[i] == 0) solid--;
                        }
                    } else {
                        for (int i = 0; i < 4096; i++) if (newIdArray[i] != 0) solid++;
                    }
                    BukkitQueue17.fieldCompactId.set(section, 0);
                    BukkitQueue17.fieldIds.set(section, newIdArray);
                } else {
                    for (int i = 0; i < 4096; i++) {
                        if (charArray[i] != 0) currentIdArray[i] = newIdArray[i];
                    }
                    for (int i = 0; i < 4096; i++) if (currentIdArray[i] != 0) solid++;
                }
                if (data) {
                    for (int k = 0; k < 4096; k++) {
                        int value = charArray[k];
                        if (value != 0) {
                            int dataByte = FaweCache.getData(value);
                            int kShift = k >> 1;
                            if ((k & 1) == 0) {
                                currentDataArray.a[kShift] = (byte) (currentDataArray.a[kShift] & 240 | dataByte);
                            } else {
                                currentDataArray.a[kShift] = (byte) (currentDataArray.a[kShift] & 15 | (dataByte) << 4);
                            }
                        }
                    }
                }
                getParent().setCount(0, solid, section);
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
                    nmsWorld.p(bp.x, bp.y, bp.z);
                    tile.s();
                    tile.u();
                }

            }
            // Set biomes
            if (this.biomes != null) {
                byte[] currentBiomes = nmsChunk.m();
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
                    NBTTagCompound tag = (NBTTagCompound) BukkitQueue17.fromNative(nativeTag);
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
