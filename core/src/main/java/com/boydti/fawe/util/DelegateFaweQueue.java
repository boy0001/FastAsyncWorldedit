package com.boydti.fawe.util;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.Relighter;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.object.exception.FaweException;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.io.File;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import javax.annotation.Nullable;

public class DelegateFaweQueue extends FaweQueue {
    private FaweQueue parent;

    public DelegateFaweQueue(FaweQueue parent) {
        super(parent == null ? null : parent.getWEWorld());
        this.parent = parent;
    }

    public FaweQueue getParent() {
        return parent;
    }

    public void setParent(FaweQueue parent) {
        this.parent = parent;
        setWorld(parent.getWorldName());
    }

    @Override
    public void sendChunk(int x, int z, int bitMask) {
        parent.sendChunk(x, z, bitMask);
    }

    @Override
    public boolean setMCA(Runnable whileLocked, RegionWrapper region, boolean unload) {
        return parent.setMCA(whileLocked, region, unload);
    }

    @Override
    public String getWorldName() {
        return parent.getWorldName();
    }

    @Override
    public void addEditSession(EditSession session) {
        parent.addEditSession(session);
    }

    @Override
    public World getWEWorld() {
        return parent.getWEWorld();
    }

    @Override
    public void setProgressTracker(RunnableVal2<ProgressType, Integer> progressTask) {
        parent.setProgressTracker(progressTask);
    }

    @Override
    public Set<EditSession> getEditSessions() {
        return parent.getEditSessions();
    }

    @Override
    public ConcurrentLinkedDeque<EditSession> getSessions() {
        return parent.getSessions();
    }

    @Override
    public void setSessions(ConcurrentLinkedDeque<EditSession> sessions) {
        parent.setSessions(sessions);
    }

    @Override
    public long getModified() {
        return parent.getModified();
    }

    @Override
    public void setModified(long modified) {
        parent.setModified(modified);
    }

    @Override
    public RunnableVal2<ProgressType, Integer> getProgressTask() {
        return parent.getProgressTask();
    }

    @Override
    public void setProgressTask(RunnableVal2<ProgressType, Integer> progressTask) {
        parent.setProgressTask(progressTask);
    }

    @Override
    public int getBiomeId(int x, int z) throws FaweException.FaweChunkLoadException {
        return parent.getBiomeId(x, z);
    }

    @Override
    public void setChangeTask(RunnableVal2<FaweChunk, FaweChunk> changeTask) {
        parent.setChangeTask(changeTask);
    }

    @Override
    public RunnableVal2<FaweChunk, FaweChunk> getChangeTask() {
        return parent.getChangeTask();
    }

    @Override
    public void optimize() {
        parent.optimize();
    }

    @Override
    public int setBlocks(CuboidRegion cuboid, int id, int data) {
        return parent.setBlocks(cuboid, id, data);
    }

    @Override
    public boolean setBlock(int x, int y, int z, int id, int data) {
        return parent.setBlock(x, y, z, id, data);
    }

    @Override
    public boolean setBlock(int x, int y, int z, int id) {
        return parent.setBlock(x, y, z, id);
    }

    @Override
    public boolean setBlock(int x, int y, int z, int id, int data, CompoundTag nbt) {
        return parent.setBlock(x, y, z, id, data, nbt);
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tag) {
        parent.setTile(x, y, z, tag);
    }

    @Override
    public void setEntity(int x, int y, int z, CompoundTag tag) {
        parent.setEntity(x, y, z, tag);
    }

    @Override
    public void removeEntity(int x, int y, int z, UUID uuid) {
        parent.removeEntity(x, y, z, uuid);
    }

    @Override
    public boolean setBiome(int x, int z, BaseBiome biome) {
        return parent.setBiome(x, z, biome);
    }

    @Override
    public FaweChunk<?> getFaweChunk(int x, int z) {
        return parent.getFaweChunk(x, z);
    }

    @Override
    public Collection<FaweChunk> getFaweChunks() {
        return parent.getFaweChunks();
    }

    @Override
    public void setChunk(FaweChunk chunk) {
        parent.setChunk(chunk);
    }

    @Override
    public File getSaveFolder() {
        return parent.getSaveFolder();
    }

    @Override
    public int getMaxY() {
        return parent.getMaxY();
    }

    @Override
    public void forEachBlockInChunk(int cx, int cz, RunnableVal2<Vector, BaseBlock> onEach) {
        parent.forEachBlockInChunk(cx, cz, onEach);
    }

