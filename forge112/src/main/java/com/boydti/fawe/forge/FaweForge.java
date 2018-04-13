package com.boydti.fawe.forge;


import com.boydti.fawe.Fawe;
import com.boydti.fawe.IFawe;
import com.boydti.fawe.forge.v112.ForgeQueue_All;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.regions.FaweMaskManager;
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
import java.util.HashMap;
import java.util.UUID;
import javax.management.InstanceAlreadyExistsException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.ModMetadata;
import org.apache.logging.log4j.Logger;

public class FaweForge implements IFawe {

    private final ForgeMain parent;
    private final File directory;
    private final Logger logger;
    private final ModMetadata mod;

    public FaweForge(ForgeMain plugin, Logger logger, ModMetadata mod, File directory) {
        this.parent = plugin;
        this.logger = logger;
        this.directory = directory;
        this.mod = mod;
        try {
            Fawe.set(this);
            setupInjector();
        } catch (InstanceAlreadyExistsException e) {
            MainUtil.handleError(e);
        }
    }

    public void setupInjector() {
        try {
            Fawe.setupInjector();
            com.sk89q.worldedit.forge.ForgePlayer.inject();
        } catch (Throwable e) {
            Fawe.debug("Failed to inject WorldEdit classes.");
        }
    }

    @Override
    public void debug(String s) {
        logger.debug(s);
    }

    @Override
    public boolean isOnlineMode() {
        return FMLCommonHandler.instance().getMinecraftServerInstance().isServerInOnlineMode();
    }

    @Override
    public String getPlatformVersion() {
        return "1.12";
    }

    @Override
    public int getPlayerCount() {
        return FMLCommonHandler.instance().getMinecraftServerInstance().getCurrentPlayerCount();
    }

    @Override
    public File getDirectory() {
        return directory;
    }

    private HashMap<String, FaweCommand> commands = new HashMap<>();

    public HashMap<String, FaweCommand> getCommands() {
        return commands;
    }

    @Override
    public void setupCommand(String label, FaweCommand cmd) {
        this.commands.put(label, cmd);
    }

    @Override
    public FawePlayer wrap(Object obj) {
        EntityPlayerMP player = null;
        if (obj instanceof String) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            player = server.getPlayerList().getPlayerByUsername((String) obj);
        } else if (obj instanceof UUID) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            player = server.getPlayerList().getPlayerByUUID((UUID) obj);
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
    public String getWorldName(World world) {
        if (world instanceof WorldWrapper) {
            return getWorldName(((WorldWrapper) world).getParent());
        }
        else if (world instanceof EditSession) {
            return getWorldName(((EditSession) world).getWorld());
        }
        return getWorldName(((ForgeWorld) world).getWorld());

    }

    public String getWorldName(net.minecraft.world.World w) {
        return w.getWorldInfo().getWorldName() + ";" + w.provider.getDimension();
    }

    @Override
    public FaweQueue getNewQueue(World world, boolean dontCareIfFast) {
        return new ForgeQueue_All(world);
    }

    @Override
    public FaweQueue getNewQueue(String world, boolean dontCareIfFast) {
        return new ForgeQueue_All(world);
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
            GameProfile profile = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerProfileCache().getGameProfileForUsername(name);
            return profile.getId();
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public String getName(UUID uuid) {
        try {
            GameProfile profile = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerProfileCache().getProfileByUUID(uuid);
            return profile.getName();
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public Object getBlocksHubApi() {
        return null;
    }
}
