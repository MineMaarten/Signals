package com.minemaarten.signals.rail.network;

public enum EnumHeading{
    NORTH, EAST, SOUTH, WEST;

    public EnumHeading getOpposite(){
        return EnumHeading.values()[ordinal() ^ 2];
    }

    public EnumHeading rotateCW(){
        return EnumHeading.values()[(ordinal() + 1) % 4];
    }

    public EnumHeading rotateCCW(){
        return EnumHeading.values()[(ordinal() + 3) % 4];
    }

    @Override
    public String toString(){
        return name();
    }

    public String shortString(){
        return name().substring(0, 1);
    }
}
