package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.changeset.FaweStreamChangeSet;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.EndTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.Location;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    public static File getFile(File base, String path) {
        if (Paths.get(path).isAbsolute()) {
            return new File(path);
        }
        return new File(base, path);
    }

    public static File getFile(File base, String path, String extension) {
        return getFile(base, path.endsWith("." + extension) ? path : path + "." + extension);
    }

    public static URL upload(UUID uuid, String file, String extension, final RunnableVal<OutputStream> writeTask) {
        if (writeTask == null) {
            Fawe.debug("&cWrite task cannot be null");
            return null;
        }
        final String filename;
        final String website;
        if (uuid == null) {
            uuid = UUID.randomUUID();
            website = Settings.WEB_URL + "upload.php?" + uuid;
            filename = "plot." + extension;
        } else {
            website = Settings.WEB_URL  + "save.php?" + uuid;
            filename = file + '.' + extension;
        }
        final URL url;
        try {
            url = new URL(Settings.WEB_URL  + "?key=" + uuid + "&type=" + extension);
            String boundary = Long.toHexString(System.currentTimeMillis());
            URLConnection con = new URL(website).openConnection();
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            try (OutputStream output = con.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), true)) {
                String CRLF = "\r\n";
                writer.append("--" + boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"param\"").append(CRLF);
                writer.append("Content-Type: text/plain; charset=" + StandardCharsets.UTF_8.displayName()).append(CRLF);
                String param = "value";
                writer.append(CRLF).append(param).append(CRLF).flush();
                writer.append("--" + boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"schematicFile\"; filename=\"" + filename + '"').append(CRLF);
                writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(filename)).append(CRLF);
                writer.append("Content-Transfer-Encoding: binary").append(CRLF);
                writer.append(CRLF).flush();
                writeTask.value = output;
                writeTask.run();
                output.flush();
                writer.append(CRLF).flush();
                writer.append("--" + boundary + "--").append(CRLF).flush();
            }
            int responseCode = ((HttpURLConnection) con).getResponseCode();
            if (responseCode == 200) {
                return url;
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void setPosition(CompoundTag tag, int x, int y, int z) {
        Map<String, Tag> value = ReflectionUtils.getMap(tag.getValue());
        value.put("x", new IntTag(x));
        value.put("y", new IntTag(y));
        value.put("z", new IntTag(z));
    }

    public static void setEntityInfo(CompoundTag tag, Entity entity) {
        Map<String, Tag> map = ReflectionUtils.getMap(tag.getValue());
        map.put("Id", new StringTag(entity.getState().getTypeId()));
        ListTag pos = (ListTag) map.get("Pos");
        if (pos != null) {
            Location loc = entity.getLocation();
            List<Tag> posList = ReflectionUtils.getList(pos.getValue());
            posList.set(0, new DoubleTag(loc.getX()));
            posList.set(1, new DoubleTag(loc.getY()));
            posList.set(2, new DoubleTag(loc.getZ()));
        }
    }

    public static File getJarFile() {
        try {
            URL url = Fawe.class.getProtectionDomain().getCodeSource().getLocation();
            return new File(new URL(url.toURI().toString().split("\\!")[0].replaceAll("jar:file", "file")).toURI().getPath());
        } catch (MalformedURLException | URISyntaxException | SecurityException e) {
            MainUtil.handleError(e);
            return new File(Fawe.imp().getDirectory().getParentFile(), "FastAsyncWorldEdit.jar");
        }
    }

    public static File copyFile(File jar, String resource, File output) {
        try {
            if (output == null) {
                output = Fawe.imp().getDirectory();
            }
            if (!output.exists()) {
                output.mkdirs();
            }
            File newFile = new File(output, resource);
            if (newFile.exists()) {
                return newFile;
            }
            try (InputStream stream = Fawe.imp().getClass().getResourceAsStream(resource.startsWith("/") ? resource : "/" + resource)) {
                byte[] buffer = new byte[2048];
                if (stream == null) {
                    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(jar))) {
                        ZipEntry ze = zis.getNextEntry();
                        while (ze != null) {
                            String name = ze.getName();
                            if (name.equals(resource)) {
                                new File(newFile.getParent()).mkdirs();
                                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                                    int len;
                                    while ((len = zis.read(buffer)) > 0) {
                                        fos.write(buffer, 0, len);
                                    }
                                }
                                ze = null;
                            } else {
                                ze = zis.getNextEntry();
                            }
                        }
                        zis.closeEntry();
                    }
                    return newFile;
                }
                newFile.createNewFile();
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = stream.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                return newFile;
            }
        } catch (IOException e) {
            MainUtil.handleError(e);
            Fawe.debug("&cCould not save " + resource);
        }
        return null;
    }

    public static void sendCompressedMessage(FaweStreamChangeSet set, Actor actor)
    {
        try {
            int elements = set.size();
            int compressedSize = set.getCompressedSize();
            if (compressedSize == 0) {
                return;
            }
            /*
             * BlockVector
             * - reference to the object --> 8 bytes
             * - object header (java internals) --> 8 bytes
             * - double x, y, z --> 24 bytes
             *
             * BaseBlock
             * - reference to the object --> 8 bytes
             * - object header (java internals) --> 8 bytes
             * - short id, data --> 4 bytes
             * - NBTCompound (assuming null) --> 4 bytes
             *
             * There are usually two lists for the block changes:
             * 2 * BlockVector + 2 * BaseBlock = 128b
             *
             * WE has a lot more overhead, this is just a generous lower bound
             *
             * This compares FAWE's usage to standard WE.
             */
            int total = 128 * elements;

            int ratio = total / compressedSize;
            int saved = total - compressedSize;

            if (ratio > 3 && Thread.currentThread() != Fawe.get().getMainThread() && actor != null && actor.isPlayer() && actor.getSessionKey().isActive()) {
                BBC.COMPRESSED.send(actor, saved, ratio);
            }
        } catch (Exception e) {
            MainUtil.handleError(e);
        }
    }

    public static void handleError(Throwable e) {
        handleError(e, true);
    }

    public static void handleError(Throwable e, boolean debug) {
        if (e == null) {
            return;
        }
//        if (!debug) {
            e.printStackTrace();
            return;
//        }
//        String header = "====== FAWE: " + e.getLocalizedMessage() + " ======";
//        Fawe.debug(header);
//        String[] trace = getTrace(e);
//        for (int i = 0; i < trace.length && i < 8; i++) {
//            Fawe.debug(" - " + trace[i]);
//        }
//        String[] cause = getTrace(e.getCause());
//        Fawe.debug("Cause: " + (cause.length == 0 ? "N/A" : ""));
//        for (int i = 0; i < cause.length && i < 8; i++) {
//            Fawe.debug(" - " + cause[i]);
//        }
//        Fawe.debug(StringMan.repeat("=", header.length()));
    }

    public static String[] getTrace(Throwable e) {
        if (e == null) {
            return new String[0];
        }
        StackTraceElement[] elems = e.getStackTrace();
        String[] msg = new String[elems.length];//[elems.length + 1];
//        HashSet<String> packages = new HashSet<>();
        for (int i = 0; i < elems.length; i++) {
            StackTraceElement elem = elems[i];
            elem.getLineNumber();
            String methodName = elem.getMethodName();
            int index = elem.getClassName().lastIndexOf('.');
            String className = elem.getClassName();
//            if (!(index == -1 || className.startsWith("io.netty") || className.startsWith("javax") || className.startsWith("java") || className.startsWith("sun") || className.startsWith("net.minecraft") || className.startsWith("org.spongepowered") || className.startsWith("org.bukkit") || className.startsWith("com.google"))) {
//                packages.add(className.substring(0, index-1));
//            }
            String name = className.substring(index == -1 ? 0 : index + 1);
            name = name.length() == 0 ? elem.getClassName() : name;
            String argString = "(...)";
            try {
                for (Method method : Class.forName(elem.getClassName()).getDeclaredMethods()) {
                    if (method.getName().equals(methodName)) {
                        Class<?>[] params = method.getParameterTypes();
                        argString = "";
                        String prefix = "";
                        for (Class param : params) {
                            argString += prefix + param.getSimpleName();
                            prefix = ",";
                        }
                        argString = "[" + method.getReturnType().getSimpleName() + "](" + argString + ")";
                        break;
                    }
                }
            } catch(Throwable ignore) {}
            msg[i] = name + "." + methodName + argString + ":" + elem.getLineNumber();
        }
//        msg[msg.length-1] = StringMan.getString(packages);
        return msg;
    }

    public static void smoothArray(int[] data, int width, int radius, int weight) {
        int[] copy = data.clone();
        int length = data.length / width;
        int diameter = 2 * radius + 1;
        weight += diameter * diameter - 1;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < length; y++) {
                int index = x + width * y;
                int value = 0;
                int count = 0;
                for (int x2 = Math.max(0, x - radius); x2 <= Math.min(width - 1, x + radius); x2++) {
                    for (int y2 = Math.max(0, y - radius); y2 <= Math.min(length - 1, y + radius); y2++) {
                        count++;
                        int index2 = x2 + width * y2;
                        value += data[index2];

                    }
                }
                value += data[index] * (weight - count);
                value = value / (weight);
                data[index] = value;
            }
        }
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
