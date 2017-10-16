package com.boydti.fawe.bukkit.v1_11;

import com.google.common.base.Preconditions;
import com.sk89q.jnbt.ByteArrayTag;
import com.sk89q.jnbt.ByteTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.EndTag;
import com.sk89q.jnbt.FloatTag;
import com.sk89q.jnbt.IntArrayTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.LongTag;
import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.ShortTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.internal.Constants;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import net.minecraft.server.v1_11_R1.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_11_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_11_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;

public final class FaweAdapter_1_11 implements BukkitImplAdapter
{
    private final Logger logger = Logger.getLogger(getClass().getCanonicalName());
    private final Field nbtListTagListField;
    private final Method nbtCreateTagMethod;

    public FaweAdapter_1_11() throws NoSuchFieldException, NoSuchMethodException {
        this.nbtListTagListField = NBTTagList.class.getDeclaredField("list");
        this.nbtListTagListField.setAccessible(true);

        this.nbtCreateTagMethod = NBTBase.class.getDeclaredMethod("createTag", new Class[] { Byte.TYPE });
        this.nbtCreateTagMethod.setAccessible(true);
    }

    private static void readTagIntoTileEntity(NBTTagCompound tag, TileEntity tileEntity)
    {
        tileEntity.a(tag);
    }

    private static void readTileEntityIntoTag(TileEntity tileEntity, NBTTagCompound tag)
    {
        tileEntity.save(tag);
    }

    @Nullable
    private static String getEntityId(Entity entity)
    {
        return EntityTypes.b(entity);
    }

    @Nullable
    private static Entity createEntityFromId(String id, World world)
    {
        return EntityTypes.a(new MinecraftKey(id), world);
    }

    private static void readTagIntoEntity(NBTTagCompound tag, Entity entity)
    {
        entity.f(tag);
    }

    private static void readEntityIntoTag(Entity entity, NBTTagCompound tag)
    {
        entity.e(tag);
    }

    public int getBlockId(Material material)
    {
        return material.getId();
    }

    public Material getMaterial(int id)
    {
        return Material.getMaterial(id);
    }

    public int getBiomeId(Biome biome)
    {
        BiomeBase mcBiome = CraftBlock.biomeToBiomeBase(biome);
        return mcBiome != null ? BiomeBase.a(mcBiome) : 0;
    }

    public Biome getBiome(int id)
    {
        BiomeBase mcBiome = BiomeBase.getBiome(id);
        return CraftBlock.biomeBaseToBiome(mcBiome);
    }

    public BaseBlock getBlock(Location location)
    {
        Preconditions.checkNotNull(location);

        CraftWorld craftWorld = (CraftWorld)location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        Block bukkitBlock = location.getBlock();
        BaseBlock block = new BaseBlock(bukkitBlock.getTypeId(), bukkitBlock.getData());

        TileEntity te = craftWorld.getHandle().getTileEntity(new BlockPosition(x, y, z));
        if (te != null)
        {
            NBTTagCompound tag = new NBTTagCompound();
            readTileEntityIntoTag(te, tag);
            block.setNbtData((CompoundTag)toNative(tag));
        }
        return block;
    }

    public boolean setBlock(Location location, BaseBlock block, boolean notifyAndLight)
    {
        Preconditions.checkNotNull(location);
        Preconditions.checkNotNull(block);

        CraftWorld craftWorld = (CraftWorld)location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        boolean changed = location.getBlock().setTypeIdAndData(block.getId(), (byte)block.getData(), notifyAndLight);

        CompoundTag nativeTag = block.getNbtData();
        if (nativeTag != null)
        {
            TileEntity tileEntity = craftWorld.getHandle().getTileEntity(new BlockPosition(x, y, z));
            if (tileEntity != null)
            {
                NBTTagCompound tag = (NBTTagCompound)fromNative(nativeTag);
                tag.set("x", new NBTTagInt(x));
                tag.set("y", new NBTTagInt(y));
                tag.set("z", new NBTTagInt(z));
                readTagIntoTileEntity(tag, tileEntity);
            }
        }
        return changed;
    }

    public BaseEntity getEntity(org.bukkit.entity.Entity entity)
    {
        Preconditions.checkNotNull(entity);

        CraftEntity craftEntity = (CraftEntity)entity;
        Entity mcEntity = craftEntity.getHandle();

        String id = getEntityId(mcEntity);
        if (id != null) {
            NBTTagCompound tag = new NBTTagCompound();
            readEntityIntoTag(mcEntity, tag);
            CompoundTag weTag = (CompoundTag) toNative(tag);
            return new BaseEntity(id, weTag);
        }
        return null;
    }

