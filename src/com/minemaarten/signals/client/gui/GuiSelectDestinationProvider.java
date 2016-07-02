package com.minemaarten.signals.client.gui;

import java.util.List;

import net.minecraft.tileentity.TileEntity;

import com.minemaarten.signals.api.tileentity.IDestinationProvider;
import com.minemaarten.signals.client.gui.widget.GuiButtonSpecial;
import com.minemaarten.signals.inventory.ContainerSelectDestinationProvider;

public class GuiSelectDestinationProvider extends GuiContainerBase<TileEntity>{

    private List<IDestinationProvider> providers;

    public GuiSelectDestinationProvider(TileEntity te){
        super(new ContainerSelectDestinationProvider(te), null, null);
        providers = ((ContainerSelectDestinationProvider)inventorySlots).guiProviders;
        xSize = 210;
        ySize = 8 + providers.size() * 22;
    }

    @Override
    public void initGui(){
        super.initGui();

        for(int i = 0; i < providers.size(); i++) {
            GuiButtonSpecial b = new GuiButtonSpecial(i, guiLeft + 5, guiTop + 5 + i * 22, 200, 20, providers.get(i).getLocalizedName());
            addWidget(b);
        }
    }

    @Override
    protected boolean shouldDrawBackground(){
        return false;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int i, int j){
        drawBackLayer();
        super.drawGuiContainerBackgroundLayer(partialTicks, i, j);
    }
}
