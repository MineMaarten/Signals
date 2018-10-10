package com.minemaarten.signals.block;

import net.minecraft.block.BlockRailPowered;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

import com.minemaarten.signals.client.CreativeTabSignals;
import com.minemaarten.signals.init.ModBlocks;
import com.minemaarten.signals.tileentity.TileEntityTransportRail;

public class BlockTransportRail extends BlockRailPowered implements ITileEntityProvider{
    /**
     * Together with the SHAPE, this determines which way carts will go when standing still and then getting powered.
     */
    public static final PropertyBool FORWARD = PropertyBool.create("forward");

    public BlockTransportRail(){
        setUnlocalizedName("transport_rail");
        setCreativeTab(CreativeTabSignals.getInstance());
        ModBlocks.registerBlock(this);
        GameRegistry.registerTileEntity(TileEntityTransportRail.class, "transport_rail");
    }

    @Override
    public void onMinecartPass(World world, EntityMinecart cart, BlockPos pos){
        super.onMinecartPass(world, cart, pos);
        Vec3d vec = new Vec3d(cart.motionX, cart.motionY, cart.motionZ);
        double maxAcceleration = 0.2D;
        double vecLength = vec.lengthVector();
        IBlockState state = world.getBlockState(pos);
        state = state.getBlock().getActualState(state, world, pos);
        boolean powered = state.getValue(POWERED);
        double maxSpeed = powered ? 0.1D : 0.0D;
        if(vecLength > maxSpeed) {
            vecLength = Math.max(vecLength - maxAcceleration, maxSpeed);
        } else if(vecLength < maxSpeed) {
            if(vecLength < 0.1) { //When practically standing still
                EnumFacing pushDir = BlockTeleportRail.getDirection(state.getValue(getShapeProperty()), state.getValue(FORWARD));
                vec = new Vec3d(pushDir.getFrontOffsetX(), 0, pushDir.getFrontOffsetZ()); //Push in the direction of the rail.
            }
            vecLength = Math.min(vecLength + maxAcceleration, maxSpeed);
        }
        vec = vec.normalize().scale(vecLength);
        cart.motionX = vec.x;
        cart.motionY = vec.y;
        cart.motionZ = vec.z;
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta){
        return new TileEntityTransportRail();
    }

    /**
     * Called serverside after this block is replaced with another in Chunk, but before the Tile Entity is updated
     */
    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state){
        super.breakBlock(worldIn, pos, state);
        worldIn.removeTileEntity(pos);
    }

    @Override
    protected BlockStateContainer createBlockState(){
        return new BlockStateContainer(this, getShapeProperty(), POWERED, FORWARD);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos){
        TileEntityTransportRail te = (TileEntityTransportRail)worldIn.getTileEntity(pos);
        return super.getActualState(state, worldIn, pos).withProperty(FORWARD, te.isForward());
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
        if(playerIn.isSneaking()) {
            if(!worldIn.isRemote) {
                TileEntityTransportRail te = (TileEntityTransportRail)worldIn.getTileEntity(pos);
                te.toggleForward();
                playerIn.sendMessage(new TextComponentTranslation("signals.message.teleport_rail_toggled_direction"));
            }
            return true;
        } else {
            return false;
        }
    }
}
