package com.boydti.fawe.bukkit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.Perm;
import com.boydti.fawe.util.WEManager;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.Region;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Kinda a really messy class I just copied over from an old project<br>
 *  - Still works, so cbf cleaning it up<br>
 *  - Completely optional to have this class enabled since things get cancelled further down anyway<br>
 *  - Useful since it informs the player why an edit changed no blocks etc.<br>
 *  - Predicts the number of blocks changed and cancels the edit if it's too large<br>
 *  - Predicts where the edit will effect and cancels it if it's outside a region<br>
 *  - Restricts the brush iteration limit<br>
 *  @deprecated as I plan on replacing it at some point
 */
@Deprecated
public class WEListener implements Listener {

    public final HashSet<String> rad1 = new HashSet<>(Arrays.asList("forestgen", "pumpkins", "drain", "fixwater", "fixlava", "replacenear", "snow", "thaw", "ex", "butcher", "size"));
    public final HashSet<String> rad2 = new HashSet<>(Arrays.asList("fill", "fillr", "removenear", "remove"));
    public final HashSet<String> rad2_1 = new HashSet<>(Arrays.asList("hcyl", "cyl"));
    public final HashSet<String> rad2_2 = new HashSet<>(Arrays.asList("sphere", "pyramid"));
    public final HashSet<String> rad2_3 = new HashSet<>(Arrays.asList("brush smooth"));
    public final HashSet<String> rad3_1 = new HashSet<>(Arrays.asList("brush gravity"));
    public final HashSet<String> rad3_2 = new HashSet<>(Arrays.asList("brush sphere", "brush cylinder"));

    public final HashSet<String> region = new HashSet<>(Arrays.asList("move", "set", "replace", "overlay", "walls", "outline", "deform", "hollow", "smooth", "naturalize", "paste", "count", "distr",
    "copy", "cut", "green", "setbiome"));
    public final HashSet<String> regionExtend = new HashSet<>(Arrays.asList("stack"));
    public final HashSet<String> unregioned = new HashSet<>(Arrays.asList("paste", "redo", "undo", "rotate", "flip", "generate", "schematic", "schem"));
    public final HashSet<String> unsafe1 = new HashSet<>(Arrays.asList("cs", ".s", "restore", "snapshot", "delchunks", "listchunks"));
    public final HashSet<String> restricted = new HashSet<>(Arrays.asList("up"));
    public final HashSet<String> other = new HashSet<>(Arrays.asList("undo", "redo", "schematic", "schem", "count"));

    public boolean checkCommand(final List<String> list, final String cmd) {
        for (final String identifier : list) {
            if (("/" + identifier).equals(cmd) || ("//" + identifier).equals(cmd) || ("/worldedit:/" + identifier).equals(cmd) || ("/worldedit:" + identifier).equals(cmd)) {
                return true;
            }
        }
        return false;
    }

    public String reduceCmd(final String cmd, final boolean single) {
        if (cmd.startsWith("/worldedit:/")) {
            return cmd.substring(12);
        }
        if (cmd.startsWith("/worldedit:")) {
            return cmd.substring(11);
        }
        if (cmd.startsWith("//")) {
            return cmd.substring(2);
        }
        if (single && cmd.startsWith("/")) {
            return cmd.substring(1);
        }
        return cmd;
    }

    public int getInt(final String s) {
        try {
            int max = 0;
            final String[] split = s.split(",");
            for (final String rad : split) {
                final int val = Integer.parseInt(rad);
                if (val > max) {
                    max = val;
                }
            }
            return max;
        } catch (final NumberFormatException e) {
            return 0;
        }
    }

    public boolean checkVolume(final FawePlayer<Player> player, final long volume, final long max, final Cancellable e) {
        if (volume > max) {
            MainUtil.sendMessage(FawePlayer.wrap(player.getName()), BBC.WORLDEDIT_VOLUME.s().replaceAll("%current%", volume + "").replaceAll("%max%", max + ""));
            e.setCancelled(true);
        }
        if (Perm.hasPermission(player, "fawe.admin") && !Perm.hasPermission(player, "fawe.bypass")) {
            BBC.WORLDEDIT_BYPASS.send(player);
        }
        return true;
    }

