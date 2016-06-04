package com.minemaarten.signals.api;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Only implement when requiring custom behaviour. For most situations, use {@link IRail}.
 * 
 * With {@link IRail} you are limited to a fixed Block -> IRail mapping. With this interface you can specify for a given position and state,
 * what IRail to return.
 * 
 * Implement this interface and annotate it with {@link SignalsRail}. An instance of this class will be instantiated
 * and registered in the postInit phase.
 */
public interface IRailMapper{
    /**
     * Return a rail for a given position and state. Return null when the block isn't applicable for your logic.
     * The returned instance of IRail doesn't have to be registered as well, so no annotation of {@link SignalsRail} is required.
     */
    public IRail getRail(World world, BlockPos pos, IBlockState state);
}
