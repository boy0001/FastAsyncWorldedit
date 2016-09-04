package com.boydti.fawe.nukkit.core;

import cn.nukkit.command.Command;
import cn.nukkit.command.CommandExecutor;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginIdentifiableCommand;
import cn.nukkit.plugin.Plugin;
import com.sk89q.util.StringUtil;

/**
 * An implementation of a dynamically registered {@link org.bukkit.command.Command} attached to a plugin
 */
public class DynamicPluginCommand extends Command implements PluginIdentifiableCommand {

    protected final CommandExecutor owner;
    private final Plugin plugin;
    protected String[] permissions = new String[0];

    public DynamicPluginCommand(String[] aliases, String desc, String usage, CommandExecutor owner, Plugin plugin) {
        super(aliases[0], desc, usage, aliases);
        this.owner = owner;
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        return owner.onCommand(sender, this, label, args);
    }

    public Object getOwner() {
        return owner;
    }

    public void setPermissions(String[] permissions) {
        this.permissions = permissions;
        if (permissions != null) {
            super.setPermission(StringUtil.joinString(permissions, ";"));
        }
    }

    public String[] getPermissions() {
        return permissions;
    }

    @Override
    public boolean testPermissionSilent(CommandSender sender) {
        return super.testPermissionSilent(sender);
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }
}