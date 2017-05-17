/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.boydti.fawe.nukkit.core;

import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.AbstractPlatform;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.MultiUserPlatform;
import com.sk89q.worldedit.extension.platform.Preference;
import com.sk89q.worldedit.util.command.CommandMapping;
import com.sk89q.worldedit.util.command.Description;
import com.sk89q.worldedit.util.command.Dispatcher;
import com.sk89q.worldedit.world.World;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

public class NukkitPlatform extends AbstractPlatform implements MultiUserPlatform {

    private final NukkitWorldEdit mod;
    private boolean hookingEvents = false;
    private NukkitCommandManager commandManager;

    public NukkitPlatform(NukkitWorldEdit mod) {
        this.mod = mod;
        this.commandManager = new NukkitCommandManager(mod.getServer().getCommandMap());
    }

    boolean isHookingEvents() {
        return hookingEvents;
    }

    @Override
    public int resolveItem(String name) {
        Item item = Item.fromString(name);
        return item == null ? 0 : item.getId();
    }

    public NukkitWorldEdit getMod() {
        return mod;
    }

    @Override
    public boolean isValidMobType(String type) {
        return true;
    }

    @Override
    public void reload() {
        getConfiguration().load();
    }

    @Override
    public int schedule(long delay, long period, Runnable task) {
        TaskManager.IMP.repeat(task, (int) period);
        return 0; // TODO This isn't right, but we only check for -1 values
    }

    @Override
    public List<? extends com.sk89q.worldedit.world.World> getWorlds() {
        Collection<Level> levels = mod.getServer().getLevels().values();
        List<com.sk89q.worldedit.world.World> ret = new ArrayList<>(levels.size());
        for (Level level : levels) {
            ret.add(new NukkitWorld(level));
        }
        return ret;
    }

    @Nullable
    @Override
    public Player matchPlayer(Player player) {
        if (player instanceof NukkitPlayer) {
            return player;
        } else {
            cn.nukkit.Player currentPlayer = mod.getServer().getPlayer(player.getName());
            return currentPlayer != null ? new NukkitPlayer(this, currentPlayer) : null;
        }
    }

    @Nullable
    @Override
    public World matchWorld(World world) {
        if (world instanceof NukkitWorld) {
            return world;
        } else {
            Level level = NukkitWorldEdit.inst().getServer().getLevelByName(world.getName());
            return level != null ? new NukkitWorld(level) : null;
        }
    }

    @Override
    public void registerCommands(Dispatcher dispatcher) {
        for (CommandMapping command : dispatcher.getCommands()) {
            Description description = command.getDescription();
            List<String> permissions = description.getPermissions();
            String[] permissionsArray = new String[permissions.size()];
            permissions.toArray(permissionsArray);
            commandManager.register(new CommandInfo(description.getUsage(), description.getDescription(), command.getAllAliases(), permissionsArray), mod, mod);
        }
    }

    @Override
    public void registerGameHooks() {
        // We registered the events already anyway, so we just 'turn them on'
        hookingEvents = true;
    }

    @Override
    public NukkitConfiguration getConfiguration() {
        return mod.getWEConfig();
    }

    @Override
    public String getVersion() {
        return mod.getInternalVersion();
    }

    @Override
    public String getPlatformName() {
        return "Nukkit-Official";
    }

    @Override
    public String getPlatformVersion() {
        return mod.getInternalVersion();
    }

    @Override
    public Map<Capability, Preference> getCapabilities() {
        Map<Capability, Preference> capabilities = new EnumMap<>(Capability.class);
        capabilities.put(Capability.CONFIGURATION, Preference.NORMAL);
        capabilities.put(Capability.WORLDEDIT_CUI, Preference.NORMAL);
        capabilities.put(Capability.GAME_HOOKS, Preference.NORMAL);
        capabilities.put(Capability.PERMISSIONS, Preference.NORMAL);
        capabilities.put(Capability.USER_COMMANDS, Preference.NORMAL);
        capabilities.put(Capability.WORLD_EDITING, Preference.PREFERRED);
        return capabilities;
    }

    @Override
    public Collection<Actor> getConnectedUsers() {
        List<Actor> users = new ArrayList<Actor>();
        for (Map.Entry<UUID, cn.nukkit.Player> entry : mod.getServer().getOnlinePlayers().entrySet()) {
            users.add(new NukkitPlayer(this, entry.getValue()));
        }
        return users;
    }
}