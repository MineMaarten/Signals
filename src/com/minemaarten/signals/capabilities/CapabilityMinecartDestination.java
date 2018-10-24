package com.minemaarten.signals.capabilities;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.minecraft.block.BlockHopper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.entity.minecart.MinecartUpdateEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.minemaarten.signals.api.IRail;
import com.minemaarten.signals.api.access.IDestinationAccessor;
import com.minemaarten.signals.api.access.ISignal.EnumLampStatus;
import com.minemaarten.signals.chunkloading.ChunkLoadManager;
import com.minemaarten.signals.config.SignalsConfig;
import com.minemaarten.signals.init.ModItems;
import com.minemaarten.signals.inventory.EngineItemHandler;
import com.minemaarten.signals.lib.Log;
import com.minemaarten.signals.lib.SignalsUtils;
import com.minemaarten.signals.network.GuiSynced;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.PacketSpawnParticle;
import com.minemaarten.signals.network.PacketUpdateMinecartEngineState;
import com.minemaarten.signals.rail.RailManager;
import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.rail.network.NetworkSignal;
import com.minemaarten.signals.rail.network.NetworkState;
import com.minemaarten.signals.rail.network.RailNetwork;
import com.minemaarten.signals.rail.network.mc.MCPos;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;
import com.minemaarten.signals.tileentity.IGUITextFieldSensitive;

public class CapabilityMinecartDestination implements IGUITextFieldSensitive, IDestinationAccessor{
    @CapabilityInject(CapabilityMinecartDestination.class)
    public static Capability<CapabilityMinecartDestination> INSTANCE;
    private static final Pattern EMPTY_PATTERN = Pattern.compile("");

    @GuiSynced
    private String destinationStations = ""; //'\n' separated list of destinations
    private Pattern[] destinationRegexes = new Pattern[0]; //Cache of the regexes of the destinations
    @GuiSynced
    private int curDestinationIndex;
    @GuiSynced
    private String invalidDestinations = ""; //Destinations become invalid when the regex is invalid.

    private boolean chunkloading; //True when a ChunkLoader upgrade has been applied.
    private String chunkloadingPlayer = ""; //The player that keeps this cart chunkloaded.
    private boolean motorized; //True when an engine upgrade has been applied.
    @GuiSynced
    private int fuelLeft;
    @GuiSynced
    private int totalBurnTime;
    private int hopperTimer;

    private final InventoryBasic fuelInv = new InventoryBasic("cartEngineInv", true, 5){
        @Override
        public boolean isItemValidForSlot(int index, ItemStack stack){
            return stack == null || TileEntityFurnace.isItemFuel(stack);
        }
    };
    private final IItemHandler fuelItemHandler = new InvWrapper(fuelInv);
    private final IItemHandler engineItemHandler = new EngineItemHandler(this, fuelItemHandler);
    private boolean motorActive;
    public boolean travelingBetweenDimensions;

