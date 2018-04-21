package com.minemaarten.signals.client.gui;

import java.awt.Point;
import java.io.IOException;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

import com.minemaarten.signals.client.gui.widget.IGuiWidget;
import com.minemaarten.signals.client.gui.widget.WidgetTextFieldNumber;
import com.minemaarten.signals.inventory.ContainerBase;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.PacketGuiButton;
import com.minemaarten.signals.tileentity.TileEntityRailLink;

public class GuiRailLink extends GuiContainerBase<TileEntityRailLink>{

    private WidgetTextFieldNumber delayField;

    public GuiRailLink(TileEntityRailLink te){
        super(new ContainerBase<>(te), te, null);
    }

    @Override
    public void initGui(){
        super.initGui();

        int x = width / 2 - 50;
        int y = height / 2 - 5;
        addLabel(TextFormatting.WHITE + I18n.format("signals.gui.rail_link.holdDelay"), x, y - 12);
        delayField = new WidgetTextFieldNumber(fontRenderer, x, y, 100, fontRenderer.FONT_HEIGHT);
        delayField.setDecimals(0);
        delayField.minValue = 0;
        delayField.setTooltip(I18n.format("signals.gui.rail_link.holdDelay.tooltip"));
        addWidget(delayField);
        addLabel(TextFormatting.WHITE + I18n.format("signals.gui.rail_link.ticks"), x + 110, y);

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
    public void onFieldSynced(){
        super.onFieldSynced();
        delayField.setValue(te.getHoldDelay());
    }

    @Override
    public void actionPerformed(IGuiWidget widget){
        //NO OP
    }

    @Override
    protected void keyTyped(char key, int keyCode) throws IOException{
        if(keyCode == 1) {
            NetworkHandler.sendToServer(new PacketGuiButton(delayField.getValue()));
        }
        super.keyTyped(key, keyCode);
    }
}
