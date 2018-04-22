package com.minemaarten.signals.util;

import java.util.stream.Stream;

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
        int yDiff = y - from.y;
        if(yDiff == 0) {
            if(xDiff == 1) return EnumHeading.EAST;
            if(xDiff == -1) return EnumHeading.WEST;
        }
        if(xDiff == 0) {
            if(yDiff == 1) return EnumHeading.SOUTH;
            if(yDiff == -1) return EnumHeading.NORTH;
        }
        return null;//throw new IllegalStateException("No heading for pos " + this + " and from " + from);
    }

    @Override
    public Pos2D offset(EnumHeading heading){
        switch(heading){
            case NORTH:
                return new Pos2D(x, y - 1);
            case SOUTH:
                return new Pos2D(x, y + 1);
            case WEST:
                return new Pos2D(x - 1, y);
            case EAST:
                return new Pos2D(x + 1, y);
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public int compareTo(Pos2D o){
        int xComp = Integer.compare(x, o.x);
        if(xComp != 0) return xComp;
        return Integer.compare(y, o.y);
    }

    @Override
    public Stream<Pos2D> allHorizontalNeighbors(){
        return EnumHeading.valuesStream().map(this::offset);
    }

    @Override
    public Pos2D min(Pos2D other){
        return new Pos2D(Math.min(x, other.x), Math.min(y, other.y));
    }

    @Override
    public Pos2D max(Pos2D other){
        return new Pos2D(Math.max(x, other.x), Math.max(y, other.y));
    }

    @Override
    public boolean isInAABB(Pos2D min, Pos2D max){
        return min.x <= x && x <= max.x && min.y <= y && y <= max.y;
    }
}
