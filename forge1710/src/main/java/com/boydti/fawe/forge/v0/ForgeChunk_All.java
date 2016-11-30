package com.boydti.fawe.forge.v0;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.object.BytePair;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class ForgeChunk_All extends CharFaweChunk<Chunk, ForgeQueue_All> {

    public final byte[][] byteIds;
    public final NibbleArray[] datas;

    public ForgeChunk_All(FaweQueue parent, int x, int z) {
        super(parent, x, z);
        this.byteIds = new byte[16][];
        this.datas = new NibbleArray[16];
    }

    public ForgeChunk_All(FaweQueue parent, int x, int z, char[][] ids, short[] count, short[] air, byte[] heightMap, byte[][] byteIds, NibbleArray[] datas) {
        super(parent, x, z, ids, count, air, heightMap);
        this.byteIds = byteIds;
        this.datas = datas;
    }

    @Override
    public CharFaweChunk copy(boolean shallow) {
        ForgeChunk_All copy;
        if (shallow) {
            copy = new ForgeChunk_All(getParent(), getX(), getZ(), ids, count, air, heightMap, byteIds, datas);
            copy.biomes = biomes;
            copy.chunk = chunk;
        } else {
            copy = new ForgeChunk_All(getParent(), getX(), getZ(), (char[][]) MainUtil.copyNd(ids), count.clone(), air.clone(), heightMap.clone(), (byte[][]) MainUtil.copyNd(byteIds), datas.clone());
            copy.biomes = biomes;
            copy.chunk = chunk;
            copy.biomes = biomes.clone();
            copy.chunk = chunk;
        }
        return copy;
    }

    @Override
    public Chunk getNewChunk() {
        World world = ((ForgeQueue_All) getParent()).getWorld();
        return world.getChunkProvider().provideChunk(getX(), getZ());
    }

    public byte[] getByteIdArray(int i) {
        return this.byteIds[i];
    }

    public NibbleArray getDataArray(int i) {
        return datas[i];
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
//                this.relight[i]++;
            default:
                vs2[j] = (char) ((id << 4) + data);
                vs[j] = (byte) id;
                if (data != 0) {
                    NibbleArray dataArray = datas[i];
                    if (dataArray == null) {
                        datas[i] = dataArray = new NibbleArray(4096, 4);
                    }
                    dataArray.set(x, y & 15, z, data);
                }
                return;
        }
    }

    @Override
    public ForgeChunk_All call() {
        net.minecraft.world.chunk.Chunk nmsChunk = this.getChunk();
        net.minecraft.world.World nmsWorld = nmsChunk.worldObj;
        nmsChunk.setChunkModified();
        nmsChunk.sendUpdates = true;
        try {
            boolean flag = !nmsWorld.provider.hasNoSky;
            // Sections
            ExtendedBlockStorage[] sections = nmsChunk.getBlockStorageArray();
            Map<ChunkPosition, TileEntity> tiles = nmsChunk.chunkTileEntityMap;
            List<Entity>[] entities = nmsChunk.entityLists;

            // Set heightmap
            getParent().setHeightMap(this, heightMap);

            // Remove entities
            for (int i = 0; i < 16; i++) {
                int count = this.getCount(i);
                if (count == 0) {
                    continue;
                } else if (count >= 4096) {
                    entities[i].clear();
                } else {
                    char[] array = this.getIdArray(i);
                    Collection<Entity> ents = new ArrayList<>(entities[i]);
                    for (Entity entity : ents) {
                        if (entity instanceof EntityPlayer) {
                            continue;
                        }
                        int x = ((int) Math.round(entity.posX) & 15);
                        int z = ((int) Math.round(entity.posZ) & 15);
                        int y = (int) Math.round(entity.posY);
                        if (array == null) {
                            continue;
                        }
                        if (y < 0 || y > 255 || array[FaweCache.CACHE_J[y][z][x]] != 0) {
                            nmsWorld.removeEntity(entity);
                        }
                    }
                }
            }
            // Set entities
            Set<UUID> createdEntities = new HashSet<>();
            Set<CompoundTag> entitiesToSpawn = this.getEntities();
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
                Entity entity = EntityList.createEntityByName(id, nmsWorld);
                if (entity != null) {
                    NBTTagCompound tag = (NBTTagCompound) ForgeQueue_All.methodFromNative.invoke(null, nativeTag);
                    tag.removeTag("UUIDMost");
                    tag.removeTag("UUIDLeast");
                    entity.readFromNBT(tag);
                    entity.setPositionAndRotation(x, y, z, yaw, pitch);
                    nmsWorld.spawnEntityInWorld(entity);
                }
            }
            // Run change task if applicable
            if (getParent().getChangeTask() != null) {
                CharFaweChunk previous = getParent().getPrevious(this, sections, tiles, entities, createdEntities, false);
                getParent().getChangeTask().run(previous, this);
            }
            // Trim tiles
            Set<Map.Entry<ChunkPosition, TileEntity>> entryset = tiles.entrySet();
            Iterator<Map.Entry<ChunkPosition, TileEntity>> iterator = entryset.iterator();
            while (iterator.hasNext()) {
                Map.Entry<ChunkPosition, TileEntity> tile = iterator.next();
                ChunkPosition pos = tile.getKey();
                int lx = pos.chunkPosX & 15;
                int ly = pos.chunkPosY;
                int lz = pos.chunkPosZ & 15;
                int j = FaweCache.CACHE_I[ly][lz][lx];
                char[] array = this.getIdArray(j);
                if (array == null) {
                    continue;
                }
                int k = FaweCache.CACHE_J[ly][lz][lx];
                if (array[k] != 0) {
                    tile.getValue().invalidate();;
                    iterator.remove();
                }
            }
            HashSet<UUID> entsToRemove = this.getEntityRemoves();
            if (entsToRemove.size() > 0) {
                for (int i = 0; i < entities.length; i++) {
                    Collection<Entity> ents = new ArrayList<>(entities[i]);
                    for (Entity entity : ents) {
                        if (entsToRemove.contains(entity.getUniqueID())) {
                            nmsWorld.removeEntity(entity);
                        }
                    }
                }
            }
            // Efficiently merge sections
            for (int j = 0; j < sections.length; j++) {
                if (this.getCount(j) == 0) {
                    continue;
                }
                byte[] newIdArray = this.getByteIdArray(j);
                if (newIdArray == null) {
                    continue;
                }
                NibbleArray newDataArray = this.getDataArray(j);
                ExtendedBlockStorage section = sections[j];
                if ((section == null) || (this.getCount(j) >= 4096)) {
                    sections[j] = section = new ExtendedBlockStorage(j << 4, !getParent().getWorld().provider.hasNoSky);
                    section.setBlockLSBArray(newIdArray);
                    section.setBlockMetadataArray(newDataArray);
                    continue;
                }
                byte[] currentIdArray = section.getBlockLSBArray();
                NibbleArray currentDataArray = section.getMetadataArray();
                boolean data = currentDataArray != null && newDataArray != null;
                if (!data) {
                    section.setBlockMetadataArray(newDataArray);
                }
                boolean fill = true;
                int solid = 0;
                char[] charArray = this.getIdArray(j);
                for (int k = 0; k < newIdArray.length; k++) {
                    char combined = charArray[k];
                    switch (combined) {
                        case 0:
                            fill = false;
                            continue;
                        case 1:
                            fill = false;
                            if (currentIdArray[k] != 0) {
                                solid++;
                            }
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
                                int newData = newDataArray.get(x, y, z);
                                currentDataArray.set(x, y, z, newData);
                            }
                            continue;
                    }
                }
                getParent().setCount(0, solid, section);
                if (fill) {
                    this.setCount(j, Short.MAX_VALUE);
                }
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
                        nmsChunk.getBiomeArray()[((z & 0xF) << 4 | x & 0xF)] = (byte) biome;
                    }
                }
            }
            // Set tiles
            Map<BytePair, CompoundTag> tilesToSpawn = this.getTiles();
            int bx = this.getX() << 4;
            int bz = this.getZ() << 4;

            for (Map.Entry<BytePair, CompoundTag> entry : tilesToSpawn.entrySet()) {
                CompoundTag nativeTag = entry.getValue();
                BytePair pair = entry.getKey();
                int x = MathMan.unpair16x((byte) pair.get0()) + bx;
                int y = pair.get1() & 0xFF;
                int z = MathMan.unpair16y((byte) pair.get0()) + bz;
                TileEntity tileEntity = nmsWorld.getTileEntity(x, y, z);
                if (tileEntity != null) {
                    NBTTagCompound tag = (NBTTagCompound) ForgeQueue_All.methodFromNative.invoke(null, nativeTag);
                    tileEntity.readFromNBT(tag); // ReadTagIntoTile
                }
            }
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
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
                    nmsChunk.getBiomeArray()[((z & 0xF) << 4 | x & 0xF)] = (byte) biome;
                }
            }
        }
        return this;
    }

    public boolean hasEntities(Chunk nmsChunk) {
        for (int i = 0; i < nmsChunk.entityLists.length; i++) {
            List slice = nmsChunk.entityLists[i];
            if (slice != null && !slice.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
