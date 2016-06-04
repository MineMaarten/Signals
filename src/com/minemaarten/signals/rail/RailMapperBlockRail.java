package com.minemaarten.signals.rail;

import net.minecraft.block.BlockRailBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.minemaarten.signals.api.IRail;
import com.minemaarten.signals.api.IRailMapper;

public class RailMapperBlockRail implements IRailMapper{

    @Override
    public IRail getRail(World world, BlockPos pos, IBlockState state){
        if(state.getBlock() instanceof BlockRailBase) {
            return RailBase.getInstance();
        } else {
            return null;
        }
    }

}
