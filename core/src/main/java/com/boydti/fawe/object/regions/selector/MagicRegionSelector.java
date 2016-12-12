package com.boydti.fawe.object.regions.selector;

import com.boydti.fawe.object.regions.MagicRegion;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.limit.SelectorLimits;
import com.sk89q.worldedit.world.World;
import java.util.List;
import javax.annotation.Nullable;

public class MagicRegionSelector implements RegionSelector {

    private MagicRegion region;

    public MagicRegionSelector(@Nullable World world, Vector pos) {
        this.region = new MagicRegion(world);
    }

    @Nullable
    @Override
    public World getWorld() {
        return this.region.getWorld();
    }

    @Override
    public void setWorld(@Nullable World world) {
        this.region.setWorld(world);
    }

    @Override
    public boolean selectPrimary(Vector position, SelectorLimits limits) {

    }

    @Override
    public boolean selectSecondary(Vector position, SelectorLimits limits) {

    }

    @Override
    public void explainPrimarySelection(Actor actor, LocalSession session, Vector position) {

    }

    @Override
    public void explainSecondarySelection(Actor actor, LocalSession session, Vector position) {

    }

    @Override
    public void explainRegionAdjust(Actor actor, LocalSession session) {

    }

    @Override
    public BlockVector getPrimaryPosition() throws IncompleteRegionException {
        return null;
    }

    @Override
    public Region getRegion() throws IncompleteRegionException {
        return null;
    }

    @Override
    public Region getIncompleteRegion() {
        return null;
    }

    @Override
    public boolean isDefined() {
        return false;
    }

    @Override
    public int getArea() {
        return 0;
    }

    @Override
    public void learnChanges() {

    }

    @Override
    public void clear() {

    }

    @Override
    public String getTypeName() {
        return null;
    }

    @Override
    public List<String> getInformationLines() {
        return null;
    }
}
