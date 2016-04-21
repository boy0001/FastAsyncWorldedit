package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.changeset.DiskStorageHistory;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.EndTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class MainUtil {

    private MainUtil() {}

    /*
     * Generic non plugin related utils
     *  e.g. sending messages
     */
    public static void sendMessage(final FawePlayer<?> player, String message) {
        message = BBC.color(message);
        if (player == null) {
            Fawe.debug(message);
        } else {
            player.sendMessage(message);
        }
    }

    public static void sendAdmin(final String s) {
        for (final FawePlayer<?> player : Fawe.imp().getPlayers()) {
            if (player.hasPermission("fawe.admin")) {
                player.sendMessage(s);
            }
        }
        Fawe.debug(s);
    }

    public static void iterateFiles(File directory, RunnableVal<File> task) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (null != files) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        iterateFiles(files[i], task);
                    } else {
                        task.run(files[i]);
                    }
                }
            }
        }
    }

    public static String secToTime(long time) {
            StringBuilder toreturn = new StringBuilder();
            if (time>=33868800) {
                int years = (int) (time/33868800);
                time-=years*33868800;
                toreturn.append(years+"y ");
            }
            if (time>=604800) {
                int weeks = (int) (time/604800);
                time-=weeks*604800;
                toreturn.append(weeks+"w ");
            }
            if (time>=86400) {
                int days = (int) (time/86400);
                time-=days*86400;
                toreturn.append(days+"d ");
            }
            if (time>=3600) {
                int hours = (int) (time/3600);
                time-=hours*3600;
                toreturn.append(hours+"h ");
            }
            if (time>=60) {
                int minutes = (int) (time/60);
                time-=minutes*60;
                toreturn.append(minutes+"m ");
            }
            if (toreturn.equals("")||time>0){
                toreturn.append((time)+"s ");
            }
            return toreturn.toString().trim();
    }

    public static long timeToSec(String string) {
        if (MathMan.isInteger(string)) {
            return Long.parseLong(string);
        }
        string = string.toLowerCase().trim().toLowerCase();
        if (string.equalsIgnoreCase("false")) {
            return 0;
        }
        String[] split = string.split(" ");
        long time = 0;
        for (String value : split) {
            int nums = Integer.parseInt(value.replaceAll("[^\\d]", ""));
            String letters = value.replaceAll("[^a-z]", "");
            switch (letters) {
                case "week":
                case "weeks":
                case "wks":
                case "w":

                    time += 604800 * nums;
                case "days":
                case "day":
                case "d":
                    time += 86400 * nums;
                case "hour":
                case "hr":
                case "hrs":
                case "hours":
                case "h":
                    time += 3600 * nums;
                case "minutes":
                case "minute":
                case "mins":
                case "min":
                case "m":
                    time += 60 * nums;
                case "seconds":
                case "second":
                case "secs":
                case "sec":
                case "s":
                    time += nums;
            }
        }
        return time;
    }

    public static List<DiskStorageHistory> getBDFiles(FaweLocation origin, UUID user, int radius, long timediff) {
        File history = new File(Fawe.imp().getDirectory(), "history" + File.separator + origin.world);
        if (!history.exists()) {
            return new ArrayList<>();
        }
        long now = System.currentTimeMillis();
        ArrayList<File> files = new ArrayList<>();
        for (File userFile : history.listFiles()) {
            if (!userFile.isDirectory()) {
                continue;
            }
            UUID userUUID;
            try {
                userUUID = UUID.fromString(userFile.getName());
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (user != null && !userUUID.equals(user)) {
                continue;
            }
            ArrayList<Integer> ids = new ArrayList<>();
            for (File file : userFile.listFiles()) {
                if (file.getName().endsWith(".bd")) {
                    if (timediff > Integer.MAX_VALUE || now - file.lastModified() <= timediff) {
                        files.add(file);
                    }
                }
            }
        }
        World world = origin.getWorld();
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                long value = a.lastModified() - b.lastModified();
                return value == 0 ? 0 : value < 0 ? 1 : -1;
            }
        });
        ArrayList<DiskStorageHistory> result = new ArrayList<>();
        for (File file : files) {
            UUID uuid = UUID.fromString(file.getParentFile().getName());
            DiskStorageHistory dsh = new DiskStorageHistory(world, uuid, Integer.parseInt(file.getName().split("\\.")[0]));
            int[] headerAndFooter = dsh.readHeaderAndFooter(new RegionWrapper(origin.x - 512, origin.x + 512, origin.z - 512, origin.z + 512));
            RegionWrapper region = new RegionWrapper(headerAndFooter[0], headerAndFooter[2], headerAndFooter[1], headerAndFooter[3]);
            if (region.distance(origin.x, origin.z) <= radius) {
                result.add(dsh);
            }
        }
        return result;
    }

    public static void deleteOlder(File directory, final long timeDiff) {
        final long now = System.currentTimeMillis();
        iterateFiles(directory, new RunnableVal<File>() {
            @Override
            public void run(File file) {
                long age = now - file.lastModified();
                if (age > timeDiff) {
                    Fawe.debug("Deleting file: " + file);
                    file.delete();
                }
            }
        });
    }
    
    public static boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (null != files) {
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    if (file.isDirectory()) {
                        deleteDirectory(files[i]);
                    } else {
                        Fawe.debug("Deleting file: " + file);
                        file.delete();
                    }
                }
            }
        }
        return (directory.delete());
    }

    public static boolean isValidTag(Tag tag) {
        if (tag instanceof EndTag) {
            return false;
        }
        else if (tag instanceof ListTag) {
            ListTag lt = (ListTag) tag;
            if ((lt).getType() == EndTag.class) {
                return false;
            }
        }
        else if (tag instanceof CompoundTag) {
            for (Entry<String, Tag> entry : ((CompoundTag) tag).getValue().entrySet()) {
                if (!isValidTag(entry.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isValidSign(CompoundTag tag) {
        Map<String, Tag> values = tag.getValue();
        return values.size() > 4 && values.containsKey("Text1");
    }
}
