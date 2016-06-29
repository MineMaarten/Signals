package com.minemaarten.signals.capabilities;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import net.minecraft.block.BlockHopper;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.entity.minecart.MinecartUpdateEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;

import com.minemaarten.signals.block.BlockSignalBase.EnumLampStatus;
import com.minemaarten.signals.init.ModItems;
import com.minemaarten.signals.lib.SignalsUtils;
import com.minemaarten.signals.network.GuiSynced;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.PacketSpawnParticle;
import com.minemaarten.signals.network.PacketUpdateMinecartEngineState;
import com.minemaarten.signals.network.PacketUpdateMinecartPath;
import com.minemaarten.signals.rail.DestinationPathFinder.AStarRailNode;
import com.minemaarten.signals.rail.RailCacheManager;
import com.minemaarten.signals.rail.RailWrapper;
import com.minemaarten.signals.tileentity.IGUITextFieldSensitive;
import com.minemaarten.signals.tileentity.TileEntitySignalBase;

public class CapabilityMinecartDestination implements IGUITextFieldSensitive{
    @CapabilityInject(CapabilityMinecartDestination.class)
    public static Capability<CapabilityMinecartDestination> INSTANCE;

    @GuiSynced
    public String destinationStations = ""; //'\n' separated list of destinations
    @GuiSynced
    private int curDestinationIndex;

    private AStarRailNode curPath;
    private List<BlockPos> nbtLoadedPath;
    
    private boolean motorized; //True when an engine upgrade has been applied.
    @GuiSynced
    private int fuelLeft;
    @GuiSynced
    private int totalBurnTime;
    private int hopperTimer;
    
    private InventoryBasic fuelInv = new InventoryBasic("cartEngineInv", true, 5){
    	public boolean isItemValidForSlot(int index, ItemStack stack) {
    		return stack == null || TileEntityFurnace.isItemFuel(stack);
    	}
    };
    private boolean motorActive;
    public boolean travelingBetweenDimensions;

    public static void register(){
        CapabilityManager.INSTANCE.register(CapabilityMinecartDestination.class, new Capability.IStorage<CapabilityMinecartDestination>(){
            @Override
            public NBTBase writeNBT(Capability<CapabilityMinecartDestination> capability, CapabilityMinecartDestination instance, EnumFacing side){
                NBTTagCompound tag = new NBTTagCompound();

                tag.setString("destinations", instance.destinationStations);
                tag.setInteger("destIndex", instance.curDestinationIndex);

                if(instance.curPath != null) {
                    AStarRailNode curNode = instance.curPath;
                    NBTTagList nodeList = new NBTTagList();
                    while(curNode != null) {
                        NBTTagCompound nodeTag = new NBTTagCompound();
                        nodeTag.setInteger("x", curNode.getRail().getX());
                        nodeTag.setInteger("y", curNode.getRail().getY());
                        nodeTag.setInteger("z", curNode.getRail().getZ());
                        nodeList.appendTag(nodeTag);

                        curNode = curNode.getNextNode();
                    }
                    tag.setTag("path", nodeList);
                }
                
                tag.setBoolean("motorized", instance.motorized);
                if(instance.motorized){
                	tag.setInteger("fuelLeft", instance.fuelLeft);
                	tag.setInteger("totalBurnTime", instance.totalBurnTime);
                	SignalsUtils.writeInventoryToNBT(tag, instance.fuelInv);
                }
                
                return tag;
            }

            @Override
            public void readNBT(Capability<CapabilityMinecartDestination> capability, CapabilityMinecartDestination instance, EnumFacing side, NBTBase base){
                NBTTagCompound tag = (NBTTagCompound)base;

                instance.destinationStations = tag.getString("destinations");
                instance.curDestinationIndex = tag.getInteger("destIndex");

                if(tag.hasKey("path")) {
                    instance.nbtLoadedPath = new ArrayList<BlockPos>();
                    NBTTagList nodeList = tag.getTagList("path", 10);
                    for(int i = 0; i < nodeList.tagCount(); i++) {
                        NBTTagCompound nodeTag = nodeList.getCompoundTagAt(i);
                        instance.nbtLoadedPath.add(new BlockPos(nodeTag.getInteger("x"), nodeTag.getInteger("y"), nodeTag.getInteger("z")));
                    }
                } else {
                    instance.curPath = null;
                }
                
                instance.motorized = tag.getBoolean("motorized");
                if(instance.motorized){
                	instance.fuelLeft = tag.getInteger("fuelLeft");
                	instance.totalBurnTime = tag.getInteger("totalBurnTime");
                	SignalsUtils.readInventoryFromNBT(tag, instance.fuelInv);
                }
            }
        }, new Callable<CapabilityMinecartDestination>(){
            @Override
            public CapabilityMinecartDestination call() throws Exception{
                return new CapabilityMinecartDestination();
            }
        });
    }

