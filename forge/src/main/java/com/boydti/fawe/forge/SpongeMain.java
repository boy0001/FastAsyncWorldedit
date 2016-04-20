package com.boydti.fawe.forge;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FawePlayer;
import com.google.inject.Inject;
import com.sk89q.worldedit.WorldEdit;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Server;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.profile.GameProfileManager;
import org.spongepowered.api.world.World;

@Plugin(id = "com.boydti.fawe", name = "FastAsyncWorldEdit", description = "Lagless WorldEdit, Area restrictions, Memory mangement, Block logging", url = "https://github.com/boy0001/FastAsyncWorldedit", version = "3.3.18")
public class SpongeMain {
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


    @Listener
    public void onGamePreInit(GamePreInitializationEvent event) {
        plugin = this.game.getPluginManager().fromInstance(this).get();
        this.server = this.game.getServer();
        new FaweSponge(this);
    }

    @Listener
    public void onQuit(ClientConnectionEvent.Disconnect event) {
        Player player = event.getTargetEntity();
        FawePlayer fp = FawePlayer.wrap(player);
        fp.unregister();
        Fawe.get().unregister(player.getName());
    }

    @Listener
    public void onMove(DisplaceEntityEvent.TargetPlayer event) {
        if (Settings.STORE_HISTORY_ON_DISK) {
            World from = event.getFromTransform().getExtent();
            World to = event.getToTransform().getExtent();
            if (!from.equals(to)) {
                Player player = event.getTargetEntity();
                FawePlayer fp = FawePlayer.wrap(player);
                for (com.sk89q.worldedit.world.World world : WorldEdit.getInstance().getServer().getWorlds()) {
                    if (world.getName().equals(to.getName())) {
                        fp.getSession().clearHistory();
                        fp.loadSessionFromDisk(world);
                        return;
                    }
                }
            }
        }
    }
}
