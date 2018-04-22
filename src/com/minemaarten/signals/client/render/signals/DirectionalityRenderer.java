package com.minemaarten.signals.client.render.signals;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

import org.lwjgl.opengl.GL11;

import com.google.common.collect.ImmutableList;
import com.minemaarten.signals.client.BakedRenderer;
import com.minemaarten.signals.lib.HeadingUtils;
import com.minemaarten.signals.rail.network.EnumHeading;
import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.rail.network.RailEdge;
import com.minemaarten.signals.rail.network.mc.MCNetworkRail;
import com.minemaarten.signals.rail.network.mc.MCPos;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public class DirectionalityRenderer{

    private BakedRenderer bakedRenderer = new BakedRenderer();

    public void render(BufferBuilder b){
        b.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION);
        bakedRenderer.render(b);
        Tessellator.getInstance().draw();
    }

    public void updateRender(){
        BakedRenderer bakedRenderer = new BakedRenderer();
        for(RailEdge<MCPos> edge : RailNetworkManager.getInstance().getNetwork().getAllEdges()) {
            if(edge.directionality.canTravelForwards) build(bakedRenderer, edge.edge);
            if(edge.directionality.canTravelBackwards) build(bakedRenderer, edge.edge.reverse());
        }
        this.bakedRenderer = bakedRenderer;
    }

    private void build(BakedRenderer bakedRenderer, ImmutableList<NetworkRail<MCPos>> edge){

        for(int edgeIndex = 1; edgeIndex < edge.size() - 1; edgeIndex++) {
            NetworkRail<MCPos> prevRail = edge.get(edgeIndex - 1);
            MCNetworkRail curRail = (MCNetworkRail)edge.get(edgeIndex);
            NetworkRail<MCPos> nextRail = edge.get(edgeIndex + 1);

            EnumHeading prevHeading = curRail.pos.getRelativeHeading(prevRail.pos);
            EnumHeading nextHeading = nextRail.pos.getRelativeHeading(curRail.pos);
            if(prevHeading == null || nextHeading == null || prevHeading != nextHeading) continue;

            MCPos pos = curRail.pos;
            EnumFacing facing = HeadingUtils.toFacing(nextHeading).getOpposite();
            EnumFacing rotatedFacing = facing.rotateY();
            EnumFacing rotatedFacing2 = facing.rotateYCCW();
            int yOffset = AbstractRailRenderer.getRailHeightOffset(curRail, facing);

            Vec3d posVec = new Vec3d(pos.getX() + 0.5, pos.getY() + (yOffset != 0 ? 0.6001 : 0.1001), pos.getZ() + 0.5);

            double arrowSize = 0.1;
            double spacing = 0.1;

            for(int i = -2; i < -1; i++) {
                Vec3d shiftedPosVec = posVec.addVector(facing.getFrontOffsetX() * spacing * i, spacing * i * yOffset + 0.001, facing.getFrontOffsetZ() * spacing * i);
                Vec3d vecBack = shiftedPosVec.addVector(facing.getFrontOffsetX() * arrowSize, arrowSize * yOffset, facing.getFrontOffsetZ() * arrowSize);
                Vec3d c1 = vecBack.addVector(rotatedFacing.getFrontOffsetX() * arrowSize, 0, rotatedFacing.getFrontOffsetZ() * arrowSize);
                Vec3d c2 = vecBack.addVector(rotatedFacing2.getFrontOffsetX() * arrowSize, 0, rotatedFacing2.getFrontOffsetZ() * arrowSize);

                bakedRenderer.add(shiftedPosVec.x, shiftedPosVec.y, shiftedPosVec.z);
                bakedRenderer.add(c1.x, c1.y, c1.z);
                //buffer.pos(shiftedPosVec.x, shiftedPosVec.y, shiftedPosVec.z).color(r, g, b, 1).endVertex();
                bakedRenderer.add(c2.x, c2.y, c2.z);
            }
        }
    }
}
