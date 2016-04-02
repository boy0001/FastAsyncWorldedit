package com.boydti.fawe.forge;

import com.boydti.fawe.Fawe;
import com.sk89q.worldedit.world.biome.BiomeData;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.biome.BiomeGenBase;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.biome.BiomeTypes;

/**
 * Created by Jesse on 4/2/2016.
 */
public class SpongeUtil {
    private static BiomeType[] biomes;
    private static HashMap<String, Integer> biomeMap;
    public static Map<Integer, BiomeData> biomeData;

    public static void initBiomeCache() {
        try {
            Class<?> clazz = Class.forName("com.sk89q.worldedit.forge.ForgeBiomeRegistry");
            Field bdf = clazz.getDeclaredField("biomeData");
            bdf.setAccessible(true);
            biomeData = (Map<Integer, BiomeData>) bdf.get(null);
            biomes = new BiomeType[256];
            biomeMap = new HashMap<>();
            int lastId = 0;
            loop:
            for (Map.Entry<Integer, BiomeData> entry : biomeData.entrySet()) {
                int id = entry.getKey();
                BiomeData data = entry.getValue();
                String name = data.getName().toUpperCase().replaceAll(" ", "_").replaceAll("[+]", "_PLUS");
                if (name.endsWith("_M") || name.contains("_M_")) {
                    name = name.replaceAll("_M", "_MOUNTAINS");
                }
                if (name.endsWith("_F") || name.contains("_F_")) {
                    name = name.replaceAll("_F", "_FOREST");
                }
                try {
                    biomes[id] = (BiomeType) BiomeTypes.class.getField(name).get(null);
                    biomeMap.put(biomes[id].getId(), id);
                    lastId = id;
                }
                catch (Throwable e) {
                    Field[] fields = BiomeTypes.class.getDeclaredFields();
                    for (Field field : fields) {
                        if (field.getName().replaceAll("_", "").equals(name.replaceAll("_", ""))) {
                            biomes[id] = (BiomeType) field.get(null);
                            biomeMap.put(biomes[id].getId(), id);
                            lastId = id;
                            continue loop;
                        }
                    }
                    Fawe.debug("Unknown biome: " + name);
                    biomes[id] = biomes[lastId];
                    biomeMap.put(biomes[lastId].getId(), lastId);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static BiomeType getBiome(String biome) {
        if (biomes == null) {
            initBiomeCache();
        }
        return biomes[biomeMap.get(biome.toUpperCase())];
    }

    public static BiomeType getBiome(int index) {
        return (BiomeType) BiomeGenBase.getBiome(index);
    }
}
