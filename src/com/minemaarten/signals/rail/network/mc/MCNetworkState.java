package com.minemaarten.signals.rail.network.mc;

import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.PacketUpdateTrainPath;
import com.minemaarten.signals.rail.network.NetworkState;
import com.minemaarten.signals.rail.network.RailRoute;
import com.minemaarten.signals.rail.network.Train;

public class MCNetworkState extends NetworkState<MCPos>{
    @Override
    protected void onCartRouted(Train<MCPos> train, RailRoute<MCPos> route){
        super.onCartRouted(train, route);
        NetworkHandler.sendToAll(new PacketUpdateTrainPath((MCTrain)train));
    }
}
