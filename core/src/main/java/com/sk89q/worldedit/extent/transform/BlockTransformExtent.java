package com.sk89q.worldedit.extent.transform;

import com.boydti.fawe.FaweCache;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.transform.Transform;
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
public class BlockTransformExtent extends AbstractDelegateExtent {

    private static final double RIGHT_ANGLE = Math.toRadians(90);

    private final Transform transform;
    private final BlockRegistry blockRegistry;
    private final BaseBlock[] BLOCK_TRANSFORM;
    private final BaseBlock[] BLOCK_TRANSFORM_INVERSE;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     * @param blockRegistry the block registry used for block direction data
     */
    public BlockTransformExtent(Extent extent, Transform transform, BlockRegistry blockRegistry) {
        super(extent);
        checkNotNull(transform);
        checkNotNull(blockRegistry);
        this.transform = transform;
        this.blockRegistry = blockRegistry;
        BLOCK_TRANSFORM = new BaseBlock[FaweCache.CACHE_BLOCK.length];
        BLOCK_TRANSFORM_INVERSE = new BaseBlock[FaweCache.CACHE_BLOCK.length];
        Transform inverse = transform.inverse();
        for (int i = 0; i < BLOCK_TRANSFORM.length; i++) {
            BaseBlock block = FaweCache.CACHE_BLOCK[i];
            if (block != null) {
                BLOCK_TRANSFORM[i] = transform(new BaseBlock(block), transform, blockRegistry);
                BLOCK_TRANSFORM_INVERSE[i] = transform(new BaseBlock(block), inverse, blockRegistry);
            }
        }
    }

    /**
     * Get the transform.
     *
     * @return the transform
     */
    public Transform getTransform() {
        return transform;
    }

    @Override
    public BaseBlock getBlock(Vector position) {
        return transformFast(super.getBlock(position));
    }

    @Override
    public BaseBlock getLazyBlock(Vector position) {
        return transformFast(super.getLazyBlock(position));
    }

    @Override
    public boolean setBlock(Vector location, BaseBlock block) throws WorldEditException {
        return super.setBlock(location, transformFastInverse(block));
    }

    private final BaseBlock transformFast(BaseBlock block) {
        BaseBlock newBlock = BLOCK_TRANSFORM[FaweCache.getCombined(block)];
        if (block.hasNbtData()) {
            newBlock.setNbtData(block.getNbtData());
        }
        return newBlock;
    }

    private final BaseBlock transformFastInverse(BaseBlock block) {
        BaseBlock newBlock = BLOCK_TRANSFORM_INVERSE[FaweCache.getCombined(block)];
        if (block.hasNbtData()) {
            newBlock.setNbtData(block.getNbtData());
        }
        return newBlock;
    }

    /**
     * Transform the given block using the given transform.
     *
     * <p>The provided block is modified.</p>
     *
     * @param block the block
     * @param transform the transform
     * @param registry the registry
     * @return the same block
     */
    public static BaseBlock transform(BaseBlock block, Transform transform, BlockRegistry registry) {
        return transform(block, transform, registry, block);
    }

    /**
     * Transform the given block using the given transform.
     *
     * @param block the block
     * @param transform the transform
     * @param registry the registry
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

        for (State state : states.values()) {
            if (state.hasDirection()) {
                StateValue value = state.getValue(block);
                if (value != null && value.getDirection() != null) {
                    StateValue newValue = getNewStateValue(state, transform, value.getDirection());
                    if (newValue != null) {
                        if (changedBlock.hasWildcardData()) {
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
     * @param state the state
     * @param transform the transform
     * @param oldDirection the old direction to transform
     * @return a new state or null if none could be found
     */
    @Nullable
    private static StateValue getNewStateValue(State state, Transform transform, Vector oldDirection) {
        Vector newDirection = new Vector(transform.apply(oldDirection)).subtract(transform.apply(Vector.ZERO)).normalize();
        StateValue newValue = null;
        double closest = -2;
        boolean found = false;

        for (StateValue v : state.valueMap().values()) {
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