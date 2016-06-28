package com.minemaarten.signals.client.gui;

import java.awt.Point;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.tileentity.TileEntity;

import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.client.gui.widget.IGuiWidget;
import com.minemaarten.signals.client.gui.widget.WidgetComboBox;
import com.minemaarten.signals.inventory.ContainerMinecart;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.PacketUpdateTextfieldEntity;
import com.minemaarten.signals.rail.RailCacheManager;

public class GuiMinecart extends GuiContainerBase<TileEntity>{

    private WidgetComboBox[] stationNameFields;
    private final CapabilityMinecartDestination cap;
    private final EntityMinecart cart;

    public GuiMinecart(EntityMinecart cart){
        super(new ContainerMinecart(cart), null, null);
        this.cart = cart;
        cap = cart.getCapability(CapabilityMinecartDestination.INSTANCE, null);
    }

    @Override
    public void initGui(){
        super.initGui();
        WidgetComboBox[] oldFields = stationNameFields;
        stationNameFields = new WidgetComboBox[cap.getTotalDestinations() + 1];
        for(int i = 0; i < stationNameFields.length; i++) {
            stationNameFields[i] = new WidgetComboBox(fontRendererObj, width / 2 - 50, height / 2 - 5 - stationNameFields.length * 6 + i * 12, 100, fontRendererObj.FONT_HEIGHT);
            stationNameFields[i].setElements(RailCacheManager.getAllStationNames());
            addWidget(stationNameFields[i]);
        }
        if(oldFields != null) {
            for(int i = 0; i < oldFields.length && i < stationNameFields.length; i++) {
            	if(oldFields[i].isFocused()) stationNameFields[i].setText(oldFields[i].getText());
            	stationNameFields[i].setFocused(oldFields[i].isFocused());
            }
        }
    }

    @Override
    protected boolean shouldDrawBackground(){
        fontRendererObj.drawString(cap.getCurrentDestination(), width / 2, height / 2 + stationNameFields.length * 12, 0xFFFFFFFF);
        return false;
    }

    @Override
    protected Point getInvTextOffset(){
        return null;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int i, int j){
        super.drawGuiContainerBackgroundLayer(partialTicks, i, j);
        drawHorizontalLine(width / 2 - 70, width / 2 - 55, height / 2 - stationNameFields.length * 6 + cap.getDestinationIndex() * 12, 0xFFFFFFFF);
    }

    @Override
    public void updateScreen(){
        super.updateScreen();
        for(int i = 0; i < stationNameFields.length; i++) {
            if(!stationNameFields[i].isFocused()) {
                stationNameFields[i].setText(i < cap.getTotalDestinations() ? cap.getDestination(i) : "");
            }
        }
        if(cap.getTotalDestinations() != stationNameFields.length - 1) scheduleRefresh();
    }

    @Override
    public void onKeyTyped(IGuiWidget widget){
        super.onKeyTyped(widget);

        cap.setText(0, getDestinationString());
        NetworkHandler.sendToServer(new PacketUpdateTextfieldEntity(cart, 0));
    }

    private String getDestinationString(){
        StringBuilder sb = new StringBuilder();
        for(WidgetComboBox widget : stationNameFields) {
            if(sb.length() > 0) sb.append("\n");
            sb.append(widget.getText());
        }
        return sb.toString();
    }
}
