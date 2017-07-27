package com.boydti.fawe.sponge;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.IFawe;
import com.boydti.fawe.SpongeCommand;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.GameProfileManager;
import org.spongepowered.api.text.serializer.TextSerializers;


import static org.spongepowered.api.Sponge.getGame;

public class FaweSponge implements IFawe {

    public final SpongeMain plugin;

    public FaweSponge instance;

    public FaweSponge(SpongeMain plugin) {
        instance = this;
        this.plugin = plugin;
        try {
            Fawe.set(this);
            Fawe.setupInjector();
            com.sk89q.worldedit.sponge.SpongePlayer.inject();
        } catch (final Throwable e) {
            MainUtil.handleError(e);
        }
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
    public void setupVault() {
        debug("Permission hook not implemented yet!");
    }

    @Override
    public TaskManager getTaskManager() {
        return new SpongeTaskMan(plugin);
    }

    @Override
    public FaweQueue getNewQueue(World world, boolean fast) {
        return new com.boydti.fawe.sponge.v1_12.SpongeQueue_1_12(getWorldName(world));
    }

    @Override
    public FaweQueue getNewQueue(String world, boolean fast) {
        return new com.boydti.fawe.sponge.v1_12.SpongeQueue_1_12(world);
    }

    @Override
    public String getWorldName(World world) {
        return world.getName();
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
        } catch (Throwable e) {
            debug("[FAWE] &cFailed to load up metrics.");
        }
    }

    @Override
    public String getPlatform() {
        return "sponge";
    }

    @Override
    public UUID getUUID(String name) {
        try {
            GameProfileManager pm = Sponge.getServer().getGameProfileManager();
            GameProfile profile = pm.get(name).get();
            return profile != null ? profile.getUniqueId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getName(UUID uuid) {
        try {
            GameProfileManager pm = Sponge.getServer().getGameProfileManager();
            GameProfile profile = pm.get(uuid).get();
            return profile != null ? profile.getName().orElse(null) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Object getBlocksHubApi() {
        return null;
    }
}
