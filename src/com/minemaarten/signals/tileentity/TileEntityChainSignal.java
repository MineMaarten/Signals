package com.minemaarten.signals.tileentity;

import com.minemaarten.signals.rail.network.NetworkSignal.EnumSignalType;

public class TileEntityChainSignal extends TileEntityBlockSignal{

    @Override
    public EnumSignalType getSignalType(){
        return EnumSignalType.CHAIN;
    }
}