    @Override
    public void forEachTileInChunk(int cx, int cz, RunnableVal2<Vector, BaseBlock> onEach) {
        parent.forEachTileInChunk(cx, cz, onEach);
    }

    @Override
    @Deprecated
    public boolean regenerateChunk(int x, int z) {
        return parent.regenerateChunk(x, z);
    }

    @Override
    public boolean regenerateChunk(int x, int z, @Nullable BaseBiome biome, Long seed) {
        return parent.regenerateChunk(x, z, biome, seed);
    }

    @Override
    public void startSet(boolean parallel) {
        parent.startSet(parallel);
    }

    @Override
    public void endSet(boolean parallel) {
        parent.endSet(parallel);
    }

    @Override
    public int cancel() {
        return parent.cancel();
    }

    @Override
    public void sendBlockUpdate(FaweChunk chunk, FawePlayer... players) {
        parent.sendBlockUpdate(chunk, players);
    }

    @Deprecated
    @Override
    public boolean next() {
        return parent.next();
    }

    @Override
    public boolean next(int amount, long time) {
        return parent.next(amount, time);
    }

    @Override
    public void saveMemory() {
        parent.saveMemory();
    }

    @Override
    public void sendChunk(FaweChunk chunk) {
        parent.sendChunk(chunk);
    }

    @Override
    public void clear() {
        parent.clear();
    }

    @Override
    public void addNotifyTask(int x, int z, Runnable runnable) {
        parent.addNotifyTask(x, z, runnable);
    }

    @Override
    public boolean hasBlock(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return parent.hasBlock(x, y, z);
    }

    @Override
    public void addNotifyTask(Runnable runnable) {
        parent.addNotifyTask(runnable);
    }

    @Override
    public int getCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return parent.getCombinedId4Data(x, y, z);
    }

    @Override
    public int getCachedCombinedId4Data(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return parent.getCachedCombinedId4Data(x, y, z);
    }

    @Override
    public int getAdjacentLight(int x, int y, int z) {
        return parent.getAdjacentLight(x, y, z);
    }

    @Override
    public boolean hasSky() {
        return parent.hasSky();
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        return parent.getSkyLight(x, y, z);
    }

    @Override
    public int getLight(int x, int y, int z) {
        return parent.getLight(x, y, z);
    }

    @Override
    public int getEmmittedLight(int x, int y, int z) {
        return parent.getEmmittedLight(x, y, z);
    }

    @Override
    public CompoundTag getTileEntity(int x, int y, int z) throws FaweException.FaweChunkLoadException {
        return parent.getTileEntity(x, y, z);
    }

    @Override
    public int getCombinedId4Data(int x, int y, int z, int def) {
        return parent.getCombinedId4Data(x, y, z, def);
    }

    @Override
    public int getCachedCombinedId4Data(int x, int y, int z, int def) {
        return parent.getCachedCombinedId4Data(x, y, z, def);
    }

    @Override
    public int getCombinedId4DataDebug(int x, int y, int z, int def, EditSession session) {
        return parent.getCombinedId4DataDebug(x, y, z, def, session);
    }

    @Override
    public int getBrightness(int x, int y, int z) {
        return parent.getBrightness(x, y, z);
    }

    @Override
    public int getOpacityBrightnessPair(int x, int y, int z) {
        return parent.getOpacityBrightnessPair(x, y, z);
    }

    @Override
    public int getOpacity(int x, int y, int z) {
        return parent.getOpacity(x, y, z);
    }

    @Override
    public int size() {
        return parent.size();
    }

    @Override
    public boolean isEmpty() {
        return parent.isEmpty();
    }

    @Override
    public void flush() {
        parent.flush();
    }

    @Override
    public SetQueue.QueueStage getStage() {
        return parent.getStage();
    }

    @Override
    public void setStage(SetQueue.QueueStage stage) {
        parent.setStage(stage);
    }

    @Override
    public void flush(int time) {
        parent.flush(time);
    }

    @Override
    public void runTasks() {
        parent.runTasks();
    }

    @Override
    public void addTask(Runnable whenFree) {
        parent.addTask(whenFree);
    }

    @Override
    public boolean enqueue() {
        return parent.enqueue();
    }

    @Override
    public void dequeue() {
        parent.dequeue();
    }

    @Override
    public Relighter getRelighter() {
        return parent.getRelighter();
    }

    @Override
    public Settings getSettings() {
        return parent.getSettings();
    }

    @Override
    public void setSettings(Settings settings) {
        parent.setSettings(settings);
    }

    @Override
    public void setWorld(String world) {
        parent.setWorld(world);
    }
}
