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
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Logger;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.platform.PlatformReadyEvent;
import com.sk89q.worldedit.extension.platform.Platform;
import java.io.File;


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
        // TODO load FAWE
        config.load();
        this.platform = new NukkitPlatform(this);
        WorldEdit.getInstance().getPlatformManager().register(platform);
        logger.info("WorldEdit for Nukkit (version " + getInternalVersion() + ") is loaded");
        WorldEdit.getInstance().getEventBus().post(new PlatformReadyEvent());
    }

    @Override
    public void onDisable() {
        WorldEdit.getInstance().getPlatformManager().unregister(platform);
    }

    /**
     * Get the configuration.
     *
     * @return the Nukkit configuration
     */
    public NukkitConfiguration getWEConfig() {
        return this.config;
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
    public Platform getPlatform() {
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