package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.EndTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
import java.io.File;
import java.lang.reflect.Array;
import java.util.Map;
import java.util.Map.Entry;

public class MainUtil {
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
        for (final FawePlayer<?> player : Fawe.get().getCachedPlayers()) {
            if (player.hasPermission("fawe.admin")) {
                player.sendMessage(s);
            }
        }
        Fawe.debug(s);
    }

    public static void warnDeprecated(Class... alternatives) {
        StackTraceElement[] stack = new RuntimeException().getStackTrace();
        if (stack.length > 1) {
            try {
                StackTraceElement creatorElement = stack[stack.length - 2];
                String className = creatorElement.getClassName();
                Class clazz = Class.forName(className);
                String creator = clazz.getSimpleName();
                String packageName = clazz.getPackage().getName();

                StackTraceElement deprecatedElement = stack[stack.length - 1];
                String myName = Class.forName(deprecatedElement.getFileName()).getSimpleName();
                Fawe.debug("@" + creator + " from " + packageName +": " + myName + " is deprecated.");
                Fawe.debug(" - Alternatives: " + StringMan.getString(alternatives));
            } catch (Throwable ignore) {}
        }
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

    /**
     * The int[] will be in the form: [chunkx, chunkz, pos1x, pos1z, pos2x, pos2z, isedge] and will represent the bottom and top parts of the chunk
     */
    public static void chunkTaskSync(RegionWrapper region, final RunnableVal<int[]> task) {
        final int p1x = region.minX;
        final int p1z = region.minZ;
        final int p2x = region.maxX;
        final int p2z = region.maxZ;
        final int bcx = p1x >> 4;
        final int bcz = p1z >> 4;
        final int tcx = p2x >> 4;
        final int tcz = p2z >> 4;
        task.value = new int[7];
        for (int x = bcx; x <= tcx; x++) {
            for (int z = bcz; z <= tcz; z++) {
                task.value[0] = x;
                task.value[1] = z;
                task.value[2] = task.value[0] << 4;
                task.value[3] = task.value[1] << 4;
                task.value[4] = task.value[2] + 15;
                task.value[5] = task.value[3] + 15;
                task.value[6] = 0;
                if (task.value[0] == bcx) {
                    task.value[2] = p1x;
                    task.value[6] = 1;
                }
                if (task.value[0] == tcx) {
                    task.value[4] = p2x;
                    task.value[6] = 1;
                }
                if (task.value[1] == bcz) {
                    task.value[3] = p1z;
                    task.value[6] = 1;
                }
                if (task.value[1] == tcz) {
                    task.value[5] = p2z;
                    task.value[6] = 1;
                }
                task.run();
            }
        }
    }

    public static Object copyNd(Object arr) {
        if (arr.getClass().isArray()) {
            int innerArrayLength = Array.getLength(arr);
            Class component = arr.getClass().getComponentType();
            Object newInnerArray = Array.newInstance(component, innerArrayLength);
            //copy each elem of the array
            for (int i = 0; i < innerArrayLength; i++) {
                Object elem = copyNd(Array.get(arr, i));
                Array.set(newInnerArray, i, elem);
            }
            return newInnerArray;
        } else {
            return arr;//cant deep copy an opac object??
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
        if (values.size() > 4 && values.containsKey("Text1")) {
            Tag text1 = values.get("Text1");
            Object value = text1.getValue();
            return value != null && value instanceof String && ((String) value).length() > 0;
        }
        return false;
    }
}
