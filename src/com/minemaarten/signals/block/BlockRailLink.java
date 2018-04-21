package com.minemaarten.signals.block;

import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import com.minemaarten.signals.proxy.CommonProxy.EnumGuiId;
import com.minemaarten.signals.tileentity.TileEntityRailLink;

public class BlockRailLink extends BlockBase{
    public static PropertyBool CONNECTED = PropertyBool.create("connected");

    public BlockRailLink(){
        super(TileEntityRailLink.class, "rail_link");
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos){
        return new AxisAlignedBB(4 / 16F, 0.0F, 4 / 16F, 12 / 16F, 16 / 16F, 12 / 16F);
    }

    @Override
    protected BlockStateContainer createBlockState(){
        return new BlockStateContainer(this, CONNECTED);
    }

    @Override
    public int getMetaFromState(IBlockState state){
        return state.getValue(CONNECTED) ? 1 : 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta){
        return super.getStateFromMeta(meta).withProperty(CONNECTED, meta == 1);
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
    public EnumGuiId getGuiID(){
        return EnumGuiId.RAIL_LINK;
    }

}
