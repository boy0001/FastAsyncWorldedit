package com.sk89q.worldedit.extent.transform;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.ByteTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.internal.helper.MCDirections;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.State;
import com.sk89q.worldedit.world.registry.StateValue;
import java.util.Map;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Transforms blocks themselves (but not their position) according to a
 * given transform.
 */
public class BlockTransformExtent extends ResettableExtent {
    private final BlockRegistry registry;
    private Transform transform;
    private Transform transformInverse;
    private BaseBlock[] BLOCK_TRANSFORM;
    private BaseBlock[] BLOCK_TRANSFORM_INVERSE;

    public BlockTransformExtent(Extent parent, BlockRegistry registry) {
        this(parent, new AffineTransform(), registry);
    }

    public BlockTransformExtent(Extent parent, Transform transform, BlockRegistry registry) {
        super(parent);
        this.transform = transform;
        this.transformInverse = this.transform.inverse();
        this.registry = registry;
        cache();
    }

    private void cache() {
        BLOCK_TRANSFORM = new BaseBlock[FaweCache.CACHE_BLOCK.length];
        BLOCK_TRANSFORM_INVERSE = new BaseBlock[FaweCache.CACHE_BLOCK.length];
        for (int i = 0; i < BLOCK_TRANSFORM.length; i++) {
            BaseBlock block = FaweCache.CACHE_BLOCK[i];
            if (block != null) {
                BLOCK_TRANSFORM[i] = BlockTransformExtent.transform(new BaseBlock(block), transform, registry);
                BLOCK_TRANSFORM_INVERSE[i] = BlockTransformExtent.transform(new BaseBlock(block), transformInverse, registry);
            }
        }
    }

    @Override
    public ResettableExtent setExtent(Extent extent) {
        return super.setExtent(extent);
    }

    public Transform getTransform() {
        return transform;
    }

    public void setTransform(Transform affine) {
        this.transform = affine;
        this.transformInverse = this.transform.inverse();
        cache();
    }

    public final BaseBlock transformFast(BaseBlock block) {
        BaseBlock newBlock = BLOCK_TRANSFORM[FaweCache.getCombined(block)];
        CompoundTag tag = block.getNbtData();
        if (tag != null) {
            newBlock = new BaseBlock(newBlock.getId(), newBlock.getData(), tag);
            if (tag.containsKey("Rot")) {
                int rot = tag.asInt("Rot");

                Direction direction = MCDirections.fromRotation(rot);

                if (direction != null) {
                    Vector applyAbsolute = transform.apply(direction.toVector());
                    Vector applyOrigin = transform.apply(Vector.ZERO);
                    applyAbsolute.mutX(applyAbsolute.getX() - applyOrigin.getX());
                    applyAbsolute.mutY(applyAbsolute.getY() - applyOrigin.getY());
                    applyAbsolute.mutZ(applyAbsolute.getZ() - applyOrigin.getZ());

                    Direction newDirection = Direction.findClosest(applyAbsolute, Direction.Flag.CARDINAL | Direction.Flag.ORDINAL | Direction.Flag.SECONDARY_ORDINAL);

                    if (newDirection != null) {
                        Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
                        values.put("Rot", new ByteTag((byte) MCDirections.toRotation(newDirection)));
                    }
                }
            }
        }
        return newBlock;
    }

