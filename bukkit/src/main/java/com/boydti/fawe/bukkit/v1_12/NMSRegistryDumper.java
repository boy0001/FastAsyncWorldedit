package com.boydti.fawe.bukkit.v1_12;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.minecraft.server.v1_12_R1.BaseBlockPosition;
import net.minecraft.server.v1_12_R1.Block;
import net.minecraft.server.v1_12_R1.BlockStateDirection;
import net.minecraft.server.v1_12_R1.BlockStateList;
import net.minecraft.server.v1_12_R1.EnumDirection;
import net.minecraft.server.v1_12_R1.EnumPistonReaction;
import net.minecraft.server.v1_12_R1.IBlockData;
import net.minecraft.server.v1_12_R1.IBlockState;
import net.minecraft.server.v1_12_R1.Material;
import net.minecraft.server.v1_12_R1.MinecraftKey;
import net.minecraft.server.v1_12_R1.Vec3D;

@SuppressWarnings({"rawtypes", "unchecked"})
public class NMSRegistryDumper {

    private final Field fieldDirection;
    private File file;
    private Gson gson;

    public NMSRegistryDumper(File file) throws NoSuchFieldException {
        this.file = file;
        GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
        builder.registerTypeAdapter(BaseBlockPosition.class, new BaseBlockPositionAdapter());
        builder.registerTypeAdapter(Vec3D.class, new Vec3DAdapter());
        this.gson = builder.create();
        this.fieldDirection = EnumDirection.class.getDeclaredField("m");
        this.fieldDirection.setAccessible(true);
    }

    public void run() throws Exception {
        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
        for (Block block : Block.REGISTRY) {
            MinecraftKey key = Block.REGISTRY.b(block);
            list.add(getProperties(block, key));
        }
        Collections.sort(list, new MapComparator());
        String out = gson.toJson(list);
        this.write(out);
    }

    private Map<String, Object> getProperties(Block b, MinecraftKey key) throws IllegalAccessException {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("legacyId", Block.getId(b));
        map.put("id", key.toString());
        map.put("unlocalizedName", b.a());
        map.put("localizedName", b.getName());
        map.put("states", getStates(b));
        map.put("material", getMaterial(b));
        return map;
    }

    private Map<String, Map> getStates(Block b) throws IllegalAccessException {
        Map<String, Map> map = new LinkedHashMap<String, Map>();
        BlockStateList bs = b.s();
        Collection<IBlockState<?>> props = bs.d();
        for (IBlockState prop : props) {
            map.put(prop.a(), dataValues(b, prop));
        }

        return map;
    }

    private final Vec3D[] rotations = {
            new Vec3D(0, 0, -1),
            new Vec3D(0.5, 0, -1),
            new Vec3D(1, 0, -1),
            new Vec3D(1, 0, -0.5),
            new Vec3D(1, 0, 0),
            new Vec3D(1, 0, 0.5),
            new Vec3D(1, 0, 1),
            new Vec3D(0.5, 0, 1),
            new Vec3D(0, 0, 1),
            new Vec3D(-0.5, 0, 1),
            new Vec3D(-1, 0, 1),
            new Vec3D(-1, 0, 0.5),
            new Vec3D(-1, 0, 0),
            new Vec3D(-1, 0, -0.5),
            new Vec3D(-1, 0, -1),
            new Vec3D(-0.5, 0, -1)
    };


    private BaseBlockPosition addDirection(Object orig, BaseBlockPosition addend) {
        if (orig instanceof BaseBlockPosition) {
            BaseBlockPosition ov = ((BaseBlockPosition) orig);
            return new BaseBlockPosition(addend.getX() + ov.getX(), addend.getY() + ov.getY(), addend.getZ() + ov.getZ());
        }
        return addend;
    }

