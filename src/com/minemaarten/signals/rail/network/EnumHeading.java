package com.minemaarten.signals.rail.network;

import java.util.Arrays;
import java.util.stream.Stream;

public enum EnumHeading{
    NORTH, EAST, SOUTH, WEST;

    public static EnumHeading[] VALUES = EnumHeading.values();

    public EnumHeading getOpposite(){
        return EnumHeading.values()[ordinal() ^ 2];
    }

    public EnumHeading rotateCW(){
        return EnumHeading.values()[(ordinal() + 1) % 4];
    }

    public EnumHeading rotateCCW(){
        return EnumHeading.values()[(ordinal() + 3) % 4];
    }

    public static Stream<EnumHeading> valuesStream(){
        return Arrays.stream(EnumHeading.values());
    }

    public static EnumHeading getOpposite(EnumHeading heading){
        return heading == null ? null : heading.getOpposite();
    }

    @Override
    public String toString(){
        return name();
    }

    public String shortString(){
        return name().substring(0, 1);
    }
}