    public final BaseBlock transformFastInverse(BaseBlock block) {
        BaseBlock newBlock = BLOCK_TRANSFORM_INVERSE[FaweCache.getCombined(block)];
        CompoundTag tag = block.getNbtData();
        if (tag != null) {
            newBlock = new BaseBlock(newBlock.getId(), newBlock.getData(), tag);
            if (tag.containsKey("Rot")) {
                int rot = tag.asInt("Rot");

                Direction direction = MCDirections.fromRotation(rot);

                if (direction != null) {
                    Vector applyAbsolute = transformInverse.apply(direction.toVector());
                    Vector applyOrigin = transformInverse.apply(Vector.ZERO);
                    applyAbsolute.mutX(applyAbsolute.getX() - applyOrigin.getX());
                    applyAbsolute.mutY(applyAbsolute.getY() - applyOrigin.getY());
                    applyAbsolute.mutZ(applyAbsolute.getZ() - applyOrigin.getZ());

                    Direction newDirection = Direction.findClosest(applyAbsolute, Direction.Flag.CARDINAL | Direction.Flag.ORDINAL | Direction.Flag.SECONDARY_ORDINAL);

                    if (newDirection != null) {
                        Map<String, Tag> values = ReflectionUtils.getMap(tag.getValue());
                        values.put("Rot", new ByteTag((byte) MCDirections.toRotation(newDirection)));
                    }
                }
            }
        }
        return newBlock;
    }

    @Override
    public BaseBlock getLazyBlock(int x, int y, int z) {
        return transformFast(super.getLazyBlock(x, y, z));
    }

    @Override
    public BaseBlock getLazyBlock(Vector position) {
        return transformFast(super.getLazyBlock(position));
    }

    @Override
    public BaseBlock getBlock(Vector position) {
        return transformFast(super.getBlock(position));
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        return super.getBiome(position);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) throws WorldEditException {
        return super.setBlock(x, y, z, transformFastInverse(block));
    }


    @Override
    public boolean setBlock(Vector location, BaseBlock block) throws WorldEditException {
        return super.setBlock(location, transformFastInverse(block));
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        return super.setBiome(position, biome);
    }

    /**
     * Transform the given block using the given transform.
     * <p>
     * <p>The provided block is modified.</p>
     *
     * @param block     the block
     * @param transform the transform
     * @param registry  the registry
     * @return the same block
     */
    public static BaseBlock transform(BaseBlock block, Transform transform, BlockRegistry registry) {
        return transform(block, transform, registry, block);
    }

    /**
     * Transform the given block using the given transform.
     *
     * @param block        the block
     * @param transform    the transform
     * @param registry     the registry
     * @param changedBlock the block to change
     * @return the changed block
     */
    private static BaseBlock transform(BaseBlock block, Transform transform, BlockRegistry registry, BaseBlock changedBlock) {
        checkNotNull(block);
        checkNotNull(transform);
        checkNotNull(registry);

        Map<String, ? extends State> states = registry.getStates(block);

        if (states == null) {
            return changedBlock;
        }

        for (Map.Entry<String, ? extends State> entry : states.entrySet()) {
            State state = entry.getValue();
            if (state.hasDirection()) {
                StateValue value = state.getValue(block);
                if (value != null && value.getDirection() != null) {
                    StateValue newValue = getNewStateValue(state, transform, value.getDirection());
                    if (newValue != null) {
                        if (changedBlock.isImmutable()) {
                            changedBlock = new BaseBlock(changedBlock.getId(), changedBlock.getData(), changedBlock.getNbtData());
                        }
                        newValue.set(changedBlock);
                    }
                }
            }
        }

        return changedBlock;
    }

    /**
     * Get the new value with the transformed direction.
     *
     * @param state        the state
     * @param transform    the transform
     * @param oldDirection the old direction to transform
     * @return a new state or null if none could be found
     */
    @Nullable
    private static StateValue getNewStateValue(State state, Transform transform, Vector oldDirection) {
        Vector newDirection = new Vector(transform.apply(oldDirection)).subtract(transform.apply(Vector.ZERO)).normalize();
        StateValue newValue = null;
        double closest = -2;
        boolean found = false;

        for (Map.Entry<String, ? extends StateValue> entry : state.valueMap().entrySet()) {
            StateValue v = entry.getValue();
            if (v.getDirection() != null) {
                double dot = v.getDirection().normalize().dot(newDirection);
                if (dot >= closest) {
                    closest = dot;
                    newValue = v;
                    found = true;
                }
            }
        }

        if (found) {
            return newValue;
        } else {
            return null;
        }
    }

    public static Class<?> inject() {
        return BlockTransformExtent.class;
    }
}