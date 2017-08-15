package com.boydti.fawe.util.metrics;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.configuration.file.YamlConfiguration;
import com.boydti.fawe.object.io.PGZIPOutputStream;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HttpsURLConnection;

/**
 * bStats collects some data for plugin authors.
 *
 * Check out https://bStats.org/ to learn more about bStats!
 */
public class BStats implements Closeable {

    // The version of this bStats class
    public static final int B_STATS_VERSION = 1;

    // The url to which the data is sent
    private final String url;

    // The plugin
    private final String plugin;
    private final String platform;
    private final boolean online;
    private final String serverVersion;
    private final String pluginVersion;
    private Thread task;

    // Is bStats enabled on this server?
    private boolean enabled;

    // The uuid of the server
    private UUID serverUUID;

    // Should failed requests be logged?
    private boolean logFailedRequests = false;

    // A list with all known metrics class objects including this one
    private static final List<Object> knownMetricsInstances = new ArrayList<>();

    public BStats() {
        this("FastAsyncWorldEdit", Fawe.get().getVersion().toString(), Fawe.imp().getPlatformVersion(), Fawe.imp().getPlatform(), Fawe.imp().isOnlineMode());
    }

    private BStats(String plugin, String pluginVersion, String serverVersion, String platform, boolean online) {
        this.url = "https://bStats.org/submitData/" + platform;
        this.plugin = plugin;
        this.pluginVersion = pluginVersion;
        this.serverVersion = serverVersion;
        this.platform = platform;
        this.online = online;

        StringBuilder data = new StringBuilder();

        File configFile = new File(getJarFile().getParentFile(), "bStats" + File.separator + "config.yml");

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Check if the config file exists
        if (!config.isSet("serverUuid")) {

            // Add default values
            config.addDefault("enabled", true);
            // Every server gets it's unique random id.
            config.addDefault("serverUuid", UUID.randomUUID().toString());
            // Should failed request be logged?
            config.addDefault("logFailedRequests", false);

            // Inform the server owners about bStats
            config.options().header(
                    "bStats collects some data for plugin authors like how many servers are using their plugins.\n" +
                            "To honor their work, you should not disable it.\n" +
                            "This has nearly no effect on the server performance!\n" +
                            "Check out https://bStats.org/ to learn more :)"
            ).copyDefaults(true);
            try {
                config.save(configFile);
            } catch (IOException ignored) { }
        }

        // Load the data
        serverUUID = UUID.fromString(config.getString("serverUuid"));

        Class<?> usedMetricsClass = getFirstBStatsClass();
        if (usedMetricsClass == null) {
            // Failed to get first metrics class
            return;
        }
        if (usedMetricsClass == getClass()) {
            // We are the first! :)
            linkMetrics(this);
            enabled = true;
            startSubmitting();
        } else {
            // We aren't the first so we link to the first metrics class
            try {
                usedMetricsClass.getMethod("linkMetrics", Object.class).invoke(null,this);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                if (logFailedRequests) {
                    System.out.println("Failed to link to first metrics class " + usedMetricsClass.getName() + "!");
                }
            }
        }
    }

    /**
     * Links an other metrics class with this class.
     * This method is called using Reflection.
     *
     * @param metrics An object of the metrics class to link.
     */
    public static void linkMetrics(Object metrics) {
        knownMetricsInstances.add(metrics);
    }

    /**
     * Gets the plugin specific data.
     * This method is called using Reflection.
     *
     * @return The plugin specific data.
     */
    public JsonObject getPluginData() {
        JsonObject data = new JsonObject();

        data.addProperty("pluginName", plugin);
        data.addProperty("pluginVersion", pluginVersion);

        JsonArray customCharts = new JsonArray();
        data.add("customCharts", customCharts);

        return data;
    }

