package com.minemaarten.signals.client.gui;

import java.awt.Point;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import org.lwjgl.opengl.GL11;

import com.minemaarten.signals.capabilities.destinationproviders.DestinationProviderItems;
import com.minemaarten.signals.client.gui.widget.GuiButtonSpecial;
import com.minemaarten.signals.inventory.ContainerItemHandlerDestination;

public class GuiItemHandlerDestination extends GuiContainerBase<TileEntity>{

    private GuiButtonSpecial damageButton, nbtButton, modButton, oreDictButton, blacklistButton;

    public GuiItemHandlerDestination(TileEntity te){
        super(new ContainerItemHandlerDestination(te), null, null);
        DestinationProviderItems p = ((ContainerItemHandlerDestination)inventorySlots).provider;
        xSize = Math.max(78, Minecraft.getMinecraft().fontRenderer.getStringWidth(p.getLocalizedName()) + 2);
        ySize = 68;
    }

    @Override
    public void initGui(){
        super.initGui();

        damageButton = new GuiButtonSpecial(0, guiLeft + 5, guiTop + 18, 20, 20, "").setRenderStacks(new ItemStack(Items.IRON_PICKAXE));
        damageButton.setTooltipText("Damage");

        nbtButton = new GuiButtonSpecial(1, guiLeft + 27, guiTop + 18, 20, 20, "").setRenderStacks(new ItemStack(Items.ENCHANTED_BOOK));
        nbtButton.setTooltipText("NBT");

        modButton = new GuiButtonSpecial(2, guiLeft + 5, guiTop + 40, 20, 20, "Mod");
        modButton.setTooltipText("Mod similarity");

        oreDictButton = new GuiButtonSpecial(3, guiLeft + 27, guiTop + 40, 20, 20, "").setRenderStacks(new ItemStack(Items.IRON_INGOT));
        oreDictButton.setTooltipText("Ore Dictionary");

        blacklistButton = new GuiButtonSpecial(4, guiLeft + 53, guiTop + 30, 20, 20, "").setRenderStacks(new ItemStack(Items.DYE, 1, 0));
        blacklistButton.setTooltipText("Whitelist");

        addWidget(damageButton);
        addWidget(nbtButton);
        addWidget(modButton);
        addWidget(oreDictButton);
        addWidget(blacklistButton);
    }

    @Override
    public void updateScreen(){
        super.updateScreen();
        DestinationProviderItems p = ((ContainerItemHandlerDestination)inventorySlots).provider;

        blacklistButton.setRenderStacks(new ItemStack(Items.DYE, 1, p.blacklist ? 0 : 15));
        blacklistButton.setTooltipText(I18n.format(String.format("signals.gui.destination_provider.items.%s", p.blacklist ? "blacklist" : "whitelist")));

        damageButton.setTooltipText(I18n.format(String.format("signals.gui.destination_provider.items.%s_damage", p.checkDamage ? "check" : "ignore")));
        nbtButton.setTooltipText(I18n.format(String.format("signals.gui.destination_provider.items.%s_nbt", p.checkNBT ? "check" : "ignore")));
        modButton.setTooltipText(I18n.format(String.format("signals.gui.destination_provider.items.%s_mod_similarity", p.checkModSimilarity ? "check" : "ignore")));
        oreDictButton.setTooltipText(I18n.format(String.format("signals.gui.destination_provider.items.%s_ore_dictionary", p.checkOreDictionary ? "check" : "ignore")));
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
        DestinationProviderItems p = ((ContainerItemHandlerDestination)inventorySlots).provider;
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + 12, 0xFF222222);
        drawDarkGreyTextCentered(guiLeft + xSize / 2, guiTop, p.getLocalizedName());
        super.drawGuiContainerBackgroundLayer(partialTicks, i, j);
        if(!p.checkDamage) drawRedCross(damageButton.x + 2, damageButton.y + 2);
        if(!p.checkNBT) drawRedCross(nbtButton.x + 2, nbtButton.y + 2);
        if(!p.checkModSimilarity) drawRedCross(modButton.x + 2, modButton.y + 2);
        if(!p.checkOreDictionary) drawRedCross(oreDictButton.x + 2, oreDictButton.y + 2);
    }

    private static void drawRedCross(int x, int y){
        int size = 16;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.color(1, 0, 0);
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        bufferBuilder.pos(x, y + 1, 0.0D).endVertex();
        bufferBuilder.pos(x + size - 1, y + size, 0.0D).endVertex();
        bufferBuilder.pos(x + size, y + size - 1, 0.0D).endVertex();
        bufferBuilder.pos(x + 1, y, 0.0D).endVertex();

        bufferBuilder.pos(x, y + size - 1, 0.0D).endVertex();
        bufferBuilder.pos(x + 1, y + size, 0.0D).endVertex();
        bufferBuilder.pos(x + size, y + 1, 0.0D).endVertex();
        bufferBuilder.pos(x + size - 1, y, 0.0D).endVertex();

        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
    }
}
