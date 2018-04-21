package com.minemaarten.signals.rail.network.mc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.minemaarten.signals.api.IRail;
import com.minemaarten.signals.lib.HeadingUtils;
import com.minemaarten.signals.rail.RailManager;
import com.minemaarten.signals.rail.network.INetworkObjectProvider;
import com.minemaarten.signals.rail.network.NetworkObject;
import com.minemaarten.signals.tileentity.TileEntityRailLink;
import com.minemaarten.signals.tileentity.TileEntitySignalBase;
import com.minemaarten.signals.tileentity.TileEntityStationMarker;

public class NetworkObjectProvider implements INetworkObjectProvider<MCPos>{

    @Override
    public NetworkObject<MCPos> provide(MCPos pos){
        World world = pos.getWorld();
        return world == null ? null : provide(world, pos.getPos());
    }

    public NetworkObject<MCPos> provide(World world, BlockPos pos){
        MCPos mcPos = new MCPos(world.provider.getDimension(), pos);
        IBlockState state = world.getBlockState(pos);
        IRail rail = RailManager.getInstance().getRail(world, pos, state);
        if(rail != null) {
            return new MCNetworkRail(mcPos, state.getBlock(), rail.getDirection(world, pos, state), rail.getValidDirections(world, pos, state));
        }

        TileEntity te = world.getTileEntity(pos);
        if(te instanceof TileEntityRailLink) {
            TileEntityRailLink railLink = (TileEntityRailLink)te;
            MCPos linkedPos = railLink.getLinkedPosition();
            if(linkedPos != null) return new MCNetworkRailLink(mcPos, linkedPos, railLink.getHoldDelay());
        }

        if(te instanceof TileEntitySignalBase) {
            TileEntitySignalBase signal = (TileEntitySignalBase)te;
            return new MCNetworkSignal(mcPos, HeadingUtils.fromFacing(signal.getFacing()), signal.getSignalType());
        }

        if(te instanceof TileEntityStationMarker) {
            TileEntityStationMarker stationMarker = (TileEntityStationMarker)te;
            return new MCNetworkStation(mcPos, stationMarker.getStationName());
        }

        return null;
    }

    public Set<MCTrain> provideTrains(List<EntityMinecart> carts){
        List<List<EntityMinecart>> cartGroups = new ArrayList<>();
        for(EntityMinecart cart : carts) {
            List<EntityMinecart> cartGroup = cartGroups.stream().filter(c -> RailManager.getInstance().areLinked(c.get(0), cart)).findFirst().orElse(null);
            if(cartGroup == null) {
                cartGroup = new ArrayList<>();
                cartGroups.add(cartGroup);
            }
            cartGroup.add(cart);
        }

        return cartGroups.stream().map(MCTrain::new).collect(Collectors.toSet());
    }

    @Override
    public NetworkObject<MCPos> provideRemovalMarker(MCPos pos){
        return new NetworkRemovalMarker(pos);
    }
}
