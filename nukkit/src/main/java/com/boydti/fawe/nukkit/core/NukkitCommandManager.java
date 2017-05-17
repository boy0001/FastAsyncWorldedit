package com.boydti.fawe.nukkit.core;

import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.SimpleCommandMap;
import cn.nukkit.plugin.Plugin;

public class NukkitCommandManager {
    private final SimpleCommandMap commandMap;

    public NukkitCommandManager(SimpleCommandMap map) {
        this.commandMap = map;
    }

    public boolean register(CommandInfo command, Plugin plugin, CommandExecutor executor) {
        if (command == null || commandMap == null) {
            return false;
        }
        DynamicPluginCommand cmd = new DynamicPluginCommand(
                command.getAliases(),
                command.getDesc(), "/" + command.getAliases()[0] + " " + command.getUsage(),
                executor,
                plugin);
        cmd.setPermissions(command.getPermissions());
        for (String alias : command.getAliases()) {
            commandMap.register(alias, cmd);
        }
        return true;
    }
}
