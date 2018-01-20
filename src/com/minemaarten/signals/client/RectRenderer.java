package com.minemaarten.signals.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.Vec3d;

public class RectRenderer{
    private List<Vec3d> verteces = new ArrayList<Vec3d>();
    private float r, g, b;

    private boolean hasPos;
    private double x, y, z;
    private final double width = 0.075;

    public void setColor(float r, float g, float b){
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public void render(BufferBuilder buffer){
        for(Vec3d v : verteces) {
            buffer.pos(v.x, v.y, v.z).color(r, g, b, 1).endVertex();
        }
    }

    private void add(double x, double y, double z){
        verteces.add(new Vec3d(x, y, z));
    }

    public void pos(double x, double y, double z){
        if(hasPos) {
            if(x == this.x) {
                double startX = x - width;
                double endX = x + width;
                add(startX, y, z);
                add(endX, y, z);
                add(endX, this.y, this.z);
                add(startX, this.y, this.z);

                add(endX, y, z);
                add(startX, y, z);
                add(startX, this.y, this.z);
                add(endX, this.y, this.z);
            } else { //if z == this.z
                double startZ = z - width;
                double endZ = z + width;
                add(x, y, startZ);
                add(x, y, endZ);
                add(this.x, this.y, endZ);
                add(this.x, this.y, startZ);

                add(x, y, endZ);
                add(x, y, startZ);
                add(this.x, this.y, startZ);
                add(this.x, this.y, endZ);
            }
        } else {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        hasPos = !hasPos;
    }

}
