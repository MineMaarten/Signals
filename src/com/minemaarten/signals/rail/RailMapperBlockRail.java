package com.minemaarten.signals.rail;

import net.minecraft.block.BlockRailBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.minemaarten.signals.api.IRail;
import com.minemaarten.signals.api.IRailMapper;
import com.minemaarten.signals.api.SignalsRail;

@SignalsRail
public class RailMapperBlockRail implements IRailMapper{

	private final RailBase RAIL = new RailBase();
	
    @Override
    public IRail getRail(World world, BlockPos pos, IBlockState state){
        return state.getBlock() instanceof BlockRailBase ? RAIL : null;
    }

}
