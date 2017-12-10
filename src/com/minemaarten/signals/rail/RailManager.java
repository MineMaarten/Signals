package com.minemaarten.signals.rail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.ASMDataTable.ASMData;

import com.minemaarten.signals.SignalsAccessor;
import com.minemaarten.signals.api.ICartHopperBehaviour;
import com.minemaarten.signals.api.ICartLinker;
import com.minemaarten.signals.api.IRail;
import com.minemaarten.signals.api.IRailMapper;
import com.minemaarten.signals.api.Signals;
import com.minemaarten.signals.api.access.SignalsAccessorProvidingEvent;
import com.minemaarten.signals.api.tileentity.IDestinationProvider;
import com.minemaarten.signals.capabilities.CapabilityDestinationProvider;
import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.lib.Constants;
import com.minemaarten.signals.lib.Log;
import com.minemaarten.signals.rail.DestinationPathFinder.AStarRailNode;

public class RailManager{
    private static final RailManager INSTANCE = new RailManager();

    private final List<IRailMapper> railMappers = new ArrayList<>();
    private final Map<Block, IRail> blockToRails = new HashMap<>();
    private final List<IDestinationProvider> destinationProviders = new ArrayList<>();
    private final List<ICartHopperBehaviour<?>> hopperBehaviours = new ArrayList<>();
    private final List<ICartLinker> cartLinkers = new ArrayList<>();

    public static RailManager getInstance(){
        return INSTANCE;
    }

    public void initializeAPIImplementors(ASMDataTable asmData){
        Set<ASMData> allAnnotatedClasses = asmData.getAll(Signals.class.getName());
        for(ASMData annotatedClass : allAnnotatedClasses) {
            try {
                Class<?> clazz = Class.forName(annotatedClass.getClassName());
                Log.info("Found class annotating @SignalRail : " + annotatedClass.getClassName());

                if(IRail.class.isAssignableFrom(clazz)) {
                    IRail rail = (IRail)clazz.newInstance();
                    for(Block applicableBlock : rail.getApplicableBlocks()) {
                        if(applicableBlock != null) {
                            registerRail(applicableBlock, rail);
                        } else {
                            Log.warning("IRail \"" + annotatedClass.getClassName() + "\" returned a null block as applicable!");
                        }
                    }
                    Log.info("Successfully registered the IRail for \"" + annotatedClass.getClassName() + "\".");
                }

                if(IRailMapper.class.isAssignableFrom(clazz)) {
                    IRailMapper railMapper = (IRailMapper)clazz.newInstance();
                    registerCustomRailMapper(railMapper);
                    Log.info("Successfully registered the IRailMapper for \"" + annotatedClass.getClassName() + "\".");
                }

                if(IDestinationProvider.class.isAssignableFrom(clazz)) {
                    IDestinationProvider destinationProvider = (IDestinationProvider)clazz.newInstance();
                    destinationProviders.add(destinationProvider);
                    Log.info("Successfully registered the IDestinationProvider for \"" + annotatedClass.getClassName() + "\".");
                }

                if(ICartHopperBehaviour.class.isAssignableFrom(clazz)) {
                    ICartHopperBehaviour<?> hopperBehaviour = (ICartHopperBehaviour<?>)clazz.newInstance();
                    hopperBehaviours.add(hopperBehaviour);
                    Log.info("Successfully registered the ICartHopperBehaviour for \"" + annotatedClass.getClassName() + "\".");
                }

                if(ICartLinker.class.isAssignableFrom(clazz)) {
                    ICartLinker cartLinker = (ICartLinker)clazz.newInstance();
                    cartLinkers.add(cartLinker);
                    Log.info("Successfully registered the ICartLinker for \"" + annotatedClass.getClassName() + "\".");
                }

                //Log.error("Annotated class \"" + annotatedClass.getClassName() + "\" is not implementing IRail, IRailMapper nor IDestinationProvider!");
            } catch(ClassNotFoundException e) {
                e.printStackTrace();
            } catch(IllegalAccessException e) {
                Log.error("Annotated class \"" + annotatedClass.getClassName() + "\" could not be instantiated, probably because it is not marked public!");
                e.printStackTrace();
            } catch(InstantiationException e) {
                Log.error("Annotated class \"" + annotatedClass.getClassName() + "\" could not be instantiated, probably because it either does not have a constructor without arguments, or because the class is abstract!");
                e.printStackTrace();
            }
        }

        MinecraftForge.EVENT_BUS.post(new SignalsAccessorProvidingEvent(new SignalsAccessor()));
    }

    private void registerRail(Block railBlock, IRail rail){
        if(railBlock == null) throw new NullPointerException("Block is null!");
        if(rail == null) throw new NullPointerException("Rail is null!");
        blockToRails.put(railBlock, rail);
    }

    private void registerCustomRailMapper(IRailMapper rail){
        if(rail == null) throw new NullPointerException("Rail Mapper is null!");
        railMappers.add(rail);
    }

    public IRail getRailSimple(Block block){
        return blockToRails.get(block);
    }

    public IRail getRail(World world, BlockPos pos, IBlockState state){
        IRail rail = getRailSimple(state.getBlock());
        if(rail != null) return rail;
        for(IRailMapper mapper : railMappers) {
            rail = mapper.getRail(world, pos, state);
            if(rail != null) return rail;
        }
        return null;
    }

    public void onTileEntityCapabilityAttachEvent(AttachCapabilitiesEvent<TileEntity> event){
        ICapabilityProvider provider = new CapabilityDestinationProvider.Provider();
        boolean requiresCap = false;

        CapabilityDestinationProvider cap = provider.getCapability(CapabilityDestinationProvider.INSTANCE, null);
        for(IDestinationProvider destinationProvider : destinationProviders) {
            if(destinationProvider.isTileEntityApplicable(event.getObject())) {
                try {
                    cap.addDestinationProvider(destinationProvider.getClass().newInstance());
                    if(!requiresCap) {
                        requiresCap = true;
                        event.addCapability(new ResourceLocation(Constants.MOD_ID, "destinationProviderCapability"), provider);
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<ICartHopperBehaviour<?>> getHopperBehaviours(){
        return hopperBehaviours;
    }

    public boolean areLinked(EntityMinecart cart1, EntityMinecart cart2){
        return cartLinkers.stream().anyMatch(x -> x.getLinkedCarts(cart1).contains(cart2));
    }

    public AStarRailNode getPath(EntityMinecart cart){
        Stream<EntityMinecart> linkedCarts = cartLinkers.stream().flatMap(cartLinker -> cartLinker.getLinkedCarts(cart).stream());
        linkedCarts = Stream.concat(Stream.of(cart), linkedCarts).distinct(); //Append the passed cart, just in case.

        return linkedCarts.map(this::getStoredPath).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private AStarRailNode getStoredPath(EntityMinecart cart){
        return cart.getCapability(CapabilityMinecartDestination.INSTANCE, null).getPath(cart.world);
    }
}
