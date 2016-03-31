package com.boydti.fawe.config;

import java.io.File;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.StringMan;

public enum BBC {

    /*
     * Things to note about this class:
     * Can use multiple arguments %s, %s1, %s2, %s3 etc
     */
    COMMAND_SYNTAX("&cUsage: &7%s0", "Error"),
    NO_PERM("&cYou are lacking the permission node: %s0", "Error"),
    SCHEMATIC_NOT_FOUND("&cSchematic not found: &7%s0", "Error"),
    SCHEMATIC_PASTING("&7The schematic is pasting. This cannot be undone.", "Info"),
    FIX_LIGHTING_CHUNK("&7Lighting has been fixed in your current chunk. Relog to see the affect.", "Info"),
    FIX_LIGHTING_SELECTION("&7Lighting has been fixed in %s0 chunks. Relog to see the affect.", "Info"),
    NO_REGION("&cYou have no current WorldEdit region", "Error"),
    SET_REGION("&7Selection set to your current WorldEdit region", "Info"),
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
    WORLDEDIT_OOM("&cYour WorldEdit action was cancelled due to low memory.", "Info"),
    WORLDEDIT_OOM_ADMIN("&cPossible options:\n&8 - &7//fast\n&8 - &7Do smaller edits\n&8 - &7Allocate more memory\n&8 - &7Disable this safeguard", "Info"),
    NOT_PLAYER("&cYou must be a player to perform this action!", "Error"),
    OOM(
    "&8[&cCritical&8] &cDetected low memory i.e. < 1%. FAWE will take the following actions:\n&8 - &7Terminate WE block placement\n&8 - &7Clear WE history\n&8 - &7Unload non essential chunks\n&8 - &7Kill entities\n&8 - &7Garbage collect\n&cIgnore this if trying to crash server.\n&7Note: Low memory is likely (but not necessarily) caused by WE",
    "Error");

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
            e.printStackTrace();
        }
    }

    public String s() {
        return this.s;
    }

    public boolean usePrefix() {
        return this.prefix;
    }

    /**
     * @return translated and color decoded
     *
     * @see org.bukkit.ChatColor#translateAlternateColorCodes(char, String)
     */
    public String translated() {
        return ChatColor.translateAlternateColorCodes('&', this.s());
    }

    public String getCat() {
        return this.cat;
    }

    public void send(final FawePlayer<?> player, final Object... args) {
        if (player == null) {
            Fawe.debug(this.format(args));
        } else {
            player.sendMessage(this.format(args));
        }
    }

}
