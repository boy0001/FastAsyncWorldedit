package com.boydti.fawe.forge;


import com.boydti.fawe.Fawe;
import com.boydti.fawe.IFawe;
import com.boydti.fawe.forge.v0.ForgeQueue_All;
import com.boydti.fawe.object.EditSessionWrapper;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.wrappers.WorldWrapper;
import com.mojang.authlib.GameProfile;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.forge.ForgeWorld;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import javax.management.InstanceAlreadyExistsException;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Logger;

public class FaweForge implements IFawe {

    private final ForgeMain parent;
    private final File directory;
    private final Logger logger;

    public FaweForge(ForgeMain plugin, Logger logger, File directory) {
        this.parent = plugin;
        this.logger = logger;
        this.directory = directory;
        try {
            Fawe.set(this);
        } catch (InstanceAlreadyExistsException e) {
            MainUtil.handleError(e);
        }
    }

    @Override
    public void debug(String s) {
        logger.debug(s);
    }

    @Override
    public File getDirectory() {
        return directory;
    }

    @Override
    public void setupCommand(String label, FaweCommand cmd) {
        if (TaskManager.IMP != null) {
            TaskManager.IMP.task(new Runnable() {
                @Override
                public void run() {
                    ServerCommandManager scm = (ServerCommandManager) MinecraftServer.getServer().getCommandManager();
                    scm.registerCommand(new ForgeCommand(label, cmd));
                }
            });
        }
    }

    @Override
    public FawePlayer wrap(Object obj) {
        EntityPlayerMP player = null;
        if (obj instanceof String) {
            MinecraftServer server = MinecraftServer.getServer();
            player = server.getConfigurationManager().getPlayerByUsername((String) obj);
        } else if (obj instanceof UUID) {
            MinecraftServer server = MinecraftServer.getServer();
            player = server.getConfigurationManager().getPlayerByUUID((UUID) obj);
        } else if (obj instanceof EntityPlayerMP) {
            player = (EntityPlayerMP) obj;
        }
        if (player == null) {
            return null;
        }
        FawePlayer existing = Fawe.get().getCachedPlayer(player.getName());
        return existing != null ? existing : new ForgePlayer(player);
    }

    @Override
    public void setupVault() {
        // Do nothing
    }

    @Override
    public TaskManager getTaskManager() {
        return new com.boydti.fawe.forge.ForgeTaskMan(512);
    }

    @Override
    public int[] getVersion() {
        String[] version = MinecraftServer.getServer().getMinecraftVersion().split("\\.");
        return new int[] {Integer.parseInt(version[0]), Integer.parseInt(version[1]), Integer.parseInt(version[2])};
    }

    @Override
    public String getWorldName(World world) {
        if (world instanceof WorldWrapper) {
            world = ((WorldWrapper) world).getParent();
        }
        return ((ForgeWorld) world).getWorld().provider.getDimensionName();
    }

    @Override
    public FaweQueue getNewQueue(String world, boolean dontCareIfFast) {
        return new ForgeQueue_All(world);
    }

    @Override
    public EditSessionWrapper getEditSessionWrapper(EditSession session) {
        return new EditSessionWrapper(session);
    }

    @Override
    public Collection<FaweMaskManager> getMaskManagers() {
        return new ArrayList<>();
    }

    @Override
    public void startMetrics() {
        try {
            com.boydti.fawe.forge.ForgeMetrics metrics = new com.boydti.fawe.forge.ForgeMetrics("FastAsyncWorldEdit", "3.5.1");
            metrics.start();
            debug("[FAWE] &6Metrics enabled.");
        } catch (Throwable e) {
            debug("[FAWE] &cFailed to load up metrics.");
        }
    }

    @Override
    public String getPlatform() {
        return "forge";
    }

    @Override
    public UUID getUUID(String name) {
        try {
            GameProfile profile = MinecraftServer.getServer().getPlayerProfileCache().getGameProfileForUsername(name);
            return profile.getId();
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public String getName(UUID uuid) {
        try {
            GameProfile profile = MinecraftServer.getServer().getPlayerProfileCache().getProfileByUUID(uuid);
            return profile.getName();
        } catch (Throwable e) {
            return null;
        }
    }
}