    private Map<String, Object> dataValues(Block b, IBlockState prop) throws IllegalAccessException {
        //BlockState bs = b.getBlockState();
        IBlockData base = b.fromLegacyData(0);

        Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
        Map<String, Object> valueMap = new LinkedHashMap<String, Object>();
        List<Integer> dvs = new ArrayList<Integer>();
        for (Comparable val : (Iterable<Comparable>) prop.c()) {
            Map<String, Object> stateMap = new LinkedHashMap<String, Object>();
            int dv = b.toLegacyData(base.set(prop, val));
            stateMap.put("data", dv);

            Map<String, Object> addAfter = null;
            String addAfterName = null;

            dvs.add(dv);

            if (prop instanceof BlockStateDirection) {
                EnumDirection dir = EnumDirection.valueOf(val.toString().toUpperCase());
                BaseBlockPosition vec = (BaseBlockPosition) fieldDirection.get(dir);
                stateMap.put("direction", addDirection(stateMap.get("direction"), vec));
            } else if (prop.a().equals("half")) {
                if (prop.a(val).equals("top")) {
                    stateMap.put("direction", addDirection(stateMap.get("direction"), new BaseBlockPosition(0, 1, 0)));
                } else if (prop.a(val).equals("bottom")) {
                    stateMap.put("direction", addDirection(stateMap.get("direction"), new BaseBlockPosition(0, -1, 0)));
                }
            } else if (prop.a().equals("axis")) {
                if (prop.a(val).equals("x")) {
                    stateMap.put("direction", new BaseBlockPosition(1, 0, 0));
                    addAfter = new LinkedHashMap<String, Object>();
                    addAfter.put("data", dv);
                    addAfter.put("direction", new BaseBlockPosition(-1, 0, 0));
                    addAfterName = "-x";
                } else if (prop.a(val).equals("y")) {
                    stateMap.put("direction", new BaseBlockPosition(0, 1, 0));
                    addAfter = new LinkedHashMap<String, Object>();
                    addAfter.put("data", dv);
                    addAfter.put("direction", new BaseBlockPosition(0, -1, 0));
                    addAfterName = "-y";
                } else if (prop.a(val).equals("z")) {
                    stateMap.put("direction", new BaseBlockPosition(0, 0, 1));
                    addAfter = new LinkedHashMap<String, Object>();
                    addAfter.put("data", dv);
                    addAfter.put("direction", new BaseBlockPosition(0, 0, -1));
                    addAfterName = "-z";
                }
            } else if (prop.a().equals("rotation")) {
                stateMap.put("direction", rotations[Integer.valueOf(prop.a(val))]);
            } else if (prop.a().equals("facing")) { // usually already instanceof PropertyDirection, unless it's a lever
                if (prop.a(val).equals("south")) {
                    stateMap.put("direction", new BaseBlockPosition(0, 0, 1));
                } else if (prop.a(val).equals("north")) {
                    stateMap.put("direction", new BaseBlockPosition(0, 0, -1));
                } else if (prop.a(val).equals("west")) {
                    stateMap.put("direction", new BaseBlockPosition(-1, 0, 0));
                } else if (prop.a(val).equals("east")) {
                    stateMap.put("direction", new BaseBlockPosition(1, 0, 0));
                }
                /*
                // TODO fix these levers. they disappear right now
                // excluding them just means they won't get rotated
                } else if (prop.getName(val).equals("up_x")) {
                    stateMap.put("direction", new BaseBlockPosition(1, 1, 0));
                    addAfter = new LinkedHashMap<String, Object>();
                    addAfter.put("data", dv);
                    addAfter.put("direction", new BaseBlockPosition(-1, 1, 0));
                    addAfterName = "up_-x";
                } else if (prop.getName(val).equals("up_z")) {
                    stateMap.put("direction", new BaseBlockPosition(0, 1, 1));
                    addAfter = new LinkedHashMap<String, Object>();
                    addAfter.put("data", dv);
                    addAfter.put("direction", new BaseBlockPosition(0, 1, -1));
                    addAfterName = "up_-z";
                } else if (prop.getName(val).equals("down_x")) {
                    stateMap.put("direction", new BaseBlockPosition(1, -1, 0));
                    addAfter = new LinkedHashMap<String, Object>();
                    addAfter.put("data", dv);
                    addAfter.put("direction", new BaseBlockPosition(-1, -1, 0));
                    addAfterName = "down_-x";
                } else if (prop.getName(val).equals("down_z")) {
                    stateMap.put("direction", new BaseBlockPosition(0, -1, 1));
                    addAfter = new LinkedHashMap<String, Object>();
                    addAfter.put("data", dv);
                    addAfter.put("direction", new BaseBlockPosition(0, -1, -1));
                    addAfterName = "down_-z";
                }*/
            }
            valueMap.put(prop.a(val), stateMap);
            if (addAfter != null) {
                valueMap.put(addAfterName, addAfter);
            }
        }

        // attempt to calc mask
        int dataMask = 0;
        for (int dv : dvs) {
            dataMask |= dv;
        }
        dataMap.put("dataMask", dataMask);

        dataMap.put("values", valueMap);
        return dataMap;
    }

