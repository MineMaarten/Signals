package com.minemaarten.signals.rail.network;

public interface IPosition<TPos> extends Comparable<TPos>{
    public double distanceSq(TPos other);

    /**
     * Should take 'this - from' , and use those diffs to determine a heading.
     * @param from
     * @return
     */
    public EnumHeading getRelativeHeading(TPos from);
}
