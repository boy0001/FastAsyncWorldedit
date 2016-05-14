package com.boydti.fawe.forge;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FawePlayer;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import java.io.File;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import org.apache.logging.log4j.Logger;

@Mod(modid = "com.boydti.fawe", name = "FastAsyncWorldEdit", version = "3.5.0", acceptableRemoteVersions = "*")
public class ForgeMain {
    private static FaweForge IMP;
    private Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        this.logger = event.getModLog();
        File directory = new File(event.getModConfigurationDirectory() + File.separator + "FastAsyncWorldEdit");
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
        this.IMP = new FaweForge(this, event.getModLog(), directory);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player.worldObj.isRemote) {
            return;
        }
        handleQuit((EntityPlayerMP) event.player);
    }

    @Mod.EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        for (EntityPlayerMP player : (List<EntityPlayerMP>)MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            handleQuit(player);
        }
    }

    public void handleQuit(EntityPlayerMP player) {
        FawePlayer fp = FawePlayer.wrap(player);
        fp.unregister();
        Fawe.get().unregister(player.getCommandSenderName());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerChangedWorld(EntityJoinWorldEvent event) {
        Entity entity = event.entity;
        if (!(entity instanceof EntityPlayerMP)) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) entity;
        if (player.worldObj.isRemote) {
            return;
        }
        FawePlayer fp = FawePlayer.wrap(player);
        if (fp.getMeta("lastWorld") != event.world) {
            fp.setMeta("lastWorld", event.world);
            if (Settings.STORE_HISTORY_ON_DISK) {
                fp.getSession().clearHistory();
                fp.loadSessionsFromDisk(fp.getWorld());
            }
        }
    }
}
