package com.minemaarten.signals.rail.network;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class PosAABB<TPos extends IPosition<TPos>> {
    public final TPos min, max;
    private final Set<TPos> positions;

    public PosAABB(List<TPos> positions){
        this(new HashSet<>(positions));
    }

    public PosAABB(Set<TPos> positions){
        if(positions.isEmpty()) throw new IllegalArgumentException();
        this.positions = positions;

        Iterator<TPos> iterator = positions.iterator();
        TPos curPos = iterator.next();
        TPos curMin = curPos;
        TPos curMax = curPos;
        while(iterator.hasNext()) {
            curPos = iterator.next();
            curMin = curMin.min(curPos);
            curMax = curMax.max(curPos);
        }
        min = curMin;
        max = curMax;
    }

    public boolean isInAABB(TPos pos){
        return pos.isInAABB(min, max) && positions.contains(pos);
    }
}
