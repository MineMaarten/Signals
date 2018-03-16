package com.minemaarten.signals.rail.network.mc;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import net.minecraft.entity.item.EntityMinecart;

import com.minemaarten.signals.rail.network.RailRoute;
import com.minemaarten.signals.rail.network.RailSection;
import com.minemaarten.signals.rail.network.Train;

public class MCTrain extends Train<MCPos>{

    private final Set<UUID> cartIDs;
    private final Set<MCPos> positions;

    public MCTrain(List<EntityMinecart> carts){
        positions = carts.stream().map(c -> new MCPos(c.world, c.getPosition())).collect(Collectors.toSet());
        cartIDs = carts.stream().map(c -> c.getUniqueID()).collect(Collectors.toSet());
    }

    @Override
    public Set<MCPos> getPositions(){
        return positions;
    }

    @Override
    public RailRoute<MCPos> getCurRoute(){
        return null; //TODO
    }

    @Override
    public Set<RailSection<MCPos>> getClaimedSections(){
        return Collections.emptySet();//TODO
    }

    @Override
    public boolean equals(Object obj){
        return obj instanceof MCTrain && ((MCTrain)obj).cartIDs.equals(cartIDs);
    }

    @Override
    public int hashCode(){
        return cartIDs.hashCode();
    }
}