    private void startSubmitting() {
        // No delay, as this class is only instantiated after the server is loaded
        this.task = new Thread(new Runnable() {
            @Override
            public void run() {
                while (enabled) {
                    submitData();
                    try {
                        if (enabled) Thread.sleep(TimeUnit.MINUTES.toMillis(30));
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        });
        this.task.start();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    @Override
    public void close() {
        enabled = false;
    }

    /**
     * Gets the server specific data.
     *
     * @return The server specific data.
     */
    private JsonObject getServerData() {
        int playerAmount = Fawe.imp() != null ? Fawe.imp().getPlayerCount() : 1;
        int onlineMode = online ? 1 : 0;

        int managedServers = 1;

        // OS/Java specific data
        String javaVersion = System.getProperty("java.version");
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        String osVersion = System.getProperty("os.version");
        int coreCount = Runtime.getRuntime().availableProcessors();

        JsonObject data = new JsonObject();

        data.addProperty("serverUUID", serverUUID.toString());

        data.addProperty("playerAmount", playerAmount);
        data.addProperty("managedServers", managedServers);
        data.addProperty("onlineMode", onlineMode);
        data.addProperty(platform + "Version", serverVersion);

        data.addProperty("javaVersion", javaVersion);
        data.addProperty("osName", osName);
        data.addProperty("osArch", osArch);
        data.addProperty("osVersion", osVersion);
        data.addProperty("coreCount", coreCount);

        return data;
    }

    /**
     * Collects the data and sends it afterwards.
     */
    private void submitData() {
        final JsonObject data = getServerData();

        final JsonArray pluginData = new JsonArray();
        // Search for all other bStats Metrics classes to get their plugin data
        for (Object metrics : knownMetricsInstances) {
            try {
                Object plugin = metrics.getClass().getMethod("getPluginData").invoke(metrics);
                if (plugin instanceof JsonObject) {
                    pluginData.add((JsonObject) plugin);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) { }
        }

        data.add("plugins", pluginData);

        try {
            // Send the data
            sendData(data);
        } catch (Exception e) {
            // Something went wrong! :(
            if (logFailedRequests) {
                System.err.println("Could not submit plugin stats!");
            }
        }
    }

    /**
     * Gets the first bStat Metrics class.
     *
     * @return The first bStats metrics class.
     */
    private Class<?> getFirstBStatsClass() {
        Path configPath = getJarFile().toPath().getParent().resolve("bStats");
        configPath.toFile().mkdirs();
        File tempFile = new File(configPath.toFile(), "temp.txt");

        try {
            String className = readFile(tempFile);
            if (className != null) {
                try {
                    // Let's check if a class with the given name exists.
                    return Class.forName(className);
                } catch (ClassNotFoundException ignored) { }
            }
            writeFile(tempFile, getClass().getName());
            return getClass();
        } catch (IOException e) {
            if (logFailedRequests) {
                System.err.println("Failed to get first bStats class!");
            }
            return null;
        }
    }

    private File getJarFile() {
        try {
            URL url = BStats.class.getProtectionDomain().getCodeSource().getLocation();
            return new File(new URL(url.toURI().toString().split("\\!")[0].replaceAll("jar:file", "file")).toURI().getPath());
        } catch (MalformedURLException | URISyntaxException | SecurityException e) {
            return new File(".", "plugins");
        }
    }

    /**
     * Reads the first line of the file.
     *
     * @param file The file to read. Cannot be null.
     * @return The first line of the file or <code>null</code> if the file does not exist or is empty.
     * @throws IOException If something did not work :(
     */
    private String readFile(File file) throws IOException {
        if (!file.exists()) {
            return null;
        }
        try (
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader =  new BufferedReader(fileReader);
        ) {
            return bufferedReader.readLine();
        }
    }

    /**
     * Writes a String to a file. It also adds a note for the user,
     *
     * @param file The file to write to. Cannot be null.
     * @param lines The lines to write.
     * @throws IOException If something did not work :(
     */
    private void writeFile(File file, String... lines) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        try (
                FileWriter fileWriter = new FileWriter(file);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)
        ) {
            for (String line : lines) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }
        }
    }

    /**
     * Sends the data to the bStats server.
     *
     * @param data The data to send.
     * @throws Exception If the request failed.
     */
    private void sendData(JsonObject data) throws Exception {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }

        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();

        // Compress the data to save bandwidth
        byte[] compressedData = compress(data.toString());

        // Add headers
        connection.setRequestMethod("POST");
        connection.addRequestProperty("Accept", "application/json");
        connection.addRequestProperty("Connection", "close");
        connection.addRequestProperty("Content-Encoding", "gzip"); // We gzip our request
        connection.addRequestProperty("Content-Length", String.valueOf(compressedData.length));
        connection.setRequestProperty("Content-Type", "application/json"); // We send our data in JSON format
        connection.setRequestProperty("User-Agent", "MC-Server/" + B_STATS_VERSION);

        // Send data
        connection.setDoOutput(true);
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.write(compressedData);
        outputStream.flush();
        outputStream.close();

        connection.getInputStream().close(); // We don't care about the response - Just send our data :)
    }

    /**
     * Gzips the given String.
     *
     * @param str The string to gzip.
     * @return The gzipped String.
     * @throws IOException If the compression failed.
     */
    private byte[] compress(final String str) throws IOException {
        if (str == null) {
            return null;
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PGZIPOutputStream gzip = new PGZIPOutputStream(outputStream);
        gzip.write(str.getBytes("UTF-8"));
        gzip.close();
        return outputStream.toByteArray();
    }

}