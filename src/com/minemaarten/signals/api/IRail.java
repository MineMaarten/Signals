package com.minemaarten.signals.api;

import java.util.EnumSet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase.EnumRailDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Custom rails that extend BlockRailBase should work right off the bat, without requiring to use this API.
 * 
 * When your rail does not extend BlockRailBase, the most straight-forward option is to implement this interface,
 * and annotate the class with {@link Signals}. An instance of the class will be created an registered in the postInit phase.
 * 
 * If more advanced behaviour is required, implement {@link IRailMapper} and again annotate it with {@link Signals}.
 * 
 * @author Maarten
 *
 */
public interface IRail{
	/**
	 * Called only once in postInit loading stage, and should return which blocks are applicable for this rail behaviour.
	 * All other methods will only be called with a blockstate that is backed by these blocks.
	 * @return
	 */
	public Block[] getApplicableBlocks();
	
	/**
	 * Should return the current rail orientation, for the given position and state.
	 * @param world
	 * @param pos
	 * @param state
	 * @return
	 */
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
