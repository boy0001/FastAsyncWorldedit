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

import cn.nukkit.Nukkit;
import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.PluginBase;
import com.boydti.fawe.Fawe;
import com.boydti.fawe.nukkit.optimization.FaweNukkit;
import com.google.common.base.Joiner;
import com.sk89q.util.yaml.YAMLProcessor;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.event.platform.PlatformReadyEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import java.io.File;
import java.util.Arrays;
import java.util.logging.Logger;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The Nukkit implementation of WorldEdit.
 */
public class NukkitWorldEdit extends PluginBase {

    private Logger logger;

    private static NukkitWorldEdit inst;

    public static NukkitWorldEdit inst() {
        return inst;
    }

    private NukkitPlatform platform;

    private NukkitConfiguration config;

    private File workingDir;

    public NukkitWorldEdit() {
        inst = this;
    }

    @Override
    public void onEnable() {
        try {
            logger = Logger.getLogger(NukkitWorldEdit.class.getCanonicalName());
            File file = new File(getDataFolder(), "config-basic.yml");
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            config = new NukkitConfiguration(new YAMLProcessor(file, true), this);
            config.load();
            this.platform = new NukkitPlatform(this);
            getServer().getPluginManager().registerEvents(new WorldEditListener(this), this);
            WorldEdit.getInstance().getPlatformManager().register(platform);
            Fawe.set(new FaweNukkit(this));
            logger.info("WorldEdit for Nukkit (version " + getInternalVersion() + ") is loaded");
            WorldEdit.getInstance().getEventBus().post(new PlatformReadyEvent());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        WorldEdit.getInstance().getPlatformManager().unregister(platform);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        // Add the command to the array because the underlying command handling
        // code of WorldEdit expects it
        String[] split = new String[args.length + 1];
        System.arraycopy(args, 0, split, 1, args.length);
        split[0] = cmd.getName();

        CommandEvent event = new CommandEvent(wrapCommandSender(sender), Joiner.on(" ").join(Arrays.asList(split)));
        WorldEdit.getInstance().getEventBus().post(event);

        return true;
    }

    public Actor wrapCommandSender(CommandSender sender) {
        if (sender instanceof Player) {
            return wrapPlayer((Player) sender);
        }

        return new NukkitCommandSender(this, sender);
    }

    /**
     * Get the configuration.
     *
     * @return the Nukkit configuration
     */
    public NukkitConfiguration getWEConfig() {
        return this.config;
    }

    public Logger getWELogger() {
        return logger;
    }

    /**
     * Get the WorldEdit proxy for the given player.
     *
     * @param player the player
     * @return the WorldEdit player
     */
    public NukkitPlayer wrapPlayer(Player player) {
        checkNotNull(player);
        return new NukkitPlayer(platform, player);
    }

    /**
     * Get the session for a player.
     *
     * @param player the player
     * @return the session
     */
    public LocalSession getSession(Player player) {
        checkNotNull(player);
        return WorldEdit.getInstance().getSessionManager().get(wrapPlayer(player));
    }

    /**
     * Get the WorldEdit proxy for the platform.
     *
     * @return the WorldEdit platform
     */
    public NukkitPlatform getPlatform() {
        return this.platform;
    }

    /**
     * Get the working directory where WorldEdit's files are stored.
     *
     * @return the working directory
     */
    public File getWorkingDir() {
        return this.workingDir;
    }

    /**
     * Get the version of the WorldEdit Nukkit implementation.
     *
     * @return a version string
     */
    public String getInternalVersion() {
        return Nukkit.API_VERSION;
    }
}