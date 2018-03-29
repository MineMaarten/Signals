package com.minemaarten.signals.block;

import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.minemaarten.signals.proxy.CommonProxy.EnumGuiId;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;
import com.minemaarten.signals.tileentity.TileEntityStationMarker;

public class BlockStationMarker extends BlockBase{

    public static PropertyBool VALID = PropertyBool.create("valid");

    public BlockStationMarker(){
        super(TileEntityStationMarker.class, "station_marker");
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos){
        return new AxisAlignedBB(4 / 16F, 0.0F, 4 / 16F, 12 / 16F, 16 / 16F, 12 / 16F);
    }

    @Override
    protected BlockStateContainer createBlockState(){
        return new BlockStateContainer(this, VALID);
    }

    @Override
    public int getMetaFromState(IBlockState state){
        return state.getValue(VALID) ? 1 : 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta){
        return super.getStateFromMeta(meta).withProperty(VALID, meta == 1);
    }

    @Override
    public EnumGuiId getGuiID(){
        return EnumGuiId.STATION_MARKER;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state){
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state){
        return false;
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state){
        super.onBlockAdded(worldIn, pos, state);
        updateStationState(worldIn, pos, state);
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos){
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
        updateStationState(worldIn, pos, state);
    }

    public void updateStationState(World world, BlockPos pos, IBlockState state){
        boolean neighborRail = false;
        for(EnumFacing d : EnumFacing.VALUES) {
            if(RailNetworkManager.getInstance().getRail(world, pos.offset(d)) != null) {
                neighborRail = true;
                break;
            }
        }
        world.setBlockState(pos, state.withProperty(VALID, neighborRail), 2);
    }
}
