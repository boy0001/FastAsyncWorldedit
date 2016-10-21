package com.boydti.fawe.command;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweVersion;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.HastebinUtility;
import com.boydti.fawe.util.MainUtil;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

public class Reload extends FaweCommand {

    public Reload() {
        super("fawe.reload");
    }

    @Override
    public boolean execute(final FawePlayer player, final String... args) {
        if (args.length != 1) {
            BBC.COMMAND_SYNTAX.send(player, "/fawe [reload|version|debugpaste|threads]");
            return false;
        }
        switch (args[0].toLowerCase()) {
            case "version": {
                FaweVersion version = Fawe.get().getVersion();
                if (version == null) {
                    MainUtil.sendMessage(player, "No version information available.");
                    return false;
                }
                MainUtil.sendMessage(player, "Version Date: " + new Date(100 + version.year, version.month, version.day).toLocaleString());
                MainUtil.sendMessage(player, "Version Commit: " + Integer.toHexString(version.hash));
                MainUtil.sendMessage(player, "Version Build: #" + version.build);
                return true;
            }
            case "threads": {
                Map<Thread, StackTraceElement[]> stacks = Fawe.get().getMainThread().getAllStackTraces();
                for (Map.Entry<Thread, StackTraceElement[]> entry : stacks.entrySet()) {
                    Thread thread = entry.getKey();
                    Fawe.debug("--------------------------------------------------------------------------------------------");
                    Fawe.debug("Thread: " + thread.getName() + " | Id: " + thread.getId() + " | Alive: " + thread.isAlive());
                    for (StackTraceElement elem : entry.getValue()) {
                        Fawe.debug(elem);
                    }
                }
                if (player != null) {
                    player.sendMessage("&cSee console.");
                }
                return true;
            }
            case "debugpaste":
            case "paste": {
                try {
                    String settingsYML = HastebinUtility.upload(new File(Fawe.imp().getDirectory(), "config.yml"));
                    String messagesYML = HastebinUtility.upload(new File(Fawe.imp().getDirectory(), "message.yml"));
                    String commandsYML = HastebinUtility.upload(new File(Fawe.imp().getDirectory(), "commands.yml"));
                    String latestLOG;
                    try {
                        latestLOG = HastebinUtility.upload(new File(Fawe.imp().getDirectory(), "../../logs/latest.log"));
                    } catch (IOException ignored) {
                        MainUtil.sendMessage(player, "&clatest.log is too big to be pasted, will ignore");
                        latestLOG = "too big :(";
                    }
                    StringBuilder b = new StringBuilder();
                    b.append(
                            "# Welcome to this paste\n# It is meant to provide us at IntellectualSites with better information about your "
                                    + "problem\n\n# We will start with some informational files\n");
                    b.append("links.config_yml: ").append(settingsYML).append('\n');
                    b.append("links.messages_yml: ").append(messagesYML).append('\n');
                    b.append("links.commands_yml: ").append(commandsYML).append('\n');
                    b.append("links.latest_log: ").append(latestLOG).append('\n');
                    b.append("\n# Server Information\n");
                    b.append("version.server: ").append(Fawe.imp().getPlatform()).append('\n');
                    b.append("\n\n# YAY! Now, let's see what we can find in your JVM\n");
                    Runtime runtime = Runtime.getRuntime();
                    b.append("memory.free: ").append(runtime.freeMemory()).append('\n');
                    b.append("memory.max: ").append(runtime.maxMemory()).append('\n');
                    b.append("java.specification.version: '").append(System.getProperty("java.specification.version")).append("'\n");
                    b.append("java.vendor: '").append(System.getProperty("java.vendor")).append("'\n");
                    b.append("java.version: '").append(System.getProperty("java.version")).append("'\n");
                    b.append("os.arch: '").append(System.getProperty("os.arch")).append("'\n");
                    b.append("os.name: '").append(System.getProperty("os.name")).append("'\n");
                    b.append("os.version: '").append(System.getProperty("os.version")).append("'\n\n");
                    b.append("# Okay :D Great. You are now ready to create your bug report!");
                    b.append("\n# You can do so at https://github.com/boy0001/FastAsyncWorldedit/issues");

                    String link = HastebinUtility.upload(b.toString());
                    BBC.DOWNLOAD_LINK.send(player, link);
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            case "reload": {
                Fawe.get().setupConfigs();
                MainUtil.sendMessage(player, "Reloaded (" + Fawe.get().getVersion() + ").");
                return true;
            }
            default:
                BBC.COMMAND_SYNTAX.send(player, "/fawe [reload|version|debugpaste|threads]");
                return false;
        }
    }
}
