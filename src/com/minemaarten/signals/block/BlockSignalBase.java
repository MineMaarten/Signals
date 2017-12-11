package com.minemaarten.signals.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import com.minemaarten.signals.api.access.ISignal.EnumLampStatus;
import com.minemaarten.signals.rail.RailManager;
import com.minemaarten.signals.rail.RailWrapper;
import com.minemaarten.signals.tileentity.TileEntitySignalBase;

public class BlockSignalBase extends BlockBase{
    public static PropertyEnum<EnumLampStatus> LAMP_STATUS = PropertyEnum.<EnumLampStatus> create("lamp_status", EnumLampStatus.class);
    public static PropertyEnum<EnumFacing> FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

    public BlockSignalBase(Class<? extends TileEntitySignalBase> tileClass, String name){
        super(tileClass, name);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos){
        return new AxisAlignedBB(4 / 16F, 0.0F, 4 / 16F, 12 / 16F, 8 / 16F, 12 / 16F);
    }

    @Override
    protected BlockStateContainer createBlockState(){
        return new BlockStateContainer(this, FACING, LAMP_STATUS);
    }

    @Override
    public int getMetaFromState(IBlockState state){
        return state.getValue(FACING).getHorizontalIndex() * 4 + state.getValue(LAMP_STATUS).ordinal();
    }

    @Override
    public IBlockState getStateFromMeta(int meta){
        IBlockState state = super.getStateFromMeta(meta);
        return state.withProperty(FACING, EnumFacing.getHorizontal(meta / 4)).withProperty(LAMP_STATUS, EnumLampStatus.values()[meta % 4]);
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer){
        List<EnumFacing> railFacings = new ArrayList<>(4);
        for(EnumFacing horFacing : EnumFacing.HORIZONTALS) {
            BlockPos railPos = pos.offset(horFacing);
            if(RailManager.getInstance().getRail(worldIn, railPos, worldIn.getBlockState(railPos)) != null) {
                railFacings.add(horFacing);
            }
        }

        EnumFacing orientation;
        if(railFacings.size() == 1) { //When exactly one rail is connecting, connect the signal to the rail
            orientation = railFacings.get(0).rotateY();
        } else { //Else, fall back onto player orientation.
            orientation = placer.getHorizontalFacing();
        }
        return getDefaultState().withProperty(FACING, orientation);
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess worldIn, BlockPos pos, EnumFacing side){
        EnumLampStatus lampStatus = state.getValue(LAMP_STATUS);
        if(!(worldIn instanceof WorldServer) || state.getBlock() != this || (lampStatus != EnumLampStatus.GREEN && lampStatus != EnumLampStatus.YELLOW) || state.getValue(BlockSignalBase.FACING).rotateY() != side) return 0;
        TileEntitySignalBase signal = (TileEntitySignalBase)worldIn.getTileEntity(pos);
        signal.setWorld((WorldServer)worldIn);
        for(RailWrapper rail : signal.getConnectedRails()) {
            for(TileEntitySignalBase s : rail.getSignals().values()) {
                if(s != signal) {
                    lampStatus = s.getLampStatus();
                    if(lampStatus != EnumLampStatus.GREEN && lampStatus != EnumLampStatus.YELLOW) return 0;
                }
            }
        }
        return 15;
    }

    /**
     * Can this block provide power. Only wire currently seems to have this change based on its state.
     */
    @Override
    public boolean canProvidePower(IBlockState state){
        return true;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state){
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state){
        return false;
    }

}
