package com.minemaarten.signals.util.railnode;

import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

import com.minemaarten.signals.rail.network.IRemovalMarker;
import com.minemaarten.signals.rail.network.NetworkObject;
import com.minemaarten.signals.util.Pos2D;

public class TestRemovalNode extends NetworkObject<Pos2D> implements IRemovalMarker{

    public TestRemovalNode(Pos2D pos){
        super(pos);
    }

    @Override
    public List<Pos2D> getNetworkNeighbors(){
        throw new NotImplementedException("");
    }
}
