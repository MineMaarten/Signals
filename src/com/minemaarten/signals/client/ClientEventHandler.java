package com.minemaarten.signals.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import org.lwjgl.opengl.GL11;

import com.minemaarten.signals.tileentity.TileEntitySignalBase;

public class ClientEventHandler{
    @SubscribeEvent
    public void onWorldRender(RenderWorldLastEvent event){
        Tessellator t = Tessellator.getInstance();
        VertexBuffer wr = t.getBuffer();
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if(player.inventory.armorItemInSlot(3) == null || player.inventory.armorItemInSlot(3).getItem() != Items.GOLDEN_HELMET) return;
        
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
        for(TileEntity te : tes) {
            if(te instanceof TileEntitySignalBase) {
                for(TileEntitySignalBase signal : ((TileEntitySignalBase)te).getNextSignals()) {
                    wr.pos(te.getPos().getX() + 0.5, te.getPos().getY() + 0.5, te.getPos().getZ() + 0.5).color(1F, 1F, 1F, 1F).endVertex();
                    wr.pos(signal.getPos().getX() + 0.5, signal.getPos().getY() + 0.5, signal.getPos().getZ() + 0.5).color(1F, 1F, 1F, 1F).endVertex();
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
}
