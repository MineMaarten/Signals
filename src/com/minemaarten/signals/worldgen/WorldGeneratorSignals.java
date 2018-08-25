package com.minemaarten.signals.worldgen;

import java.util.Random;

import net.minecraft.block.BlockRail;
import net.minecraft.block.BlockRailBase.EnumRailDirection;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import com.minemaarten.signals.block.BlockSignalBase;
import com.minemaarten.signals.init.ModBlocks;

public class WorldGeneratorSignals implements IWorldGenerator{

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider){
        // TODO when adding worldgen, resolve the note at https://github.com/MineMaarten/Signals/pull/80 regarding the possibility of rails not being included in the network.
        if(chunkX == 0) {
            int x = chunkX * 16 + 8;
            int y = 4;
            int startZ = chunkZ * 16;
            for(int z = startZ; z < startZ + 16; z++) {
                world.setBlockState(new BlockPos(x, y, z), Blocks.STONE.getDefaultState(), 0);
                world.setBlockState(new BlockPos(x, y + 1, z), Blocks.RAIL.getDefaultState().withProperty(BlockRail.SHAPE, EnumRailDirection.NORTH_SOUTH), 0);
                if(z % 256 == 0) {
                    world.setBlockState(new BlockPos(x + 1, y + 1, z), ModBlocks.BLOCK_SIGNAL.getDefaultState().withProperty(BlockSignalBase.FACING, EnumFacing.NORTH), 0);

                }
            }
        }
    }

}
