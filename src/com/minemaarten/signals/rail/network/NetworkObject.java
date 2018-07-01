package com.minemaarten.signals.rail.network;

import java.util.List;

import com.minemaarten.signals.rail.NetworkController;

/**
 * Any object that participates in a rail network, currently: rails, signals and station markers.
 * Immutable, and should expect to be reused in transitions from old to new networks to prevent allocations.
 * @author Maarten
 *
 * @param <TPos>
 */
public abstract class NetworkObject<TPos> {
    public final TPos pos;

    public NetworkObject(TPos pos){
        this.pos = pos;
    }

    public int getColor(){
        return NetworkController.NOTHING_COLOR;
    }

    public abstract List<TPos> getNetworkNeighbors();

    @Override
    public String toString(){
        return pos.toString();
    }

    /**
     * Should be overriden, to equal only when for the rail network, there's no noticeable difference. This is used to determine if a block update should cause the network to update.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj){
        return obj instanceof NetworkObject && ((NetworkObject<TPos>)obj).pos.equals(pos);
    }

    @Override
    public int hashCode(){
        return pos.hashCode() * 13; //Do multiply to prevent that key.hashCode() == value.hashCode(), because this will cancel out https://stackoverflow.com/questions/26599345/hashmap-yields-same-hashcode-for-different-contents
    }
}
