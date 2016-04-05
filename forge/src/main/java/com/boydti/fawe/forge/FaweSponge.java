package com.boydti.fawe.forge;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.IFawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.forge.v0.SpongeEditSessionWrapper_0;
import com.boydti.fawe.forge.v1_8.SpongeQueue_1_8;
import com.boydti.fawe.object.EditSessionWrapper;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.forge.ForgeWorldEdit;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.serializer.TextSerializers;


import static org.spongepowered.api.Sponge.getGame;

public class FaweSponge implements IFawe {

    public final SpongeMain plugin;

    public FaweSponge instance;

    private ForgeWorldEdit worldedit;

    public ForgeWorldEdit getWorldEditPlugin() {
        if (this.worldedit == null) {
            this.worldedit = ForgeWorldEdit.inst;
        }
        return this.worldedit;
    }

    public FaweSponge(SpongeMain plugin) {
        instance = this;
        this.plugin = plugin;
        try {
            Fawe.set(this);
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        TaskManager.IMP.later(() -> SpongeUtil.initBiomeCache(), 0);
    }

    @Override
    public void debug(String message) {
        message = BBC.color(message);
        Sponge.getServer().getConsole().sendMessage(TextSerializers.LEGACY_FORMATTING_CODE.deserialize(BBC.color(message)));
    }

    @Override
    public File getDirectory() {
        return new File("config/FastAsyncWorldEdit");
    }

    @Override
    public void setupCommand(String label, FaweCommand cmd) {
        getGame().getCommandManager().register(plugin, new SpongeCommand(cmd), label);
    }

    @Override
    public FawePlayer wrap(Object obj) {
        if (obj.getClass() == String.class) {
            String name = (String) obj;
            FawePlayer existing = Fawe.get().getCachedPlayer(name);
            if (existing != null) {
                return existing;
            }
            Player player = Sponge.getServer().getPlayer(name).orElseGet(null);
            return player != null ? new SpongePlayer(player) : null;
        } else if (obj instanceof Player) {
            Player player = (Player) obj;
            FawePlayer existing = Fawe.get().getCachedPlayer(player.getName());
            return existing != null ? existing : new SpongePlayer(player);
        } else {
            return null;
        }
    }

    @Override
    public void setupWEListener() {
        // Do nothing
    }

    @Override
    public void setupVault() {
        debug("[FAWE] Permission hook not implemented yet!");
    }

    @Override
    public TaskManager getTaskManager() {
        return new SpongeTaskMan(plugin);
    }

    @Override
    public int[] getVersion() {
        debug("[FAWE] Checking minecraft version: Sponge: ");
        String version = Sponge.getGame().getPlatform().getMinecraftVersion().getName();
        String[] split = version.split("\\.");
        return new int[]{Integer.parseInt(split[0]), Integer.parseInt(split[1]), split.length == 3 ? Integer.parseInt(split[2]) : 0};
    }

    @Override
    public FaweQueue getNewQueue(String world) {
        return new SpongeQueue_1_8(world);
    }

    @Override
    public EditSessionWrapper getEditSessionWrapper(EditSession session) {
        return new SpongeEditSessionWrapper_0(session);
    }

    @Override
    public Collection<FaweMaskManager> getMaskManagers() {
        return new ArrayList<>();
    }

    @Override
    public void startMetrics() {
        try {
            SpongeMetrics metrics = new SpongeMetrics(Sponge.getGame(), Sponge.getPluginManager().fromInstance(plugin).get());
            metrics.start();
            debug("[FAWE] &6Metrics enabled.");
        } catch (IOException e) {
            debug("[FAWE] &cFailed to load up metrics.");
        }
    }

    @Override
    public Set<FawePlayer> getPlayers() {
        HashSet<FawePlayer> players = new HashSet<>();
        for (Player player : Sponge.getServer().getOnlinePlayers()) {
            players.add(wrap(player));
        }
        return players;
    }
}
