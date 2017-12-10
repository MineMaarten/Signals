package com.minemaarten.signals.client.gui;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.InventoryPlayer;

import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.client.gui.widget.IGuiWidget;
import com.minemaarten.signals.inventory.ContainerMinecart;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.PacketUpdateTextfieldEntity;

public class GuiMinecart extends GuiDestinations<CapabilityMinecartDestination>{
    private final EntityMinecart cart;
    private final boolean isMotorized;

    public GuiMinecart(InventoryPlayer playerInventory, EntityMinecart cart, boolean isMotorized){
        super(new ContainerMinecart(playerInventory, cart, isMotorized), cart.getCapability(CapabilityMinecartDestination.INSTANCE, null));
        this.cart = cart;
        xSize = isMotorized ? 295 : 120;
        this.isMotorized = isMotorized;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int i, int j){
        super.drawGuiContainerBackgroundLayer(partialTicks, i, j);
        drawDarkGreyText(guiLeft, guiTop, "signals.gui.cart.schedule");

        if(isMotorized) {
            drawVerticalLine(guiLeft + 120, guiTop - 1, guiTop + ySize, 0xFF222222);
            drawDarkGreyText(guiLeft + 120, guiTop, "signals.gui.cart.engine_fuel");

            int barSize = 87;
            drawHorizontalLine(guiLeft + 164, guiLeft + 164 + barSize, guiTop + 67, 0xFF222222);
            int fuelSize = destinationAccessor.getScaledFuel(barSize + 1);
            if(fuelSize > 0) drawHorizontalLine(guiLeft + 164, guiLeft + 164 + fuelSize - 1, guiTop + 67, 0xFFFF0000);
        }
    }

    @Override
    public void onKeyTyped(IGuiWidget widget){
        super.onKeyTyped(widget);

        NetworkHandler.sendToServer(new PacketUpdateTextfieldEntity(cart, 0));
    }
}
