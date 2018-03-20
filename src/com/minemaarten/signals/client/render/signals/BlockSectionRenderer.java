package com.minemaarten.signals.client.render.signals;

import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.rail.network.RailObjectHolder;
import com.minemaarten.signals.rail.network.RailSection;
import com.minemaarten.signals.rail.network.mc.MCPos;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public class BlockSectionRenderer extends AbstractRailRenderer<RailSection<MCPos>>{

    @Override
    protected boolean isAdjacent(RailSection<MCPos> s1, RailSection<MCPos> s2){
        return s1.isAdjacent(s2);
    }

    @Override
    protected Iterable<RailSection<MCPos>> getRenderableSections(){
        return RailNetworkManager.getInstance().getNetwork().getAllSections();
    }

    @Override
    protected NetworkRail<MCPos> getRootNode(RailSection<MCPos> section){
        return section.iterator().next();
    }

    @Override
    protected RailObjectHolder<MCPos> getNeighborProvider(RailSection<MCPos> section){
        return RailNetworkManager.getInstance().getNetwork().railObjects;
    }

    @Override
    protected boolean shouldTraverse(RailSection<MCPos> section, NetworkRail<MCPos> rail){
        return section.containsRail(rail.pos);
    }
}
