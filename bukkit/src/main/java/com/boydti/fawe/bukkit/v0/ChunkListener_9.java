package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.FaweTimer;
import com.boydti.fawe.util.MathMan;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExpEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.BrewingStandFuelEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;

public class ChunkListener_9 extends ChunkListener {

    private Exception exception;
    private StackTraceElement[] elements;

    public ChunkListener_9() {
        super();
    }

    private void reset() {
        physSkip = 0;
        physStart = System.currentTimeMillis();
        physCancel = false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockBurnEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockCanBuildEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockDamageEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockDispenseEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockExpEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockExplodeEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockFadeEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockFromToEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockGrowEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockIgniteEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event( BlockPistonEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockPlaceEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BrewEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BrewingStandFuelEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(CauldronLevelChangeEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(FurnaceBurnEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(FurnaceSmeltEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(LeavesDecayEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(NotePlayEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(SignChangeEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void event(BlockRedstoneEvent event) { reset(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPhysics(BlockPhysicsEvent event) {
        if (physicsFreeze) {
            event.setCancelled(true);
            return;
        }
        if (physCancel) {
            Block block = event.getBlock();
            long pair = MathMan.pairInt(block.getX() >> 4, block.getZ() >> 4);
            if (physCancelPair == pair) {
                event.setCancelled(true);
                return;
            }
            if (badChunks.containsKey(pair)) {
                physCancelPair = pair;
                event.setCancelled(true);
                return;
            }
            if (System.currentTimeMillis() - physStart > Settings.IMP.TICK_LIMITER.PHYSICS_MS) {
                physCancelPair = pair;
                event.setCancelled(true);
                return;
            }
        }
        FaweTimer timer = Fawe.get().getTimer();
        if (timer.getTick() != physTick) {
            physTick = timer.getTick();
            physStart = System.currentTimeMillis();
            physSkip = 0;
            physCancel = false;
            return;
        }
        if ((++physSkip & 1023) == 0) {
            if (System.currentTimeMillis() - physStart > Settings.IMP.TICK_LIMITER.PHYSICS_MS) {
                Block block = event.getBlock();
                int cx = block.getX() >> 4;
                int cz = block.getZ() >> 4;
                physCancelPair = MathMan.pairInt(cx, cz);
                if (rateLimit <= 0) {
                    rateLimit = 20;
                    Fawe.debug("[FAWE `tick-limiter`] Detected and cancelled physics lag source at " + block.getLocation());
                }
                cancelNearby(cx, cz);
                event.setCancelled(true);
                physCancel = true;
                return;
            }
        }
    }

    private StackTraceElement[] getElements(Exception ex) {
        if (elements == null || ex != exception) {
            exception = ex;
            elements = ex.getStackTrace();
        }
        return elements;
    }

    @Override
    protected int getDepth(Exception ex) {
        return getElements(ex).length;
    }

    @Override
    protected StackTraceElement getElement(Exception ex, int i) {
        StackTraceElement[] elems = getElements(ex);
        return elems.length > i ? elems[i] : null;
    }
}