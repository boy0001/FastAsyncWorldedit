package com.boydti.fawe.nukkit.core;


public class CommandInfo {

    private final String[] aliases;
    private final String usage, desc;
    private final String[] permissions;

    public CommandInfo(String usage, String desc, String[] aliases) {
        this(usage, desc, aliases, null);
    }

    public CommandInfo(String usage, String desc, String[] aliases, String[] permissions) {
        this.usage = usage;
        this.desc = desc;
        this.aliases = aliases;
        this.permissions = permissions;
    }

    public String[] getAliases() {
        return aliases;
    }

    public String getName() {
        return aliases[0];
    }

    public String getUsage() {
        return usage;
    }

    public String getDesc() {
        return desc;
    }

    public String[] getPermissions() {
        return permissions;
    }

}