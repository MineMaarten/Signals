package com.minemaarten.signals.rail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase.EnumRailDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.minemaarten.signals.api.IRail;
import com.minemaarten.signals.api.IRailMapper;

public class RailManager{
    private static final RailManager INSTANCE = new RailManager();
    private final List<IRailMapper> railMappers = new ArrayList<IRailMapper>();
    private final Map<Block, IRail> blockToRails = new HashMap<Block, IRail>();

    public static RailManager getInstance(){
        return INSTANCE;
    }

    public RailManager(){
        registerRail(Blocks.RAIL, RailBase.getInstance());
        registerRail(Blocks.DETECTOR_RAIL, RailBase.getInstance());
        registerRail(Blocks.GOLDEN_RAIL, RailBase.getInstance());
        registerRail(Blocks.ACTIVATOR_RAIL, RailBase.getInstance());
        registerCustomRailMapper(new RailMapperBlockRail());
    }

    public void registerRail(Block railBlock, IRail rail){
        if(railBlock == null) throw new NullPointerException("Block is null!");
        if(rail == null) throw new NullPointerException("Rail is null!");
        blockToRails.put(railBlock, rail);
    }

    public void registerCustomRailMapper(IRailMapper rail){
        if(rail == null) throw new NullPointerException("Rail Mapper is null!");
        railMappers.add(rail);
    }

    public IRail getRail(World world, BlockPos pos, IBlockState state){
        IRail rail = blockToRails.get(state.getBlock());
        if(rail != null) return rail;
        for(IRailMapper mapper : railMappers) {
            rail = mapper.getRail(world, pos, state);
            if(rail != null) return rail;
        }
        return null;
    }

    public EnumRailDirection getRailDirection(World world, BlockPos pos){
        IBlockState state = world.getBlockState(pos);
        IRail rail = getRail(world, pos, state);
        return rail != null ? rail.getDirection(world, pos, state) : null;
    }
}
