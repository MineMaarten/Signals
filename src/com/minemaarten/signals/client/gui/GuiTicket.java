package com.minemaarten.signals.client.gui;

import net.minecraft.inventory.Container;

import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.client.gui.widget.WidgetTextField;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.PacketUpdateTicket;

public class GuiTicket extends GuiDestinations<CapabilityMinecartDestination>{

    private WidgetTextField itemNameField;
    private final String initialItemName;

    public GuiTicket(Container container, CapabilityMinecartDestination accessor, String itemName){
        super(container, accessor);
        this.initialItemName = itemName;
    }

    @Override
    public void initGui(){
        super.initGui();

        itemNameField = new WidgetTextField(fontRenderer, guiLeft + 10, 30, 100, fontRenderer.FONT_HEIGHT);
        itemNameField.setText(initialItemName);
        addWidget(itemNameField);

    }

    @Override
    public void onGuiClosed(){
        String newItemName = itemNameField.getText().equals(initialItemName) ? "" : itemNameField.getText(); //Substitute "" to indicate no changes should be made.
        NetworkHandler.sendToServer(new PacketUpdateTicket(destinationAccessor.getText(0), newItemName));
        super.onGuiClosed();
    }

    @Override
    protected boolean showDestination(){
        return false;
    }

}
