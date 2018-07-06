package com.minemaarten.signals.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import org.lwjgl.opengl.GL11;

import com.minemaarten.signals.block.BlockSignalBase;
import com.minemaarten.signals.client.render.signals.BlockSectionRenderer;
import com.minemaarten.signals.client.render.signals.ClaimedPosRenderer;
import com.minemaarten.signals.client.render.signals.DirectionalityRenderer;
import com.minemaarten.signals.client.render.signals.PathRenderer;
import com.minemaarten.signals.client.render.signals.RailEdgeRenderer;
import com.minemaarten.signals.config.SignalsConfig;
import com.minemaarten.signals.config.SignalsConfig.NetworkVisualizationSettings;
import com.minemaarten.signals.rail.network.NetworkStation;
import com.minemaarten.signals.rail.network.mc.MCPos;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public class ClientEventHandler{

    public static final ClientEventHandler INSTANCE = new ClientEventHandler();
    public final BlockSectionRenderer blockSectionRenderer = new BlockSectionRenderer();
    public final RailEdgeRenderer edgeRenderer = new RailEdgeRenderer();
    public final PathRenderer pathRenderer = new PathRenderer();
    public final ClaimedPosRenderer claimRenderer = new ClaimedPosRenderer();
    public final DirectionalityRenderer directionalityRenderer = new DirectionalityRenderer();

    private ClientEventHandler(){

    }

    @SubscribeEvent
    public void onWorldRender(RenderWorldLastEvent event){
        Tessellator t = Tessellator.getInstance();
        BufferBuilder b = t.getBuffer();

        EntityPlayer player = Minecraft.getMinecraft().player;
        if(!shouldRender()) return;

        double playerX = player.prevPosX + (player.posX - player.prevPosX) * event.getPartialTicks();
        double playerY = player.prevPosY + (player.posY - player.prevPosY) * event.getPartialTicks();
        double playerZ = player.prevPosZ + (player.posZ - player.prevPosZ) * event.getPartialTicks();
        int dimensionID = player.world.provider.getDimension();
        GL11.glPushMatrix();
        GL11.glTranslated(-playerX, -playerY, -playerZ);

        GL11.glPointSize(10);

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        b.setTranslation(0, 0, 0);

        NetworkVisualizationSettings visualizationSettings = player.isSneaking() ? SignalsConfig.client.networkVisualization.sneaking : SignalsConfig.client.networkVisualization.notSneaking;
        switch(visualizationSettings.renderType){
            case EDGES:
                edgeRenderer.render(dimensionID, b);
                break;
            case PATHS:
                pathRenderer.render(dimensionID, b);
                break;
            case SECTION:
                blockSectionRenderer.render(dimensionID, b);
                break;
        }
        //claimRenderer.render(b);
        if(visualizationSettings.renderDirectionality) {
            directionalityRenderer.render(dimensionID, b);
        }

        b.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        List<NetworkStation<MCPos>> stations = RailNetworkManager.getInstance().getNetwork().railObjects.getStations();
        for(int i = 0; i < stations.size(); i++) {
            NetworkStation<MCPos> station1 = stations.get(i);
            if(station1.pos.getDimID() != dimensionID) continue;
            for(int j = 0; j < i; j++) {
                NetworkStation<MCPos> station2 = stations.get(j);
                if(station2.pos.getDimID() != dimensionID) continue;

                if(station1.stationName.equals(station2.stationName) && station1.pos.getDimID() == station2.pos.getDimID()) {
                    drawBetween(b, station1.pos.getPos(), station2.pos.getPos(), 1, 0, 1, 0, 1);
                }
            }
        }

        t.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GL11.glPopMatrix();
    }

    private boolean shouldRender(){
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        Item item = player.inventory.getCurrentItem().getItem();
        if(SignalsConfig.client.networkVisualization.isValid(item)) {
            return true;
        }

        RayTraceResult ray = Minecraft.getMinecraft().objectMouseOver;
        return ray != null && ray.typeOfHit == RayTraceResult.Type.BLOCK && player.world.getBlockState(ray.getBlockPos()).getBlock() instanceof BlockSignalBase;
    }

    private void drawBetween(BufferBuilder buffer, BlockPos p1, BlockPos p2, double offset, float r, float g, float b, float a){
        drawBetween(buffer, p1, p2, offset, offset, r, g, b, a);
    }

    private static void drawBetween(BufferBuilder buffer, BlockPos p1, BlockPos p2, double offset1, double offset2, float r, float g, float b, float alpha){
        buffer.pos(p1.getX() + 0.5, p1.getY() + offset1, p1.getZ() + 0.5).color(r, g, b, alpha).endVertex();
        buffer.pos(p2.getX() + 0.5, p2.getY() + offset2, p2.getZ() + 0.5).color(r, g, b, alpha).endVertex();
    }
}
