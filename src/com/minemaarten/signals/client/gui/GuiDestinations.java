package com.minemaarten.signals.client.gui;

import java.awt.Point;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;

import com.minemaarten.signals.api.access.IDestinationAccessor;
import com.minemaarten.signals.client.gui.widget.IGuiWidget;
import com.minemaarten.signals.client.gui.widget.WidgetComboBox;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public abstract class GuiDestinations<DestinationAccessor extends IDestinationAccessor> extends
        GuiContainerBase<TileEntity>{

    private WidgetComboBox[] stationNameFields;
    protected final DestinationAccessor destinationAccessor;

    public GuiDestinations(Container container, DestinationAccessor destinationAccessor){
        super(container, null, null);
        this.destinationAccessor = destinationAccessor;
        ySize = 200;
        xSize = 120;
    }

    @Override
    public void initGui(){
        super.initGui();
        WidgetComboBox[] oldFields = stationNameFields;
        stationNameFields = new WidgetComboBox[destinationAccessor.getTotalDestinations() + 1];
        for(int i = 0; i < stationNameFields.length; i++) {
            stationNameFields[i] = new WidgetComboBox(fontRenderer, guiLeft + 10, height / 2 - 5 - stationNameFields.length * 7 + i * 14, 100, fontRenderer.FONT_HEIGHT);
            stationNameFields[i].setElements(RailNetworkManager.getInstance().getNetwork().stationNames);
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

    protected boolean showDestination(){
        return true;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int i, int j){
        drawBackLayer();

        if(showDestination()) {
            String dest = destinationAccessor.getCurrentDestination();
            if(dest.equals("")) {
                drawDarkGreyTextCentered(guiLeft + 60, guiTop + ySize - 18, "signals.gui.cart.no_destination");
            } else {
                drawDarkGreyTextCentered(guiLeft + 60, guiTop + ySize - 18, "signals.gui.cart.destination", dest);
            }
        }

        if(destinationAccessor.getTotalDestinations() > 0) {
            if(showDestination()) drawDestinationRect(destinationAccessor.getDestinationIndex(), 0xFF005500);
            for(int invalidDest : destinationAccessor.getInvalidDestinationIndeces()) {
                drawDestinationRect(invalidDest, 0xAAFF0000);
            }
        }

        super.drawGuiContainerBackgroundLayer(partialTicks, i, j);
    }

    private void drawDestinationRect(int destinationIndex, int color){
        int indicatorX = guiLeft + 7;
        int indicatorY = height / 2 - stationNameFields.length * 7 + destinationIndex * 14 - 8;
        drawRect(indicatorX, indicatorY, indicatorX + 106, indicatorY + fontRenderer.FONT_HEIGHT + 6, color);
    }

    @Override
    public void updateScreen(){
        super.updateScreen();
        String[] destinations = destinationAccessor.getDestinations();
        for(int i = 0; i < stationNameFields.length; i++) {
            if(!stationNameFields[i].isFocused()) {
                stationNameFields[i].setText(i < destinations.length ? destinations[i] : "");
            }
        }
        if(destinationAccessor.getTotalDestinations() != stationNameFields.length - 1) scheduleRefresh();
    }

    @Override
    public void onKeyTyped(IGuiWidget widget){
        super.onKeyTyped(widget);

        destinationAccessor.setDestinations(getDestinations());
    }

    private List<String> getDestinations(){
        return Stream.of(stationNameFields).map(widget -> widget.getText()).collect(Collectors.toList());
    }
}
