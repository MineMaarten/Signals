package com.minemaarten.signals.tileentity;

import com.minemaarten.signals.rail.network.mc.MCPos;

public class TileEntityTeleportRail extends TileEntityRailLinkBase{
    @Override
    protected boolean isDestinationValid(MCPos destination){
        return true; //TODO only allow locations that would be allowed by portals
    }
}