    @Override
    public void setText(int textFieldID, String text){
        destinationStations = text;
    }

    @Override
    public String getText(int textFieldID){
        return destinationStations;
    }

    public String getDestination(int index){
        return getDestinations()[index];
    }

    private String[] getDestinations(){
        return destinationStations.equals("") ? new String[0] : destinationStations.split("\n");
    }

    public int getTotalDestinations(){
        return getDestinations().length;
    }

    public String getCurrentDestination(){
        String[] destinations = getDestinations();
        if(curDestinationIndex >= destinations.length || curDestinationIndex == -1) nextDestination();
        return curDestinationIndex >= 0 ? destinations[curDestinationIndex] : "";
    }

    public int getDestinationIndex(){
        return curDestinationIndex;
    }

    public void nextDestination(){
        String[] destinations = getDestinations();
        if(++curDestinationIndex >= destinations.length) {
            curDestinationIndex = destinations.length > 0 ? 0 : -1;
        }
    }

    public void setPath(EntityMinecart cart, AStarRailNode path){
        curPath = path;
        nbtLoadedPath = null;
        sendUpdatePacket(cart);
    }
    
    private void sendUpdatePacket(EntityMinecart cart){
    	NetworkHandler.sendToAll(new PacketUpdateMinecartPath(cart));
    }

    public AStarRailNode getPath(World world){
        if(nbtLoadedPath != null) {
            AStarRailNode prevNode = null;
            for(int i = nbtLoadedPath.size() - 1; i >= 0; i--) {
                AStarRailNode curNode = new AStarRailNode(RailCacheManager.getInstance(world).getRail(world, nbtLoadedPath.get(i)), null, null);
                if(prevNode != null) curNode.checkImprovementAndUpdate(prevNode);
                prevNode = curNode;
            }
            curPath = prevNode;
            nbtLoadedPath = null;
        }
        return curPath;
    }
    
    public List<BlockPos> getNBTPath(){
    	return nbtLoadedPath;
    }
    
    public void setMotorized(){
    	motorized = true;
    }
    
    public boolean isMotorized(){
    	return motorized;
    }
    
    /**
     * Tries to use fuel and returns true if succeeded.
     * @return
     */
    public boolean useFuel(EntityMinecart cart){
    	if(motorized){
    		if(fuelLeft == 0){
    			for(int i = 0; i < fuelInv.getSizeInventory(); i++){
    				ItemStack fuel = fuelInv.getStackInSlot(i);
    				if(fuel != null){
        				int fuelValue = TileEntityFurnace.getItemBurnTime(fuel);
        				if(fuelValue > 0){
        					fuel.stackSize--;
        					if(fuel.stackSize <= 0){
        						fuelInv.setInventorySlotContents(i, fuel.getItem().getContainerItem(fuel));
        					}
        					fuelLeft += fuelValue;
        					totalBurnTime = fuelValue;
        					break;
        				}
        			}
    			}
    		}
    		if(fuelLeft > 0){
    			fuelLeft--;
    			NetworkHandler.sendToAllAround(new PacketSpawnParticle(EnumParticleTypes.SMOKE_LARGE, cart.getPositionVector().xCoord, cart.getPositionVector().yCoord, cart.getPositionVector().zCoord, 0, 0, 0), cart.worldObj);
    			return true;
    		}
    	}
    	return false;
    }
    
    public InventoryBasic getFuelInv(){
    	return fuelInv;
    }
    
    public int getScaledFuel(int barLength){
    	return totalBurnTime == 0 ? 0 : barLength * fuelLeft / totalBurnTime;
    }
    
    public void onCartBroken(EntityMinecart cart){
    	if(motorized && !travelingBetweenDimensions){
    		motorized = false;
    		cart.dropItem(ModItems.cartEngine, 1);
    		for(int i = 0; i < fuelInv.getSizeInventory(); i++){
    			ItemStack fuel = fuelInv.getStackInSlot(i);
    			if(fuel != null) cart.entityDropItem(fuel, 0);
    		}
    	}
    	travelingBetweenDimensions = false;
    }

	public void setEngineActive(boolean active) {
		motorActive = active;
	}
	
