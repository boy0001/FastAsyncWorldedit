/*
 * Copyright 2011-2013 Tyler Blair. All rights reserved.
 * Ported to Minecraft Forge by Mike Primm
 * 1.7.x update by Dries007
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ''AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and contributors and should not be interpreted as representing official policies,
 * either expressed or implied, of anybody else.
 */

package com.boydti.fawe.forge;

import com.boydti.fawe.util.MainUtil;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ForgeMetrics {

    /**
     * The current revision number
     */
    private final static int REVISION = 7;

    /**
     * The base url of the metrics domain
     */
    private static final String BASE_URL = "http://report.mcstats.org";

    /**
     * The url used to report a server's status
     */
    private static final String REPORT_URL = "/plugin/%s";

    /**
     * Interval of time to ping (in minutes)
     */
    private static final int PING_INTERVAL = 15;

    /**
     * The mod this metrics submits for
     */
    private final String modName;

    private final String modVersion;

    /**
     * The metrics configuration file
     */
    private final Configuration configuration;

    /**
     * The metrics configuration file
     */
    private final File configurationFile;

    /**
     * Unique server id
     */
    private final String guid;

    /**
     * Debug mode
     */
    private final boolean debug;

    private Thread thread = null;
    private boolean firstPost = true;
    int tickCount;

    public ForgeMetrics(final String modName, final String modVersion) throws IOException {
        if (modName == null || modVersion == null) {
            throw new IllegalArgumentException("modName and modVersion cannot be null");
        }

        this.modName = modName;
        this.modVersion = modVersion;

        // load the config
        configurationFile = getConfigFile();
        configuration = new Configuration(configurationFile);

        // Get values, and add some defaults, if needed
        configuration.get(Configuration.CATEGORY_GENERAL, "opt-out", false, "Set to true to disable all reporting");
        guid = configuration.get(Configuration.CATEGORY_GENERAL, "guid", UUID.randomUUID().toString(), "Server unique ID").getString();
        debug = configuration.get(Configuration.CATEGORY_GENERAL, "debug", false, "Set to true for verbose debug").getBoolean(false);
        configuration.save();
    }

    /**
     * Start measuring statistics. This will immediately create an async
     * repeating task as the plugin and send the initial data to the metrics
     * backend, and then after that it will post in increments of PING_INTERVAL
     * * 1200 ticks.
     *
     * @return True if statistics measuring is running, otherwise false.
     */
    public boolean start() {
        // Did we opt out?
        if (isOptOut()) {
            return false;
        }

        FMLCommonHandler.instance().bus().register(this);

        return true;
    }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent tick) {
        if (tick.phase != TickEvent.Phase.END) return;

        if (tickCount++ % (PING_INTERVAL * 1200) != 0) return;

        if (thread == null) {
            thread = new Thread(new Runnable() {
                public void run() {
                    try {
                        // Disable Task, if it is running and the server owner decided
                        // to opt-out
                        if (isOptOut()) {
                            FMLCommonHandler.instance().bus().unregister(ForgeMetrics.this);
                            return;
                        }
                        // We use the inverse of firstPost because if it
                        // is the first time we are posting,
                        // it is not a interval ping, so it evaluates to
                        // FALSE
                        // Each time thereafter it will evaluate to
                        // TRUE, i.e PING!
                        postPlugin(!firstPost);
                        // After the first post we set firstPost to
                        // false
                        // Each post thereafter will be a ping
                        firstPost = false;
                    } catch (IOException e) {
                        if (debug) {
                            FMLLog.info("[Metrics] Exception - %s", e.getMessage());
                        }
                    } finally {
                        thread = null;
                    }
                }
            });
            thread.start();
        }
    }

    /**
     * Stop processing
     */
    public void stop() {
    }

    /**
     * Has the server owner denied plugin metrics?
     *
     * @return true if metrics should be opted out of it
     */
    public boolean isOptOut() {
        // Reload the metrics file
        configuration.load();
        return configuration.get(Configuration.CATEGORY_GENERAL, "opt-out", false).getBoolean(false);
    }

    /**
     * Enables metrics for the server by setting "opt-out" to false in the
     * config file and starting the metrics task.
     *
     * @throws java.io.IOException
     */
    public void enable() throws IOException {
        // Check if the server owner has already set opt-out, if not, set it.
        if (isOptOut()) {
            configuration.getCategory(Configuration.CATEGORY_GENERAL).get("opt-out").set("false");
            configuration.save();
        }
        // Enable Task, if it is not running
        FMLCommonHandler.instance().bus().register(this);
    }

    /**
     * Disables metrics for the server by setting "opt-out" to true in the
     * config file and canceling the metrics task.
     *
     * @throws java.io.IOException
     */
    public void disable() throws IOException {
        // Check if the server owner has already set opt-out, if not, set it.
        if (!isOptOut()) {
            configuration.getCategory(Configuration.CATEGORY_GENERAL).get("opt-out").set("true");
            configuration.save();
        }
        FMLCommonHandler.instance().bus().unregister(this);
    }

    /**
     * Gets the File object of the config file that should be used to store data
     * such as the GUID and opt-out status
     *
     * @return the File object for the config file
     */
    public File getConfigFile() {
        return new File(Loader.instance().getConfigDir(), "PluginMetrics.cfg");
    }

    /**
     * Generic method that posts a plugin to the metrics website
     */
    private void postPlugin(final boolean isPing) throws IOException {
        // Server software specific section
        String pluginName = modName;
        boolean onlineMode = MinecraftServer.getServer().isServerInOnlineMode();
        String pluginVersion = modVersion;
        String serverVersion;
        if (MinecraftServer.getServer().isDedicatedServer()) {
            serverVersion = "MinecraftForge (MC: " + MinecraftServer.getServer().getMinecraftVersion() + ")";
        } else {
            serverVersion = "MinecraftForgeSSP (MC: " + MinecraftServer.getServer().getMinecraftVersion() + ")";
        }
        int playersOnline = MinecraftServer.getServer().getCurrentPlayerCount();

        // END server software specific section -- all code below does not use any code outside of this class / Java

        // Construct the post data
        StringBuilder json = new StringBuilder(1024);
        json.append('{');

        // The plugin's description file containg all of the plugin data such as name, version, author, etc
        appendJSONPair(json, "guid", guid);
        appendJSONPair(json, "plugin_version", pluginVersion);
        appendJSONPair(json, "server_version", serverVersion);
        appendJSONPair(json, "players_online", Integer.toString(playersOnline));

        // New data as of R6
        String osname = System.getProperty("os.name");
        String osarch = System.getProperty("os.arch");
        String osversion = System.getProperty("os.version");
        String java_version = System.getProperty("java.version");
        int coreCount = Runtime.getRuntime().availableProcessors();

        // normalize os arch .. amd64 -> x86_64
        if (osarch.equals("amd64")) {
            osarch = "x86_64";
        }

        appendJSONPair(json, "osname", osname);
        appendJSONPair(json, "osarch", osarch);
        appendJSONPair(json, "osversion", osversion);
        appendJSONPair(json, "cores", Integer.toString(coreCount));
        appendJSONPair(json, "auth_mode", onlineMode ? "1" : "0");
        appendJSONPair(json, "java_version", java_version);

        // If we're pinging, append it
        if (isPing) {
            appendJSONPair(json, "ping", "1");
        }

        // close json
        json.append('}');

        // Create the url
        URL url = new URL(BASE_URL + String.format(REPORT_URL, urlEncode(pluginName)));

        // Connect to the website
        URLConnection connection;

        // Mineshafter creates a socks proxy, so we can safely bypass it
        // It does not reroute POST requests so we need to go around it
        if (isMineshafterPresent()) {
            connection = url.openConnection(Proxy.NO_PROXY);
        } else {
            connection = url.openConnection();
        }


        byte[] uncompressed = json.toString().getBytes();
        byte[] compressed = gzip(json.toString());

        // Headers
        connection.addRequestProperty("User-Agent", "MCStats/" + REVISION);
        connection.addRequestProperty("Content-Type", "application/json");
        connection.addRequestProperty("Content-Encoding", "gzip");
        connection.addRequestProperty("Content-Length", Integer.toString(compressed.length));
        connection.addRequestProperty("Accept", "application/json");
        connection.addRequestProperty("Connection", "close");

        connection.setDoOutput(true);

        // Write the data
        OutputStream os = connection.getOutputStream();
        os.write(compressed);
        os.flush();

        // Now read the response
        final BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String response = reader.readLine();

        // close resources
        os.close();
        reader.close();

        if (response == null || response.startsWith("ERR") || response.startsWith("7")) {
            if (response == null) {
                response = "null";
            } else if (response.startsWith("7")) {
                response = response.substring(response.startsWith("7,") ? 2 : 1);
            }

            throw new IOException(response);
        }
    }

    /**
     * GZip compress a string of bytes
     *
     * @param input
     * @return
     */
    public static byte[] gzip(String input) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzos = null;

        try {
            gzos = new GZIPOutputStream(baos);
            gzos.write(input.getBytes("UTF-8"));
        } catch (IOException e) {
            MainUtil.handleError(e);
        } finally {
            if (gzos != null) try {
                gzos.close();
            } catch (IOException ignore) {
            }
        }

        return baos.toByteArray();
    }

    /**
     * Check if mineshafter is present. If it is, we need to bypass it to send POST requests
     *
     * @return true if mineshafter is installed on the server
     */
    private boolean isMineshafterPresent() {
        try {
            Class.forName("mineshafter.MineServer");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Appends a json encoded key/value pair to the given string builder.
     *
     * @param json
     * @param key
     * @param value
     * @throws java.io.UnsupportedEncodingException
     */
    private static void appendJSONPair(StringBuilder json, String key, String value) throws UnsupportedEncodingException {
        boolean isValueNumeric = false;

        try {
            if (value.equals("0") || !value.endsWith("0")) {
                Double.parseDouble(value);
                isValueNumeric = true;
            }
        } catch (NumberFormatException e) {
            isValueNumeric = false;
        }

        if (json.charAt(json.length() - 1) != '{') {
            json.append(',');
        }

        json.append(escapeJSON(key));
        json.append(':');

        if (isValueNumeric) {
            json.append(value);
        } else {
            json.append(escapeJSON(value));
        }
    }

    /**
     * Escape a string to create a valid JSON string
     *
     * @param text
     * @return
     */
    private static String escapeJSON(String text) {
        StringBuilder builder = new StringBuilder();

        builder.append('"');
        for (int index = 0; index < text.length(); index++) {
            char chr = text.charAt(index);

            switch (chr) {
                case '"':
                case '\\':
                    builder.append('\\');
                    builder.append(chr);
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                default:
                    if (chr < ' ') {
                        String t = "000" + Integer.toHexString(chr);
                        builder.append("\\u" + t.substring(t.length() - 4));
                    } else {
                        builder.append(chr);
                    }
                    break;
            }
        }
        builder.append('"');

        return builder.toString();
    }

    /**
     * Encode text as UTF-8
     *
     * @param text the text to encode
     * @return the encoded text, as UTF-8
     */
    private static String urlEncode(final String text) throws UnsupportedEncodingException {
        return URLEncoder.encode(text, "UTF-8");
    }

}