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

package com.sk89q.worldedit.command;

import com.boydti.fawe.wrappers.LocationMaskedPlayerWrapper;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.Logging;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.scripting.RhinoCraftScriptEngine;
import java.io.File;


import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.minecraft.util.commands.Logging.LogMode.ALL;

/**
 * Commands related to scripting.
 */
@Command(aliases = {}, desc = "Run craftscripts: [More Info](https://goo.gl/dHDxLG)")
public class ScriptingCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public ScriptingCommands(final WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(aliases = {"cs"}, usage = "<filename> [args...]", desc = "Execute a CraftScript", min = 1, max = -1)
    @CommandPermissions("worldedit.scripting.execute")
    @Logging(ALL)
    public void execute(final Player player, final LocalSession session, final CommandContext args) throws WorldEditException {
        final String[] scriptArgs = args.getSlice(1);
        final String name = args.getString(0);

        if (!player.hasPermission("worldedit.scripting.execute." + name)) {
            player.printError("You don't have permission to use that script.");
            return;
        }

        session.setLastScript(name);

        final File dir = this.worldEdit.getWorkingDirectoryFile(this.worldEdit.getConfiguration().scriptsDir);
        final File f = this.worldEdit.getSafeOpenFile(player, dir, name, "js", "js");
        try {
            new RhinoCraftScriptEngine();
        } catch (NoClassDefFoundError e) {
            player.printError("Failed to find an installed script engine.");
            player.printError("Download: https://github.com/downloads/mozilla/rhino/rhino1_7R4.zip");
            player.printError("Extract: `js.jar` to `plugins` or `mods` directory`");
            player.printError("More info: https://github.com/boy0001/CraftScripts/");
            return;
        }
        this.worldEdit.runScript(LocationMaskedPlayerWrapper.unwrap(player), f, scriptArgs);
    }

    @Command(aliases = {".s"}, usage = "[args...]", desc = "Execute last CraftScript", min = 0, max = -1)
    @CommandPermissions("worldedit.scripting.execute")
    @Logging(ALL)
    public void executeLast(final Player player, final LocalSession session, final CommandContext args) throws WorldEditException {
        final String lastScript = session.getLastScript();

        if (!player.hasPermission("worldedit.scripting.execute." + lastScript)) {
            player.printError("You don't have permission to use that script.");
            return;
        }

        if (lastScript == null) {
            player.printError("Use /cs with a script name first.");
            return;
        }

        final String[] scriptArgs = args.getSlice(0);

        final File dir = this.worldEdit.getWorkingDirectoryFile(this.worldEdit.getConfiguration().scriptsDir);
        final File f = this.worldEdit.getSafeOpenFile(player, dir, lastScript, "js", "js");

        try {
            this.worldEdit.runScript(LocationMaskedPlayerWrapper.unwrap(player), f, scriptArgs);
        } catch (final WorldEditException ex) {
            player.printError("Error while executing CraftScript.");
        }
    }

    public static Class<?> inject() {
        return ScriptingCommands.class;
    }
}
