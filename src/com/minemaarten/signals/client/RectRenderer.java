package com.minemaarten.signals.client;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import org.lwjgl.opengl.GL11;

public class RectRenderer{
    private BakedRenderer bakedRenderer = new BakedRenderer();
    private float r, g, b;

    private boolean hasPos;
    private double x, y, z;
    public double width = 0.075;

    public void setColor(float r, float g, float b){
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public void render(BufferBuilder buffer){
        GL11.glColor3f(r, g, b);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        bakedRenderer.render(buffer);
        Tessellator.getInstance().draw();
        GL11.glColor3f(1, 1, 1);
    }

    private void add(double x, double y, double z){
        bakedRenderer.add(x, y, z);
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
