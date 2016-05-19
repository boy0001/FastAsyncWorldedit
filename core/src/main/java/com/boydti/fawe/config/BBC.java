package com.boydti.fawe.config;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.configuration.file.YamlConfiguration;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.StringMan;
import com.sk89q.worldedit.extension.platform.Actor;
import java.io.File;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public enum BBC {

    /*
     * Things to note about this class:
     * Can use multiple arguments %s, %s1, %s2, %s3 etc
     */
    PREFIX("&8(&4&lFAWE&8)&7", "Info"),
    SCHEMATIC_PASTING("&7The schematic is pasting. This cannot be undone.", "Info"),
    FIX_LIGHTING_SELECTION("&7Lighting has been fixed in %s0 chunks. (It may take a second for the packets to send)", "Info"),
    SET_REGION("&7Selection set to your current WorldEdit region", "Info"),
    WORLDEDIT_COMMAND_LIMIT("&7Please wait until your current action completes", "Info"),
    WORLDEDIT_DELAYED("&7Please wait while we process your WorldEdit action...", "Info"),
    WORLDEDIT_RUN("&7Apologies for the delay. Now executing: %s", "Info"),
    WORLDEDIT_COMPLETE("&7WorldEdit action completed.", "Info"),
    REQUIRE_SELECTION_IN_MASK("&7%s of your selection is not within your mask. You can only make edits within allowed regions.", "Info"),
    WORLDEDIT_VOLUME("&7You cannot select a volume of %current%. The maximum volume you can modify is %max%.", "Info"),
    WORLDEDIT_ITERATIONS("&7You cannot iterate %current% times. The maximum number of iterations allowed is %max%.", "Info"),
    WORLDEDIT_UNSAFE("&7Access to that command has been blocked", "Info"),
    WORLDEDIT_DANGEROUS_WORLDEDIT("&cFAWE processed unsafe WorldEdit at %s0 by %s1", "Info"),
    WORLDEDIT_BYPASS("&7&oTo bypass your restrictions use &c/wea", "Info"),
    WORLDEDIT_EXTEND("&cYour WorldEdit may have extended outside your allowed region.", "Error"),
    WORLDEDIT_BYPASSED("&7Currently bypassing WorldEdit restriction.", "Info"),
    WORLDEDIT_UNMASKED("&6Your WorldEdit is now unrestricted.", "Info"),
    WORLDEDIT_RESTRICTED("&6Your WorldEdit is now restricted.", "Info"),
    WORLDEDIT_OOM_ADMIN("&cPossible options:\n&8 - &7//fast\n&8 - &7Do smaller edits\n&8 - &7Allocate more memory\n&8 - &7Disable this safeguard", "Info"),
    COMPRESSED("History compressed. Saved ~ %s0b (%s1x smaller)", "Info"),
    ACTION_COMPLETE("Action completed in %s0 seconds", "Info"),

    COMMAND_COPY("%s0 blocks were copied", "WorldEdit.Copy"),
    COMMAND_PASTE("The clipboard has been pasted at %s0", "WorldEdit.Paste"),
    COMMAND_ROTATE("The clipboard has been rotated", "WorldEdit.Rotate"),
    COMMAND_FLIPPED("The clipboard has been flipped", "WorldEdit.Flip"),
    COMMAND_REGEN("Region regenerated.", "WorldEdit.Regen"),
    COMMAND_TREE("%s0 trees created.", "WorldEdit.Tree"),
    COMMAND_FLORA("%s0 flora created.", "WorldEdit.Flora"),
    COMMAND_HISTORY_CLEAR("History cleared", "WorldEdit.History"),
    COMMAND_REDO_FAIL("Nothing left to redo.", "WorldEdit.History"),
    COMMAND_REDO_SUCCESS("Redo successful.", "WorldEdit.History"),
    COMMAND_UNDO_FAIL("Nothing left to undo.", "WorldEdit.History"),
    COMMAND_UNDO_SUCCESS("Undo successful.", "WorldEdit.History"),

    OPERATION("Operation complete (%s0)", "WorldEdit.Operation"),

    SELECTION_WAND("Left click: select pos #1; Right click: select pos #2", "WorldEdit.Selection"),
    SELECTION_WAND_DISABLE("Edit wand disabled.", "WorldEdit.Selection"),
    SELECTION_WAND_ENABLE("Edit wand enabled.", "WorldEdit.Selection"),
    SELECTION_CHUNK("Chunk selected (%s0)", "WorldEdit.Selection"),
    SELECTION_CHUNKS("Chunks selected (%s0) - (%s1)", "WorldEdit.Selection"),
    SELECTION_CONTRACT("Region contracted %s0 blocks.", "WorldEdit.Selection"),
    SELECTION_COUNT("Counted %s0 blocks.", "WorldEdit.Selection"),
    SELECTION_DISTR("# total blocks: %s0", "WorldEdit.Selection"),
    SELECTION_EXPAND("Region expanded %s0 blocks", "WorldEdit.Selection"),
    SELECTION_EXPAND_VERT("Region expanded %s0 blocks (top to bottom)", "WorldEdit.Selection"),
    SELECTION_INSET("Region inset", "WorldEdit.Selection"),
    SELECTION_OUTSET("Region outset", "WorldEdit.Selection"),
    SELECTION_SHIFT("Region shifted", "WorldEdit.Selection"),
    SELECTION_CLEARED("Selection cleared", "WorldEdit.Selection"),

    BRUSH_BUTCHER("Butcher brush equiped (%s0)", "WorldEdit.Brush"),
    BRUSH_CLIPBOARD("Clipboard brush shape equipped", "WorldEdit.Brush"),
    BRUSH_CYLINDER("Cylinder brush shape equipped (%s0 by %s1).", "WorldEdit.Brush"),
    BRUSH_EXTINGUISHER("Extinguisher equipped (%s0).", "WorldEdit.Brush"),
    BRUSH_GRAVITY("Gravity brush equipped (%s0)", "WorldEdit.Brush"),
    BRUSH_HEIGHT("Height brush equipped (%s0)", "WorldEdit.Brush"),
    BRUSH_HEIGHT_INVALID("Invalid height map file (%s0)", "WorldEdit.Brush"),
    BRUSH_SMOOTH("Smooth brush equipped (%s0 x %s1 using %s2).", "WorldEdit.Brush"),
    BRUSH_SPHERE("Sphere brush shape equipped (%s0).", "WorldEdit.Brush"),

    SCHEMATIC_DELETE("%s0 has been deleted.", "Worldedit.Schematic"),
    SCHEMATIC_FORMAT("Available clipboard formats (Name: Lookup names)", "Worldedit.Schematic"),
    SCHEMATIC_LIST("Available schematics (Filename (Format)):", "Worldedit.Schematic"),
    SCHEMATIC_LOADED("%s0 loaded. Paste it with //paste", "Worldedit.Schematic"),
    SCHEMATIC_SAVED("%s0 saved.", "Worldedit.Schematic"),

    CLIPBOARD_CLEARED("Clipboard cleared", "WorldEdit.Clipboard"),

    VISITOR_BLOCK("%s0 blocks affected", "WorldEdit.Visitor"),
    VISITOR_ENTITY("%s0 entities affected", "WorldEdit.Visitor"),
    VISITOR_FLAT("%s0 columns affected", "WorldEdit.Visitor"),

    SELECTOR_CUBOID_POS1("First position set to %s0 %s1.", "WorldEdit.Selector"),
    SELECTOR_CUBOID_POS2("Second position set to %s0 %s1.", "WorldEdit.Selector"),

    PROGRESS_MESSAGE("[ Queue: %s0 | Dispatched: %s1 ]", "Progress"),
    PROGRESS_DONE   ("[ Took: %s0s ]", "Progress"),


    COMMAND_SYNTAX("&cUsage: &7%s0", "Error"),
    NO_PERM("&cYou are lacking the permission node: %s0", "Error"),
    SCHEMATIC_NOT_FOUND("&cSchematic not found: &7%s0", "Error"),
    NO_REGION("&cYou have no current WorldEdit region", "Error"),
    NOT_PLAYER("&cYou must be a player to perform this action!", "Error"),
    OOM(
            "&8[&cCritical&8] &cDetected low memory i.e. < 1%. FAWE will take the following actions:\n&8 - &7Terminate WE block placement\n&8 - &7Clear WE history\n&8 - &7Unload non essential chunks\n&8 - &7Kill entities\n&8 - &7Garbage collect\n&cIgnore this if trying to crash server.\n&7Note: Low memory is likely (but not necessarily) caused by WE",
            "Error"),

    WORLDEDIT_CANCEL_COUNT("&cCancelled %s0 edits.", "Cancel"),
    WORLDEDIT_CANCEL_REASON("&cYour WorldEdit action was cancelled:&7 %s0&c.", "Cancel"),
    WORLDEDIT_CANCEL_REASON_MANUAL("Manual cancellation", "Cancel"),
    WORLDEDIT_CANCEL_REASON_LOW_MEMORY("Low memory", "Cancel"),
    WORLDEDIT_CANCEL_REASON_MAX_CHANGES("Too many blocks changed", "Cancel"),
    WORLDEDIT_CANCEL_REASON_MAX_CHECKS("Too many block checks", "Cancel"),
    WORLDEDIT_CANCEL_REASON_MAX_TILES("Too many blockstates", "Cancel"),
    WORLDEDIT_CANCEL_REASON_MAX_ENTITIES("Too many entities", "Cancel"),
    WORLDEDIT_CANCEL_REASON_MAX_ITERATIONS("Max iterations", "Cancel"),
    WORLDEDIT_CANCEL_REASON_MAX_FAILS("Outside allowed region", "Cancel"),
    WORLDEDIT_FAILED_LOAD_CHUNK("&cSkipped loading chunk: &7%s0;%s1&c. Try increasing chunk-wait.", "Cancel"),

    LOADING_CLIPBOARD("Loading clipboard from disk, please wait.", "History"),
    INDEXING_HISTORY("Indexing %s history objects on disk, please wait.", "History"),
    INDEXING_COMPLETE("Indexing complete. Took: %s seconds!", "History"),




    ;


    private static final HashMap<String, String> replacements = new HashMap<>();
    /**
     * Translated
     */
    private String s;
    /**
     * Default
     */
    private String d;
    /**
     * What locale category should this translation fall under
     */
    private String cat;
    /**
     * Should the string be prefixed?
     */
    private boolean prefix;

    /**
     * Constructor for custom strings.
     */
    BBC() {
        /*
         * use setCustomString();
         */
    }

    /**
     * Constructor
     *
     * @param d default
     * @param prefix use prefix
     */
    BBC(final String d, final boolean prefix, final String cat) {
        this.d = d;
        if (this.s == null) {
            this.s = d;
        }
        this.prefix = prefix;
        this.cat = cat.toLowerCase();
    }

    /**
     * Constructor
     *
     * @param d default
     */
    BBC(final String d, final String cat) {
        this(d, true, cat.toLowerCase());
    }

    public String format(final Object... args) {
        String m = this.s;
        for (int i = args.length - 1; i >= 0; i--) {
            if (args[i] == null) {
                continue;
            }
            m = m.replaceAll("%s" + i, args[i].toString());
        }
        if (args.length > 0) {
            m = m.replaceAll("%s", args[0].toString());
        }
        return m;
    }

    public static void load(final File file) {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            final YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            final Set<String> keys = yml.getKeys(true);
            final EnumSet<BBC> all = EnumSet.allOf(BBC.class);
            final HashSet<String> allNames = new HashSet<>();
            final HashSet<String> allCats = new HashSet<>();
            final HashSet<String> toRemove = new HashSet<>();
            for (final BBC c : all) {
                allNames.add(c.name());
                allCats.add(c.cat.toLowerCase());
            }
            final HashSet<BBC> captions = new HashSet<>();
            boolean changed = false;
            for (final String key : keys) {
                if (!yml.isString(key)) {
                    if (!allCats.contains(key)) {
                        toRemove.add(key);
                    }
                    continue;
                }
                final String[] split = key.split("\\.");
                final String node = split[split.length - 1].toUpperCase();
                final BBC caption = allNames.contains(node) ? valueOf(node) : null;
                if (caption != null) {
                    final String value = yml.getString(key);
                    if (!split[0].equalsIgnoreCase(caption.cat)) {
                        changed = true;
                        yml.set(key, null);
                        yml.set(caption.cat + "." + caption.name().toLowerCase(), value);
                    }
                    captions.add(caption);
                    caption.s = value;
                } else {
                    toRemove.add(key);
                }
            }
            for (final String remove : toRemove) {
                changed = true;
                yml.set(remove, null);
            }
            replacements.clear();
            for (final char letter : "1234567890abcdefklmnor".toCharArray()) {
                replacements.put("&" + letter, "\u00a7" + letter);
            }
            replacements.put("\\\\n", "\n");
            replacements.put("\\n", "\n");
            replacements.put("&-", "\n");
            for (final BBC caption : all) {
                if (!captions.contains(caption)) {
                    changed = true;
                    yml.set(caption.cat + "." + caption.name().toLowerCase(), caption.d);
                }
                caption.s = StringMan.replaceFromMap(caption.s, replacements);
            }
            if (changed) {
                yml.save(file);
            }
        } catch (final Exception e) {
            MainUtil.handleError(e);
        }
    }

    @Override
    public String toString() {
        return s();
    }

    public boolean isEmpty() {
        return length() == 0;
    }

    public int length() {
        return toString().length();
    }

    public static String color(String string) {
        return StringMan.replaceFromMap(string, replacements);
    }

    public String s() {
        return this.s;
    }

    public boolean usePrefix() {
        return this.prefix;
    }

    public String getCat() {
        return this.cat;
    }

    public void send(Actor actor, final Object... args) {
        if (isEmpty()) {
            return;
        }
        if (actor == null) {
            Fawe.debug((PREFIX.isEmpty() ? "" : PREFIX.s() + " ") + this.format(args));
        } else {
            actor.print((PREFIX.isEmpty() ? "" : PREFIX.s() + " ") + this.format(args));
        }
    }

    public void send(final FawePlayer<?> player, final Object... args) {
        if (isEmpty()) {
            return;
        }
        if (player == null) {
            Fawe.debug((PREFIX.isEmpty() ? "" : PREFIX.s() + " ") + this.format(args));
        } else {
            player.sendMessage((PREFIX.isEmpty() ? "" : PREFIX.s() + " ") + this.format(args));
        }
    }

}
