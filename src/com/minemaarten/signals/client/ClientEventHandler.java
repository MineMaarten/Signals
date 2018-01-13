package com.minemaarten.signals.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.BlockRailBase.EnumRailDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import org.lwjgl.opengl.GL11;

import com.minemaarten.signals.api.IRail;
import com.minemaarten.signals.init.ModItems;
import com.minemaarten.signals.lib.Vec3iUtils;
import com.minemaarten.signals.rail.RailManager;
import com.minemaarten.signals.tileentity.TileEntitySignalBase;
import com.minemaarten.signals.tileentity.TileEntitySignalBase.SignalBlockNode;
import com.minemaarten.signals.tileentity.TileEntityStationMarker;

public class ClientEventHandler{

    @SubscribeEvent
    public void onWorldRender(RenderWorldLastEvent event){
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();
        EntityPlayer player = Minecraft.getMinecraft().player;
        if(player.inventory.getCurrentItem().getItem() != ModItems.RAIL_CONFIGURATOR) return;

        double playerX = player.prevPosX + (player.posX - player.prevPosX) * event.getPartialTicks();
        double playerY = player.prevPosY + (player.posY - player.prevPosY) * event.getPartialTicks();
        double playerZ = player.prevPosZ + (player.posZ - player.prevPosZ) * event.getPartialTicks();
        GL11.glPushMatrix();
        GL11.glTranslated(-playerX, -playerY, -playerZ);
        GL11.glPointSize(10);

        //Iterable<RailWrapper> rails = RailCacheManager.getInstance(player.worldObj).getAllRails();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        List<TileEntity> tes = player.world.loadedTileEntityList;
        b.setTranslation(0, 0, 0);
        b.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        //for(RailWrapper rail : rails) {
        //      wr.pos(rail.getX() + 0.5, rail.getY() + 0.5, rail.getZ() + 0.5).color(1F, 1F, 1F, 1F).endVertex();
        //}

        List<TileEntityStationMarker> markers = new ArrayList<>();
        for(TileEntity te : tes) {
            if(player.isSneaking() && te instanceof TileEntitySignalBase) {
                TileEntitySignalBase teSignal = (TileEntitySignalBase)te;

                int colorHash = teSignal.getPos().hashCode();
                renderSignalBlocks(b, teSignal.getSignalBlockInfo(), colorHash);
                renderSignalDirection(b, teSignal, colorHash);
                for(TileEntitySignalBase signal : teSignal.getNextSignals()) {
                    drawBetween(b, signal.getPos(), te.getPos(), 0.5, 1, 1, 1, 1);
                }
            } else if(te instanceof TileEntityStationMarker) {
                markers.add((TileEntityStationMarker)te);
            }
        }
        for(int i = 0; i < markers.size(); i++) {
            TileEntityStationMarker marker1 = markers.get(i);
            for(int j = 0; j < i; j++) {
                TileEntityStationMarker marker2 = markers.get(j);
                if(marker1.getStationName().equals(marker2.getStationName())) {
                    drawBetween(b, marker1.getPos(), marker2.getPos(), 1, 0, 1, 0, 1);
                }
            }
        }
        //for(RailWrapper rail : rails) {
        //              wr.pos(rail.getX() + 0.5, rail.getY() + 0.5, rail.getZ() + 0.5).color(1F, 1F, 1F, 1F).endVertex();
        //}

        t.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GL11.glPopMatrix();
    }

    private void renderSignalDirection(BufferBuilder buffer, TileEntitySignalBase signal, int colorHash){
        BlockPos pos = signal.getNeighborPos();
        IBlockState blockState = signal.getWorld().getBlockState(pos);
        IRail rail = RailManager.getInstance().getRail(signal.getWorld(), pos, blockState);
        EnumRailDirection railDir = rail == null ? EnumRailDirection.EAST_WEST : rail.getDirection(signal.getWorld(), pos, blockState);

        EnumFacing signalFacing = signal.getFacing().getOpposite();
        EnumFacing rotatedFacing = signalFacing.rotateY();
        EnumFacing rotatedFacing2 = signalFacing.rotateYCCW();

        float r = getR(colorHash);
        float g = getG(colorHash);
        float b = getB(colorHash);

        Vec3d posVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);

        boolean isAscending;
        switch(signalFacing){
            case EAST:
                isAscending = railDir == EnumRailDirection.ASCENDING_WEST;
                break;
            case WEST:
                isAscending = railDir == EnumRailDirection.ASCENDING_EAST;
                break;
            case NORTH:
                isAscending = railDir == EnumRailDirection.ASCENDING_SOUTH;
                break;
            case SOUTH:
                isAscending = railDir == EnumRailDirection.ASCENDING_NORTH;
                break;
            default:
                isAscending = false;
                break;
        }

        boolean isDescending = railDir.isAscending() && !isAscending;
        double yOffset = isAscending ? 0.5 : (isDescending ? -0.5 : 0);

