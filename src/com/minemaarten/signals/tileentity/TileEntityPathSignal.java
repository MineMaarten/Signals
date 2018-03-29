package com.minemaarten.signals.tileentity;

import com.minemaarten.signals.rail.network.NetworkSignal.EnumSignalType;

public class TileEntityPathSignal extends TileEntitySignalBase{

    @Override
    public EnumSignalType getSignalType(){
        return EnumSignalType.BLOCK; //TODO throw new NotImplementedException("Path signals are not supported!");
    }

}
