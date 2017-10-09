package com.boydti.fawe.nukkit.core;


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
import com.sk89q.jnbt.ShortTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Converts between Jcn.nukkit.nbt.tag. and Minecraft cn.nukkit.nbt.tag. classes.
 */
public final class NBTConverter {

    private static Field tagsField;

    private NBTConverter() {
    }

    static {
        try {
            tagsField = cn.nukkit.nbt.tag.CompoundTag.class.getDeclaredField("tags");
            tagsField.setAccessible(true);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static Map<String, cn.nukkit.nbt.tag.Tag> getMap(cn.nukkit.nbt.tag.CompoundTag other) {
        try {
            return (Map<String, cn.nukkit.nbt.tag.Tag>) tagsField.get(other);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public static CompoundTag fromNativeLazy(cn.nukkit.nbt.tag.CompoundTag other) {
        try {
            Map tags = (Map) tagsField.get(other);
            CompoundTag ct = new CompoundTag(tags);
            return ct;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public static cn.nukkit.nbt.tag.CompoundTag toNativeLazy(CompoundTag tag) {
        try {
            Map map = tag.getValue();
            cn.nukkit.nbt.tag.CompoundTag ct = new cn.nukkit.nbt.tag.CompoundTag();
            tagsField.set(ct, map);
            return ct;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public static CompoundTag fromNative(cn.nukkit.nbt.tag.CompoundTag other) {
        Map<String, cn.nukkit.nbt.tag.Tag> tags = other.getTags();
        Map<String, Tag> map = new HashMap<String, Tag>();
        for (Entry<String, cn.nukkit.nbt.tag.Tag> entry : tags.entrySet()) {
            map.put(entry.getKey(), fromNative(entry.getValue()));
        }
        CompoundTag tag = new CompoundTag(map);
        return tag;
    }

    public static cn.nukkit.nbt.tag.Tag toNative(Tag tag) {
        if (tag instanceof IntArrayTag) {
            return toNative((IntArrayTag) tag);

        } else if (tag instanceof ListTag) {
            return toNative((ListTag) tag);

        } else if (tag instanceof LongTag) {
            return toNative((LongTag) tag);

        } else if (tag instanceof StringTag) {
            return toNative((StringTag) tag);

        } else if (tag instanceof IntTag) {
            return toNative((IntTag) tag);

        } else if (tag instanceof ByteTag) {
            return toNative((ByteTag) tag);

        } else if (tag instanceof ByteArrayTag) {
            return toNative((ByteArrayTag) tag);

        } else if (tag instanceof CompoundTag) {
            return toNative((CompoundTag) tag);

        } else if (tag instanceof FloatTag) {
            return toNative((FloatTag) tag);

        } else if (tag instanceof ShortTag) {
            return toNative((ShortTag) tag);

        } else if (tag instanceof DoubleTag) {
            return toNative((DoubleTag) tag);
        } else {
            throw new IllegalArgumentException("Can't convert tag of type " + tag.getClass().getCanonicalName());
        }
    }

    private static cn.nukkit.nbt.tag.IntArrayTag toNative(IntArrayTag tag) {
        int[] value = tag.getValue();
        return new cn.nukkit.nbt.tag.IntArrayTag("", Arrays.copyOf(value, value.length));
    }

    private static cn.nukkit.nbt.tag.ListTag toNative(ListTag tag) {
        cn.nukkit.nbt.tag.ListTag list = new cn.nukkit.nbt.tag.ListTag();
        for (Tag child : (List<? extends Tag>) tag.getValue()) {
            if (child instanceof EndTag) {
                continue;
            }
            list.add(toNative(child));
        }
        return list;
    }

    private static cn.nukkit.nbt.tag.LongTag toNative(LongTag tag) {
        return new cn.nukkit.nbt.tag.LongTag("", tag.getValue());
    }

    private static cn.nukkit.nbt.tag.StringTag toNative(StringTag tag) {
        return new cn.nukkit.nbt.tag.StringTag("", tag.getValue());
    }

    private static cn.nukkit.nbt.tag.IntTag toNative(IntTag tag) {
        return new cn.nukkit.nbt.tag.IntTag("", tag.getValue());
    }

    private static cn.nukkit.nbt.tag.ByteTag toNative(ByteTag tag) {
        return new cn.nukkit.nbt.tag.ByteTag("", tag.getValue());
    }

    private static cn.nukkit.nbt.tag.ByteArrayTag toNative(ByteArrayTag tag) {
        byte[] value = tag.getValue();
        return new cn.nukkit.nbt.tag.ByteArrayTag("", Arrays.copyOf(value, value.length));
    }

    private static cn.nukkit.nbt.tag.CompoundTag toNative(CompoundTag tag) {
        cn.nukkit.nbt.tag.CompoundTag compound = new cn.nukkit.nbt.tag.CompoundTag();
        for (Entry<String, Tag> child : tag.getValue().entrySet()) {
            cn.nukkit.nbt.tag.Tag value = toNative(child.getValue());
            value.setName(child.getKey());
            compound.put(child.getKey(), value);
        }
        return compound;
    }

    private static cn.nukkit.nbt.tag.FloatTag toNative(FloatTag tag) {
        return new cn.nukkit.nbt.tag.FloatTag("", tag.getValue());
    }

    private static cn.nukkit.nbt.tag.ShortTag toNative(ShortTag tag) {
        return new cn.nukkit.nbt.tag.ShortTag("", tag.getValue());
    }

    private static cn.nukkit.nbt.tag.DoubleTag toNative(DoubleTag tag) {
        return new cn.nukkit.nbt.tag.DoubleTag("", tag.getValue());
    }

    private static Tag fromNative(cn.nukkit.nbt.tag.Tag other) {
        if (other instanceof cn.nukkit.nbt.tag.IntArrayTag) {
            return fromNative((cn.nukkit.nbt.tag.IntArrayTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.ListTag) {
            return fromNative((cn.nukkit.nbt.tag.ListTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.EndTag) {
            return fromNative((cn.nukkit.nbt.tag.EndTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.LongTag) {
            return fromNative((cn.nukkit.nbt.tag.LongTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.StringTag) {
            return fromNative((cn.nukkit.nbt.tag.StringTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.IntTag) {
            return fromNative((cn.nukkit.nbt.tag.IntTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.ByteTag) {
            return fromNative((cn.nukkit.nbt.tag.ByteTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.ByteArrayTag) {
            return fromNative((cn.nukkit.nbt.tag.ByteArrayTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.CompoundTag) {
            return fromNative((cn.nukkit.nbt.tag.CompoundTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.FloatTag) {
            return fromNative((cn.nukkit.nbt.tag.FloatTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.ShortTag) {
            return fromNative((cn.nukkit.nbt.tag.ShortTag) other);

        } else if (other instanceof cn.nukkit.nbt.tag.DoubleTag) {
            return fromNative((cn.nukkit.nbt.tag.DoubleTag) other);
        } else {
            throw new IllegalArgumentException("Can't convert other of type " + other.getClass().getCanonicalName());
        }
    }

    private static IntArrayTag fromNative(cn.nukkit.nbt.tag.IntArrayTag other) {
        int[] value = other.data;
        return new IntArrayTag(Arrays.copyOf(value, value.length));
    }

    private static ListTag fromNative(cn.nukkit.nbt.tag.ListTag other) {
        other = (cn.nukkit.nbt.tag.ListTag) other.copy();
        List<Tag> list = new ArrayList<Tag>();
        Class<? extends Tag> listClass = StringTag.class;
        int tags = other.size();
        for (int i = 0; i < tags; i++) {
            Tag child = fromNative(other.get(0));
            other.remove(0);
            list.add(child);
            listClass = child.getClass();
        }
        return new ListTag(listClass, list);
    }

    private static EndTag fromNative(cn.nukkit.nbt.tag.EndTag other) {
        return new EndTag();
    }

    private static LongTag fromNative(cn.nukkit.nbt.tag.LongTag other) {
        return new LongTag(other.data);
    }

    private static StringTag fromNative(cn.nukkit.nbt.tag.StringTag other) {
        return new StringTag(other.data);
    }

    private static IntTag fromNative(cn.nukkit.nbt.tag.IntTag other) {
        return new IntTag(other.data);
    }

    private static ByteTag fromNative(cn.nukkit.nbt.tag.ByteTag other) {
        return new ByteTag((byte) other.data);
    }

    private static ByteArrayTag fromNative(cn.nukkit.nbt.tag.ByteArrayTag other) {
        byte[] value = other.data;
        return new ByteArrayTag(Arrays.copyOf(value, value.length));
    }

    private static FloatTag fromNative(cn.nukkit.nbt.tag.FloatTag other) {
        return new FloatTag(other.data);
    }

    private static ShortTag fromNative(cn.nukkit.nbt.tag.ShortTag other) {
        return new ShortTag((short) other.data);
    }

    private static DoubleTag fromNative(cn.nukkit.nbt.tag.DoubleTag other) {
        return new DoubleTag(other.data);
    }

}