        double arrowSize = 0.2;
        double spacing = 0.2;
        for(int i = -2; i < 0; i++) {
            Vec3d shiftedPosVec = posVec.addVector(signalFacing.getFrontOffsetX() * spacing * i, spacing * i * yOffset, signalFacing.getFrontOffsetZ() * spacing * i);
            Vec3d vecBack = shiftedPosVec.addVector(signalFacing.getFrontOffsetX() * arrowSize, arrowSize * i * yOffset, signalFacing.getFrontOffsetZ() * arrowSize);
            Vec3d c1 = vecBack.addVector(rotatedFacing.getFrontOffsetX() * arrowSize, 0, rotatedFacing.getFrontOffsetZ() * arrowSize);
            Vec3d c2 = vecBack.addVector(rotatedFacing2.getFrontOffsetX() * arrowSize, 0, rotatedFacing2.getFrontOffsetZ() * arrowSize);

            buffer.pos(shiftedPosVec.x, shiftedPosVec.y, shiftedPosVec.z).color(r, g, b, 1).endVertex();
            buffer.pos(c1.x, c1.y, c1.z).color(r, g, b, 1).endVertex();
            buffer.pos(shiftedPosVec.x, shiftedPosVec.y, shiftedPosVec.z).color(r, g, b, 1).endVertex();
            buffer.pos(c2.x, c2.y, c2.z).color(r, g, b, 1).endVertex();
        }

        if(railDir.isAscending()) {

        } else {

        }
    }

    private void renderSignalBlocks(BufferBuilder buffer, SignalBlockNode node, int colorHash){
        renderSignalBlocks(buffer, node, getR(colorHash), getG(colorHash), getB(colorHash), false);
    }

    private void renderSignalBlocks(BufferBuilder buffer, SignalBlockNode node, float r, float g, float b, boolean goingDown){
        boolean hasHigherNeighbor = false, hasLowerNeighbor = false;
        for(SignalBlockNode neighbor : node.nextNeighbors) {}

        for(SignalBlockNode neighbor : node.nextNeighbors) {
            boolean isLowerNeighbor = neighbor.railPos.getY() < node.railPos.getY();
            boolean isHigherNeighbor = neighbor.railPos.getY() > node.railPos.getY();

            Vec3d interpolated = Vec3iUtils.interpolate(node.railPos, neighbor.railPos);
            SignalBlockNode neighborsNeighbor = neighbor.nextNeighbors.isEmpty() ? null : neighbor.nextNeighbors.get(0);
            boolean neighborsNeighborIsHigher = neighborsNeighbor != null && neighborsNeighbor.railPos.getY() > neighbor.railPos.getY();

            buffer.pos(node.railPos.getX() + 0.5, node.railPos.getY() + (isHigherNeighbor || goingDown ? 0.6 : 0.1), node.railPos.getZ() + 0.5).color(r, g, b, 1).endVertex();
            buffer.pos(interpolated.x + 0.5, node.railPos.getY() + (isHigherNeighbor ? 1.1 : 0.1), interpolated.z + 0.5).color(r, g, b, 1).endVertex();
            buffer.pos(interpolated.x + 0.5, node.railPos.getY() + (isHigherNeighbor ? 1.1 : 0.1), interpolated.z + 0.5).color(r, g, b, 1).endVertex();
            buffer.pos(neighbor.railPos.getX() + 0.5, neighbor.railPos.getY() + (isLowerNeighbor || neighborsNeighborIsHigher ? 0.6 : 0.1), neighbor.railPos.getZ() + 0.5).color(r, g, b, 1).endVertex();

            renderSignalBlocks(buffer, neighbor, r, g, b, isLowerNeighbor);
        }
    }

    private void drawBetween(BufferBuilder buffer, BlockPos p1, BlockPos p2, double offset1, double offset2, int colorHash){
        drawBetween(buffer, p1, p2, offset1, offset2, getR(colorHash), getG(colorHash), getB(colorHash), 1);
    }

    private float getR(int colorHash){
        return colorHash % 256 / 256F;
    }

    private float getG(int colorHash){
        return colorHash % 415 / 415F;
    }

    private float getB(int colorHash){
        return colorHash % 351 / 351F;
    }

    private void drawBetween(BufferBuilder buffer, BlockPos p1, BlockPos p2, double offset, float r, float g, float b, float a){
        drawBetween(buffer, p1, p2, offset, offset, r, g, b, a);
    }

    private static void drawBetween(BufferBuilder buffer, BlockPos p1, BlockPos p2, double offset1, double offset2, float r, float g, float b, float alpha){
        buffer.pos(p1.getX() + 0.5, p1.getY() + offset1, p1.getZ() + 0.5).color(r, g, b, alpha).endVertex();
        buffer.pos(p2.getX() + 0.5, p2.getY() + offset2, p2.getZ() + 0.5).color(r, g, b, alpha).endVertex();
    }
}
