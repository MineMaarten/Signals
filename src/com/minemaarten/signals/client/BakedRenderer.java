package com.minemaarten.signals.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.Vec3d;

public class BakedRenderer{
    private double[] bakedPositions; //Double array for cache locality

    private List<Vec3d> verteces = new ArrayList<Vec3d>();

    public void render(BufferBuilder buffer){
        if(bakedPositions == null) {
            bakedPositions = new double[verteces.size() * 3];
            for(int i = 0; i < verteces.size(); i++) {
                Vec3d vertex = verteces.get(i);
                int startIndex = i * 3;
                bakedPositions[startIndex + 0] = vertex.x;
                bakedPositions[startIndex + 1] = vertex.y;
                bakedPositions[startIndex + 2] = vertex.z;
            }
        }

        for(int i = 0; i < bakedPositions.length; i += 3) {
            buffer.pos(bakedPositions[i], bakedPositions[i + 1], bakedPositions[i + 2]).endVertex();
        }
    }

    public void add(double x, double y, double z){
        verteces.add(new Vec3d(x, y, z));
        bakedPositions = null;
    }
}
