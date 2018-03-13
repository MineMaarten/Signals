package com.minemaarten.signals.util;

import com.minemaarten.signals.rail.network.EnumHeading;
import com.minemaarten.signals.rail.network.IPosition;

public class Pos2D implements IPosition<Pos2D>{
    public final int x, y;

    public Pos2D(int x, int y){
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object other){
        if(other instanceof Pos2D) {
            Pos2D pos = (Pos2D)other;
            return pos.x == x && pos.y == y;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(){
        return x * 1000 + y;
    }

    @Override
    public String toString(){
        return "(" + x + ", " + y + ")";
    }

    @Override
    public double distanceSq(Pos2D other){
        int diffX = x - other.x;
        int diffY = y - other.y;
        return diffX * diffX + diffY * diffY;
    }

    @Override
    public EnumHeading getRelativeHeading(Pos2D from){
        int xDiff = x - from.x;
        if(xDiff > 0) return EnumHeading.EAST;
        if(xDiff < 0) return EnumHeading.WEST;
        int yDiff = y - from.y;
        if(yDiff > 0) return EnumHeading.SOUTH;
        if(yDiff < 0) return EnumHeading.NORTH;
        throw new IllegalStateException("No heading for pos " + this + " and from " + from);
    }

    @Override
    public int compareTo(Pos2D o){
        int xComp = Integer.compare(x, o.x);
        if(xComp != 0) return xComp;
        return Integer.compare(y, o.y);
    }
}
