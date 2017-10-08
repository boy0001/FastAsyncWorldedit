package com.boydti.fawe.object.clipboard;


import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.MainUtil;
import com.google.common.io.Resources;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public class ItemWikiScraper {
    private static String PE = "https://minecraft.gamepedia.com/index.php?title=Bedrock_Edition_data_values&action=edit&section=1";
    private static String PC = "https://minecraft.gamepedia.com/index.php?title=Java_Edition_data_values/Item_IDs&action=edit";

    private Map<ClipboardRemapper.RemapPlatform, Map<String, Integer>> cache = new HashMap<>();

    public Map<String, Integer> expand(Map<String, Integer> map) {
        HashMap<String, Integer> newMap = new HashMap<>(map);
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            newMap.put(entry.getKey().replace("_", ""), entry.getValue());
        }
        return newMap;
    }

    public synchronized Map<String, Integer> scapeOrCache(ClipboardRemapper.RemapPlatform platform) throws IOException {
        Map<String, Integer> map;
        try {
            Map<String, Integer> cached = cache.get(platform);
            if (cached != null) return cached;

            File file = new File("lib" + File.separator + "item-mappings-" + platform.name().toLowerCase() + ".json");
            Gson gson = new Gson();
            if (file.exists()) {
                try {
                    String str = Resources.toString(file.toURL(), Charset.defaultCharset());
                    return gson.fromJson(str, new TypeToken<Map<String, Integer>>() {
                    }.getType());
                } catch (Throwable ignore) {
                    ignore.printStackTrace();
                }
            }
            map = scrape(platform);
            java.io.File parent = file.getParentFile();
            parent.mkdirs();
            file.createNewFile();
            Files.write(file.toPath(), gson.toJson(map).getBytes(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            map = new HashMap<>();
        }
        cache.put(platform, map);
        return map;
    }

    private Map<String, Integer> scrape(ClipboardRemapper.RemapPlatform platform) throws IOException {
        Fawe.debug("Downloading item mappings for " + platform + ". Please wait...");
        String url = (platform == ClipboardRemapper.RemapPlatform.PC) ? PC : PE;
        String text = MainUtil.getText(url);

        String header = platform == ClipboardRemapper.RemapPlatform.PE ? "=== Item IDs ===" : "{{";
        String footer = "{{-}}";
        String prefix = "{{id table|";

        HashMap<String, Integer> map = new HashMap<>();

        int headerIndex = text.indexOf(header);
        if (headerIndex == -1) return map;
        int endIndex = text.indexOf(footer, headerIndex);
        String part = text.substring(headerIndex, endIndex == -1 ? text.length() : endIndex);

        int id = 255;
        for (String line : part.split("\n")) {
            String lower = line.toLowerCase();
            if (lower.startsWith(prefix)) {
                line = line.substring(prefix.length(), line.indexOf("}}"));
                String[] split = line.split("\\|");
                String nameId = null;
                for (String entry : split) {
                    String[] pair = entry.split("=");
                    switch (pair[0].toLowerCase()) {
                        case "dv":
                            id = Integer.parseInt(pair[1]);
                            break;
                        case "nameid":
                            nameId = pair[1];
                            break;
                    }
                }
                if (nameId == null) nameId = split[0].toLowerCase().replace(' ', '_');
                map.put(nameId, id);
            }
            id++;
        }
        Fawe.debug("Download complete.");
        return map;
    }
}
