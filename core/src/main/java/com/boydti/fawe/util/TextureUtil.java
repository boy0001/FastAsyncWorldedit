package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

public class TextureUtil {
    private final File folder;
    private static final int[] FACTORS = new int[766];
    static {
        for (int i = 1; i < FACTORS.length; i++) {
            FACTORS[i] = 65535 / i;
        }
    }
    protected int[] blockColors = new int[Character.MAX_VALUE + 1];
    protected long[] blockDistance = new long[Character.MAX_VALUE + 1];
    protected long[] distances;
    protected int[] validColors;
    protected char[] validBlockIds;
    protected int[] validLayerColors;
    protected char[][] validLayerBlocks;


    /**
     * https://github.com/erich666/Mineways/blob/master/Win/biomes.cpp
     */
    protected BiomeColor[] validBiomes;
    private BiomeColor[] biomes = new BiomeColor[] {
            //    ID    Name             Temperature, rainfall, grass, foliage colors
            //    - note: the colors here are just placeholders, they are computed in the program
            new BiomeColor(  0, "Ocean",				    	0.5f, 0.5f, 0x92BD59, 0x77AB2F ),	// default values of temp and rain
            new BiomeColor(  1, "Plains",				    	0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(  2, "Desert",				    	2.0f, 0.0f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(  3, "Extreme Hills",		    	0.2f, 0.3f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(  4, "Forest",				    	0.7f, 0.8f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(  5, "Taiga",				    	0.25f, 0.8f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(  6, "Swampland",			    	0.8f, 0.9f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(  7, "River",				    	0.5f, 0.5f, 0x92BD59, 0x77AB2F ),	// default values of temp and rain
            new BiomeColor(  8, "Nether",					    2.0f, 0.0f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(  9, "End",						0.5f, 0.5f, 0x92BD59, 0x77AB2F ),	// default values of temp and rain
            new BiomeColor( 10, "Frozen Ocean",				0.0f, 0.5f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 11, "Frozen River",				0.0f, 0.5f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 12, "Ice Plains",			    	0.0f, 0.5f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 13, "Ice Mountains",			    0.0f, 0.5f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 14, "Mushroom Island",			0.9f, 1.0f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 15, "Mushroom Island Shore",  	0.9f, 1.0f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 16, "Beach",				    	0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 17, "Desert Hills",				2.0f, 0.0f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 18, "Forest Hills",				0.7f, 0.8f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 19, "Taiga Hills",				0.25f, 0.8f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 20, "Extreme Hills Edge",	    	0.2f, 0.3f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 21, "Jungle",				    	0.95f, 0.9f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 22, "Jungle Hills",				0.95f, 0.9f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 23, "Jungle Edge",				0.95f, 0.8f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 24, "Deep Ocean",			    	0.5f, 0.5f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 25, "Stone Beach",				0.2f, 0.3f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 26, "Cold Beach",			    	0.05f, 0.3f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 27, "Birch Forest",				0.6f, 0.6f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 28, "Birch Forest Hills",	    	0.6f, 0.6f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 29, "Roofed Forest",		    	0.7f, 0.8f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 30, "Cold Taiga",			    	-0.5f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 31, "Cold Taiga Hills",			-0.5f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 32, "Mega Taiga",			    	0.3f, 0.8f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 33, "Mega Taiga Hills",			0.3f, 0.8f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 34, "Extreme Hills+",		    	0.2f, 0.3f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 35, "Savanna",					1.2f, 0.0f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 36, "Savanna Plateau",			1.0f, 0.0f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 37, "Mesa",						2.0f, 0.0f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 38, "Mesa Plateau F",			    2.0f, 0.0f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 39, "Mesa Plateau",				2.0f, 0.0f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 40, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 41, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 42, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 43, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 44, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 45, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 46, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 47, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 48, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 49, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 50, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 51, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 52, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 53, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 54, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 55, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 56, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 57, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 58, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 59, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 60, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 61, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 62, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 63, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 64, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 65, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 66, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 67, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 68, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 69, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 70, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 71, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 72, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 73, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 74, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 75, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 76, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 77, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 78, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 79, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 80, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 81, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 82, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 83, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 84, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 85, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 86, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 87, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 88, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 89, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 90, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 91, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 92, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 93, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 94, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 95, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 96, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 97, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 98, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor( 99, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(100, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(101, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(102, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(103, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(104, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(105, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(106, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(107, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(108, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(109, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(110, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(111, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(112, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(113, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(114, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(115, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(116, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(117, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(118, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(119, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(120, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(121, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(122, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(123, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(124, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(125, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(126, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(127, "The Void",            		0.5f, 0.5f, 0x92BD59, 0x77AB2F ),	// default values of temp and rain; also, no height differences
            new BiomeColor(128, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(129, "Sunflower Plains",			0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(130, "Desert M",					2.0f, 0.0f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(131, "Extreme Hills M",			0.2f, 0.3f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(132, "Flower Forest",			    0.7f, 0.8f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(133, "Taiga M",					0.25f, 0.8f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(134, "Swampland M",				0.8f, 0.9f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(135, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(136, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(137, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(138, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(139, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(140, "Ice Plains Spikes",		    0.0f, 0.5f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(141, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(142, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(143, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(144, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(145, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(146, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(147, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(148, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(149, "Jungle M",					0.95f, 0.9f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(150, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(151, "JungleEdge M",				0.95f, 0.8f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(152, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(153, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(154, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(155, "Birch Forest M",			    0.6f, 0.6f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(156, "Birch Forest Hills M",		0.6f, 0.6f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(157, "Roofed Forest M",			0.7f, 0.8f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(158, "Cold Taiga M",			    -0.5f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(159, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(160, "Mega Spruce Taiga",		    0.25f, 0.8f, 0x92BD59, 0x77AB2F ),	// special exception, temperature not 0.3
            new BiomeColor(161, "Mega Spruce Taiga Hills",	0.25f, 0.8f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(162, "Extreme Hills+ M",			0.2f, 0.3f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(163, "Savanna M",				    1.1f, 0.0f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(164, "Savanna Plateau M",		    1.0f, 0.0f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(165, "Mesa (Bryce)",				2.0f, 0.0f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(166, "Mesa Plateau F M",			2.0f, 0.0f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(167, "Mesa Plateau M",			    2.0f, 0.0f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(168, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(169, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(170, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(171, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(172, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(173, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(174, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(175, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(176, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(177, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(178, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(179, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(180, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(181, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(182, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(183, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(184, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(185, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(186, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(187, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(188, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(189, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(190, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(191, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(192, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(193, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(194, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(195, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(196, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(197, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(198, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(199, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(200, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(201, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(202, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(203, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(204, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(205, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(206, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(207, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(208, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(209, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(210, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(211, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(212, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(213, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(214, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(215, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(216, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(217, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(218, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(219, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(220, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(221, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(222, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(223, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(224, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(225, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(226, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(227, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(228, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(229, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(230, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(231, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(232, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(233, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(234, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(235, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(236, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(237, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(238, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(239, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(240, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(241, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(242, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(243, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(244, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(245, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(246, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(247, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(248, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(249, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(250, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(251, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(252, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(253, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(254, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
            new BiomeColor(255, "Unknown Biome",				0.8f, 0.4f, 0x92BD59, 0x77AB2F ),
    };

    public TextureUtil() throws FileNotFoundException {
        this(MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.TEXTURES));
    }

    public TextureUtil(File folder) throws FileNotFoundException {
        this.folder = folder;
        if (!folder.exists()) {
            throw new FileNotFoundException("Please create a `FastAsyncWorldEdit/textures` folder with `.minecraft/versions` jar or mods in it.");
        }
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

    public BaseBlock getNearestBlock(BaseBlock block) {
        int color = getColor(block);
        if (color == 0) return null;
        return getNextNearestBlock(color);
    }

    public BaseBlock getNextNearestBlock(int color) {
        long min = Long.MAX_VALUE;
        int closest = 0;
        int red1 = (color >> 16) & 0xFF;
        int green1 = (color >> 8) & 0xFF;
        int blue1 = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;
        for (int i = 0; i < validColors.length; i++) {
            int other = validColors[i];
            if (other != color && ((other >> 24) & 0xFF) == alpha) {
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

    /**
     * Returns the block combined ids as an array
     * @param color
     * @return
     */
    public char[] getNearestLayer(int color) {
        char[] closest = null;
        long min = Long.MAX_VALUE;
        int red1 = (color >> 16) & 0xFF;
        int green1 = (color >> 8) & 0xFF;
        int blue1 = (color >> 0) & 0xFF;
        int alpha = (color >> 24) & 0xFF;
        for (int i = 0; i < validLayerColors.length; i++) {
            int other = validLayerColors[i];
            if (((other >> 24) & 0xFF) == alpha) {
                long distance = colorDistance(red1, green1, blue1, other);
                if (distance < min) {
                    min = distance;
                    closest = validLayerBlocks[i];
                }
            }
        }
        return closest;
    }

    public BaseBlock getLighterBlock(BaseBlock block) {
        return getNearestBlock(block, false);
    }

    public BaseBlock getDarkerBlock(BaseBlock block) {
        return getNearestBlock(block, true);
    }

    public int getColor(BaseBlock block) {
        return blockColors[block.getCombined()];
    }

    public BiomeColor getBiome(int biome) {
        return biomes[biome];
    }

    public BiomeColor getNearestBiome(int color) {
        int grass = blockColors[2 << 4];
        if (grass == 0) {
            return null;
        }
        BiomeColor closest = null;
        long min = Long.MAX_VALUE;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = (color >> 0) & 0xFF;
        for (int i = 0; i < validBiomes.length; i++) {
            BiomeColor biome = validBiomes[i];
            long distance = colorDistance(red, green, blue, biome.grass);
            if (distance < min) {
                min = distance;
                closest = biome;
            }
        }
        return closest;
    }

    public File getFolder() {
        return folder;
    }

    public long colorDistance(int c1, int c2) {
        int red1 = (c1 >> 16) & 0xFF;
        int green1 = (c1 >> 8) & 0xFF;
        int blue1 = (c1 >> 0) & 0xFF;
        return colorDistance(red1, green1, blue1, c2);
    }

    public void loadModTextures() throws IOException {
        Int2ObjectOpenHashMap<Integer> colorMap = new Int2ObjectOpenHashMap<>();
        Int2ObjectOpenHashMap<Long> distanceMap = new Int2ObjectOpenHashMap<>();
        Gson gson = new Gson();
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
                                    JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"));
                                    Type type = new TypeToken<Map<String, Object>>(){}.getType();
                                    Map<String, Object> root = gson.fromJson(reader, type);
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
                    HashSet<Integer> map2 = new HashSet<>();
                    for (String id : bundled.stateMap.keySet()) {
                        if (id.startsWith(modId)) {
                            BaseBlock block = bundled.findByState(id);
                            BundledBlockData.BlockEntry state = bundled.findById(block.getId());
                            if (FaweCache.hasNBT(block.getId())) {
                                continue;
                            }
                            // Ignore non blocks
                            if (!state.material.isFullCube() && !state.material.isRenderedAsNormalBlock()) {
                                switch (block.getId()) {
                                    case 20:
                                    case 95:
                                        break;
                                    default:
                                        continue;
                                }
                            } else if (!state.material.isFullCube() || !state.material.isRenderedAsNormalBlock()) {
                                switch (block.getId()) {
                                    case 165:
                                    case 52:
                                        continue;
                                }
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
                                switch (block.getId()) {
                                    case 17:
                                    case 162:
                                        combined += 12;
                                        break;
                                    case 43:
                                        combined += 8;
                                        break;
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
                                long distance = getDistance(image, color);
                                distanceMap.put((int) combined, (Long) distance);
                                colorMap.put((int) combined, (Integer) color);
                            }
                        }
                    }
                }
                {
                    Integer grass = colorMap.remove(FaweCache.getCombined(BlockID.GRASS, 0));
                    if (grass != null) {
                        blockColors[FaweCache.getCombined(BlockID.GRASS, 0)] = grass;
                        // assets\minecraft\textures\colormap
                        ZipEntry grassEntry = zipFile.getEntry("assets/minecraft/textures/colormap/grass.png");
                        if (grassEntry != null) {
                            try (InputStream is = zipFile.getInputStream(grassEntry)) {
                                BufferedImage image = ImageIO.read(is);
                                for (int i = 0; i < biomes.length; i++) {
                                    BiomeColor biome = biomes[i];
                                    float adjTemp = MathMan.clamp(biome.temperature, 0.0f, 1.0f);
                                    float adjRainfall = MathMan.clamp(biome.rainfall, 0.0f, 1.0f) * adjTemp;
                                    int x = (int) (255 - adjTemp * 255);
                                    int z = (int) (255 - adjRainfall * 255);
                                    int color = image.getRGB(x, z);
                                    biome.grass = color;
                                }
                            }
                            // swampland: perlin - avoid
                            biomes[6].grass = 0;
                            biomes[134].grass = 0;
                            // roofed forest: averaged w/ 0x28340A
                            biomes[29].grass = multiplyColor(biomes[29].grass, 0x28340A + (255 << 24));
                            biomes[157].grass = multiplyColor(biomes[157].grass, 0x28340A + (255 << 24));
                            // mesa : 0x90814D
                            biomes[37].grass = 0x90814D + (255 << 24);
                            biomes[38].grass = 0x90814D + (255 << 24);
                            biomes[39].grass = 0x90814D + (255 << 24);
                            biomes[165].grass = 0x90814D + (255 << 24);
                            biomes[166].grass = 0x90814D + (255 << 24);
                            biomes[167].grass = 0x90814D + (255 << 24);
                            List<BiomeColor> valid = new ArrayList<>();
                            for (int i = 0; i < biomes.length; i++) {
                                BiomeColor biome = biomes[i];
                                biome.grass = multiplyColor(biome.grass, grass);
                                if (biome.grass != 0 && !biome.name.equalsIgnoreCase("Unknown Biome")) {
                                    valid.add(biome);
                                }
                            }
                            this.validBiomes = valid.toArray(new BiomeColor[valid.size()]);
                        }

                    }
                }
                // Close the file
                zipFile.close();
            }
        }
        // Convert the color map to a simple array
        validBlockIds = new char[colorMap.size()];
        validColors = new int[colorMap.size()];
        int index = 0;
        for (Int2ObjectMap.Entry<Integer> entry : colorMap.int2ObjectEntrySet()) {
            int combinedId = entry.getIntKey();
            int color = entry.getValue();
            blockColors[combinedId] = color;
            validBlockIds[index] = (char) combinedId;
            validColors[index] = color;
            index++;
        }
        ArrayList<Long> distances = new ArrayList<>(distanceMap.values());
        Collections.sort(distances);
        this.distances = new long[distances.size()];
        for (int i = 0; i < this.distances.length; i++) {
            this.distances[i] =  distances.get(i);
        }
        for (Int2ObjectMap.Entry<Long> entry : distanceMap.int2ObjectEntrySet()) {
            blockDistance[entry.getIntKey()] = entry.getValue();
        }
        calculateLayerArrays();
    }

    public int multiplyColor(int c1, int c2) {
        int alpha1 = (c1 >> 24) & 0xFF;
        int alpha2 = (c2 >> 24) & 0xFF;
        int red1 = (c1 >> 16) & 0xFF;
        int green1 = (c1 >> 8) & 0xFF;
        int blue1 = (c1 >> 0) & 0xFF;
        int red2 = (c2 >> 16) & 0xFF;
        int green2 = (c2 >> 8) & 0xFF;
        int blue2 = (c2 >> 0) & 0xFF;
        int red = ((red1 * red2)) / 255;
        int green = ((green1 * green2)) / 255;
        int blue = ((blue1 * blue2)) / 255;
        int alpha = ((alpha1 * alpha2)) / 255;
        return (alpha << 24) + (red << 16) + (green << 8) + (blue << 0);
    }

    public int averageColor(int c1, int c2) {
        int alpha1 = (c1 >> 24) & 0xFF;
        int alpha2 = (c2 >> 24) & 0xFF;
        int red1 = (c1 >> 16) & 0xFF;
        int green1 = (c1 >> 8) & 0xFF;
        int blue1 = (c1 >> 0) & 0xFF;
        int red2 = (c2 >> 16) & 0xFF;
        int green2 = (c2 >> 8) & 0xFF;
        int blue2 = (c2 >> 0) & 0xFF;
        int red = ((red1 + red2)) / 2;
        int green = ((green1 + green2)) / 2;
        int blue = ((blue1 + blue2)) / 2;
        int alpha = ((alpha1 + alpha2)) / 2;
        return (alpha << 24) + (red << 16) + (green << 8) + (blue << 0);
    }

    /**
     * Assumes the top layer is a transparent color and the bottom is opaque
     */
    protected int combineTransparency(int top, int bottom) {
        int alpha1 = (top >> 24) & 0xFF;
        int alpha2 = 255 - alpha1;
        int red1 = (top >> 16) & 0xFF;
        int green1 = (top >> 8) & 0xFF;
        int blue1 = (top >> 0) & 0xFF;
        int red2 = (bottom >> 16) & 0xFF;
        int green2 = (bottom >> 8) & 0xFF;
        int blue2 = (bottom >> 0) & 0xFF;
        int red = ((red1 * alpha1) + (red2 * alpha2)) / 255;
        int green = ((green1 * alpha1) + (green2 * alpha2)) / 255;
        int blue = ((blue1 * alpha1) + (blue2 * alpha2)) / 255;
        return (red << 16) + (green << 8) + (blue << 0) + (255 << 24);
    }

    protected void calculateLayerArrays() {
        Int2ObjectOpenHashMap<char[]> colorLayerMap = new Int2ObjectOpenHashMap<>();
        for (int i = 0; i < validBlockIds.length; i++) {
            int color = validColors[i];
            int combined = validBlockIds[i];
            if (hasAlpha(color)) {
                for (int j = 0; j < validBlockIds.length; j++) {
                    int colorOther = validColors[j];
                    if (!hasAlpha(colorOther)) {
                        int combinedOther = validBlockIds[j];
                        int combinedColor = combineTransparency(color, colorOther);
                        colorLayerMap.put(combinedColor, new char[] {(char) combined, (char) combinedOther});
                    }
                }
            }
        }
        this.validLayerColors = new int[colorLayerMap.size()];
        this.validLayerBlocks = new char[colorLayerMap.size()][];
        int index = 0;
        for (Int2ObjectMap.Entry<char[]> entry : colorLayerMap.int2ObjectEntrySet()) {
            validLayerColors[index] = entry.getIntKey();
            validLayerBlocks[index++] = entry.getValue();
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
        int intensity1 = 2 * red1 + 4 * green1 + 3 * blue1;
        for (int i = 0; i < validColors.length; i++) {
            int other = validColors[i];
            if (other != color && ((other >> 24) & 0xFF) == alpha) {
                int red2 = (other >> 16) & 0xFF;
                int green2 = (other >> 8) & 0xFF;
                int blue2 = (other >> 0) & 0xFF;
                int intensity2 = 2 * red2 + 4 * green2 + 3 * blue2;
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
    private void addTextureNames(String modelName, Map<String, Object> root, Map<String, String> texturesMap) {
        Map textures = (Map) root.get("textures");
        if (textures == null) {
            return;
        }
        Set<String> names = new HashSet<>();
        String all = (String) textures.get("all");
        if (all == null) {
            all = (String) textures.get("top");
        }
        if (all == null) {
            all = (String) textures.get("pattern");
        }
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
                if (!textures.containsKey("all")) {
                    names.add(name.replaceAll("_top", ""));
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
                        break;
                    case "mushroom":
                        names.add(name.replaceAll("mushroom_block_skin_", "mushroom_block_"));
                        break;
                    case "half":
                        names.add(name.replaceAll("half_", "double_stone_"));
                        break;
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

    protected boolean hasAlpha(int color) {
        int alpha = (color >> 24) & 0xFF;
        return alpha != 255;
    }

    protected long colorDistance(int red1, int green1, int blue1, int c2) {
        int red2 = (c2 >> 16) & 0xFF;
        int green2 = (c2 >> 8) & 0xFF;
        int blue2 = (c2 >> 0) & 0xFF;
        int rmean = (red1 + red2) >> 1;
        int r = red1 - red2;
        int g = green1 - green2;
        int b = blue1 - blue2;
        int hd = hueDistance(red1, green1, blue1, red2, green2, blue2);
        return (((512 + rmean) * r * r) >> 8) + 4 * g * g + (((767 - rmean) * b * b) >> 8) + (hd * hd);
    }

    protected static int hueDistance(int red1, int green1, int blue1, int red2, int green2, int blue2) {
        int total1 = (red1 + green1 + blue1);
        int total2 = (red2 + green2 + blue2);
        if (total1 == 0 || total2 == 0) {
            return 0;
        }
        int factor1 = FACTORS[total1];
        int factor2 = FACTORS[total2];
        long r = (512 * (red1 * factor1 - red2 * factor2)) >> 10;
        long g = (green1 * factor1 - green2 * factor2);
        long b = (767 * (blue1 * factor1 - blue2 * factor2)) >> 10;
        return (int) ((r * r + g * g + b * b) >> 25);
    }

    protected int getColor(BufferedImage image) {
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
        int red = (int) (totalRed / a);
        int green = (int) (totalGreen / a);
        int blue = (int) (totalBlue / a);
        int alpha = (int) (totalAlpha / a);
        return (alpha << 24) + (red << 16) + (green << 8) + (blue << 0);
    }

    public long getDistance(BufferedImage image, int c1) {
        long totalDistSqr = 0;
        int width = image.getWidth();
        int height = image.getHeight();
        int area = width * height;
        int red1 = (c1 >> 16) & 0xFF;
        int green1 = (c1 >> 8) & 0xFF;
        int blue1 = (c1 >> 0) & 0xFF;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int c2 = image.getRGB(x, y);
                long distance = colorDistance(red1, green1, blue1, c2);
                totalDistSqr += distance * distance;
            }
        }
        return totalDistSqr / area;
    }

    public static class BiomeColor {
        public int id;
        public String name;
        public float temperature;
        public float rainfall;
        public int grass;
        public int foliage;

        public BiomeColor(int id, String name, float temperature, float rainfall, int grass, int foliage) {
            this.id = id;
            this.name = name;
            this.temperature = temperature;
            this.rainfall = rainfall;
            this.grass = grass;
            this.foliage = foliage;
        }
    }
}
