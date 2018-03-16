package com.minemaarten.signals.lib;

import net.minecraft.util.EnumFacing;

import com.minemaarten.signals.rail.network.EnumHeading;

public class HeadingUtils{
    public static EnumFacing toFacing(EnumHeading heading){
        switch(heading){
            case NORTH:
                return EnumFacing.NORTH;
            case SOUTH:
                return EnumFacing.SOUTH;
            case WEST:
                return EnumFacing.WEST;
            case EAST:
                return EnumFacing.EAST;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static EnumHeading fromFacing(EnumFacing facing){
        switch(facing){
            case NORTH:
                return EnumHeading.NORTH;
            case SOUTH:
                return EnumHeading.SOUTH;
            case WEST:
                return EnumHeading.WEST;
            case EAST:
                return EnumHeading.EAST;
            default:
                throw new IllegalArgumentException(facing.toString());
        }
    }
}