	public void onCartUpdate(MinecartUpdateEvent event){
		EntityMinecart cart = event.getMinecart();
		if(!cart.worldObj.isRemote){
			if(isMotorized()){
				boolean shouldRun = true;
				EnumFacing cartDir = cart.getAdjustedHorizontalFacing();
				if(new Vec3d(cart.motionX, cart.motionY, cart.motionZ).lengthVector() < 0.05){
					shouldRun = false;
					if(hopperTimer > 0){
						hopperTimer--;
					}
					if(hopperTimer == 0){
						hopperTimer = extractFuelFromHopper(cart, event.getPos()) ? 8 : 40;
					}
				}else{
					hopperTimer = 0;
		    		RailWrapper rail = RailCacheManager.getInstance(cart.worldObj).getRail(cart.worldObj, event.getPos());
		    		
		    		if(rail == null){
		    			shouldRun = false;
		    		}else{
		    			TileEntitySignalBase signal = TileEntitySignalBase.getNeighborSignal(rail, cartDir.getOpposite());
			    		shouldRun = signal == null || signal.getLampStatus() != EnumLampStatus.RED;
		    			if(!shouldRun){
		    				cart.motionX = 0;
		    				cart.motionZ = 0;
		    			}
		    		}
				}
	    		
	    		if(shouldRun && useFuel(cart)){
	    			if(!motorActive) NetworkHandler.sendToAllAround(new PacketUpdateMinecartEngineState(cart, true), new NetworkRegistry.TargetPoint(cart.worldObj.provider.getDimension(), cart.getPositionVector().xCoord, cart.getPositionVector().yCoord, cart.getPositionVector().zCoord, 64));
	    			motorActive = true;
	    			
	    			double acceleration = 0.03D;
	    			cart.motionX += cartDir.getFrontOffsetX() * acceleration;
	    			cart.motionZ += cartDir.getFrontOffsetZ() * acceleration;
	    			cart.motionX = MathHelper.clamp_double(cart.motionX, -cart.getMaxCartSpeedOnRail(), cart.getMaxCartSpeedOnRail());
	    			cart.motionZ = MathHelper.clamp_double(cart.motionZ, -cart.getMaxCartSpeedOnRail(), cart.getMaxCartSpeedOnRail());
	    		}else{
	    			if(motorActive) NetworkHandler.sendToAllAround(new PacketUpdateMinecartEngineState(cart, true), new NetworkRegistry.TargetPoint(cart.worldObj.provider.getDimension(), cart.getPositionVector().xCoord, cart.getPositionVector().yCoord, cart.getPositionVector().zCoord, 64));
	    			motorActive = false;
	    		}
	    	}
		}else{
			if(motorActive){
				cart.worldObj.spawnParticle(EnumParticleTypes.SMOKE_LARGE, cart.getPositionVector().xCoord, cart.getPositionVector().yCoord, cart.getPositionVector().zCoord, 0, 0, 0);
			}
		}
	}
	
	/**
	 * 
	 * @param cart
	 * @return true if there was a valid hopper (not necessarily if extracted an item)
	 */
	private boolean extractFuelFromHopper(EntityMinecart cart, BlockPos pos){
		boolean foundHopper = false;
		for(EnumFacing dir : EnumFacing.VALUES){
			BlockPos neighbor = pos;
			for(int offsetTimes = 0; offsetTimes < (dir == EnumFacing.UP ? 2 : 1); offsetTimes++){
				neighbor = neighbor.offset(dir);
				TileEntity te = cart.worldObj.getTileEntity(neighbor);
				if(te instanceof TileEntityHopper){
					EnumFacing hopperDir = (EnumFacing)cart.worldObj.getBlockState(neighbor).getValue(BlockHopper.FACING);
					if(hopperDir.getOpposite() == dir){
						TileEntityHopper hopper = (TileEntityHopper)te;
						for(int i = 0; i < hopper.getSizeInventory(); i++){
							ItemStack stack = hopper.getStackInSlot(i);
							if(stack != null && getFuelInv().isItemValidForSlot(0, stack)){
								InvWrapper invWrapper = new InvWrapper(getFuelInv());
								ItemStack inserted = stack.copy();
								inserted.stackSize = 1;
								ItemStack left = ItemHandlerHelper.insertItemStacked(invWrapper, inserted, false);
								if(left == null){
									stack.stackSize--;
									if(stack.stackSize <= 0){
										hopper.setInventorySlotContents(i, null);
									}
									hopper.markDirty();
									return true;
								}
							}
						}
						foundHopper = true;
					}
				}
			}
		}
		return foundHopper;
	}

    public static class Provider implements ICapabilitySerializable<NBTBase>{
        private final CapabilityMinecartDestination cap = new CapabilityMinecartDestination();

        @Override
        public boolean hasCapability(Capability<?> capability, EnumFacing facing){
            return capability == INSTANCE;
        }

        @Override
        public <T> T getCapability(Capability<T> capability, EnumFacing facing){
            if(hasCapability(capability, facing)) {
                return (T)cap;
            } else {
                return null;
            }
        }

        @Override
        public NBTBase serializeNBT(){
            return INSTANCE.getStorage().writeNBT(INSTANCE, cap, null);
        }

        @Override
        public void deserializeNBT(NBTBase nbt){
            INSTANCE.getStorage().readNBT(INSTANCE, cap, null, nbt);
        }
    }
}