    @Nullable
    public org.bukkit.entity.Entity createEntity(Location location, BaseEntity state)
    {
        Preconditions.checkNotNull(location);
        Preconditions.checkNotNull(state);

        CraftWorld craftWorld = (CraftWorld)location.getWorld();
        WorldServer worldServer = craftWorld.getHandle();

        Entity createdEntity = createEntityFromId(state.getTypeId(), craftWorld.getHandle());
        if (createdEntity != null)
        {
            CompoundTag nativeTag = state.getNbtData();
            if (nativeTag != null)
            {
                NBTTagCompound tag = (NBTTagCompound)fromNative(nativeTag);
                for (String name : Constants.NO_COPY_ENTITY_NBT_FIELDS) {
                    tag.remove(name);
                }
                readTagIntoEntity(tag, createdEntity);
            }
            createdEntity.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());

            worldServer.addEntity(createdEntity, CreatureSpawnEvent.SpawnReason.CUSTOM);
            return createdEntity.getBukkitEntity();
        }
        return null;
    }

    public Tag toNative(NBTBase foreign) {
        if (foreign == null) {
            return null;
        }
        if (foreign instanceof NBTTagCompound) {
            Map<String, Tag> values = new HashMap<String, Tag>();
            Set<String> foreignKeys = ((NBTTagCompound) foreign).c(); // map.keySet

            for (String str : foreignKeys) {
                NBTBase base = ((NBTTagCompound) foreign).get(str);
                values.put(str, toNative(base));
            }
            return new CompoundTag(values);
        } else if (foreign instanceof NBTTagByte) {
            return new ByteTag(((NBTTagByte) foreign).g()); // getByte
        } else if (foreign instanceof NBTTagByteArray) {
            return new ByteArrayTag(((NBTTagByteArray) foreign).c()); // data
        } else if (foreign instanceof NBTTagDouble) {
            return new DoubleTag(((NBTTagDouble) foreign).asDouble()); // getDouble
        } else if (foreign instanceof NBTTagFloat) {
            return new FloatTag(((NBTTagFloat) foreign).i()); // getFloat
        } else if (foreign instanceof NBTTagInt) {
            return new IntTag(((NBTTagInt) foreign).e()); // getInt
        } else if (foreign instanceof NBTTagIntArray) {
            return new IntArrayTag(((NBTTagIntArray) foreign).d()); // data
        } else if (foreign instanceof NBTTagList) {
            try {
                return toNativeList((NBTTagList) foreign);
            } catch (Throwable e) {
                logger.log(Level.WARNING, "Failed to convert NBTTagList", e);
                return new ListTag(ByteTag.class, new ArrayList<ByteTag>());
            }
        } else if (foreign instanceof NBTTagLong) {
            return new LongTag(((NBTTagLong) foreign).d()); // getLong
        } else if (foreign instanceof NBTTagShort) {
            return new ShortTag(((NBTTagShort) foreign).f()); // getShort
        } else if (foreign instanceof NBTTagString) {
            return new StringTag(((NBTTagString) foreign).c_()); // data
        } else if (foreign instanceof NBTTagEnd) {
            return new EndTag();
        } else {
            throw new IllegalArgumentException("Don't know how to make native " + foreign.getClass().getCanonicalName());
        }
    }

    public ListTag toNativeList(NBTTagList foreign) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        List<Tag> values = new ArrayList<Tag>();
        int type = foreign.g();

        List foreignList;
        foreignList = (List) nbtListTagListField.get(foreign);
        for (int i = 0; i < foreign.size(); i++) {
            NBTBase element = (NBTBase) foreignList.get(i);
            values.add(toNative(element)); // List elements shouldn't have names
        }

        Class<? extends Tag> cls = NBTConstants.getClassFromType(type);
        return new ListTag(cls, values);
    }

    public NBTBase fromNative(Tag foreign) {
        if (foreign == null) {
            return null;
        }
        if (foreign instanceof CompoundTag) {
            NBTTagCompound tag = new NBTTagCompound();
            for (Map.Entry<String, Tag> entry : ((CompoundTag) foreign)
                    .getValue().entrySet()) {
                tag.set(entry.getKey(), fromNative(entry.getValue()));
            }
            return tag;
        } else if (foreign instanceof ByteTag) {
            return new NBTTagByte(((ByteTag) foreign).getValue());
        } else if (foreign instanceof ByteArrayTag) {
            return new NBTTagByteArray(((ByteArrayTag) foreign).getValue());
        } else if (foreign instanceof DoubleTag) {
            return new NBTTagDouble(((DoubleTag) foreign).getValue());
        } else if (foreign instanceof FloatTag) {
            return new NBTTagFloat(((FloatTag) foreign).getValue());
        } else if (foreign instanceof IntTag) {
            return new NBTTagInt(((IntTag) foreign).getValue());
        } else if (foreign instanceof IntArrayTag) {
            return new NBTTagIntArray(((IntArrayTag) foreign).getValue());
        } else if (foreign instanceof ListTag) {
            NBTTagList tag = new NBTTagList();
            ListTag<Tag> foreignList = (ListTag) foreign;
            for (Tag t : foreignList.getValue()) {
                tag.add(fromNative(t));
            }
            return tag;
        } else if (foreign instanceof LongTag) {
            return new NBTTagLong(((LongTag) foreign).getValue());
        } else if (foreign instanceof ShortTag) {
            return new NBTTagShort(((ShortTag) foreign).getValue());
        } else if (foreign instanceof StringTag) {
            return new NBTTagString(((StringTag) foreign).getValue());
        } else if (foreign instanceof EndTag) {
            try {
                return (NBTBase) nbtCreateTagMethod.invoke(null, (byte) 0);
            } catch (Exception e) {
                return null;
            }
        } else {
            throw new IllegalArgumentException("Don't know how to make NMS " + foreign.getClass().getCanonicalName());
        }
    }

}
