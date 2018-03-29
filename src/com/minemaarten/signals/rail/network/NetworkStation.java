package com.minemaarten.signals.rail.network;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import com.minemaarten.signals.rail.NetworkController;

public abstract class NetworkStation<TPos extends IPosition<TPos>> extends NetworkObject<TPos>{

    public final String stationName;

    public NetworkStation(TPos pos, String stationName){
        super(pos);
        Validate.notNull(stationName);
        this.stationName = stationName;
    }

    public boolean isTrainApplicable(Train<TPos> train, Pattern destinationRegex){
        return destinationRegex.matcher(stationName).matches();
    }

    public abstract List<TPos> getConnectedRailPositions(RailNetwork<TPos> network);

    @Override
    public int getColor(){
        return NetworkController.STATION_COLOR;
    }

    @Override
    public boolean equals(Object obj){
        return super.equals(obj) && obj instanceof NetworkStation && ((NetworkStation<?>)obj).stationName.equals(stationName);
    }

    @Override
    public int hashCode(){
        return super.hashCode() * 31 + stationName.hashCode();
    }

    @Override
    public String toString(){
        return super.toString() + "(Station: " + stationName + ")";
    }
}
