package com.minemaarten.signals.client.render.signals;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.rail.network.RailObjectHolder;
import com.minemaarten.signals.rail.network.mc.MCPos;
import com.minemaarten.signals.rail.network.mc.MCTrain;
import com.minemaarten.signals.rail.network.mc.MCTrainClient;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public class ClaimedPosRenderer extends AbstractRailRenderer<MCTrainClient>{

    @Override
    protected boolean canRender(MCTrainClient section){
        return !section.clientClaimedPositions.isEmpty();
    }

    @Override
    protected boolean isAdjacent(MCTrainClient s1, MCTrainClient s2){
        return false;
    }

    @Override
    protected Iterable<MCTrainClient> getRenderableSections(){
        Stream<MCTrain> allTrains = RailNetworkManager.getInstance().getAllTrains();
        return allTrains.map(t -> (MCTrainClient)t).filter(this::canRender).collect(Collectors.toList());
    }

    @Override
    protected NetworkRail<MCPos> getRootNode(MCTrainClient section){
        return (NetworkRail<MCPos>)RailNetworkManager.getInstance().getNetwork().railObjects.get(section.clientClaimedPositions.iterator().next());
    }

    @Override
    protected RailObjectHolder<MCPos> getNeighborProvider(MCTrainClient section){
        return RailNetworkManager.getInstance().getNetwork().railObjects.subSelectionForPos(section.clientClaimedPositions);
    }

    @Override
    protected boolean shouldTraverse(MCTrainClient section, NetworkRail<MCPos> rail){
        return true;
    }

    @Override
    public double getLineWidth(){
        return 0.075 / 2;
    }

    @Override
    public double getHeightOffset(){
        return 1;
    }
}
