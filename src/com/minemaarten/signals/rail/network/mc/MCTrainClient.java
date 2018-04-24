package com.minemaarten.signals.rail.network.mc;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.ImmutableSet;
import com.minemaarten.signals.client.ClientEventHandler;
import com.minemaarten.signals.rail.network.NetworkState;
import com.minemaarten.signals.rail.network.RailNetwork;
import com.minemaarten.signals.rail.network.RailRoute;

public class MCTrainClient extends MCTrain{
    public Set<MCPos> clientClaimedPositions = Collections.emptySet();

    public MCTrainClient(int id, ImmutableSet<UUID> cartIDs){
        super(id, cartIDs);
    }

    @Override
    protected void onPositionChanged(){
        //NOP
    }

    @Override
    protected void updateIntersections(){
        //NOP
    }

    @Override
    protected void updateClaimedSections(RailNetwork<MCPos> network){
        //NOP
    }

    @Override
    protected boolean trySetClaims(RailNetwork<MCPos> network, NetworkState<MCPos> state, RailRoute<MCPos> path){
        return true; //NOP
    }

    @Override
    public void setPath(RailRoute<MCPos> path){
        super.setPath(path);
        ClientEventHandler.INSTANCE.pathRenderer.updateSpecificSection(this);
        ClientEventHandler.INSTANCE.claimRenderer.updateSpecificSection(this);
    }
}
