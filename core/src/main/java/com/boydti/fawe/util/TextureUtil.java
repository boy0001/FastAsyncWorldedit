package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class TextureUtil {
    private final File folder;

    private int[] blockColors = new int[Character.MAX_VALUE + 1];
    private int[] validColors;
    private int[] validBlockIds;

    public TextureUtil() {
        this(MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.TEXTURES));
    }

    public TextureUtil(File folder) {
        this.folder = folder;
    }

    public BaseBlock getNearestBlock(int color) {
        long min = Long.MAX_VALUE;
        int closest = 0;
        int red1 = (color >> 16) & 0xFF;
        int green1 = (color >> 8) & 0xFF;
        int blue1 = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;
        for (int i = 0; i < validColors.length; i++) {
            int other = validColors[i];
            if (((other >> 24) & 0xFF) == alpha) {
                long distance = colorDistance(red1, green1, blue1, other);
                if (distance < min) {
                    min = distance;
                    closest = validBlockIds[i];
                }
            }
        }
        if (min == Long.MAX_VALUE) return null;
        return FaweCache.CACHE_BLOCK[closest];
    }

    public BaseBlock getLighterBlock(BaseBlock block) {
        return getNearestBlock(block, false);
    }

    public BaseBlock getDarkerBlock(BaseBlock block) {
        return getNearestBlock(block, false);
    }

    public int getColor(BaseBlock block) {
        return blockColors[block.getCombined()];
    }

    public File getFolder() {
        return folder;
    }

    public void loadModTextures() throws IOException, ParseException {
        Int2ObjectOpenHashMap<Integer> colorMap = new Int2ObjectOpenHashMap<>();
        if (folder.exists()) {
            // Get all the jar files
            for (File file : folder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }
            })) {
                ZipFile zipFile = new ZipFile(file);

                BundledBlockData bundled = BundledBlockData.getInstance();

                // Get all the groups in the current jar
                // The vanilla textures are in `assets/minecraft`
                // A jar may contain textures for multiple mods
                Set<String> mods = new HashSet<String>();
                {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        String name = entry.getName();
                        Path path = Paths.get(name);
                        if (path.startsWith("assets" + File.separator)) {
                            String[] split = path.toString().split(Pattern.quote(File.separator));
                            if (split.length > 1) {
                                String modId = split[1];
                                mods.add(modId);
                            }
                        }
                        continue;
                    }
                }
                for (String modId : mods) {
                    String modelsDir = "assets" + "/" + modId + "/" + "models" + "/" + "block";
                    String texturesDir = "assets" + "/" + modId + "/" + "textures" + "/" + "blocks";
                    Map<String, String> texturesMap = new ConcurrentHashMap<>();
                    { // Read models
                        Enumeration<? extends ZipEntry> entries = zipFile.entries();
                        while (entries.hasMoreElements()) {
                            ZipEntry entry = entries.nextElement();
                            if (entry.isDirectory()) {
                                continue;
                            }
                            String name = entry.getName();
                            if (!name.endsWith(".json")) {
                                continue;
                            }
                            Path path = Paths.get(name);
                            if (path.startsWith(modelsDir)) {
                                String[] split = path.toString().split("[/|\\\\|\\.]");
                                String blockName = getFileName(path.toString());
                                // Ignore special models
                                if (blockName.startsWith("#")) {
                                    continue;
                                }
                                try (InputStream is = zipFile.getInputStream(entry)) { //Read from a file, or a HttpRequest, or whatever.
                                    JSONParser parser = new JSONParser();
                                    JSONObject root = (JSONObject) parser.parse(new InputStreamReader(is, "UTF-8"));
                                    // Try to work out the texture names for this file
                                    addTextureNames(blockName, root, texturesMap);
                                }
                            }
                        }
                    }
                    // Add some possible variations for the file names to try and match it to a block
                    // - As vanilla minecraft doesn't use consistent naming for the assets and block names
                    for (String key : new ArrayList<>(texturesMap.keySet())) {
                        String value = texturesMap.get(key);
                        texturesMap.put(alphabetize(key), value);
                        String[] split = key.split("_");
                        if (split.length > 1) {
                            key = StringMan.join(Arrays.copyOfRange(split, 0, split.length - 1), "_");
                            texturesMap.putIfAbsent(key, value);
                        }
                    }
                    // Try to match the textures to a block
                    Int2ObjectOpenHashMap<String> idMap = new Int2ObjectOpenHashMap<>();
                    for (String id : bundled.stateMap.keySet()) {
                        if (id.startsWith(modId)) {
                            BaseBlock block = bundled.findByState(id);
                            BundledBlockData.BlockEntry state = bundled.findById(block.getId());
                            // Ignore non blocks
                            if (!state.material.isRenderedAsNormalBlock()) {
                                continue;
                            }
                            if (state.material.getLightValue() != 0) {
                                continue;
                            }
                            id = id.substring(modId.length() + 1).replaceAll(":", "_");
                            String texture = texturesMap.remove(id);
                            if (texture == null) {
                                texture = texturesMap.remove(alphabetize(id));
                            }
                            if (texture != null) {
                                int combined = block.getCombined();
                                if (id.startsWith("log_") || id.startsWith("log2_")) {
                                    combined += 12;
                                }
                                idMap.put(combined, texture);
                            }
                        }
                    }
                    { // Calculate the colors for each  block
                        for (Int2ObjectMap.Entry<String> entry : idMap.int2ObjectEntrySet()) {
                            int combined = entry.getIntKey();
                            String path = texturesDir + "/" + entry.getValue() + ".png";
                            ZipEntry textureEntry = zipFile.getEntry(path);
                            try (InputStream is = zipFile.getInputStream(textureEntry)) {
                                BufferedImage image = ImageIO.read(is);
                                int color = getColor(image);
                                colorMap.put((int) combined, (Integer) color);
                            }
                        }
                    }
                }
                // Close the file
                zipFile.close();
            }
        }
        // Convert the color map to a simple array
        validBlockIds = new int[colorMap.size()];
        validColors = new int[colorMap.size()];
        int index = 0;
        for (Int2ObjectMap.Entry<Integer> entry : colorMap.int2ObjectEntrySet()) {
            int combinedId = entry.getIntKey();
            int color = entry.getValue();
            blockColors[combinedId] = color;
            validBlockIds[index] = combinedId;
            validColors[index] = color;
            index++;
        }
    }

    protected BaseBlock getNearestBlock(BaseBlock block, boolean darker) {
        int color = getColor(block);
        if (color == 0) {
            return block;
        }
        BaseBlock darkerBlock = getNearestBlock(color, darker);
        return darkerBlock != null ? darkerBlock : block;
    }

    protected BaseBlock getNearestBlock(int color, boolean darker) {
        long min = Long.MAX_VALUE;
        int closest = 0;
        int red1 = (color >> 16) & 0xFF;
        int green1 = (color >> 8) & 0xFF;
        int blue1 = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;
        int intensity1 = red1 + green1 + blue1;
        for (int i = 0; i < validColors.length; i++) {
            int other = validColors[i];
            if (other != color && ((other >> 24) & 0xFF) == alpha) {
                int red2 = (other >> 16) & 0xFF;
                int green2 = (other >> 8) & 0xFF;
                int blue2 = (other >> 0) & 0xFF;
                int intensity2 = red2 + green2 + blue2;
                if (darker ? intensity2 >= intensity1 : intensity1 >= intensity2) {
                    continue;
                }
                long distance = colorDistance(red1, green1, blue1, other);
                if (distance < min) {
                    min = distance;
                    closest = validBlockIds[i];
                }
            }
        }
        if (min == Long.MAX_VALUE) return null;
        return FaweCache.CACHE_BLOCK[closest];
    }

    private String getFileName(String path) {
        String[] split = path.toString().split("[/|\\\\]");
        String name = split[split.length - 1];
        int dot = name.indexOf('.');
        if (dot != -1) {
            name = name.substring(0, dot);
        }
        return name;
    }

    private String alphabetize(String asset) {
        String[] split = asset.split("_");
        Arrays.sort(split);
        return StringMan.join(split, "_");
    }

    /**
     * Unfortunately the names used for the textures don't match the block id <br>
     *  - This will try to guess possible relevant block ids <br>
     *  - Match some by reformatting <br>
     *  - Match by reordering <br>
     *  - Match by appending / removing <br>
     *  - Match by hardcoded values <br>
     */
    private void addTextureNames(String modelName, JSONObject root, Map<String, String> texturesMap) {
        JSONObject textures = (JSONObject) root.get("textures");
        if (textures == null) {
            return;
        }
        Set<String> names = new HashSet<>();
        String all = (String) textures.get("all");
        if (all != null) {
            String textureName = getFileName(all);
            // Add the model
            names.add(modelName);
            names.add(textureName);
            for (String name : new ArrayList<>(names)) {
                if (name.contains("big_oak")) {
                    name = name.replaceAll("big_oak", "oak");
                    names.add(name);
                }
                String[] split = name.split("_");
                switch (split[0]) {
                    case "glass":
                        names.add(name.replaceAll("glass_", "stained_glass_"));
                        break;
                    case "log":
                        names.add(name.replaceAll("log_", "log2_"));
                        break;
                    case "leaves":
                        names.add(name.replaceAll("leaves_", "leaves2_"));
                    case "mushroom":
                        names.add(name.replaceAll("mushroom_block_skin_", "mushroom_block_"));
                    default:
                        continue;
                }
            }
            for (String name : names) {
                texturesMap.putIfAbsent(name, textureName);
            }
        } else {
            if (textures.containsKey("side") && textures.containsKey("end") && !textures.containsKey("bottom") && !textures.containsKey("top") && !textures.containsKey("platform")) {
                String side = (String) textures.get("side");
            } else if (textures.containsKey("up")) {
                // TODO: Just mushroom, not super important
                String up = (String) textures.get("up");
            }
        }
    }

    private boolean hasAlpha(int color) {
        int alpha = (color >> 24) & 0xFF;
        return alpha != 255;
    }

    public long colorDistance(int c1, int c2) {
        int red1 = (c1 >> 16) & 0xFF;
        int green1 = (c1 >> 8) & 0xFF;
        int blue1 = (c1 >> 0) & 0xFF;
        return colorDistance(red1, green1, blue1, c2);
    }

    private long colorDistance(int red1, int green1, int blue1, int c2) {
        int red2 = (c2 >> 16) & 0xFF;
        int green2 = (c2 >> 8) & 0xFF;
        int blue2 = (c2 >> 0) & 0xFF;
        int rmean = (red1 + red2) >> 1;
        int r = red1 - red2;
        int g = green1 - green2;
        int b = blue1 - blue2;
        return (((512 + rmean) * r * r) >> 8) + 4 * g * g + (((767 - rmean) * b * b) >> 8);
    }

    public int getColor(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        long totalRed = 0;
        long totalGreen = 0;
        long totalBlue = 0;
        long totalAlpha = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int color = image.getRGB(x, y);
                totalRed += (color >> 16) & 0xFF;
                totalGreen += (color >> 8) & 0xFF;
                totalBlue += (color >> 0) & 0xFF;
                totalAlpha += (color >> 24) & 0xFF;
            }
        }
        int a = width * height;
        Color color = new Color((int) (totalRed / a), (int) (totalGreen / a), (int) (totalBlue / a), (int) (totalAlpha / a));
        return color.getRGB();
    }
}
