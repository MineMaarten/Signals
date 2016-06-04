package com.minemaarten.signals.client.render.tileentity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

import org.apache.commons.lang3.text.WordUtils;

import com.minemaarten.signals.tileentity.TileEntitySignalBase;

public class SignalStatusRenderer extends TileEntitySpecialRenderer<TileEntitySignalBase> {

	@Override
	public void renderTileEntityAt(TileEntitySignalBase te, double x, double y,
			double z, float partialTicks, int destroyStage) {
		String message = te.getMessage();
		if(message.equals("")) return;
		
		GlStateManager.pushMatrix();
		GlStateManager.translate((float)x + 0.5F, (float)y + 1.0, (float)z + 0.5F);
		GlStateManager.rotate(180 + Minecraft.getMinecraft().getRenderManager().playerViewY, 0.0F, -1.0F, 0.0F);
		GlStateManager.rotate(Minecraft.getMinecraft().getRenderManager().playerViewX, -1.0F, 0.0F,0.0F);
		double scale = 1/32D;
		GlStateManager.scale(scale, -scale, scale);
		
		String[] splitted = WordUtils.wrap(message, 40).split("\r\n");
		FontRenderer f = Minecraft.getMinecraft().fontRendererObj;
		for(int i = 0; i < splitted.length; i++){
			String line = splitted[i];
			f.drawString(line, -f.getStringWidth(line) / 2, (i - splitted.length + 1) * (f.FONT_HEIGHT + 1), 0xFFFFFFFF);
		}
		
		GlStateManager.popMatrix();
	}

}
