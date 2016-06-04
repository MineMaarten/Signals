package com.minemaarten.signals.api;

import java.util.EnumSet;

import net.minecraft.block.BlockRailBase.EnumRailDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IRail{
    public EnumRailDirection getDirection(World world, BlockPos pos, IBlockState state);

    /**
     * The valid directions this rail can be _set_ to. Any element returned here should be allowed to be passed in setDirection
     * @param world
     * @param pos
     * @param state
     * @return
     */
    public EnumSet<EnumRailDirection> getValidDirections(World world, BlockPos pos, IBlockState state);

    /**
     * Should set the rail to the given railDir.
     * @param world
     * @param pos
     * @param originalState
     * @param railDir
     */
    public void setDirection(World world, BlockPos pos, IBlockState originalState, EnumRailDirection railDir);
}
