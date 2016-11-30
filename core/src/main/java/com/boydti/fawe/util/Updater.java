package com.boydti.fawe.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweVersion;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Scanner;

public class Updater {
    public static void update(String platform, FaweVersion currentVersion) {
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
                if (currentVersion == null || version.isNewer(currentVersion)) {
                    URL download = new URL(downloadUrl.replaceAll("%platform%", platform).replaceAll("%version%", versionString));
                    try (ReadableByteChannel rbc = Channels.newChannel(download.openStream())) {
                        File jarFile = MainUtil.getJarFile();
                        File outFile = new File(jarFile.getParent(), "update" + File.separator + jarFile.getName());
                        File outFileParent = outFile.getParentFile();
                        if (!outFileParent.exists()) {
                            outFileParent.mkdirs();
                        }
                        try (FileOutputStream fos = new FileOutputStream(outFile)) {
                            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                        }
                        Fawe.debug("Updated FAWE to " + versionString);
                    }
                }
            }
        } catch (Throwable ignore) {}
    }
}