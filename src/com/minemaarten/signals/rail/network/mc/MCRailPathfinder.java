package com.minemaarten.signals.rail.network.mc;

import java.util.regex.Pattern;

import net.minecraft.entity.item.EntityMinecart;

import com.minemaarten.signals.rail.RailCacheManager;
import com.minemaarten.signals.rail.network.EnumHeading;
import com.minemaarten.signals.rail.network.NetworkState;
import com.minemaarten.signals.rail.network.RailNetwork;
import com.minemaarten.signals.rail.network.RailPathfinder;
import com.minemaarten.signals.rail.network.RailRoute;

public class MCRailPathfinder extends RailPathfinder<MCPos>{

    public MCRailPathfinder(RailNetwork<MCPos> network, NetworkState<MCPos> state){
        super(network, state);
    }

    /**
     * @param start
     * @param destination
     * @return Returns the first node starting with a signal (or destination), up to the 'start'.
     */
    public RailRoute<MCPos> pathfindToDestination(MCPos start, EntityMinecart cart, Pattern destinationRegex, EnumHeading direction){
        return pathfindToDestination(start, direction, RailCacheManager.getInstance(start.getWorld()).getStationRails(cart, destinationRegex));
    }
}