    public static void register(){
        CapabilityManager.INSTANCE.register(CapabilityMinecartDestination.class, new Capability.IStorage<CapabilityMinecartDestination>(){
            @Override
            public NBTBase writeNBT(Capability<CapabilityMinecartDestination> capability, CapabilityMinecartDestination instance, EnumFacing side){
                NBTTagCompound tag = new NBTTagCompound();

                tag.setString("destinations", instance.destinationStations);
                tag.setInteger("destIndex", instance.curDestinationIndex);

                tag.setBoolean("chunkloading", instance.chunkloading);
                tag.setString("chunkloadingPlayer", instance.chunkloadingPlayer);
                tag.setBoolean("motorized", instance.motorized);
                if(instance.motorized) {
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
                instance.recompileRegexes();
                instance.curDestinationIndex = tag.getInteger("destIndex");

                instance.chunkloading = tag.getBoolean("chunkloading");
                instance.chunkloadingPlayer = tag.getString("chunkloadingPlayer");
                instance.motorized = tag.getBoolean("motorized");
                if(instance.motorized) {
                    instance.fuelLeft = tag.getInteger("fuelLeft");
                    instance.totalBurnTime = tag.getInteger("totalBurnTime");
                    SignalsUtils.readInventoryFromNBT(tag, instance.fuelInv);
                }
            }
        }, CapabilityMinecartDestination::new);
    }

    @Override
    public void setText(int textFieldID, String text){
        destinationStations = text;
        recompileRegexes();
    }

    @Override
    public void setDestinations(String... destinations){
        Validate.noNullElements(destinations, "The destinations array contains null at position %d");

        destinationStations = StringUtils.joinWith("\n", (Object[])destinations);
        recompileRegexes();
    }

    private void recompileRegexes(){
        String[] destinations = getDestinations();
        destinationRegexes = new Pattern[destinations.length];
        invalidDestinations = "";
        for(int i = 0; i < destinations.length; i++) {
            try {
                destinationRegexes[i] = Pattern.compile(destinations[i]);
            } catch(PatternSyntaxException e) {
                if(!invalidDestinations.equals("")) {
                    invalidDestinations += ",";
                }
                invalidDestinations += "" + i;
                destinationRegexes[i] = EMPTY_PATTERN;
            }
        }
        getCurrentDestination(); //Update to a valid destination index.
    }

    @Override
    public int[] getInvalidDestinationIndeces(){
        if(invalidDestinations.equals("")) return new int[0];
        String[] strings = invalidDestinations.split(",");
        int[] ints = new int[strings.length];
        for(int i = 0; i < strings.length; i++) {
            ints[i] = Integer.parseInt(strings[i]);
        }
        return ints;
    }

    @Override
    public String getText(int textFieldID){
        return destinationStations;
    }

    public String getDestination(int index){
        return getDestinations()[index];
    }

    @Override
    public String[] getDestinations(){
        return destinationStations.equals("") ? new String[0] : destinationStations.split("\n");
    }

    @Override
    public int getTotalDestinations(){
        return getDestinations().length;
    }

    @Override
    public String getCurrentDestination(){
        String[] destinations = getDestinations();
        if(curDestinationIndex >= destinations.length || curDestinationIndex == -1) nextDestination();
        return curDestinationIndex >= 0 ? destinations[curDestinationIndex] : "";
    }

    @Override
    public int getDestinationIndex(){
        getCurrentDestination(); //Trigger updating destination index.
        return curDestinationIndex;
    }

    public void nextDestination(){
        setCurrentDestinationIndex(curDestinationIndex + 1);
    }

    @Override
    public void setCurrentDestinationIndex(int index){
        String[] destinations = getDestinations();
        if(index >= destinations.length || index < 0) {
            curDestinationIndex = destinations.length > 0 ? 0 : -1;
        } else {
            curDestinationIndex = index;
        }
    }

    public Pattern getCurrentDestinationRegex(){
        getCurrentDestination();
        return curDestinationIndex >= 0 ? destinationRegexes[curDestinationIndex] : EMPTY_PATTERN;
    }

    public boolean setChunkloading(EntityPlayer associatedPlayer, EntityMinecart cart){
        chunkloadingPlayer = associatedPlayer.getGameProfile().getId().toString();
        if(ChunkLoadManager.INSTANCE.markAsChunkLoader(associatedPlayer, cart)) {
            chunkloading = true;
            return true;
        } else {
            return false;
        }
    }

    public boolean isChunkLoading(){
        return chunkloading;
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
        if(motorized) {
            if(fuelLeft == 0) {
                for(int i = 0; i < fuelInv.getSizeInventory(); i++) {
                    ItemStack fuel = fuelInv.getStackInSlot(i);
                    if(!fuel.isEmpty()) {
                        int fuelValue = TileEntityFurnace.getItemBurnTime(fuel);
                        if(fuelValue > 0) {
                            fuel.shrink(1);
                            if(fuel.isEmpty()) {
                                fuelInv.setInventorySlotContents(i, fuel.getItem().getContainerItem(fuel));
                            }
                            fuelLeft += fuelValue;
                            totalBurnTime = fuelValue;
                            break;
                        }
                    }
                }
            }
            if(fuelLeft > 0) {
                fuelLeft--;
                double randX = cart.getPositionVector().x + (cart.world.rand.nextDouble() - 0.5) * 0.5;
                double randY = cart.getPositionVector().y + (cart.world.rand.nextDouble() - 0.5) * 0.5;
                double randZ = cart.getPositionVector().z + (cart.world.rand.nextDouble() - 0.5) * 0.5;
                NetworkHandler.sendToAllAround(new PacketSpawnParticle(EnumParticleTypes.SMOKE_LARGE, randX, randY, randZ, 0, 0, 0), cart.world);
                return true;
            }
        }
        return false;
    }

    public InventoryBasic getFuelInv(){
        return fuelInv;
    }

    public IItemHandler getEngineItemHandler(){
        return motorized ? fuelItemHandler : engineItemHandler;
    }

    public int getScaledFuel(int barLength){
        return totalBurnTime == 0 ? 0 : barLength * fuelLeft / totalBurnTime;
    }

    public void onCartBroken(EntityMinecart cart){
        if(!travelingBetweenDimensions) {
            if(motorized) {
                motorized = false;
                cart.dropItem(ModItems.CART_ENGINE, 1);
                for(int i = 0; i < fuelInv.getSizeInventory(); i++) {
                    ItemStack fuel = fuelInv.getStackInSlot(i);
                    if(fuel != null) cart.entityDropItem(fuel, 0);
                }
            }
        }

        if(chunkloading) {
            ChunkLoadManager.INSTANCE.unmarkAsChunkLoader(cart);
            if(!travelingBetweenDimensions) {
                chunkloading = false;
                if(!SignalsConfig.disableChunkLoaderUpgrades) cart.dropItem(ModItems.CHUNKLOADER_UPGRADE, 1);
            }
        }

        travelingBetweenDimensions = false;
    }

    public void onCartJoinWorld(EntityMinecart cart){
        if(chunkloading) {
            if(!ChunkLoadManager.INSTANCE.markAsChunkLoader(chunkloadingPlayer, cart)) {
                Log.warning("Could not chunkload cart for player '" + chunkloadingPlayer + "'!");
            }
        }
    }

    public void setEngineActive(boolean active){
        motorActive = active;
    }

    public void onCartUpdate(MinecartUpdateEvent event){
        EntityMinecart cart = event.getMinecart();
        if(!cart.world.isRemote) {
            if(isMotorized()) {
                boolean shouldRun = true;
                EnumFacing cartDir = cart.getAdjustedHorizontalFacing();
                if(new Vec3d(cart.motionX, cart.motionY, cart.motionZ).lengthVector() < 0.05) {
                    shouldRun = false;
                    if(hopperTimer > 0) {
                        hopperTimer--;
                    }
                    if(hopperTimer == 0) {
                        hopperTimer = extractFuelFromHopper(cart, event.getPos()) ? 8 : 40;
                    }
                } else {
                    hopperTimer = 0;

                    NetworkRail<MCPos> rail = RailNetworkManager.getInstance(cart.world.isRemote).getRail(cart.world, event.getPos());
                    if(rail == null) { //When not traveling over a Signals managed rail network 
                        //Try to look up a rail using block states.
                        IBlockState state = cart.world.getBlockState(event.getPos());
                        IRail r = RailManager.getInstance().getRail(cart.world, event.getPos(), state);
                        shouldRun = r != null; //Power the engine when a rail is found
                    } else {
                        RailNetwork<MCPos> network = RailNetworkManager.getInstance(cart.world.isRemote).getNetwork();
                        NetworkSignal<MCPos> signal = network.railObjects.getNeighborSignals(rail.getPotentialNeighborObjectLocations()).filter(s -> s.getRailPos().equals(rail.getPos())).findFirst().orElse(null);

                        NetworkState<MCPos> state = RailNetworkManager.getInstance(cart.world.isRemote).getState();
                        shouldRun = signal == null || state.getLampStatus(signal.getPos()) == EnumLampStatus.GREEN;
                        if(!shouldRun) {
                            cart.motionX = 0;
                            cart.motionZ = 0;
                        }
                    }
                }

                if(shouldRun && useFuel(cart)) {
                    if(!motorActive) NetworkHandler.sendToAllAround(new PacketUpdateMinecartEngineState(cart, true), new NetworkRegistry.TargetPoint(cart.world.provider.getDimension(), cart.getPositionVector().x, cart.getPositionVector().y, cart.getPositionVector().z, 64));
                    motorActive = true;

                    double acceleration = 0.03D;
                    cart.motionX += cartDir.getFrontOffsetX() * acceleration;
                    cart.motionZ += cartDir.getFrontOffsetZ() * acceleration;
                    cart.motionX = MathHelper.clamp(cart.motionX, -cart.getMaxCartSpeedOnRail(), cart.getMaxCartSpeedOnRail());
                    cart.motionZ = MathHelper.clamp(cart.motionZ, -cart.getMaxCartSpeedOnRail(), cart.getMaxCartSpeedOnRail());
                } else {
                    if(motorActive) NetworkHandler.sendToAllAround(new PacketUpdateMinecartEngineState(cart, true), new NetworkRegistry.TargetPoint(cart.world.provider.getDimension(), cart.getPositionVector().x, cart.getPositionVector().y, cart.getPositionVector().z, 64));
                    motorActive = false;
                }
            }

            if(chunkloading) {
                double x = cart.posX + cart.world.rand.nextDouble() - 0.5;
                double y = cart.posY + cart.world.rand.nextDouble() - 0.5;
                double z = cart.posZ + cart.world.rand.nextDouble() - 0.5;
                NetworkHandler.sendToAllAround(new PacketSpawnParticle(EnumParticleTypes.PORTAL, x, y, z, 0, 0, 0), cart.world);

                if(SignalsConfig.disableChunkLoaderUpgrades) {
                    ChunkLoadManager.INSTANCE.unmarkAsChunkLoader(cart);
                    chunkloading = false;
                }
            }
        } else {
            if(motorActive) {
                cart.world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, cart.getPositionVector().x, cart.getPositionVector().y, cart.getPositionVector().z, 0, 0, 0);
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
        for(EnumFacing dir : EnumFacing.VALUES) {
            BlockPos neighbor = pos;
            for(int offsetTimes = 0; offsetTimes < (dir == EnumFacing.UP ? 2 : 1); offsetTimes++) {
                neighbor = neighbor.offset(dir);
                TileEntity te = cart.world.getTileEntity(neighbor);
                if(te instanceof TileEntityHopper) {
                    EnumFacing hopperDir = cart.world.getBlockState(neighbor).getValue(BlockHopper.FACING);
                    if(hopperDir.getOpposite() == dir) {
                        TileEntityHopper hopper = (TileEntityHopper)te;
                        for(int i = 0; i < hopper.getSizeInventory(); i++) {
                            ItemStack stack = hopper.getStackInSlot(i);
                            if(!stack.isEmpty() && getFuelInv().isItemValidForSlot(0, stack)) {
                                ItemStack inserted = stack.copy();
                                inserted.setCount(1);
                                ItemStack left = ItemHandlerHelper.insertItemStacked(getEngineItemHandler(), inserted, false);
                                if(left.isEmpty()) {
                                    stack.shrink(1);
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
