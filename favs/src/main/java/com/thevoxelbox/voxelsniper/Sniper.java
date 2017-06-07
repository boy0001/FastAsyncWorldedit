package com.thevoxelbox.voxelsniper;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.bukkit.wrapper.AsyncWorld;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.logging.LoggingChangeSet;
import com.boydti.fawe.object.ChangeSetFaweQueue;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.MaskedFaweQueue;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.WEManager;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.MutableClassToInstanceMap;
import com.sk89q.worldedit.LocalSession;
import com.thevoxelbox.voxelsniper.brush.IBrush;
import com.thevoxelbox.voxelsniper.brush.SnipeBrush;
import com.thevoxelbox.voxelsniper.brush.perform.PerformBrush;
import com.thevoxelbox.voxelsniper.brush.perform.Performer;
import com.thevoxelbox.voxelsniper.event.SniperMaterialChangedEvent;
import com.thevoxelbox.voxelsniper.event.SniperReplaceMaterialChangedEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.material.MaterialData;

/**
 *
 */
public class Sniper {
    private VoxelSniper plugin;
    private final UUID player;
    private boolean enabled = true;
//    private LinkedList<FaweChangeSet> undoList = new LinkedList<>();
    private Map<String, SniperTool> tools = new HashMap<>();

    public Sniper(VoxelSniper plugin, Player player) {
        this.plugin = plugin;
        this.player = player.getUniqueId();
        SniperTool sniperTool = new SniperTool(this);
        sniperTool.assignAction(SnipeAction.ARROW, Material.ARROW);
        sniperTool.assignAction(SnipeAction.GUNPOWDER, Material.SULPHUR);
        tools.put(null, sniperTool);
    }

    public String getCurrentToolId() {
        return getToolId((getPlayer().getItemInHand() != null) ? getPlayer().getItemInHand().getType() : null);
    }

    public String getToolId(Material itemInHand) {
        if (itemInHand == null) {
            return null;
        }

        for (Map.Entry<String, SniperTool> entry : tools.entrySet()) {
            if (entry.getValue().hasToolAssigned(itemInHand)) {
                return entry.getKey();
            }
        }
        return null;
    }

    // Added
    private AsyncWorld permanentWorld;

    public void storeUndo(Undo undo) {
    }

    // Added
    public AsyncWorld getWorld() {
        AsyncWorld world = permanentWorld;
        if (world == null) {
            Player bukkitPlayer = getPlayer();
            World bukkitWorld = bukkitPlayer.getWorld();
            world = AsyncWorld.wrap(bukkitWorld);
            permanentWorld = world;
        }
        return world;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(player);
    }

