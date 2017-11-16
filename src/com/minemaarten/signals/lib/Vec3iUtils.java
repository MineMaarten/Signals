package com.minemaarten.signals.lib;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class Vec3iUtils{
    public static Vec3d interpolate(Vec3i v1, Vec3i v2){
        return new Vec3d((v1.getX() + v2.getX()) / 2D, (v1.getY() + v2.getY()) / 2D, (v1.getZ() + v2.getZ()) / 2D);
    }
}
