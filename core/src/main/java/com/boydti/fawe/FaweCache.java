package com.boydti.fawe;

import com.boydti.fawe.object.PseudoRandom;
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
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.blocks.BaseBlock;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FaweCache {
    /**
     * [ y | x | z ] => index
     */
    public final static short[][][] CACHE_I = new short[256][16][16];
    /**
     * [ y | x | z ] => index
     */
    public final static short[][][] CACHE_J = new short[256][16][16];

    /**
     *  [ i | j ] => x
     */
    public final static byte[][] CACHE_X = new byte[16][4096];
    /**
     *  [ i | j ] => y
     */
    public final static short[][] CACHE_Y = new short[16][4096];
    /**
     *  [ i | j ] => z
     */
    public final static byte[][] CACHE_Z = new byte[16][4096];

    /**
     * [ combined ] => id
     * (combined >> 4) = id
     */
    public final static short[] CACHE_ID = new short[65535];
    /**
     * [ combined ] => data
     * (combined & 0xF) = data
     */
    public final static byte[] CACHE_DATA = new byte[65535];

    /**
     * Immutable BaseBlock cache
     * [ combined ] => block
     */
    public final static BaseBlock[] CACHE_BLOCK = new BaseBlock[Short.MAX_VALUE];

    /**
     * Faster than java random (since it just needs to look random)
     */
    public final static PseudoRandom RANDOM = new PseudoRandom();

    /**
     * Get the cached BaseBlock object for an id/data<br>
     *  - The block is immutable
     * @param id
     * @param data
     * @return
     */
    public static BaseBlock getBlock(int id, int data) {
        return CACHE_BLOCK[(id << 4) + data];
    }

    /**
     * Get the combined data for a block
     * @param id
     * @param data
     * @return
     */
    public static int getCombined(int id, int data) {
        return (id << 4) + data;
    }

    public static int getId(int combined) {
        return combined >> 4;
    }

    public static int getData(int combined) {
        return combined & 15;
    }

    /**
     * Get the combined id for a block
     * @param block
     * @return
     */
    public static int getCombined(BaseBlock block) {
        return getCombined(block.getId(), block.getData());
    }

    static {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    final short i = (short) (y >> 4);
                    final short j = (short) (((y & 0xF) << 8) | (z << 4) | x);
                    CACHE_I[y][x][z] = i;
                    CACHE_J[y][x][z] = j;
                    CACHE_X[i][j] = (byte) x;
                    CACHE_Y[i][j] = (short) y;
                    CACHE_Z[i][j] = (byte) z;
                }
            }
        }
        for (int i = 0; i < 65535; i++) {
            final int j = i >> 4;
        final int k = i & 0xF;
        CACHE_ID[i] = (short) j;
        CACHE_DATA[i] = (byte) k;
        }

        for (int i = 0; i < Short.MAX_VALUE; i++) {
            int id = i >> 4;
            int data = i & 0xf;
            CACHE_BLOCK[i] = new BaseBlock(id, data) {
                @Override
                public void setData(int data) {
                    throw new IllegalStateException("Cannot set data");
                }

                @Override
                public void setId(int id) {
                    throw new IllegalStateException("Cannot set id");
                }

                @Override
                public BaseBlock flip() {
                    BaseBlock clone = new BaseBlock(getId(), getData(), getNbtData());
                    return clone.flip();
                }

                @Override
                public BaseBlock flip(CuboidClipboard.FlipDirection direction) {
                    BaseBlock clone = new BaseBlock(getId(), getData(), getNbtData());
                    return clone.flip(direction);
                }

                @Override
                public boolean hasWildcardData() {
                    return true;
                }
            };
        }
    }

    /**
     * Check if an id might have data
     * @param id
     * @return
     */
    public static boolean hasData(int id) {
        switch (id) {
            case 0:
            case 2:
            case 4:
            case 13:
            case 14:
            case 15:
            case 20:
            case 21:
            case 22:
            case 25:
            case 30:
            case 32:
            case 37:
            case 39:
            case 40:
            case 41:
            case 42:
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
            case 51:
            case 52:
            case 56:
            case 57:
            case 58:
            case 60:
            case 7:
            case 9:
            case 11:
            case 73:
            case 74:
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            case 85:
            case 87:
            case 88:
            case 101:
            case 102:
            case 103:
            case 110:
            case 112:
            case 113:
            case 117:
            case 121:
            case 122:
            case 123:
            case 124:
            case 129:
            case 133:
            case 138:
            case 137:
            case 140:
            case 165:
            case 166:
            case 169:
            case 170:
            case 172:
            case 173:
            case 174:
            case 181:
            case 182:
            case 188:
            case 189:
            case 190:
            case 191:
            case 192:
                return false;
            default:
                return true;
        }
    }

    /**
     * Check if an id might have nbt
     * @param id
     * @return
     */
    public static boolean hasNBT(int id) {
        switch (id) {
            case 54:
            case 130:
            case 142:
            case 27:
            case 137:
            case 52:
            case 154:
            case 84:
            case 25:
            case 144:
            case 138:
            case 176:
            case 177:
            case 63:
            case 119:
            case 68:
            case 323:
            case 117:
            case 116:
            case 28:
            case 66:
            case 157:
            case 61:
            case 62:
            case 140:
            case 146:
            case 149:
            case 150:
            case 158:
            case 23:
            case 123:
            case 124:
            case 29:
            case 33:
            case 151:
            case 178:
                return true;
            default:
                return false;
        }
    }

    public static Map<String, Object> asMap(Object... pairs) {
        HashMap<String, Object> map = new HashMap<String, Object>(pairs.length >> 1);
        for (int i = 0; i < pairs.length; i+=2) {
            String key = (String) pairs[i];
            Object value = pairs[i + 1];
            map.put(key, value);
        }
        return map;
    }


    public static ShortTag asTag(short value) {
        return new ShortTag(value);
    }

    public static IntTag asTag(int value) {
        return new IntTag(value);
    }

    public static DoubleTag asTag(double value) {
        return new DoubleTag(value);
    }

    public static ByteTag asTag(byte value) {
        return new ByteTag(value);
    }

    public static FloatTag asTag(float value) {
        return new FloatTag(value);
    }

    public static LongTag asTag(long value) {
        return new LongTag(value);
    }

    public static ByteArrayTag asTag(byte[] value) {
        return new ByteArrayTag(value);
    }

    public static IntArrayTag asTag(int[] value) {
        return new IntArrayTag(value);
    }

    public static StringTag asTag(String value) {
        return new StringTag(value);
    }

    public static CompoundTag asTag(Map<String, Object> value) {
        HashMap<String, Tag> map = new HashMap<>();
        for (Map.Entry<String, Object> entry : value.entrySet()) {
            Object child = entry.getValue();
            Tag tag = asTag(child);
            map.put(entry.getKey(), tag);
        }
        return new CompoundTag(map);
    }

    public static Tag asTag(Object value) {
        if (value instanceof Integer) {
            return asTag((int) value);
        } else if (value instanceof  Short) {
            return asTag((short) value);
        } else if (value instanceof  Double) {
            return asTag((double) value);
        } else if (value instanceof  Byte) {
            return asTag((byte) value);
        } else if (value instanceof  Float) {
            return asTag((float) value);
        } else if (value instanceof  Long) {
            return asTag((long) value);
        } else if (value instanceof  String) {
            return asTag((String) value);
        } else if (value instanceof  Map) {
            return asTag((Map) value);
        } else if (value instanceof  Collection) {
            return asTag((Collection) value);
        } else if (value instanceof  byte[]) {
            return asTag((byte[]) value);
        } else if (value instanceof  Tag) {
            return (Tag) value;
        } else if (value == null) {
            return null;
        } else {
            Class<? extends Object> clazz = value.getClass();
            if (clazz.getName().startsWith("com.intellectualcrafters.jnbt")) {
                try {
                    if (clazz.getName().equals("com.intellectualcrafters.jnbt.EndTag")) {
                        return new EndTag();
                    }
                    Field field = clazz.getDeclaredField("value");
                    field.setAccessible(true);
                    return asTag(field.get(value));
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    public static ListTag asTag(Object... values) {
        Class clazz = null;
        List<Tag> list = new ArrayList<>();
        for (Object value : values) {
            Tag tag = asTag(value);
            if (clazz == null) {
                clazz = tag.getClass();
            }
            list.add(tag);
        }
        return new ListTag(clazz, list);
    }

    public static ListTag asTag(Collection values) {
        Class clazz = null;
        List<Tag> list = new ArrayList<>();
        for (Object value : values) {
            Tag tag = asTag(value);
            if (clazz == null) {
                clazz = tag.getClass();
            }
            list.add(tag);
        }
        return new ListTag(clazz, list);
    }
}
