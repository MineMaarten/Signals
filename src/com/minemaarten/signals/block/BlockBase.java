package com.minemaarten.signals.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

import com.minemaarten.signals.Signals;
import com.minemaarten.signals.client.CreativeTabSignals;
import com.minemaarten.signals.init.ModBlocks;
import com.minemaarten.signals.proxy.CommonProxy.EnumGuiId;

public class BlockBase extends BlockContainer{

    private final Class<? extends TileEntity> tileClass;

    public BlockBase(Class<? extends TileEntity> tileClass, String name){
        super(Material.ROCK);
        setUnlocalizedName(name);
        this.tileClass = tileClass;
        GameRegistry.registerTileEntity(tileClass, name);
        setCreativeTab(CreativeTabSignals.getInstance());
        ModBlocks.registerBlock(this);
        setHardness(1);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta){
        try {
            return tileClass.newInstance();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public EnumGuiId getGuiID(){
        return null;
    }

    /**
     * The type of render function called. 3 for standard block models, 2 for TESR's, 1 for liquids, -1 is no render
     */
    public EnumBlockRenderType getRenderType(IBlockState state)
    {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing face, float par7, float par8, float par9){
    	if(player.isSneaking() || getGuiID() == null) return false;
        else {
            if(!world.isRemote) {
                TileEntity te = world.getTileEntity(pos);
                if(te != null) {
                    player.openGui(Signals.instance, getGuiID().ordinal(), world, pos.getX(), pos.getY(), pos.getZ());
                }
            }

            return true;
        }
    }
}
