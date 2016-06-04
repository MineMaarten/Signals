package com.minemaarten.signals.client.gui;

import java.awt.Point;

import com.minemaarten.signals.client.gui.widget.IGuiWidget;
import com.minemaarten.signals.client.gui.widget.WidgetComboBox;
import com.minemaarten.signals.inventory.ContainerBase;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.PacketUpdateTextfield;
import com.minemaarten.signals.rail.RailCacheManager;
import com.minemaarten.signals.tileentity.TileEntityStationMarker;

public class GuiStationMarker extends GuiContainerBase<TileEntityStationMarker>{

    private WidgetComboBox stationNameField;

    public GuiStationMarker(TileEntityStationMarker te){
        super(new ContainerBase(te), te, null);
    }

    @Override
    public void initGui(){
        super.initGui();
        stationNameField = new WidgetComboBox(fontRendererObj, width / 2 - 50, height / 2 - 5, 100, fontRendererObj.FONT_HEIGHT);
        stationNameField.setElements(RailCacheManager.getAllStationNames());
        addWidget(stationNameField);
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
        if(!stationNameField.isFocused()) {
            stationNameField.setText(te.getStationName());
        }
    }

    @Override
    public void onKeyTyped(IGuiWidget widget){
        super.onKeyTyped(widget);
        te.setText(0, stationNameField.getText());
        NetworkHandler.sendToServer(new PacketUpdateTextfield(te, 0));
    }
}
