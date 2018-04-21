package com.minemaarten.signals.client.gui;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.apache.commons.lang3.text.WordUtils;
import org.lwjgl.opengl.GL11;

import com.minemaarten.signals.Signals;
import com.minemaarten.signals.client.gui.widget.IGuiWidget;
import com.minemaarten.signals.client.gui.widget.IWidgetListener;
import com.minemaarten.signals.client.gui.widget.WidgetLabel;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.PacketGuiButton;

@SideOnly(Side.CLIENT)
public class GuiContainerBase<Tile extends TileEntity> extends GuiContainer implements IWidgetListener{

    public final Tile te;
    private final ResourceLocation guiTexture;
    protected final List<IGuiWidget> widgets = new ArrayList<>();
    private boolean refreshScheduled;
    private boolean hasInit; //Fix for some weird race condition occuring in 1.8 where drawing is called before initGui().

    public GuiContainerBase(Container par1Container, Tile te, String guiTexture){
        super(par1Container);
        this.te = te;
        this.guiTexture = guiTexture != null ? new ResourceLocation(guiTexture) : null;
    }

    protected void addWidget(IGuiWidget widget){
        widgets.add(widget);
        widget.setListener(this);
    }

    protected void addWidgets(Iterable<IGuiWidget> widgets){
        for(IGuiWidget widget : widgets) {
            addWidget(widget);
        }
    }

    protected void addLabel(String text, int x, int y){
        addWidget(new WidgetLabel(x, y, text));
    }

    protected void removeWidget(IGuiWidget widget){
        widgets.remove(widget);
    }

    @Override
    public void initGui(){
        super.initGui();
        hasInit = true;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int i, int j){
        if(shouldDrawBackground()) {
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            bindGuiTexture();
            int xStart = (width - xSize) / 2;
            int yStart = (height - ySize) / 2;
            drawTexturedModalRect(xStart, yStart, 0, 0, xSize, ySize);
        }

        GL11.glColor4d(1, 1, 1, 1);
        GL11.glDisable(GL11.GL_LIGHTING);
        for(IGuiWidget widget : widgets) {
            widget.render(i, j, partialTicks);
        }
        for(IGuiWidget widget : widgets) {
            widget.postRender(i, j, partialTicks);
        }
    }

    /**
     * Draws dark gray back layer with inventory spots.
     */
    protected void drawBackLayer(){
        GlStateManager.disableTexture2D();
        int borderWidth = 1;
        drawRect(guiLeft - borderWidth, guiTop - borderWidth, guiLeft + xSize + borderWidth, guiTop + ySize + borderWidth, 0xFF222222);
        drawRect(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize, 0xFF333333);
        for(Slot slot : inventorySlots.inventorySlots) {
            drawRect(guiLeft + slot.xPos, guiTop + slot.yPos, guiLeft + slot.xPos + 16, guiTop + slot.yPos + 16, 0xFF222222);
        }
        GlStateManager.enableTexture2D();
    }

    protected void drawDarkGreyTextCentered(int x, int y, String localizationKey, Object... args){
        x -= fontRenderer.getStringWidth(I18n.format(localizationKey, args)) / 2 + 1;
        drawDarkGreyText(x, y, localizationKey, args);
    }

    protected void drawDarkGreyText(int x, int y, String localizationKey, Object... args){
        String text = I18n.format(localizationKey, args);
        drawRect(x, y, x + fontRenderer.getStringWidth(text) + 3, y + fontRenderer.FONT_HEIGHT + 3, 0xFF222222);
        fontRenderer.drawString(text, x + 2, y + 2, 0xFFFFFFFF);
    }

    protected boolean shouldDrawBackground(){
        return true;
    }

