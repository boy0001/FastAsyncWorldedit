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
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.blocks.ImmutableBlock;
import com.sk89q.worldedit.blocks.ImmutableDatalessBlock;
import com.sk89q.worldedit.blocks.ImmutableNBTBlock;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FaweCache {
    /**
     * [ y | z | x ] => index
     */
    public final static short[][][] CACHE_I = new short[256][16][16];
    /**
     * [ y | z | x ] => index
     */
    public final static short[][][] CACHE_J = new short[256][16][16];

    /**
     * [ i | j ] => x
     */
    public final static byte[][] CACHE_X = new byte[16][4096];
    /**
     * [ i | j ] => y
     */
    public final static short[][] CACHE_Y = new short[16][4096];
    /**
     * [ i | j ] => z
     */
    public final static byte[][] CACHE_Z = new byte[16][4096];

    public final static boolean[] CACHE_PASSTHROUGH = new boolean[65535];
    public final static boolean[] CACHE_TRANSLUSCENT = new boolean[65535];

    /**
     * Immutable biome cache
     */
    public final static BaseBiome[] CACHE_BIOME = new BaseBiome[256];

    /**
     * Immutable BaseBlock cache
     * [ combined ] => block
     */
    public final static BaseBlock[] CACHE_BLOCK = new BaseBlock[Character.MAX_VALUE + 1];

    public final static BaseItem[] CACHE_ITEM = new BaseItem[Character.MAX_VALUE + 1];

    /**
     * Faster than java random (since it just needs to look random)
     */
    public final static PseudoRandom RANDOM = new PseudoRandom();

    public final static Color[] CACHE_COLOR = new Color[Character.MAX_VALUE + 1];

    /**
     * Get the cached BaseBlock object for an id/data<br>
     * - The block is immutable
     *
     * @param id
     * @param data
     * @return
     */
    public static final BaseBlock getBlock(int id, int data) {
        return CACHE_BLOCK[(id << 4) + data];
    }

    public static final BaseItem getItem(int id, int data) {
        return CACHE_ITEM[(id << 4) + data];
    }

    /**
     * Get the combined data for a block
     *
     * @param id
     * @param data
     * @return
     */
    public static final int getCombined(int id, int data) {
        return (id << 4) + data;
    }

    public static final int getId(int combined) {
        return combined >> 4;
    }

    public static final int getData(int combined) {
        return combined & 15;
    }

    /**
     * Get the combined id for a block
     *
     * @param block
     * @return
     */
    public static final int getCombined(BaseBlock block) {
        return getCombined(block.getId(), block.getData());
    }

    public static final BaseBlock getBlock(String block, boolean allAllowed, boolean allowNoData) throws InputParseException {
        ParserContext context = new ParserContext();
        context.setRestricted(!allAllowed);
        context.setPreferringWildcard(allowNoData);
        return WorldEdit.getInstance().getBlockFactory().parseFromInput(block, context);
    }

    public static final Color getColor(int id, int data) {
        Color exact = CACHE_COLOR[getCombined(id, data)];
        if (exact != null) {
            return exact;
        }
        Color base = CACHE_COLOR[getCombined(id, 0)];
        if (base != null) {
            return base;
        }
        return CACHE_COLOR[0];
    }

    public static final BaseBiome getBiome(int id) {
        return CACHE_BIOME[id];
    }

    static {
        for (int i = 0; i < Character.MAX_VALUE; i++) {
            int id = i >> 4;
            int data = i & 0xf;
            if (FaweCache.hasNBT(id)) {
                CACHE_BLOCK[i] = new ImmutableNBTBlock(id, data);
            } else if (FaweCache.hasData(id)) {
                CACHE_BLOCK[i] = new ImmutableBlock(id, data);
            } else {
                CACHE_BLOCK[i] = new ImmutableDatalessBlock(id);
            }
            CACHE_ITEM[i] = new BaseItem(id, (short) data) {

                @Override
                public void setData(short data) {
                    throw new IllegalStateException("Cannot set data");
                }

                @Override
                public void setDamage(short data) {
                    throw new IllegalStateException("Cannot set data");
                }

                @Override
                public void setType(int id) {
                    throw new IllegalStateException("Cannot set id");
                }
            };
        }
        for (int i = 0; i < 256; i++) {
            CACHE_BIOME[i] = new BaseBiome(i) {
                @Override
                public void setId(int id) {
                    throw new IllegalStateException("Cannot set id");
                }
            };
        }
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    final short i = (short) (y >> 4);
                    final short j = (short) (((y & 0xF) << 8) | (z << 4) | x);
                    CACHE_I[y][z][x] = i;
                    CACHE_J[y][z][x] = j;
                    CACHE_X[i][j] = (byte) x;
                    CACHE_Y[i][j] = (short) y;
                    CACHE_Z[i][j] = (byte) z;
                }
            }
        }
        try {
            BundledBlockData bundled = BundledBlockData.getInstance();
            bundled.loadFromResource();
            for (int i = 0; i < Character.MAX_VALUE; i++) {
                int id = i >> 4;
                int data = i & 0xf;
                CACHE_TRANSLUSCENT[i] = BlockType.isTranslucent(id);
                CACHE_PASSTHROUGH[i] = BlockType.canPassThrough(id, data);
                BundledBlockData.BlockEntry blockEntry = bundled.findById(id);
                if (blockEntry != null) {
                    BundledBlockData.FaweBlockMaterial material = blockEntry.material;
                    if (material != null) {
                        CACHE_TRANSLUSCENT[i] = !material.isOpaque();
                        CACHE_PASSTHROUGH[i] = !material.isMovementBlocker();
                    }
                }
            }
        } catch (Throwable ignore) {
            ignore.printStackTrace();
        }
        try {
            CACHE_COLOR[getCombined(0, 0)] = new Color(128, 128, 128); //Air
            CACHE_COLOR[getCombined(1, 0)] = new Color(180, 180, 180); //stone
            CACHE_COLOR[getCombined(2, 0)] = new Color(0, 225, 0); //grass
            CACHE_COLOR[getCombined(3, 0)] = new Color(168, 117, 68); //dirt
            CACHE_COLOR[getCombined(4, 0)] = new Color(125, 125, 125); //cobblestone
            CACHE_COLOR[getCombined(5, 0)] = new Color(185, 133, 83); //wood planks
            CACHE_COLOR[getCombined(6, 0)] = new Color(0, 210, 0); //saplings
            CACHE_COLOR[getCombined(7, 0)] = new Color(60, 60, 60); //bedrock
            CACHE_COLOR[getCombined(8, 0)] = new Color(0, 0, 255); //water new Color(flowing)
            CACHE_COLOR[getCombined(9, 0)] = new Color(0, 0, 235); //water new Color(stationary)
            CACHE_COLOR[getCombined(10, 0)] = new Color(255, 155, 102); //lava new Color(flowing)
            CACHE_COLOR[getCombined(11, 0)] = new Color(255, 129, 61); //lava new Color(stationary)
            CACHE_COLOR[getCombined(12, 0)] = new Color(228, 216, 174); //sand
            CACHE_COLOR[getCombined(13, 0)] = new Color(190, 190, 210); //gravel
            CACHE_COLOR[getCombined(14, 0)] = new Color(245, 232, 73); //gold ore
            CACHE_COLOR[getCombined(15, 0)] = new Color(211, 179, 160); //iron ore
            CACHE_COLOR[getCombined(16, 0)] = new Color(61, 61, 61); //coal ore
            CACHE_COLOR[getCombined(17, 0)] = new Color(165, 103, 53); //wood
            CACHE_COLOR[getCombined(18, 0)] = new Color(76, 150, 24); //leaves
            CACHE_COLOR[getCombined(20, 0)] = new Color(158, 255, 243); //glass
            CACHE_COLOR[getCombined(24, 0)] = new Color(226, 206, 140); //sandstone
            CACHE_COLOR[getCombined(31, 0)] = new Color(0, 210, 0); //long grass
            CACHE_COLOR[getCombined(32, 0)] = new Color(224, 162, 64); //shrub
            CACHE_COLOR[getCombined(37, 0)] = new Color(255, 248, 56); //yellow flower
            CACHE_COLOR[getCombined(38, 0)] = new Color(225, 0, 0); //red rose
            CACHE_COLOR[getCombined(41, 0)] = new Color(255, 215, 0); //gold block
            CACHE_COLOR[getCombined(42, 0)] = new Color(135, 135, 135); //iron block
            CACHE_COLOR[getCombined(44, 0)] = new Color(165, 165, 165); //step
            CACHE_COLOR[getCombined(50, 0)] = new Color(255, 248, 56); //torch
            CACHE_COLOR[getCombined(53, 0)] = new Color(185, 133, 83); //wood stairs
            CACHE_COLOR[getCombined(59, 0)] = new Color(205, 222, 61); //wheat crops
            CACHE_COLOR[getCombined(65, 0)] = new Color(185, 133, 83); //ladder
            CACHE_COLOR[getCombined(67, 0)] = new Color(125, 125, 125); //cobblestone stairs
            CACHE_COLOR[getCombined(78, 0)] = new Color(230, 255, 255); //snow layer
            CACHE_COLOR[getCombined(79, 0)] = new Color(180, 255, 236); //ice
            CACHE_COLOR[getCombined(81, 0)] = new Color(76, 150, 24); //cactus
            CACHE_COLOR[getCombined(82, 0)] = new Color(150, 150, 180); //clay
            CACHE_COLOR[getCombined(83, 0)] = new Color(89, 255, 89); //reed
            CACHE_COLOR[getCombined(85, 0)] = new Color(185, 133, 83); //wood fence
            CACHE_COLOR[getCombined(99, 0)] = new Color(168, 125, 99); //large brown mushroom
            CACHE_COLOR[getCombined(100, 0)] = new Color(186, 27, 27); //large red mushroom
            CACHE_COLOR[getCombined(102, 0)] = new Color(158, 255, 243); //glass pane
            CACHE_COLOR[getCombined(106, 0)] = new Color(0, 150, 0); //vines
            CACHE_COLOR[getCombined(110, 0)] = new Color(100, 90, 100); //mycelium
            CACHE_COLOR[getCombined(111, 0)] = new Color(96, 188, 30); //lily pad
            CACHE_COLOR[getCombined(128, 0)] = new Color(226, 206, 140); //sandstone stairs
            CACHE_COLOR[getCombined(134, 0)] = new Color(185, 133, 83); //spruce wood stairs
            CACHE_COLOR[getCombined(141, 0)] = new Color(205, 222, 61); //carrot crops
            CACHE_COLOR[getCombined(142, 0)] = new Color(205, 222, 61); //potato crops
            CACHE_COLOR[getCombined(161, 0)] = new Color(67, 132, 21); //dark oak leaves
            CACHE_COLOR[getCombined(38, 8)] = new Color(255, 250, 155); //daisy flower
            CACHE_COLOR[getCombined(175, 8)] = new Color(0, 200, 0); //double tall grass and flowers top
            CACHE_COLOR[getCombined(35, 0)] = new Color(254, 254, 254); // White - Wools colors borrowed from Sk89q's draw script
            CACHE_COLOR[getCombined(35, 1)] = new Color(255, 100, 0); // Orange
            CACHE_COLOR[getCombined(35, 2)] = new Color(200, 0, 200); // Magenta
            CACHE_COLOR[getCombined(35, 3)] = new Color(87, 132, 223); // Light blue
            CACHE_COLOR[getCombined(35, 4)] = new Color(255, 255, 0); // Yellow
            CACHE_COLOR[getCombined(35, 5)] = new Color(0, 255, 0); // Green
            CACHE_COLOR[getCombined(35, 6)] = new Color(255, 180, 200); // Pink
            CACHE_COLOR[getCombined(35, 7)] = new Color(72, 72, 72); // Gray
            CACHE_COLOR[getCombined(35, 8)] = new Color(173, 173, 173); // Light grey
            CACHE_COLOR[getCombined(35, 9)] = new Color(0, 100, 160); // Cyan
            CACHE_COLOR[getCombined(35, 10)] = new Color(120, 0, 200); // Purple
            CACHE_COLOR[getCombined(35, 11)] = new Color(0, 0, 175); // Blue
            CACHE_COLOR[getCombined(35, 12)] = new Color(100, 60, 0); // Brown
            CACHE_COLOR[getCombined(35, 13)] = new Color(48, 160, 0); // Cactus green
            CACHE_COLOR[getCombined(35, 14)] = new Color(255, 0, 0); // Red
            CACHE_COLOR[getCombined(35, 15)] = new Color(0, 0, 0); // Black
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static boolean canPassThrough(int id, int data) {
        return CACHE_PASSTHROUGH[FaweCache.getCombined(id, data)];
    }

    public static boolean isTranslucent(int id, int data) {
        return CACHE_TRANSLUSCENT[FaweCache.getCombined(id, data)];
    }

    public static boolean isLiquidOrGas(int id) {
        switch (id) {
            case 0:
            case 8:
            case 9:
            case 10:
            case 11:
                return true;
            default:
                return false;
        }
    }

    public static boolean isLiquid(int id) {
        switch (id) {
            case 8:
            case 9:
            case 10:
            case 11:
                return true;
            default:
                return false;
        }
    }

    public static LightType getLight(int id) {
        switch (id) { // Lighting
            case 0:
            case 6:
            case 27:
            case 28:
            case 31:
            case 32:
            case 37:
            case 38:
            case 55:
            case 59:
            case 65:
            case 66:
            case 69:
            case 75:
            case 77:
            case 78:
            case 83:
            case 90:
            case 93:
            case 94:
            case 104:
            case 105:
            case 106:
            case 111:
            case 115:
            case 119:
            case 127:
            case 131:
            case 132:
            case 140:
            case 141:
            case 142:
            case 143:
            case 144:
            case 149:
            case 150:
            case 157:
            case 171:
            case 175:
            case 198:
            case 199:
            case 200:
            case 207:
            case 209:
            case 217:
                return LightType.TRANSPARENT;
            case 39:
            case 40:
            case 50:
            case 51:
            case 76:
                return LightType.TRANSPARENT_EMIT;
            case 10:
            case 11:
            case 62:
            case 74:
            case 89:
            case 122:
            case 124:
            case 130:
            case 138:
            case 169:
            case 213:
                return LightType.SOLID_EMIT;
            default:
                return LightType.OCCLUDING;
        }
    }

    public static boolean hasLight(int id) {
        switch (id) {
            case 39:
            case 40:
            case 50:
            case 51:
            case 76:
            case 10:
            case 11:
            case 62:
            case 74:
            case 89:
            case 122:
            case 124:
            case 130:
            case 138:
            case 169:
            case 213:
                return true;
            default:
                return false;
        }
    }

    public enum LightType {
        TRANSPARENT, OCCLUDING, SOLID_EMIT, TRANSPARENT_EMIT
    }

    public static boolean isTransparent(int id) {
        switch (id) {
            case 0:
            case 6:
            case 8:
            case 9:
            case 10:
            case 11:
            case 18:
            case 20:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 44:
            case 46:
            case 50:
            case 51:
            case 53:
            case 54:
            case 55:
            case 59:
            case 60:
            case 63:
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 75:
            case 76:
            case 77:
            case 78:
            case 79:
            case 81:
            case 83:
            case 85:
            case 89:
            case 90:
            case 92:
            case 93:
            case 94:
            case 95:
            case 96:
            case 101:
            case 102:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 111:
            case 113:
            case 114:
            case 115:
            case 116:
            case 117:
            case 118:
            case 119:
            case 120:
            case 122:
            case 126:
            case 127:
            case 128:
            case 130:
            case 131:
            case 132:
            case 134:
            case 135:
            case 136:
            case 138:
            case 139:
            case 140:
            case 141:
            case 142:
            case 143:
            case 144:
            case 145:
            case 146:
            case 147:
            case 148:
            case 149:
            case 150:
            case 151:
            case 152:
            case 154:
            case 156:
            case 157:
            case 160:
            case 161:
            case 163:
            case 164:
            case 167:
            case 169:
            case 171:
            case 175:
            case 176:
            case 177:
            case 178:
            case 180:
            case 182:
            case 183:
            case 184:
            case 185:
            case 186:
            case 187:
            case 190:
            case 191:
            case 192:
            case 193:
            case 194:
            case 195:
            case 196:
            case 197:
            case 198:
            case 199:
            case 200:
            case 203:
            case 205:
            case 207:
            case 208:
            case 209:
            case 212:
            case 217:
                return true;
            default:
                return false;
        }
    }

    /**
     * Check if an id might have data
     *
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
            case 7:
            case 73:
            case 74:
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
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
            case 140:
            case 165:
            case 166:
            case 169:
            case 172:
            case 173:
            case 174:
                return false;
            default:
                return true;
        }
    }

    /**
     * Check if an id might have nbt
     *
     * @param id
     * @return
     */
    public static boolean hasNBT(int id) {
        switch (id) {
            case 26:
            case 218:
            case 54:
            case 130:
            case 142:
            case 27:
            case 137:
            case 188:
            case 189:
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
            case 209:
            case 210:
            case 211:
            case 255:
            case 219:
            case 220:
            case 221:
            case 222:
            case 223:
            case 224:
            case 225:
            case 226:
            case 227:
            case 228:
            case 229:
            case 230:
            case 231:
            case 232:
            case 233:
            case 234:
                return true;
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            case 19:
            case 20:
            case 21:
            case 22:
            case 24:
            case 30:
            case 31:
            case 32:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
            case 50:
            case 51:
            case 53:
            case 55:
            case 56:
            case 57:
            case 58:
            case 59:
            case 60:
            case 64:
            case 65:
            case 67:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 85:
            case 86:
            case 87:
            case 88:
            case 89:
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
            case 96:
            case 97:
            case 98:
            case 99:
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 110:
            case 111:
            case 112:
            case 113:
            case 114:
            case 115:
            case 118:
            case 120:
            case 121:
            case 122:
            case 125:
            case 126:
            case 127:
            case 128:
            case 129:
            case 131:
            case 132:
            case 133:
            case 134:
            case 135:
            case 136:
            case 139:
            case 141:
            case 143:
            case 145:
            case 147:
            case 148:
            case 152:
            case 153:
            case 155:
            case 156:
            case 159:
            case 160:
            case 161:
            case 162:
            case 163:
            case 164:
            case 165:
            case 166:
            case 167:
            case 168:
            case 169:
            case 170:
            case 171:
            case 172:
            case 173:
            case 174:
            case 175:
            case 179:
            case 180:
            case 181:
            case 182:
            case 183:
            case 184:
            case 185:
            case 186:
            case 187:
            case 190:
            case 191:
            case 192:
            case 193:
            case 194:
            case 195:
            case 196:
            case 197:
            case 198:
            case 199:
            case 200:
            case 201:
            case 202:
            case 203:
            case 204:
            case 205:
            case 206:
            case 207:
            case 208:
            case 212:
            case 213:
            case 214:
            case 215:
            case 216:
            case 217:
            case 235:
            case 236:
            case 237:
            case 238:
            case 239:
            case 240:
            case 241:
            case 242:
            case 243:
            case 244:
            case 245:
            case 246:
            case 247:
            case 248:
            case 249:
            case 250:
            case 251:
            case 252:
                return false;
            default:
                return id > 252;
        }
    }

    public static String getMaterialName(int combined) {
        return getMaterialName(getId(combined), getData(combined));
    }

    public static String getMaterialName(int id, int data) {
        BundledBlockData.BlockEntry entry = BundledBlockData.getInstance().findById(id);
        if (entry == null) {
            return data != 0 ? id + ":" + data : id + "";
        }
        return data != 0 ? entry.id.replace("minecraft:", "") + ":" + data : entry.id.replace("minecraft:", "");
    }

    public static Map<String, Object> asMap(Object... pairs) {
        HashMap<String, Object> map = new HashMap<String, Object>(pairs.length >> 1);
        for (int i = 0; i < pairs.length; i += 2) {
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
        } else if (value instanceof Short) {
            return asTag((short) value);
        } else if (value instanceof Double) {
            return asTag((double) value);
        } else if (value instanceof Byte) {
            return asTag((byte) value);
        } else if (value instanceof Float) {
            return asTag((float) value);
        } else if (value instanceof Long) {
            return asTag((long) value);
        } else if (value instanceof String) {
            return asTag((String) value);
        } else if (value instanceof Map) {
            return asTag((Map) value);
        } else if (value instanceof Collection) {
            return asTag((Collection) value);
        } else if (value instanceof Object[]) {
            return asTag((Object[]) value);
        } else if (value instanceof byte[]) {
            return asTag((byte[]) value);
        } else if (value instanceof int[]) {
            return asTag((int[]) value);
        } else if (value instanceof Tag) {
            return (Tag) value;
        } else if (value instanceof Boolean) {
            return asTag((byte) ((boolean) value ? 1 : 0));
        } else if (value == null) {
            System.out.println("Invalid nbt: " + value);
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
            System.out.println("Invalid nbt: " + value);
            return null;
        }
    }

    public static ListTag asTag(Object... values) {
        Class clazz = null;
        List<Tag> list = new ArrayList<>(values.length);
        for (Object value : values) {
            Tag tag = asTag(value);
            if (clazz == null) {
                clazz = tag.getClass();
            }
            list.add(tag);
        }
        if (clazz == null) clazz = EndTag.class;
        return new ListTag(clazz, list);
    }

    public static ListTag asTag(Collection values) {
        Class clazz = null;
        List<Tag> list = new ArrayList<>(values.size());
        for (Object value : values) {
            Tag tag = asTag(value);
            if (clazz == null) {
                clazz = tag.getClass();
            }
            list.add(tag);
        }
        if (clazz == null) clazz = EndTag.class;
        return new ListTag(clazz, list);
    }
}
