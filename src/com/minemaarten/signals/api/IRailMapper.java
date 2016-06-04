package com.minemaarten.signals.api;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Only implement when requiring custom behaviour.
 */
public interface IRailMapper{
    /**
     * Return a rail for a given position. Return null when the block isn't applicable for your logic.
     */
    public IRail getRail(World world, BlockPos pos, IBlockState state);
}
