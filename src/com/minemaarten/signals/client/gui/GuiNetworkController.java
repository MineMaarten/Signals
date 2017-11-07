package com.minemaarten.signals.client.gui;

import java.awt.Point;
import java.io.IOException;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import com.minemaarten.signals.client.gui.widget.IGuiWidget;
import com.minemaarten.signals.inventory.ContainerBase;
import com.minemaarten.signals.inventory.ContainerNetworkController;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.PacketGuiButton;
import com.minemaarten.signals.network.PacketUpdateGui;
import com.minemaarten.signals.rail.NetworkController;
import com.minemaarten.signals.tileentity.TileEntitySignalBase.EnumForceMode;

public class GuiNetworkController extends GuiContainerBase<TileEntity>{

	private static final int SPACING = 40;
	
    public GuiNetworkController(){
        super(new ContainerNetworkController(), null, null);
    }

    @Override
    public void initGui(){
        super.initGui();
    }

    @Override
    protected boolean shouldDrawBackground(){
        return false;
    }

    @Override
    protected Point getInvTextOffset(){
        return null;
    }

    @Override
    public void updateScreen(){
        super.updateScreen();
        
    }
    
    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int i,
    		int j) {
    	super.drawGuiContainerBackgroundLayer(partialTicks, i, j);
    	
    	
    	NetworkController controller = NetworkController.getInstance(mc.world);
    	GlStateManager.pushMatrix();
    	double scale = Math.min((double)(width - SPACING) / controller.getWidth(), (double)(height - SPACING) / controller.getHeight());
    	//scale = 1;
    	GlStateManager.translate(width/2 - controller.getWidth() * scale / 2, height/2 - controller.getHeight() * scale / 2, 0);
        GlStateManager.scale(scale, scale, scale);
        drawRect(-1, -1, controller.getWidth() + 1, controller.getHeight() + 1, 0xFF111111);
        controller.render(mc.world);
    	GlStateManager.popMatrix();
    }
    
    @Override
    protected void mouseClicked(int x, int y, int button)
    		throws IOException {
    	super.mouseClicked(x, y, button);
    	
    	NetworkController controller = NetworkController.getInstance(mc.world);
    	double scale = Math.min((double)(width - SPACING) / controller.getWidth(), (double)(height - SPACING) / controller.getHeight());
    	//scale = 1;
    	double scaledX = x;
    	double scaledY = y;
    	scaledX -= (width/2 - controller.getWidth() * scale / 2 );
    	scaledY -= (height/2 - controller.getHeight() * scale / 2);
    	scaledX /= scale;
    	scaledY /= scale;
    	int posX = (int)scaledX + controller.getStartX();
    	int posZ = (int)scaledY + controller.getStartZ();
    	NetworkHandler.sendToServer(new PacketGuiButton(posX, posZ, button == 0 ? EnumForceMode.FORCED_GREEN_ONCE.ordinal() : EnumForceMode.FORCED_RED.ordinal()));
    }

    @Override
    public void onKeyTyped(IGuiWidget widget){
        super.onKeyTyped(widget);
    }
}
