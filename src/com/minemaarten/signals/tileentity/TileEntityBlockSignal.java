package com.minemaarten.signals.tileentity;

import com.minemaarten.signals.rail.network.NetworkSignal.EnumSignalType;

public class TileEntityBlockSignal extends TileEntitySignalBase{

    @Override
    public EnumSignalType getSignalType(){
        return EnumSignalType.BLOCK;
    }
}
