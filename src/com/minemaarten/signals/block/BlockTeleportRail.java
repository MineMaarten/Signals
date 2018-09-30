package com.minemaarten.signals.block;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRailPowered;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityMinecartContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.common.registry.GameRegistry;

import com.minemaarten.signals.client.CreativeTabSignals;
import com.minemaarten.signals.init.ModBlocks;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.PacketSpawnParticle;
import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.rail.network.RailEdge;
import com.minemaarten.signals.rail.network.mc.MCPos;
import com.minemaarten.signals.rail.network.mc.MCTrain;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;
import com.minemaarten.signals.tileentity.TileEntityTeleportRail;

public class BlockTeleportRail extends BlockRailBase implements ITileEntityProvider{

    /**
     * Together with the SHAPE, this determines which way carts will go after they get teleported to this teleport rail.
     */
    public static final PropertyBool FORWARD = PropertyBool.create("forward");

    public BlockTeleportRail(){
        super(false);
        setUnlocalizedName("teleport_rail");
        setCreativeTab(CreativeTabSignals.getInstance());
        ModBlocks.registerBlock(this);
        GameRegistry.registerTileEntity(TileEntityTeleportRail.class, "signals:teleport_rail");
    }

    @Override
    public boolean isFlexibleRail(IBlockAccess world, BlockPos pos){
        return false;
    }

    @Override
    public void onMinecartPass(World world, EntityMinecart cart, BlockPos pos){
        super.onMinecartPass(world, cart, pos);
        if(!world.isRemote) {

            TileEntityTeleportRail te = (TileEntityTeleportRail)world.getTileEntity(pos);
            MCPos telDestination = te.getLinkedPosition();
            if(telDestination != null) { //We can only teleport if we have a destination to go to.
                spawnParticle(cart);
                MCTrain train = RailNetworkManager.getServerInstance().getState().getTrain(cart.getUniqueID());
                if(train != null) { //Should always be true
                    if(train.getCarts().size() == train.cartIDs.size()) { //We can only teleport if all carts are loaded.
                        //Check if all carts are on adjacent teleport tracks
                        Set<MCPos> teleportRails = getNeighboringTeleportRails(world, pos);
                        for(MCPos cartPos : train.getPositions()) {
                            if(!teleportRails.contains(cartPos)) {
                                return; //When not all carts are on adjacent teleport tracks, don't teleport.
                            }
                        }

                        teleport(new HashSet<>(train.getCarts()), telDestination);
                    }
                }
            }
        }
    }

    private void teleport(Set<EntityMinecart> carts, MCPos destination){
        for(EntityMinecart cart : carts) {
            teleport(cart, destination);
        }
    }

    private void teleport(EntityMinecart cart, MCPos destination){
        if(cart instanceof EntityMinecartContainer) {
            ((EntityMinecartContainer)cart).dropContentsWhenDead = false;
        }

        int dimensionIn = destination.getDimID();
        BlockPos destPos = destination.getPos();
        if(!ForgeHooks.onTravelToDimension(cart, destination.getDimID())) return;

        MinecraftServer minecraftserver = cart.getServer();
        int i = cart.dimension;
        WorldServer worldserver = minecraftserver.getWorld(i);
        WorldServer worldserver1 = minecraftserver.getWorld(dimensionIn);
        cart.dimension = dimensionIn;

        /*if (i == 1 && dimensionIn == 1)
        {
            worldserver1 = minecraftserver.getWorld(0);
            this.dimension = 0;
        }*/

        cart.world.removeEntity(cart);
        cart.isDead = false;
        BlockPos blockpos = destination.getPos();

        /* double moveFactor = worldserver.provider.getMovementFactor() / worldserver1.provider.getMovementFactor();
        double d0 = MathHelper.clamp(this.posX * moveFactor, worldserver1.getWorldBorder().minX() + 16.0D, worldserver1.getWorldBorder().maxX() - 16.0D);
        double d1 = MathHelper.clamp(this.posZ * moveFactor, worldserver1.getWorldBorder().minZ() + 16.0D, worldserver1.getWorldBorder().maxZ() - 16.0D);
        double d2 = 8.0D;*/

        cart.moveToBlockPosAndAngles(destPos, 90.0F, 0.0F);
        /*Teleporter teleporter = worldserver1.getDefaultTeleporter();
        teleporter.placeInExistingPortal(this, f);
        blockpos = new BlockPos(this);*/

        worldserver.updateEntityWithOptionalForce(cart, false);
        Entity entity = EntityList.newEntity(cart.getClass(), worldserver1);

        if(entity != null) {
            copyDataFromOld(entity, cart);

            entity.moveToBlockPosAndAngles(blockpos, entity.rotationYaw, entity.rotationPitch);

            IBlockState state = destination.getLoadedBlockState();
            if(state.getBlock() == ModBlocks.TELEPORT_RAIL) { //If the destination is a teleport track, use its rail direction to push the cart the right direction
                EnumFacing teleportDir = getTeleportDirection(state);
                double speed = 0.2;
                entity.motionX = teleportDir.getFrontOffsetX() * speed;
                entity.motionY = teleportDir.getFrontOffsetY() * speed;
                entity.motionZ = teleportDir.getFrontOffsetZ() * speed;
            } else {
                entity.motionX = entity.motionY = entity.motionZ = 0;
            }

            boolean flag = entity.forceSpawn;
            entity.forceSpawn = true;
            worldserver1.spawnEntity(entity);
            entity.forceSpawn = flag;
            worldserver1.updateEntityWithOptionalForce(entity, false);
        }

        cart.isDead = true;
        worldserver.resetUpdateEntityTick();
        worldserver1.resetUpdateEntityTick();
    }

