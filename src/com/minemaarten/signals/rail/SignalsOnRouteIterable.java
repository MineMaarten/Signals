package com.minemaarten.signals.rail;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.minemaarten.signals.rail.DestinationPathFinder.AStarRailNode;
import com.minemaarten.signals.rail.SignalsOnRouteIterable.SignalOnRoute;
import com.minemaarten.signals.tileentity.TileEntitySignalBase;

public class SignalsOnRouteIterable implements Iterable<SignalOnRoute>{
    private final AStarRailNode startNode;

    public SignalsOnRouteIterable(AStarRailNode startNode){
        this.startNode = startNode;
    }

    @Override
    public Iterator<SignalOnRoute> iterator(){
        return new Iterator<SignalOnRoute>(){
            private AStarRailNode curNode = startNode;
            private SignalOnRoute curSignal;

            @Override
            public boolean hasNext(){
                return curSignal != null;
            }

            @Override
            public SignalOnRoute next(){
                if(curSignal == null) throw new NoSuchElementException();
                SignalOnRoute ret = curSignal;
                gotoNextSignal();
                return ret;
            }

            private Iterator<SignalOnRoute> gotoNextSignal(){
                curSignal = null;
                while(curNode != null) {
                    TileEntitySignalBase signal = curNode.getSignal(null);
                    if(signal != null) {
                        curSignal = new SignalOnRoute(signal, signal.getFacing() == curNode.pathDir);
                        curNode = curNode.getNextNode(); //Already hop to the next node for the next iteration.
                        break;
                    }

                    curNode = curNode.getNextNode();
                }
                return this;
            }
        }.gotoNextSignal();
    }

    public static class SignalOnRoute{
        public final TileEntitySignalBase signal;
        public final boolean opposite;

        public SignalOnRoute(TileEntitySignalBase signal, boolean opposite){
            this.signal = signal;
            this.opposite = opposite;
        }
    }
}
