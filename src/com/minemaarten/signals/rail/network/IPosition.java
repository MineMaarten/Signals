package com.minemaarten.signals.rail.network;

import java.util.List;

public interface IPosition<TPos> extends Comparable<TPos>{
    public double distanceSq(TPos other);

    /**
     * Should take 'this - from' , and use those diffs to determine a heading.
     * This is meant to be the opposite from 'offset', it is expected that this.offset(heading) == from (when not considering y)
     * If no heading suits, return null, indicating that 'from' is not a direct neighbor.
     * @param from
     * @return
     */
    public EnumHeading getRelativeHeading(TPos from);

    public TPos offset(EnumHeading heading);

    public List<TPos> allHorizontalNeighbors();
}
