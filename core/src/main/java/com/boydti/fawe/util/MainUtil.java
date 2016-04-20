package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.EndTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
import java.io.File;
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

    public static void deleteOlder(File directory, final long timeDiff) {
        final long now = System.currentTimeMillis();
        iterateFiles(directory, new RunnableVal<File>() {
            @Override
            public void run(File file) {
                long age = now - file.lastModified();
                if (age > timeDiff) {
                    System.out.println("Deleting file: " + file);
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
                        System.out.println("Deleting file: " + file);
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
