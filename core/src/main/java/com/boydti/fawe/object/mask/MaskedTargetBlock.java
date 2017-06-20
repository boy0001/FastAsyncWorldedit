package com.boydti.fawe.object.mask;

import com.sk89q.worldedit.BlockWorldVector;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.util.TargetBlock;
import com.sk89q.worldedit.world.World;

public class MaskedTargetBlock extends TargetBlock {
    private final Mask mask;
    private final World world;

    public MaskedTargetBlock(Mask mask, Player player, int maxDistance, double checkDistance) {
        super(player, maxDistance, checkDistance);
        this.mask = mask;
        this.world = player.getWorld();
    }

    public BlockWorldVector getMaskedTargetBlock(boolean useLastBlock) {
        boolean searchForLastBlock = true;
        BlockWorldVector lastBlock = null;
        while (getNextBlock() != null) {
            if (mask == null ? world.getBlockType(getCurrentBlock()) == BlockID.AIR : !mask.test(getCurrentBlock())) {
                if (searchForLastBlock) {
                    lastBlock = getCurrentBlock();
                    if (lastBlock.getBlockY() <= 0 || lastBlock.getBlockY() >= world.getMaxY()) {
                        searchForLastBlock = false;
                    }
                }
            } else {
                break;
            }
        }
        BlockWorldVector currentBlock = getCurrentBlock();
        return (currentBlock != null || !useLastBlock ? currentBlock : lastBlock);
    }
}
