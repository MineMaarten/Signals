package com.minemaarten.signals.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import org.lwjgl.opengl.GL11;

import com.minemaarten.signals.init.ModItems;
import com.minemaarten.signals.tileentity.TileEntitySignalBase;
import com.minemaarten.signals.tileentity.TileEntityStationMarker;

public class ClientEventHandler{
    @SubscribeEvent
    public void onWorldRender(RenderWorldLastEvent event){
        Tessellator t = Tessellator.getInstance();
        VertexBuffer wr = t.getBuffer();
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if(player.inventory.getCurrentItem() == null || player.inventory.getCurrentItem().getItem() != ModItems.railConfigurator) return;

        double playerX = player.prevPosX + (player.posX - player.prevPosX) * event.getPartialTicks();
        double playerY = player.prevPosY + (player.posY - player.prevPosY) * event.getPartialTicks();
        double playerZ = player.prevPosZ + (player.posZ - player.prevPosZ) * event.getPartialTicks();
        GL11.glPushMatrix();
        GL11.glTranslated(-playerX, -playerY, -playerZ);
        GL11.glPointSize(10);

        //Iterable<RailWrapper> rails = RailCacheManager.getInstance(player.worldObj).getAllRails();
        GlStateManager.disableTexture2D();
        List<TileEntity> tes = player.worldObj.loadedTileEntityList;
        wr.setTranslation(0, 0, 0);
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        //for(RailWrapper rail : rails) {
        //      wr.pos(rail.getX() + 0.5, rail.getY() + 0.5, rail.getZ() + 0.5).color(1F, 1F, 1F, 1F).endVertex();
        //}

        List<TileEntityStationMarker> markers = new ArrayList<TileEntityStationMarker>();
        for(TileEntity te : tes) {
            if(player.isSneaking() && te instanceof TileEntitySignalBase) {
                for(TileEntitySignalBase signal : ((TileEntitySignalBase)te).getNextSignals()) {
                    drawBetween(wr, signal.getPos(), te.getPos(), 0.5, 1, 1, 1, 1);
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
                    drawBetween(wr, marker1.getPos(), marker2.getPos(), 1, 0, 1, 0, 1);
                }
            }
        }
        //for(RailWrapper rail : rails) {
        //              wr.pos(rail.getX() + 0.5, rail.getY() + 0.5, rail.getZ() + 0.5).color(1F, 1F, 1F, 1F).endVertex();
        //}
        t.draw();
        GlStateManager.enableTexture2D();
        GL11.glPopMatrix();
    }

    private void drawBetween(VertexBuffer buffer, BlockPos p1, BlockPos p2, double offset, float r, float g, float b, float alpha){
        buffer.pos(p1.getX() + 0.5, p1.getY() + offset, p1.getZ() + 0.5).color(r, g, b, alpha).endVertex();
        buffer.pos(p2.getX() + 0.5, p2.getY() + offset, p2.getZ() + 0.5).color(r, g, b, alpha).endVertex();
    }
}
