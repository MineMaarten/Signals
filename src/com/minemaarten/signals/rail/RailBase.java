package com.minemaarten.signals.rail;

import java.util.EnumSet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRailBase.EnumRailDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.minemaarten.signals.api.IRail;
import com.minemaarten.signals.api.Signals;

@Signals
public class RailBase implements IRail{
    @Override
	public Block[] getApplicableBlocks() {
		return new Block[]{Blocks.RAIL, Blocks.DETECTOR_RAIL, Blocks.GOLDEN_RAIL, Blocks.ACTIVATOR_RAIL};
	}

    @Override
    public EnumRailDirection getDirection(World world, BlockPos pos, IBlockState state){
        return state.getValue(((BlockRailBase)state.getBlock()).getShapeProperty());
    }

    @Override
    public EnumSet<EnumRailDirection> getValidDirections(World world, BlockPos pos, IBlockState state){
        return EnumSet.copyOf(((BlockRailBase)state.getBlock()).getShapeProperty().getAllowedValues());
    }

    @Override
    public void setDirection(World world, BlockPos pos, IBlockState originalState, EnumRailDirection railDir){
        world.setBlockState(pos, originalState.withProperty(((BlockRailBase)originalState.getBlock()).getShapeProperty(), railDir), 2);
    }
}
