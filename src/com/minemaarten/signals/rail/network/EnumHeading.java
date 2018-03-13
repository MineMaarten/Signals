package com.minemaarten.signals.rail.network;

public enum EnumHeading{
    NORTH, SOUTH, EAST, WEST;

    public EnumHeading getOpposite(){
        return EnumHeading.values()[ordinal() ^ 1];
    }

    @Override
    public String toString(){
        return name();
    }

    public String shortString(){
        return name().substring(0, 1);
    }
}
