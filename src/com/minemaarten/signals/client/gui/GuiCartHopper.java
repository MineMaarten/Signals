package com.minemaarten.signals.client.gui;

import java.awt.Point;

import net.minecraft.client.resources.I18n;

import com.minemaarten.signals.api.access.ICartHopper.HopperMode;
import com.minemaarten.signals.client.gui.widget.GuiButtonSpecial;
import com.minemaarten.signals.inventory.ContainerBase;
import com.minemaarten.signals.tileentity.TileEntityCartHopper;

public class GuiCartHopper extends GuiContainerBase<TileEntityCartHopper>{

    private GuiButtonSpecial modeButton, engineInteractButton;

    public GuiCartHopper(TileEntityCartHopper te){
        super(new ContainerBase<>(te), te, null);
        xSize = 110;
        ySize = 68;
    }

    @Override
    public void initGui(){
        super.initGui();

        modeButton = new GuiButtonSpecial(0, guiLeft + 5, guiTop + 18, 100, 20, "<Mode>");
        modeButton.setTooltipText("<Mode>");

        engineInteractButton = new GuiButtonSpecial(1, guiLeft + 5, guiTop + 40, 100, 20, "<Interact with engine>");
        //  engineInteractButton.setTooltipText("<Interact with engine>");

        addWidget(modeButton);
        addWidget(engineInteractButton);
    }

    @Override
    public void updateScreen(){
        super.updateScreen();

        HopperMode hopperMode = te.getHopperMode();
        modeButton.displayString = I18n.format("signals.gui.cart_hopper.emitRedstoneWhen." + hopperMode.toString().toLowerCase());
        modeButton.setTooltipText(I18n.format("signals.gui.cart_hopper.emitRedstoneWhen." + hopperMode.toString().toLowerCase() + ".tooltip"));

        boolean interactEngine = te.isInteractingWithEngine();
        engineInteractButton.displayString = I18n.format("signals.gui.cart_hopper.interactWith." + (interactEngine ? "cartEngine" : "cartInventory"));
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
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + 12, 0xFF222222);
        drawDarkGreyTextCentered(guiLeft + xSize / 2, guiTop, te.getBlockType().getLocalizedName());
        super.drawGuiContainerBackgroundLayer(partialTicks, i, j);
    }

}