    private Map<String, Object> getMaterial(Block b) {
        IBlockData bs = b.getBlockData();
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("powerSource", b.isPowerSource(bs));
        map.put("lightOpacity", b.m(bs));
        map.put("lightValue", b.o(bs));
        map.put("usingNeighborLight", b.p(bs));
        map.put("hardness", getField(b, Block.class, "blockHardness", "strength"));
        map.put("resistance", getField(b, Block.class, "blockResistance", "durability"));
        map.put("ticksRandomly", b.isTicking());
        map.put("fullCube", b.c(bs));
        map.put("slipperiness", b.frictionFactor);
        map.put("renderedAsNormalBlock", b.l(bs));
        //map.put("solidFullCube", b.isSolidFullCube());
        Material m = b.q(bs);
        map.put("liquid", m.isLiquid());
        map.put("solid", m.isBuildable());
        map.put("movementBlocker", m.isSolid());
        //map.put("blocksLight", m.blocksLight());
        map.put("burnable", m.isBurnable());
        map.put("opaque", m.k());
        map.put("replacedDuringPlacement", m.isReplaceable());
        map.put("toolRequired", !m.isAlwaysDestroyable());
        map.put("fragileWhenPushed", m.getPushReaction() == EnumPistonReaction.DESTROY);
        map.put("unpushable", m.getPushReaction() == EnumPistonReaction.BLOCK);
        map.put("adventureModeExempt", getField(m, Material.class, "isAdventureModeExempt", "Q"));
        //map.put("mapColor", rgb(m.getMaterialMapColor().colorValue));
        map.put("ambientOcclusionLightValue", b.isOccluding(bs) ? 0.2F:1.0F);
        map.put("grassBlocking", false); // idk what this property was originally supposed to be...grass uses a combination of light values to check growth
        return map;
    }

    private Object getField(Object obj, Class<?> clazz, String name, String obfName) {
        try {
            Field f;
            try {
                f = clazz.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                f = clazz.getDeclaredField(obfName);
            }
            if (f == null) return null;
            f.setAccessible(true);
            return f.get(obj);
        } catch (IllegalAccessException ignored) {
        } catch (NoSuchFieldException ignored) {
        }
        return null;
    }

    private String rgb(int i) {
        int r = (i >> 16) & 0xFF;
        int g = (i >>  8) & 0xFF;
        int b = i & 0xFF;
        return String.format("#%02x%02x%02x", r, g, b);
    }

    private void write(String s) {
        try {
            FileOutputStream str = new FileOutputStream(file);
            str.write(s.getBytes());
        } catch (IOException e) {
            System.err.printf("Error writing registry dump: %e", e);
        }
    }


    public static class BaseBlockPositionAdapter extends TypeAdapter<BaseBlockPosition> {
        @Override
        public BaseBlockPosition read(final JsonReader in) throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override
        public void write(final JsonWriter out, final BaseBlockPosition vec) throws IOException {
            out.beginArray();
            out.value(vec.getX());
            out.value(vec.getY());
            out.value(vec.getZ());
            out.endArray();
        }
    }

    public static class Vec3DAdapter extends TypeAdapter<Vec3D> {
        @Override
        public Vec3D read(final JsonReader in) throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override
        public void write(final JsonWriter out, final Vec3D vec) throws IOException {
            out.beginArray();
            out.value(vec.x);
            out.value(vec.y);
            out.value(vec.z);
            out.endArray();
        }
    }

    private static class MapComparator implements Comparator<Map<String, Object>> {
        @Override
        public int compare(Map<String, Object> a, Map<String, Object> b) {
            return ((Integer) a.get("legacyId")).compareTo((Integer) b.get("legacyId"));
        }
    }
}