    /**
     * Sniper execution call.
     *
     * @param action       Action player performed
     * @param itemInHand   Item in hand of player
     * @param clickedBlock Block that the player targeted/interacted with
     * @param clickedFace  Face of that targeted Block
     * @return true if command visibly processed, false otherwise.
     */
    public boolean snipe(final Action action, final Material itemInHand, final Block clickedBlock, final BlockFace clickedFace) {
        switch (action) {
            case LEFT_CLICK_AIR:
            case LEFT_CLICK_BLOCK:
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                break;
            default:
                return false;
        }
        if (tools.isEmpty()) {
            return false;
        }
        String toolId = getToolId(itemInHand);
        SniperTool sniperTool = tools.get(toolId);
        if (sniperTool == null) {
            return false;
        }
        if (!sniperTool.hasToolAssigned(itemInHand)) {
            return false;
        }
        if (sniperTool.getCurrentBrush() == null) {
            getPlayer().sendMessage("No Brush selected.");
            return false;
        }
        try {
            Player player = getPlayer();
            final FawePlayer<Player> fp = FawePlayer.wrap(player);
            TaskManager.IMP.taskNow(new Runnable() {
                @Override
                public void run() {
                    if (!fp.runAction(new Runnable() {
                        @Override
                        public void run() {
                            snipeOnCurrentThread(fp, action, itemInHand, clickedBlock, clickedFace, sniperTool, toolId);
                        }
                    }, true, false)) {
                        BBC.WORLDEDIT_COMMAND_LIMIT.send(fp);
                    }
                }
            }, Fawe.isMainThread());
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    // Old method (plus world arg)
    public synchronized boolean snipeOnCurrentThread(FawePlayer fp, final Action action, final Material itemInHand, Block clickedBlock, final BlockFace clickedFace, SniperTool sniperTool, String toolId) {
        Player bukkitPlayer = getPlayer();
        World bukkitWorld = bukkitPlayer.getWorld();

        FaweQueue baseQueue = FaweAPI.createQueue(fp.getLocation().world, false);
        RegionWrapper[] mask = WEManager.IMP.getMask(fp);
        MaskedFaweQueue maskQueue = new MaskedFaweQueue(baseQueue, mask);
        com.sk89q.worldedit.world.World worldEditWorld = fp.getWorld();
        FaweChangeSet changeSet = FaweChangeSet.getDefaultChangeSet(worldEditWorld, fp.getUUID());
        if (Fawe.imp().getBlocksHubApi() != null) {
            changeSet = LoggingChangeSet.wrap(fp, changeSet);
        }
        FaweQueue changeQueue;
        if (Settings.IMP.HISTORY.COMBINE_STAGES) {
            changeQueue = maskQueue;
            changeSet.addChangeTask(baseQueue);
        } else {
            changeQueue = new ChangeSetFaweQueue(changeSet, maskQueue);
        }
        AsyncWorld world = getWorld();
        world.changeWorld(bukkitWorld, changeQueue);

        if (clickedBlock != null) {
            clickedBlock = world.getBlockAt(clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ());
        }
        if (!getPlayer().hasPermission(sniperTool.getCurrentBrush().getPermissionNode())) {
            getPlayer().sendMessage("You are not allowed to use this brush. You're missing the permission node '" + sniperTool.getCurrentBrush().getPermissionNode() + "'");
            return true;
        }

        final SnipeData snipeData = sniperTool.getSnipeData();
        if (getPlayer().isSneaking()) {
            Block targetBlock;
            SnipeAction snipeAction = sniperTool.getActionAssigned(itemInHand);

            switch (action) {
                case LEFT_CLICK_BLOCK:
                case LEFT_CLICK_AIR:
                    if (clickedBlock != null) {
                        targetBlock = clickedBlock;
                    } else {
                        RangeBlockHelper rangeBlockHelper = snipeData.isRanged() ? new RangeBlockHelper(getPlayer(), world, snipeData.getRange()) : new RangeBlockHelper(getPlayer(), world);
                        targetBlock = snipeData.isRanged() ? rangeBlockHelper.getRangeBlock() : rangeBlockHelper.getTargetBlock();
                    }

                    switch (snipeAction) {
                        case ARROW:
                            if (targetBlock != null) {
                                int originalVoxel = snipeData.getVoxelId();
                                snipeData.setVoxelId(targetBlock.getTypeId());
                                SniperMaterialChangedEvent event = new SniperMaterialChangedEvent(this, toolId, new MaterialData(originalVoxel, snipeData.getData()), new MaterialData(snipeData.getVoxelId(), snipeData.getData()));
                                Bukkit.getPluginManager().callEvent(event);
                                snipeData.getVoxelMessage().voxel();
                                return true;
                            } else {
                                int originalVoxel = snipeData.getVoxelId();
                                snipeData.setVoxelId(0);
                                SniperMaterialChangedEvent event = new SniperMaterialChangedEvent(this, toolId, new MaterialData(originalVoxel, snipeData.getData()), new MaterialData(snipeData.getVoxelId(), snipeData.getData()));
                                Bukkit.getPluginManager().callEvent(event);
                                snipeData.getVoxelMessage().voxel();
                                return true;
                            }
                        case GUNPOWDER:
                            if (targetBlock != null) {
                                byte originalData = snipeData.getData();
                                snipeData.setData(targetBlock.getData());
                                SniperMaterialChangedEvent event = new SniperMaterialChangedEvent(this, toolId, new MaterialData(snipeData.getVoxelId(), originalData), new MaterialData(snipeData.getVoxelId(), snipeData.getData()));
                                Bukkit.getPluginManager().callEvent(event);
                                snipeData.getVoxelMessage().data();
                                return true;
                            } else {
                                byte originalData = snipeData.getData();
                                snipeData.setData((byte) 0);
                                SniperMaterialChangedEvent event = new SniperMaterialChangedEvent(this, toolId, new MaterialData(snipeData.getVoxelId(), originalData), new MaterialData(snipeData.getVoxelId(), snipeData.getData()));
                                Bukkit.getPluginManager().callEvent(event);
                                snipeData.getVoxelMessage().data();
                                return true;
                            }
                        default:
                            break;
                    }
                    break;
                case RIGHT_CLICK_AIR:
                case RIGHT_CLICK_BLOCK:
                    if (clickedBlock != null) {
                        targetBlock = clickedBlock;
                    } else {
                        RangeBlockHelper rangeBlockHelper = snipeData.isRanged() ? new RangeBlockHelper(getPlayer(), world, snipeData.getRange()) : new RangeBlockHelper(getPlayer(), world);
                        targetBlock = snipeData.isRanged() ? rangeBlockHelper.getRangeBlock() : rangeBlockHelper.getTargetBlock();
                    }

                    switch (snipeAction) {
                        case ARROW:
                            if (targetBlock != null) {
                                int originalId = snipeData.getReplaceId();
                                snipeData.setReplaceId(targetBlock.getTypeId());
                                SniperReplaceMaterialChangedEvent event = new SniperReplaceMaterialChangedEvent(this, toolId, new MaterialData(originalId, snipeData.getReplaceData()), new MaterialData(snipeData.getReplaceId(), snipeData.getReplaceData()));
                                Bukkit.getPluginManager().callEvent(event);
                                snipeData.getVoxelMessage().replace();
                                return true;
                            } else {
                                int originalId = snipeData.getReplaceId();
                                snipeData.setReplaceId(0);
                                SniperReplaceMaterialChangedEvent event = new SniperReplaceMaterialChangedEvent(this, toolId, new MaterialData(originalId, snipeData.getReplaceData()), new MaterialData(snipeData.getReplaceId(), snipeData.getReplaceData()));
                                Bukkit.getPluginManager().callEvent(event);
                                snipeData.getVoxelMessage().replace();
                                return true;
                            }
                        case GUNPOWDER:
                            if (targetBlock != null) {
                                byte originalData = snipeData.getReplaceData();
                                snipeData.setReplaceData(targetBlock.getData());
                                SniperReplaceMaterialChangedEvent event = new SniperReplaceMaterialChangedEvent(this, toolId, new MaterialData(snipeData.getReplaceId(), originalData), new MaterialData(snipeData.getReplaceId(), snipeData.getReplaceData()));
                                Bukkit.getPluginManager().callEvent(event);
                                snipeData.getVoxelMessage().replaceData();
                                return true;
                            } else {
                                byte originalData = snipeData.getReplaceData();
                                snipeData.setReplaceData((byte) 0);
                                SniperReplaceMaterialChangedEvent event = new SniperReplaceMaterialChangedEvent(this, toolId, new MaterialData(snipeData.getReplaceId(), originalData), new MaterialData(snipeData.getReplaceId(), snipeData.getReplaceData()));
                                Bukkit.getPluginManager().callEvent(event);
                                snipeData.getVoxelMessage().replaceData();
                                return true;
                            }
                        default:
                            break;
                    }
                    break;
                default:
                    return false;
            }
        } else {
            final Block targetBlock;
            final Block lastBlock;
            final SnipeAction snipeAction = sniperTool.getActionAssigned(itemInHand);

            switch (action) {
                case RIGHT_CLICK_AIR:
                case RIGHT_CLICK_BLOCK:
                    break;
                default:
                    return false;
            }

            if (clickedBlock != null) {
                targetBlock = clickedBlock;
                lastBlock = clickedBlock.getRelative(clickedFace);
                if (lastBlock == null || targetBlock == null) {
                    getPlayer().sendMessage(ChatColor.RED + "Snipe target block must be visible.");
                    return true;
                }
            } else {
                RangeBlockHelper rangeBlockHelper = snipeData.isRanged() ? new RangeBlockHelper(getPlayer(), world, snipeData.getRange()) : new RangeBlockHelper(getPlayer(), world);
                targetBlock = snipeData.isRanged() ? rangeBlockHelper.getRangeBlock() : rangeBlockHelper.getTargetBlock();
                lastBlock = rangeBlockHelper.getLastBlock();

                if (targetBlock == null || lastBlock == null) {
                    getPlayer().sendMessage(ChatColor.RED + "Snipe target block must be visible.");
                    return true;
                }
            }

            final IBrush brush = sniperTool.getCurrentBrush();
            if (sniperTool.getCurrentBrush() instanceof PerformBrush) {
                PerformBrush performerBrush = (PerformBrush) sniperTool.getCurrentBrush();
                performerBrush.initP(snipeData);
            }

            switch (brush.getClass().getSimpleName()) {
                case "JockeyBrush":
                    TaskManager.IMP.sync(new RunnableVal<Object>() {
                        @Override
                        public void run(Object value) {
                            brush.perform(snipeAction, snipeData, targetBlock, lastBlock);
                        }
                    });
                    break;
                default:
                    brush.perform(snipeAction, snipeData, targetBlock, lastBlock);
                    break;
            }
            if (Fawe.isMainThread()) {
                SetQueue.IMP.flush(changeQueue);
            } else {
                changeQueue.flush();
            }
            if (changeSet != null) {
                if (Settings.IMP.HISTORY.COMBINE_STAGES) {
                    changeSet.closeAsync();
                } else {
                    changeSet.close();
                }
                LocalSession session = fp.getSession();
                session.remember(changeSet.toEditSession(fp));
            }
            return true;
        }
        return false;
    }

    public IBrush setBrush(String toolId, Class<? extends IBrush> brush) {
        if (!tools.containsKey(toolId)) {
            return null;
        }

        return tools.get(toolId).setCurrentBrush(brush);
    }

    public IBrush getBrush(String toolId) {
        if (!tools.containsKey(toolId)) {
            return null;
        }

        return tools.get(toolId).getCurrentBrush();
    }

    public IBrush previousBrush(String toolId) {
        if (!tools.containsKey(toolId)) {
            return null;
        }

        return tools.get(toolId).previousBrush();
    }

    public boolean setTool(String toolId, SnipeAction action, Material itemInHand) {
        for (Map.Entry<String, SniperTool> entry : tools.entrySet()) {
            if (entry.getKey() != toolId && entry.getValue().hasToolAssigned(itemInHand)) {
                return false;
            }
        }

        if (!tools.containsKey(toolId)) {
            SniperTool tool = new SniperTool(this);
            tools.put(toolId, tool);
        }
        tools.get(toolId).assignAction(action, itemInHand);
        return true;
    }

    public void removeTool(String toolId, Material itemInHand) {
        if (!tools.containsKey(toolId)) {
            SniperTool tool = new SniperTool(this);
            tools.put(toolId, tool);
        }
        tools.get(toolId).unassignAction(itemInHand);
    }

    public void removeTool(String toolId) {
        if (toolId == null) {
            return;
        }
        tools.remove(toolId);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void undo() {
        undo(1);
    }

    public void undo(int amount) {
        FawePlayer<Object> fp = FawePlayer.wrap(getPlayer());
        int count = 0;
        for (int i = 0; i < amount; i++) {
            if (fp.getSession().undo(null, fp.getPlayer()) == null) {
                break;
            }
            count++;
        }
        if (count > 0) {
            BBC.COMMAND_UNDO_SUCCESS.send(fp);
        } else {
            BBC.COMMAND_UNDO_ERROR.send(fp);
        }
    }

    public void reset(String toolId) {
        SniperTool backup = tools.remove(toolId);
        SniperTool newTool = new SniperTool(this);

        for (Map.Entry<SnipeAction, Material> entry : backup.getActionTools().entrySet()) {
            newTool.assignAction(entry.getKey(), entry.getValue());
        }
        tools.put(toolId, newTool);
    }

    public SnipeData getSnipeData(String toolId) {
        return tools.containsKey(toolId) ? tools.get(toolId).getSnipeData() : null;
    }

    public void displayInfo() {
        String currentToolId = getCurrentToolId();
        SniperTool sniperTool = tools.get(currentToolId);
        IBrush brush = sniperTool.getCurrentBrush();
        getPlayer().sendMessage("Current Tool: " + ((currentToolId != null) ? currentToolId : "Default Tool"));
        if (brush == null) {
            getPlayer().sendMessage("No brush selected.");
            return;
        }
        brush.info(sniperTool.getMessageHelper());
        if (brush instanceof Performer) {
            ((Performer) brush).showInfo(sniperTool.getMessageHelper());
        }
    }

    public SniperTool getSniperTool(String toolId) {
        return tools.get(toolId);
    }

    public class SniperTool {
        private BiMap<SnipeAction, Material> actionTools = HashBiMap.create();
        private ClassToInstanceMap<IBrush> brushes = MutableClassToInstanceMap.create();
        private Class<? extends IBrush> currentBrush;
        private Class<? extends IBrush> previousBrush;
        private SnipeData snipeData;
        private Message messageHelper;

        private SniperTool(Sniper owner) {
            this(SnipeBrush.class, new SnipeData(owner));
        }

        private SniperTool(Class<? extends IBrush> currentBrush, SnipeData snipeData) {
            this.snipeData = snipeData;
            messageHelper = new Message(snipeData);
            snipeData.setVoxelMessage(messageHelper);

            IBrush newBrushInstance = instantiateBrush(currentBrush);
            if (snipeData.owner().getPlayer().hasPermission(newBrushInstance.getPermissionNode())) {
                brushes.put(currentBrush, newBrushInstance);
                this.currentBrush = currentBrush;
            }
        }

        public boolean hasToolAssigned(Material material) {
            return actionTools.containsValue(material);
        }

        public SnipeAction getActionAssigned(Material itemInHand) {
            return actionTools.inverse().get(itemInHand);
        }

        public Material getToolAssigned(SnipeAction action) {
            return actionTools.get(action);
        }

        public void assignAction(SnipeAction action, Material itemInHand) {
            actionTools.forcePut(action, itemInHand);
        }

        public void unassignAction(Material itemInHand) {
            actionTools.inverse().remove(itemInHand);
        }

        public BiMap<SnipeAction, Material> getActionTools() {
            return ImmutableBiMap.copyOf(actionTools);
        }

        public SnipeData getSnipeData() {
            return snipeData;
        }

        public Message getMessageHelper() {
            return messageHelper;
        }

        public IBrush getCurrentBrush() {
            if (currentBrush == null) {
                return null;
            }
            return brushes.getInstance(currentBrush);
        }

        public IBrush setCurrentBrush(Class<? extends IBrush> brush) {
            Preconditions.checkNotNull(brush, "Can't set brush to null.");
            IBrush brushInstance = brushes.get(brush);
            if (brushInstance == null) {
                brushInstance = instantiateBrush(brush);
                Preconditions.checkNotNull(brushInstance, "Could not instanciate brush class.");
                if (snipeData.owner().getPlayer().hasPermission(brushInstance.getPermissionNode())) {
                    brushes.put(brush, brushInstance);
                    previousBrush = currentBrush;
                    currentBrush = brush;
                    return brushInstance;
                }
            }

            if (snipeData.owner().getPlayer().hasPermission(brushInstance.getPermissionNode())) {
                previousBrush = currentBrush;
                currentBrush = brush;
                return brushInstance;
            }

            return null;
        }

        public IBrush previousBrush() {
            if (previousBrush == null) {
                return null;
            }
            return setCurrentBrush(previousBrush);
        }

        private IBrush instantiateBrush(Class<? extends IBrush> brush) {
            try {
                return brush.newInstance();
            } catch (InstantiationException e) {
                return null;
            } catch (IllegalAccessException e) {
                return null;
            }
        }
    }

    public static Class<?> inject() {
        return Sniper.class;
    }
}