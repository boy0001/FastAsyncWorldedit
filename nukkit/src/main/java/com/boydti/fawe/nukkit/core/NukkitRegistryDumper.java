package com.boydti.fawe.nukkit.core;

import cn.nukkit.block.Block;
import cn.nukkit.block.BlockLiquid;
import cn.nukkit.item.Item;
import cn.nukkit.math.Vector3;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class NukkitRegistryDumper {

    private File file;
    private Gson gson;

    public static void main(String[] args) {
        try {
            new NukkitRegistryDumper(new File("blocks.json")).run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public NukkitRegistryDumper(File file) {
        this.file = file;
        GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
        builder.registerTypeAdapter(Vector3.class, new Vec3iAdapter());
        this.gson = builder.create();
    }

    public void run() throws Exception {
        Block.init();
        Item.init();
        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
        HashSet<String> visited = new HashSet<>();
        for (Item item : Item.getCreativeItems()) {
            try {
                if (item != null && item.getBlock() != null && !visited.contains(item.getBlock().getName())) {
                    Block block = item.getBlock();
                    visited.add(block.getName());
                    list.add(getProperties(block));

                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
//        for (Block block : Block.fullList) {
//            if (block != null) {
//                try {
//                    System.out.println("BLOCK " + block.getName());
//                    list.add(getProperties(block));
//                } catch (Throwable e) {
//                    e.printStackTrace();
//                }
//            }
//        }
        Collections.sort(list, new MapComparator());
        String out = gson.toJson(list);
        this.write(out);
        System.out.println("Wrote file: " + file.getAbsolutePath());
    }

    private Map<String, Object> getProperties(Block b) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        Item item = Item.get(b.getId(), b.getDamage());
        map.put("legacyId", b.getId());
        map.put("id", b.getName());
        map.put("unlocalizedName", b.getName());
        map.put("localizedName", b.getName());
        map.put("material", getMaterial(b));
        return map;
    }


    private Map<String, Object> getMaterial(Block b) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        Item item = Item.get(b.getId(), b.getDamage());
        map.put("powerSource", b.isPowerSource());
        map.put("lightOpacity", Block.lightFilter[b.getId()]);
        map.put("lightValue", b.getLightLevel());
//        map.put("usingNeighborLight", b.getUseNeighborBrightness(bs));
        map.put("hardness", b.getHardness());
        map.put("resistance", b.getResistance());
//        map.put("ticksRandomly", b.tickRate());
        map.put("tickRate", b.tickRate());
        map.put("fullCube", b.isSolid() && !b.isTransparent());
        map.put("slipperiness", b.getFrictionFactor());
        map.put("renderedAsNormalBlock", !b.isTransparent());
        //map.put("solidFullCube", b.isSolidFullCube());
        map.put("liquid", b instanceof BlockLiquid);
        map.put("solid", b.isSolid());
        map.put("movementBlocker", b.hasEntityCollision());
        //map.put("blocksLight", m.blocksLight());
        map.put("burnable", b.getBurnAbility() > 0);
        map.put("opaque", !b.isTransparent());
        map.put("replacedDuringPlacement", b.canBeReplaced());
        map.put("toolRequired", b.getToolType() != 0);
        map.put("canBeFlowedInto", b.canBeFlowedInto());
//        map.put("fragileWhenPushed", b instanceof BlockFlowable);
//        map.put("unpushable", m.getMobilityFlag() == EnumPushReaction.BLOCK);
//        map.put("adventureModeExempt", b.getField(m, Material.class, "isAdventureModeExempt", "field_85159_M"));
        map.put("mapColor", rgb(b.getColor().getRGB()));
        map.put("ambientOcclusionLightValue", b.isSolid() ? 0.2F : 1.0F);
//        try {
//            map.put("ambientOcclusionLightValue", b.b.getAmbientOcclusionLightValue(bs));
//        } catch (NoSuchMethodError ignored) {
//            map.put("ambientOcclusionLightValue", b.isBlockNormalCube(bs) ? 0.2F : 1.0F);
//        }
        map.put("grassBlocking", false); // idk what this property was originally supposed to be...grass uses a combination of light values to check growth
        return map;
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
            System.out.println("Error writing registry dump: " + e);
        }
    }


    public static class Vec3iAdapter extends TypeAdapter<Vector3> {
        @Override
        public Vector3 read(final JsonReader in) throws IOException {
            throw new UnsupportedOperationException();
        }
        @Override
        public void write(final JsonWriter out, final Vector3 vec) throws IOException {
            out.beginArray();
            out.value(vec.getX());
            out.value(vec.getY());
            out.value(vec.getZ());
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
