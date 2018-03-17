package com.minemaarten.signals.util.railnode;

import com.minemaarten.signals.rail.network.IRemovalMarker;
import com.minemaarten.signals.rail.network.NetworkObject;
import com.minemaarten.signals.util.Pos2D;

public class TestRemovalNode extends NetworkObject<Pos2D> implements IRemovalMarker{

    public TestRemovalNode(Pos2D pos){
        super(pos);
    }

}