    protected void bindGuiTexture(){
        if(guiTexture != null) {
            mc.getTextureManager().bindTexture(guiTexture);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int x, int y){
        if(getInvNameOffset() != null && te instanceof IInventory) {
            IInventory inv = (IInventory)te;
            String containerName = inv.hasCustomName() ? inv.getName() : I18n.format(inv.getName() + ".name");
            fontRenderer.drawString(containerName, xSize / 2 - fontRenderer.getStringWidth(containerName) / 2 + getInvNameOffset().x, 6 + getInvNameOffset().y, 4210752);
        }
        if(getInvTextOffset() != null) fontRenderer.drawString(I18n.format("container.inventory"), 8 + getInvTextOffset().x, ySize - 94 + getInvTextOffset().y, 4210752);
    }

    protected Point getInvNameOffset(){
        return new Point(0, 0);
    }

    protected Point getInvTextOffset(){
        return new Point(0, 0);
    }

    @Override
    public void drawScreen(int x, int y, float partialTick){
        if(!hasInit) return;
        super.drawScreen(x, y, partialTick);

        List<String> tooltip = new ArrayList<>();

        GL11.glColor4d(1, 1, 1, 1);
        GL11.glDisable(GL11.GL_LIGHTING);
        for(IGuiWidget widget : widgets) {
            if(widget.getBounds().contains(x, y)) widget.addTooltip(x, y, tooltip, Signals.proxy.isSneakingInGui());
        }

        if(!tooltip.isEmpty()) {
            List<String> localizedTooltip = new ArrayList<>();
            for(String line : tooltip) {
                String localizedLine = I18n.format(line);
                String[] lines = WordUtils.wrap(localizedLine, 50).split(System.getProperty("line.separator"));
                for(String locLine : lines) {
                    localizedTooltip.add(locLine);
                }
            }
            drawHoveringText(localizedTooltip, x, y, fontRenderer);
        }
    }

    @Override
    public void updateScreen(){
        if(refreshScheduled) {
            refreshScheduled = false;
            refreshScreen();
        }

        super.updateScreen();

        for(IGuiWidget widget : widgets)
            widget.update();
    }

    @Override
    protected void actionPerformed(GuiButton button){
        sendPacketToServer(button.id);
    }

    @Override
    protected void mouseClicked(int par1, int par2, int par3) throws IOException{
        super.mouseClicked(par1, par2, par3);
        for(IGuiWidget widget : widgets) {
            if(widget.getBounds().contains(par1, par2)) widget.onMouseClicked(par1, par2, par3);
            else widget.onMouseClickedOutsideBounds(par1, par2, par3);
        }
    }

    @Override
    public void actionPerformed(IGuiWidget widget){
        sendPacketToServer(widget.getID());
    }

    protected void sendPacketToServer(int id){
        NetworkHandler.sendToServer(new PacketGuiButton(id));
    }

    @Override
    public void handleMouseInput() throws IOException{
        super.handleMouseInput();
        for(IGuiWidget widget : widgets) {
            widget.handleMouseInput();
        }
    }

    @Override
    protected void keyTyped(char key, int keyCode) throws IOException{
        for(IGuiWidget widget : widgets) {
            if(widget.onKey(key, keyCode)) return;
        }
        super.keyTyped(key, keyCode);
    }

    @Override
    public void setWorldAndResolution(Minecraft par1Minecraft, int par2, int par3){
        widgets.clear();
        super.setWorldAndResolution(par1Minecraft, par2, par3);
    }

    public void refreshScreen(){
        ScaledResolution scaledresolution = new ScaledResolution(mc);
        int i = scaledresolution.getScaledWidth();
        int j = scaledresolution.getScaledHeight();
        setWorldAndResolution(mc, i, j);
    }

    public void scheduleRefresh(){
        refreshScheduled = true;
    }

    public void drawHoveringString(List<String> text, int x, int y, FontRenderer fontRenderer){
        drawHoveringText(text, x, y, fontRenderer);
    }

    /* public static void drawTexture(String texture, int x, int y){
         Minecraft.getMinecraft().getTextureManager().bindTexture(GuiUtils.getResourceLocation(texture));
         VertexBuffer wr = Tessellator.getInstance().getBuffer();
         wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
         wr.pos(x, y + 16, 0).tex(0.0, 1.0).endVertex();
         wr.pos(x + 16, y + 16, 0).tex(1.0, 1.0).endVertex();
         wr.pos(x + 16, y, 0).tex(1.0, 0.0).endVertex();
         wr.pos(x, y, 0).tex(0.0, 0.0).endVertex();
         Tessellator.getInstance().draw();
         // this.drawTexturedModalRect(x, y, 0, 0, 16, 16);
     }*/

    @Override
    public int getGuiLeft(){
        return guiLeft;
    }

    @Override
    public int getGuiTop(){
        return guiTop;
    }

    @Override
    public void onKeyTyped(IGuiWidget widget){

    }

    public void onFieldSynced(){

    }
}