    /**
     * Prepares this entity in new dimension by copying NBT data from entity in old dimension
     */
    private static void copyDataFromOld(Entity newEntity, Entity entityIn){
        NBTTagCompound nbttagcompound = entityIn.writeToNBT(new NBTTagCompound());
        nbttagcompound.removeTag("Dimension");
        newEntity.readFromNBT(nbttagcompound);
    }

    private Set<MCPos> getNeighboringTeleportRails(World world, BlockPos pos){
        Set<MCPos> ret = new HashSet<MCPos>();
        MCPos mcPos = new MCPos(world, pos);
        RailEdge<MCPos> edge = RailNetworkManager.getServerInstance().getNetwork().findEdge(mcPos);
        int startIndex = edge.getIndex(mcPos);
        Object thisRailType = edge.get(startIndex).getRailType();

        // travel backwards
        int curIndex = startIndex;
        while(curIndex >= 0) {
            NetworkRail<MCPos> neighborRail = edge.get(curIndex);
            if(!neighborRail.getRailType().equals(thisRailType) || neighborRail.getPos().getDimID() != world.provider.getDimension()) break;
            ret.add(neighborRail.getPos());
            curIndex--;
        }

        //travel forwards
        curIndex = startIndex + 1;
        while(curIndex < edge.length) {
            NetworkRail<MCPos> neighborRail = edge.get(curIndex);
            if(!neighborRail.getRailType().equals(thisRailType) || neighborRail.getPos().getDimID() != world.provider.getDimension()) break;
            ret.add(neighborRail.getPos());
            curIndex++;
        }

        return ret;
    }

    private void spawnParticle(EntityMinecart cart){
        Random rand = cart.world.rand;
        float maxSpeed = 1;
        float f = (rand.nextFloat() - 0.5F) * maxSpeed;
        float f1 = (rand.nextFloat() - 0.5F) * maxSpeed;
        float f2 = (rand.nextFloat() - 0.5F) * maxSpeed;
        NetworkHandler.sendToAllAround(new PacketSpawnParticle(EnumParticleTypes.PORTAL, cart.posX, cart.posY, cart.posZ, f, f1, f2), cart.world);
    }

    @Override
    public IProperty<EnumRailDirection> getShapeProperty(){
        return BlockRailPowered.SHAPE;
    }

    @Override
    protected BlockStateContainer createBlockState(){
        return new BlockStateContainer(this, getShapeProperty(), FORWARD);
    }

    /**
     * Convert the given metadata into a BlockState for this Block
     */
    @Override
    public IBlockState getStateFromMeta(int meta){
        return this.getDefaultState().withProperty(getShapeProperty(), BlockRailBase.EnumRailDirection.byMetadata(meta >> 1)).withProperty(FORWARD, (meta & 1) > 0);
    }

    /**
     * Convert the BlockState into the correct metadata value
     */
    @Override
    public int getMetaFromState(IBlockState state){
        return (state.getValue(getShapeProperty()).getMetadata() << 1) + (state.getValue(FORWARD) ? 1 : 0);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta){
        return new TileEntityTeleportRail();
    }

    /**
     * Called serverside after this block is replaced with another in Chunk, but before the Tile Entity is updated
     */
    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state){
        super.breakBlock(worldIn, pos, state);
        worldIn.removeTileEntity(pos);
    }

    private EnumFacing getTeleportDirection(IBlockState state){
        EnumRailDirection railDir = state.getValue(getShapeProperty());
        boolean forward = state.getValue(FORWARD);
        switch(railDir){
            case EAST_WEST:
                return forward ? EnumFacing.EAST : EnumFacing.WEST;
            case ASCENDING_EAST:
                return forward ? EnumFacing.EAST : EnumFacing.WEST;
            case ASCENDING_WEST:
                return forward ? EnumFacing.WEST : EnumFacing.EAST;
            case NORTH_SOUTH:
                return forward ? EnumFacing.NORTH : EnumFacing.SOUTH;
            case ASCENDING_NORTH:
                return forward ? EnumFacing.NORTH : EnumFacing.SOUTH;
            case ASCENDING_SOUTH:
                return forward ? EnumFacing.SOUTH : EnumFacing.NORTH;
            default:
                throw new IllegalStateException("Invalid rail dir for teleport track: " + railDir);
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ){
        if(playerIn.getHeldItem(hand).getItem() == Items.STICK) { //TODO other usage
            if(!worldIn.isRemote) {
                worldIn.setBlockState(pos, state.cycleProperty(FORWARD));
            }
            return true;
        } else {
            return false;
        }
    }
}
