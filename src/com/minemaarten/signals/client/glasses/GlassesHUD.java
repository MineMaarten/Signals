package com.minemaarten.signals.client.glasses;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import org.lwjgl.opengl.GL11;

import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;

public class GlassesHUD{
    private static final GlassesHUD INSTANCE = new GlassesHUD();

    public static GlassesHUD getInstance(){
        return INSTANCE;
    }

    private final List<GlassesMessage> messages = new LinkedList<GlassesMessage>();

    public void onNewMessage(GlassesMessage message){
    	if(message.associatedCart != null){
	        messages.add(message);
	        while(messages.size() > 20) {
	            messages.remove(0);
	        }
    	}
    }

   // @SubscribeEvent HUD disabled
    public void renderTick(TickEvent.RenderTickEvent event){
        if(event.phase == TickEvent.Phase.END) {
            Minecraft mc = FMLClientHandler.instance().getClient();
            if(mc != null && mc.thePlayer != null) {
                render2D(event.renderTickTime);
            }
        }
    }

    private void render2D(float partialTicks){
        Minecraft minecraft = FMLClientHandler.instance().getClient();
        EntityPlayer player = minecraft.thePlayer;
        ItemStack helmetStack = player.inventory.armorInventory[3];
        if(helmetStack != null && minecraft.inGameHasFocus && helmetStack.getItem() == Items.DIAMOND_HELMET) {
            ScaledResolution sr = new ScaledResolution(minecraft);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glPushMatrix();
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glColor4d(0, 1, 0, 0.8D);

            GL11.glPopMatrix();
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_TEXTURE_2D);

            for(int i = 0; i < messages.size(); i++) {
                minecraft.fontRendererObj.drawString(messages.get(i).localizedMessage, 16, 16 + i * (minecraft.fontRendererObj.FONT_HEIGHT + 1), 0xFF0000);
            }
        }
    }
}
