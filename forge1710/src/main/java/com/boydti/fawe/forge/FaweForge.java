package com.boydti.fawe.forge;


import com.boydti.fawe.Fawe;
import com.boydti.fawe.IFawe;
import com.boydti.fawe.forge.v0.ForgeQueue_All;
import com.boydti.fawe.object.EditSessionWrapper;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.wrappers.WorldWrapper;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.forge.ForgeWorld;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
            e.printStackTrace();
        }
    }

    @Override
    public void debug(String s) {
        logger.error(s);
    }

    @Override
    public File getDirectory() {
        return directory;
    }

    @Override
    public void setupCommand(final String label, final FaweCommand cmd) {
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
            List<EntityPlayerMP> list = server.getConfigurationManager().getPlayerList((String) obj);
            player = list.size() == 1 ? list.get(0) : null;
        } else if (obj instanceof EntityPlayerMP) {
            player = (EntityPlayerMP) obj;
        }
        if (player == null) {
            return null;
        }
        FawePlayer existing = Fawe.get().getCachedPlayer(player.getCommandSenderName());
        return existing != null ? existing : new ForgePlayer(player);
    }

    @Override
    public void setupWEListener() {
        // Do nothing
    }

    @Override
    public void setupVault() {
        // Do nothing
    }

    @Override
    public TaskManager getTaskManager() {
        return new ForgeTaskMan(512);
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
    public FaweQueue getNewQueue(String world) {
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
            ForgeMetrics metrics = new ForgeMetrics("FastAsyncWorldEdit", "3.4.2");
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
            return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public String getName(UUID uuid) {
        return uuid.toString();
    }
}
