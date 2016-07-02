package com.minemaarten.signals.client.gui;

import java.awt.Point;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.InventoryPlayer;
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
    private final boolean isMotorized;

    public GuiMinecart(InventoryPlayer playerInventory, EntityMinecart cart, boolean isMotorized){
        super(new ContainerMinecart(playerInventory, cart, isMotorized), null, null);
        this.cart = cart;
        cap = cart.getCapability(CapabilityMinecartDestination.INSTANCE, null);
        ySize = 200;
        xSize = isMotorized ? 295 : 120;
        this.isMotorized = isMotorized;
    }

    @Override
    public void initGui(){
        super.initGui();
        WidgetComboBox[] oldFields = stationNameFields;
        stationNameFields = new WidgetComboBox[cap.getTotalDestinations() + 1];
        for(int i = 0; i < stationNameFields.length; i++) {
            stationNameFields[i] = new WidgetComboBox(fontRendererObj, guiLeft + 10, height / 2 - 5 - stationNameFields.length * 7 + i * 14, 100, fontRendererObj.FONT_HEIGHT);
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
        return false;
    }

    @Override
    protected Point getInvTextOffset(){
        return null;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int i, int j){
        drawBackLayer();
        drawDarkGreyText(guiLeft, guiTop, "signals.gui.cart.schedule");

        String dest = cap.getCurrentDestination();
        if(dest.equals("")) {
            drawDarkGreyTextCentered(guiLeft + 60, guiTop + ySize - 18, "signals.gui.cart.no_destination");
        } else {
            drawDarkGreyTextCentered(guiLeft + 60, guiTop + ySize - 18, "signals.gui.cart.destination", dest);
        }

        if(isMotorized) {
            drawVerticalLine(guiLeft + 120, guiTop - 1, guiTop + ySize, 0xFF222222);
            drawDarkGreyText(guiLeft + 120, guiTop, "signals.gui.cart.engine_fuel");

            int barSize = 87;
            drawHorizontalLine(guiLeft + 164, guiLeft + 164 + barSize, guiTop + 67, 0xFF222222);
            int fuelSize = cap.getScaledFuel(barSize + 1);
            if(fuelSize > 0) drawHorizontalLine(guiLeft + 164, guiLeft + 164 + fuelSize - 1, guiTop + 67, 0xFFFF0000);
        }

        if(cap.getTotalDestinations() > 0) {
            drawDestinationRect(cap.getDestinationIndex(), 0xFF005500);
            for(int invalidDest : cap.getInvalidDestinationIndeces()) {
                drawDestinationRect(invalidDest, 0xAAFF0000);
            }
        }

        super.drawGuiContainerBackgroundLayer(partialTicks, i, j);
    }

    private void drawDestinationRect(int destinationIndex, int color){
        int indicatorX = guiLeft + 7;
        int indicatorY = height / 2 - stationNameFields.length * 7 + destinationIndex * 14 - 8;
        drawRect(indicatorX, indicatorY, indicatorX + 106, indicatorY + fontRendererObj.FONT_HEIGHT + 6, color);
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
