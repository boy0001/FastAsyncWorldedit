package com.boydti.fawe.forge.v189;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class ForgeChunk_All extends CharFaweChunk<Chunk, ForgeQueue_All> {
    /**
     * A FaweSections object represents a chunk and the blocks that you wish to change in it.
     *
     * @param parent
     * @param x
     * @param z
     */
    public ForgeChunk_All(FaweQueue parent, int x, int z) {
        super(parent, x, z);
    }

    public ForgeChunk_All(FaweQueue parent, int x, int z, char[][] ids, short[] count, short[] air, byte[] heightMap) {
        super(parent, x, z, ids, count, air, heightMap);
    }

    @Override
    public CharFaweChunk copy(boolean shallow) {
        ForgeChunk_All copy;
        if (shallow) {
            copy = new ForgeChunk_All(getParent(), getX(), getZ(), ids, count, air, heightMap);
            copy.biomes = biomes;
            copy.chunk = chunk;
        } else {
            copy = new ForgeChunk_All(getParent(), getX(), getZ(), (char[][]) MainUtil.copyNd(ids), count.clone(), air.clone(), heightMap.clone());
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

    @Override
    public ForgeChunk_All call() {
        net.minecraft.world.chunk.Chunk nmsChunk = this.getChunk();
        int bx = this.getX() << 4;
        int bz = this.getZ() << 4;
        nmsChunk.setModified(true);
        nmsChunk.setHasEntities(true);
        net.minecraft.world.World nmsWorld = nmsChunk.getWorld();
        try {
            boolean flag = !nmsWorld.provider.getHasNoSky();
            // Sections
            ExtendedBlockStorage[] sections = nmsChunk.getBlockStorageArray();
            Map<BlockPos, TileEntity> tiles = nmsChunk.getTileEntityMap();
            ClassInheritanceMultiMap<Entity>[] entities = nmsChunk.getEntityLists();

            // Set heightmap
            getParent().setHeightMap(this, heightMap);

            // Remove entities
            for (int i = 0; i < 16; i++) {
                int count = this.getCount(i);
                if (count == 0) {
                    continue;
                } else if (count >= 4096) {
                    entities[i] = new ClassInheritanceMultiMap<>(Entity.class);
                } else {
                    char[] array = this.getIdArray(i);
                    if (array == null || entities[i] == null || entities[i].isEmpty()) continue;
                    Collection<Entity> ents = new ArrayList<>(entities[i]);
                    for (Entity entity : ents) {
                        if (entity instanceof EntityPlayer) {
                            continue;
                        }
                        int x = (MathMan.roundInt(entity.posX) & 15);
                        int z = (MathMan.roundInt(entity.posZ) & 15);
                        int y = MathMan.roundInt(entity.posY);
                        if (y < 0 || y > 255) continue;
                        if (array[FaweCache.CACHE_J[y][z][x]] != 0) {
                            nmsWorld.removeEntity(entity);
                        }
                    }
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
            Set<Map.Entry<BlockPos, TileEntity>> entryset = tiles.entrySet();
            Iterator<Map.Entry<BlockPos, TileEntity>> iterator = entryset.iterator();
            while (iterator.hasNext()) {
                Map.Entry<BlockPos, TileEntity> tile = iterator.next();
                BlockPos pos = tile.getKey();
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
                    tile.getValue().invalidate();;
                    iterator.remove();
                }
            }
            // Efficiently merge sections
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
                ExtendedBlockStorage section = sections[j];
                if ((section == null)) {
                    if (count == countAir) {
                        continue;
                    }
                    section = new ExtendedBlockStorage(j << 4, flag);
                    section.setData(newArray);
                    sections[j] = section;
                    continue;
                } else if (count >= 4096){
                    if (count == countAir) {
                        sections[j] = null;
                        continue;
                    }
                    section.setData(newArray);
                    getParent().setCount(0, count - this.getAir(j), section);
                    continue;
                }
                char[] currentArray = section.getData();
                boolean fill = true;
                int solid = 0;
                char existingId;
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
                if (fill) {
                    this.setCount(j, Short.MAX_VALUE);
                }
            }

            // Set biomes
            if (this.biomes != null) {
                byte[] currentBiomes = nmsChunk.getBiomeArray();
                for (int i = 0 ; i < this.biomes.length; i++) {
                    if (this.biomes[i] != 0) {
                        currentBiomes[i] = this.biomes[i];
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
                BlockPos pos = new BlockPos(x, y, z); // Set pos
                TileEntity tileEntity = nmsWorld.getTileEntity(pos);
                if (tileEntity != null) {
                    NBTTagCompound tag = (NBTTagCompound) ForgeQueue_All.methodFromNative.invoke(null, nativeTag);
                    tag.setInteger("x", pos.getX());
                    tag.setInteger("y", pos.getY());
                    tag.setInteger("z", pos.getZ());
                    tileEntity.readFromNBT(tag); // ReadTagIntoTile
                }
            }
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
        return this;
    }
}
