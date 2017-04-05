package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweVersion;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Scanner;

public class Updater {
    private FaweVersion newVersion;
    private String changes;

    public String getChanges() throws IOException {
        if (changes == null) {
            try (Scanner scanner = new Scanner(new URL("http://boydti.com/fawe/cl?" + Integer.toHexString(Fawe.get().getVersion().hash)).openStream(), "UTF-8")) {
                changes = scanner.useDelimiter("\\A").next();
            }
        }
        return changes;
    }

    public void update(String platform, FaweVersion currentVersion) {
        if (currentVersion == null || platform == null) {
            return;
        }
        try {
            String downloadUrl = "http://ci.athion.net/job/FastAsyncWorldEdit/lastSuccessfulBuild/artifact/target/FastAsyncWorldEdit-%platform%-%version%.jar";
            String versionUrl = "http://empcraft.com/fawe/version.php?%platform%";
            URL url = new URL(versionUrl.replace("%platform%", platform));
            try (Scanner reader = new Scanner(url.openStream())) {
                String versionString = reader.next();
                FaweVersion version = new FaweVersion(versionString);
                if (version.isNewer(newVersion != null ? newVersion : currentVersion)) {
                    newVersion = version;
                    URL download = new URL(downloadUrl.replaceAll("%platform%", platform).replaceAll("%version%", versionString));
                    try (ReadableByteChannel rbc = Channels.newChannel(download.openStream())) {
                        File jarFile = MainUtil.getJarFile();
                        File outFile = new File(jarFile.getParent(), "update" + File.separator + jarFile.getName());
                        boolean exists = outFile.exists();
                        if (exists) {
                            outFile.delete();
                        } else {
                            File outFileParent = outFile.getParentFile();
                            if (!outFileParent.exists()) {
                                outFileParent.mkdirs();
                            }
                        }
                        try (FileOutputStream fos = new FileOutputStream(outFile)) {
                            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                        }
                        Fawe.debug("Updated FAWE to " + versionString);
                        MainUtil.sendAdmin("&7Restart to update FAWE with these changes: &c/fawe changelog &7or&c " + "http://boydti.com/fawe/cl?" + Integer.toHexString(currentVersion.hash));
                    }
                } else {
                    System.out.println("Not newer");
                }
            }
        } catch (Throwable ignore) {}
    }
}