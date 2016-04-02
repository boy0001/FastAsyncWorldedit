package com.boydti.fawe.forge;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.IFawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.EditSessionWrapper;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.TaskManager;
import com.google.inject.Inject;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.config.Settings;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.forge.ForgeWorldEdit;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import org.mcstats.Metrics;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameAboutToStartServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.profile.GameProfileManager;
import org.spongepowered.api.text.serializer.TextSerializers;

@Plugin(id = "com.boydti.fawe", name = "FastAsyncWorldEdit", description = "Lagless WorldEdit, Area restrictions, Memory mangement, Block logging", url = "https://github.com/boy0001/FastAsyncWorldedit", version = "3.3.4")
public class FaweSponge implements IFawe {
    public PluginContainer plugin;
    public FaweSponge instance;

    @Inject
    private Logger logger;
    @Inject
    private Game game;
    private Server server;

    private GameProfileManager resolver;

    private ForgeWorldEdit worldedit;

    public ForgeWorldEdit getWorldEditPlugin() {
        if (this.worldedit == null) {
            this.worldedit = ForgeWorldEdit.inst;
        }
        return this.worldedit;
    }

    public Game getGame() {
        return this.game;
    }

    public Server getServer() {
        return this.server;
    }

    public GameProfileManager getResolver() {
        if (this.resolver == null) {
            this.resolver = this.game.getServer().getGameProfileManager();
        }
        return this.resolver;
    }

    @Listener
    public void onServerAboutToStart(GameAboutToStartServerEvent event) {
        debug("FAWE: Server init");
        instance = this;
        plugin = this.game.getPluginManager().fromInstance(this).get();
        this.server = this.game.getServer();
        try {
            Fawe.set(this);
        } catch (final Throwable e) {
            e.printStackTrace();
            this.getServer().shutdown();
        }
    }

    @Override
    public void debug(String message) {
        message = C.format(message, C.replacements);
        if (!Settings.CONSOLE_COLOR) {
            message = message.replaceAll('\u00a7' + "[a-z|0-9]", "");
        }
        if (this.server == null) {
            this.logger.info(message);
            return;
        }
        this.server.getConsole().sendMessage(TextSerializers.LEGACY_FORMATTING_CODE.deserialize(BBC.color(message)));
    }

    @Override
    public File getDirectory() {
        return new File("mods/FastAsyncWorldEdit");
    }

    @Override
    public void setupCommand(String label, FaweCommand cmd) {

    }

    @Override
    public FawePlayer wrap(Object obj) {
        return null;
    }

    @Override
    public void setupWEListener() {

    }

    @Override
    public void setupVault() {

    }

    @Override
    public TaskManager getTaskManager() {
        return null;
    }

    @Override
    public int[] getVersion() {
        return new int[0];
    }

    @Override
    public FaweQueue getQueue() {
        return null;
    }

    @Override
    public EditSessionWrapper getEditSessionWrapper(EditSession session) {
        return null;
    }

    @Override
    public Collection<FaweMaskManager> getMaskManagers() {
        return null;
    }

    @Override
    public void startMetrics() {
        try {
            Metrics metrics = new Metrics(this.game, this.plugin);
            metrics.start();
            debug(C.PREFIX.s() + "&6Metrics enabled.");
        } catch (IOException e) {
            debug(C.PREFIX.s() + "&cFailed to load up metrics.");
        }
    }
}
