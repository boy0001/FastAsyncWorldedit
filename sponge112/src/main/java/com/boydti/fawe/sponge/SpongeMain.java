package com.boydti.fawe.sponge;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FawePlayer;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.profile.GameProfileManager;

@Plugin(id = "fastasyncworldedit", name = " FastAsyncWorldEdit", description = "fawe", url = "https://github.com/boy0001/FastAsyncWorldedit", version = "development", authors = "Empire92")
public class SpongeMain {
    @Inject
    public PluginContainer plugin;

    @Inject
    private Logger logger;

    @Inject
    private Game game;
    private Server server;

    private GameProfileManager resolver;

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

    @Listener(order = Order.PRE)
    public void onGamePreInit(GamePreInitializationEvent event) {
        this.server = this.game.getServer();
        new FaweSponge(this);
        Settings.IMP.QUEUE.PARALLEL_THREADS = 1;
    }

    @Listener
    public void onQuit(ClientConnectionEvent.Disconnect event) {
        Player player = event.getTargetEntity();
        FawePlayer fp = FawePlayer.wrap(player);
        fp.unregister();
        Fawe.get().unregister(player.getName());
    }
}
