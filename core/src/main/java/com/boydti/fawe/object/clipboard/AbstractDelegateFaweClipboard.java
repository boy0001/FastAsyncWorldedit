package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.object.RunnableVal2;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import java.util.List;

public class AbstractDelegateFaweClipboard extends FaweClipboard {
    private final FaweClipboard parent;

    public AbstractDelegateFaweClipboard(FaweClipboard parent) {
        this.parent = parent;
    }

    @Override
    public BaseBlock getBlock(int x, int y, int z) {
        return parent.getBlock(x, y, z);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) {
        return parent.setBlock(x, y, z, block);
    }

    @Override
    public void setId(int index, int id) {
        parent.setId(index, id);
    }

    @Override
    public void setData(int index, int data) {
        parent.setData(index, data);
    }

    @Override
    public void setAdd(int index, int id) {
        parent.setAdd(index, id);
    }

    @Override
    public boolean setTile(int x, int y, int z, CompoundTag tag) {
        return parent.setTile(x, y, z, tag);
    }

    @Override
    public Entity createEntity(Extent world, double x, double y, double z, float yaw, float pitch, BaseEntity entity) {
        return parent.createEntity(world, x, y, z, yaw, pitch, entity);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return parent.getEntities();
    }

    @Override
    public boolean remove(ClipboardEntity clipboardEntity) {
        return parent.remove(clipboardEntity);
    }

    @Override
    public void setOrigin(Vector offset) {
        parent.setOrigin(offset);
    }

    @Override
    public void setDimensions(Vector dimensions) {
        parent.setDimensions(dimensions);
    }

    @Override
    public Vector getDimensions() {
        return parent.getDimensions();
    }

    @Override
    public void forEach(RunnableVal2<Vector, BaseBlock> task, boolean air) {
        parent.forEach(task, air);
    }

    @Override
    public void streamIds(NBTStreamer.ByteReader task) {
        parent.streamIds(task);
    }

    @Override
    public void streamDatas(NBTStreamer.ByteReader task) {
        parent.streamDatas(task);
    }

    @Override
    public List<CompoundTag> getTileEntities() {
        return parent.getTileEntities();
    }
}