    public boolean checkSelection(final FawePlayer<Player> player, final int modifier, final long max, final Cancellable e) {
        final LocalSession session = Fawe.get().getWorldEdit().getSession(player.getName());
        final LocalWorld w = BukkitUtil.getLocalWorld(player.parent.getWorld());
        Region selection = null;
        try {
            selection = session.getSelection(w);
        } catch (final IncompleteRegionException e2) {}
        if (selection == null) {
            return true;
        }
        final BlockVector pos1 = selection.getMinimumPoint().toBlockVector();
        final BlockVector pos2 = selection.getMaximumPoint().toBlockVector();
        final HashSet<RegionWrapper> mask = WEManager.IMP.getMask(player);
        final RegionWrapper region = new RegionWrapper(pos1.getBlockX(), pos2.getBlockX(), pos1.getBlockZ(), pos2.getBlockZ());
        if (Settings.REQUIRE_SELECTION) {
            String arg = null;
            if (!WEManager.IMP.regionContains(region, mask)) {
                arg = "pos1 + pos2";
            } else if (!WEManager.IMP.maskContains(mask, pos1.getBlockX(), pos1.getBlockZ())) {
                arg = "pos1";
            } else if (!WEManager.IMP.maskContains(mask, pos2.getBlockX(), pos2.getBlockZ())) {
                arg = "pos2";
            }
            if (arg != null) {
                BBC.REQUIRE_SELECTION_IN_MASK.send(player, arg);
                e.setCancelled(true);
                if (Perm.hasPermission(player, "fawe.admin") && !Perm.hasPermission(player, "fawe.bypass")) {
                    BBC.WORLDEDIT_BYPASS.send(player);
                }
                return true;
            }
            if (!WEManager.IMP.regionContains(region, mask)) {
                BBC.REQUIRE_SELECTION_IN_MASK.send(player, "pos1 + pos2");
                e.setCancelled(true);
                if (Perm.hasPermission(player, "fawe.admin") && !Perm.hasPermission(player, "fawe.bypass")) {
                    BBC.WORLDEDIT_BYPASS.send(player);
                }
                return true;
            }
        }
        final long volume = Math.abs((pos1.getBlockX() - pos2.getBlockX()) * (pos1.getBlockY() - pos2.getBlockY()) * (pos1.getBlockZ() - pos2.getBlockZ())) * modifier;
        return this.checkVolume(player, volume, max, e);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public boolean onPlayerCommand(final PlayerCommandPreprocessEvent e) {
        final FawePlayer<Player> player = FawePlayer.wrap(e.getPlayer());
        final String message = e.getMessage();
        final String cmd = message.toLowerCase();
        final boolean single = true;
        final String[] split = cmd.split(" ");

        final long maxVolume = Settings.WE_MAX_VOLUME;
        final long maxIterations = Settings.WE_MAX_ITERATIONS;
        //        if (player.hasPermission("fawe.bypass")) {
        //            return true;
        //        }
        if (split.length >= 2) {
            final String reduced = this.reduceCmd(split[0], single);
            final String reduced2 = this.reduceCmd(split[0] + " " + split[1], single);
            if (this.rad1.contains(reduced)) {
                if (WEManager.IMP.delay(player, message)) {
                    e.setCancelled(true);
                    return true;
                }
                final long volume = this.getInt(split[1]) * 256;
                return this.checkVolume(player, volume, maxVolume, e);
            }
            if (this.rad2.contains(reduced)) {
                if (WEManager.IMP.delay(player, message)) {
                    e.setCancelled(true);
                    return true;
                }
                if (split.length >= 3) {
                    final long volume = this.getInt(split[2]) * 256;
                    return this.checkVolume(player, volume, maxVolume, e);
                }
                return true;
            }
            if (this.rad2_1.contains(reduced)) {
                if (WEManager.IMP.delay(player, message)) {
                    e.setCancelled(true);
                    return true;
                }
                if (split.length >= 4) {
                    final long volume = this.getInt(split[2]) * this.getInt(split[3]);
                    return this.checkVolume(player, volume, maxVolume, e);
                }
                return true;
            }
            if (this.rad2_2.contains(reduced)) {
                if (WEManager.IMP.delay(player, message)) {
                    e.setCancelled(true);
                    return true;
                }
                if (split.length >= 3) {
                    final long radius = this.getInt(split[2]);
                    final long volume = radius * radius;
                    return this.checkVolume(player, volume, maxVolume, e);
                }
                return true;
            }
            if (this.rad2_3.contains(reduced2)) {
                if (WEManager.IMP.delay(player, message)) {
                    e.setCancelled(true);
                    return true;
                }
                if (split.length >= 3) {
                    if (split.length == 4) {
                        final int iterations = this.getInt(split[3]);
                        if (iterations > maxIterations) {
                            MainUtil.sendMessage(player, BBC.WORLDEDIT_ITERATIONS.s().replaceAll("%current%", iterations + "").replaceAll("%max%", maxIterations + ""));
                            e.setCancelled(true);
                            if (Perm.hasPermission(player, "fawe.admin") && !Perm.hasPermission(player, "fawe.bypass")) {
                                BBC.WORLDEDIT_BYPASS.send(player);
                            }
                            return true;
                        }
                    }
                    final long radius = this.getInt(split[2]);
                    final long volume = radius * radius;
                    return this.checkVolume(player, volume, maxVolume, e);
                }
                return true;
            }
            if (this.rad3_1.contains(reduced2)) {
                if (WEManager.IMP.delay(player, message)) {
                    e.setCancelled(true);
                    return true;
                }
                if (split.length >= 3) {
                    int i = 2;
                    if (split[i].equalsIgnoreCase("-h")) {
                        i = 3;
                    }
                    final long radius = this.getInt(split[i]);
                    final long volume = radius * radius;
                    return this.checkVolume(player, volume, maxVolume, e);
                }
                return true;
            }
            if (this.rad3_2.contains(reduced2)) {
                if (WEManager.IMP.delay(player, message)) {
                    e.setCancelled(true);
                    return true;
                }
                if (split.length >= 4) {
                    int i = 3;
                    if (split[i].equalsIgnoreCase("-h")) {
                        i = 4;
                    }
                    final long radius = this.getInt(split[i]);
                    final long volume = radius * radius;
                    return this.checkVolume(player, volume, maxVolume, e);
                }
                return true;
            }
            if (this.regionExtend.contains(reduced)) {
                if (WEManager.IMP.delay(player, message)) {
                    e.setCancelled(true);
                    return true;
                }
                return this.checkSelection(player, this.getInt(split[1]), maxVolume, e);
            }
        }
        final String reduced = this.reduceCmd(split[0], single);
        if (Settings.WE_BLACKLIST.contains(reduced)) {
            BBC.WORLDEDIT_UNSAFE.send(player);
            e.setCancelled(true);
            if (Perm.hasPermission(player, "fawe.admin") && !Perm.hasPermission(player, "fawe.bypass")) {
                BBC.WORLDEDIT_BYPASS.send(player);
            }
        }
        if (this.restricted.contains(reduced)) {
            final HashSet<RegionWrapper> mask = WEManager.IMP.getMask(player);
            final Location loc = player.parent.getLocation();
            for (final RegionWrapper region : mask) {
                if (region.isIn(loc.getBlockX(), loc.getBlockZ())) {
                    if (WEManager.IMP.delay(player, message)) {
                        e.setCancelled(true);
                        return true;
                    }
                    return true;
                }
            }
            e.setCancelled(true);
            BBC.REQUIRE_SELECTION_IN_MASK.send(player);
            return true;
        }
        if (this.region.contains(reduced)) {
            if (WEManager.IMP.delay(player, message)) {
                e.setCancelled(true);
                return true;
            }
            return this.checkSelection(player, 1, maxVolume, e);
        }
        if (this.unregioned.contains(reduced)) {
            if (WEManager.IMP.delay(player, message)) {
                e.setCancelled(true);
                return true;
            }
        }
        if (this.other.contains(reduced)) {
            if (WEManager.IMP.delay(player, message)) {
                e.setCancelled(true);
                return true;
            }
        }
        return true;
    }
}
