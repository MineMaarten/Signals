package com.minemaarten.signals.rail;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

import com.google.common.base.Predicates;
import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.rail.network.NetworkObject;
import com.minemaarten.signals.rail.network.NetworkStation;
import com.minemaarten.signals.rail.network.RailNetwork;
import com.minemaarten.signals.rail.network.RailNetworkClient;
import com.minemaarten.signals.rail.network.RailRoute;
import com.minemaarten.signals.rail.network.Train;
import com.minemaarten.signals.rail.network.mc.MCPos;
import com.minemaarten.signals.rail.network.mc.RailNetworkManager;

public class NetworkController{
    public static final int RAIL_COLOR = 0xFF666666;
    public static final int PATH_COLOR = 0xFFAAAAAA;
    public static final int TEXT_COLOR = 0xFFFFFF; //No alpha
    public static final int STATION_COLOR = 0xFFDDDD00;
    public static final int NOTHING_COLOR = 0xFF222222;
    private static Map<Integer, NetworkController> cache = new HashMap<>();
    private static RailNetworkClient<MCPos> network = RailNetworkClient.empty();

    public static NetworkController getInstance(World world){
        if(!world.isRemote) throw new IllegalStateException("Can only be called client side!");
        return getInstance(world.provider.getDimension());
    }

    public static NetworkController getInstance(int dimension){
        if(network != RailNetworkManager.getInstance().getClientNetwork()) {
            network = RailNetworkManager.getInstance().getClientNetwork();
            cache = rebuildAll();
        }

        NetworkController controller = cache.get(dimension);
        if(controller == null) {
            controller = new NetworkController(dimension);
            controller.setColors(new int[]{0}, 1, 1, 0, 0);
            cache.put(dimension, controller);
        }
        return controller;
    }

    @SideOnly(Side.CLIENT)
    private DynamicTexture dynamicTexture;
    private int width, height;
    private int[] colors;
    private int startX, startZ;
    private final int dimensionId;
    private ResourceLocation textureLoc;

    public NetworkController(int dimensionId){
        this.dimensionId = dimensionId;
    }

    @SideOnly(Side.CLIENT)
    public void setColors(int[] colors, int width, int height, int startX, int startZ){
        this.startX = startX;
        this.startZ = startZ;
        if(dynamicTexture == null || this.width != width || this.height != height) {
            this.width = width;
            this.height = height;
            if(dynamicTexture != null) dynamicTexture.deleteGlTexture();
            dynamicTexture = new DynamicTexture(width, height);
            textureLoc = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation("networkController/" + dimensionId, dynamicTexture);
        }
        System.arraycopy(colors, 0, dynamicTexture.getTextureData(), 0, colors.length);
        dynamicTexture.updateDynamicTexture();
    }

    public int getWidth(){
        return width;
    }

    public int getHeight(){
        return height;
    }

    public int getStartX(){
        return startX;
    }

    public int getStartZ(){
        return startZ;
    }

    private List<RailRoute<MCPos>> getAllRoutes(){
        return RailNetworkManager.getInstance().getAllTrains().map(t -> t.getCurRoute()).filter(Predicates.notNull()).collect(Collectors.toList());
    }

    @SideOnly(Side.CLIENT)
    public void render(World world){
        if(textureLoc == null) setColors(new int[1], 1, 1, 0, 0);
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(textureLoc);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawModalRectWithCustomSizedTexture(0, 0, 0, 0, width, height, width, height);
        Tessellator t = Tessellator.getInstance();
        BufferBuilder buffer = t.getBuffer();

        //Draw the path
        for(RailRoute<MCPos> route : getAllRoutes()) {
            for(MCPos pathPos : route.routeRails) {
                if(pathPos.getDimID() == dimensionId) {
                    int pathX = pathPos.getX() - startX;
                    int pathZ = pathPos.getZ() - startZ;
                    Gui.drawRect(pathX, pathZ, pathX + 1, pathZ + 1, PATH_COLOR);
                }
            }
        }

        //Draw the train locations
        GlStateManager.color(0, 0, 1);
        GlStateManager.disableTexture2D();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        for(Train<MCPos> train : RailNetworkManager.getInstance().getAllTrains().collect(Collectors.toList())) {
            for(MCPos pos : train.getPositions()) {
                if(pos.getDimID() != dimensionId) continue;

                double x = pos.getX() - startX;
                double y = pos.getZ() - startZ;

                buffer.pos(x, y, 0).endVertex();
                buffer.pos(x, y + 1, 0).endVertex();
                buffer.pos(x + 1, y + 1, 0).endVertex();
                buffer.pos(x + 1, y, 0).endVertex();
            }
        }
        t.draw();
        GlStateManager.enableTexture2D();

        //Draw the destination names next to the trains
        for(EntityMinecart cart : world.getEntitiesWithinAABB(EntityMinecart.class, new AxisAlignedBB(startX, 0, startZ, startX + width, 255, startZ + height))) {
            double x = cart.posX - startX - 0.5;
            double y = cart.posZ - startZ - 0.5;
            GlStateManager.color(0, 0, 1);

            String dest = cart.getCapability(CapabilityMinecartDestination.INSTANCE, null).getCurrentDestination();
            if(dest != null) {
                GlStateManager.pushMatrix();
                double scale = 1 / 4D;
                GlStateManager.translate(x + 1.5, y, 0);
                GlStateManager.scale(scale, scale, scale);
                mc.fontRenderer.drawString(dest, 0, 0, TEXT_COLOR);
                GlStateManager.popMatrix();
            }
        }

        //Draw the station names next to the stations
        RailNetwork<MCPos> network = RailNetworkManager.getInstance().getNetwork();
        for(NetworkStation<MCPos> station : network.railObjects.getStations()) {
            double x = station.pos.getX() - startX - 0.5;
            double y = station.pos.getZ() - startZ - 0.5;

            GlStateManager.pushMatrix();
            double scale = 1 / 4D;
            GlStateManager.translate(x + 2, y, 0);
            GlStateManager.scale(scale, scale, scale);
            mc.fontRenderer.drawString(station.stationName, 0, 0, 0xFFFFFF00);
            GlStateManager.popMatrix();
        }
    }

    private void setColor(int x, int z, int color){
        setColor(x, z, color, true);
    }

    private void setColor(int x, int z, int color, boolean sendPacket){
        int index = (x - startX) + (z - startZ) * width;
        if(index < 0 || index >= colors.length) { //If we are out of bounds that means we don't properly know the right dims.
            rebuildAll();
        } else {
            colors[index] = color;
        }
    }

    public void updateColor(int color, BlockPos... positions){
        boolean rebuildAll = false;
        for(BlockPos pos : positions) {
            if(color == NOTHING_COLOR) {
                if(pos.getX() == startX || pos.getX() == startX + width || pos.getZ() == startZ || pos.getZ() == startZ + height) {
                    rebuildAll = true; //When a border rail is removed it might decrease the total map size.
                }
            } else {
                if(pos.getX() < startX || pos.getX() >= startX + width || pos.getZ() < startZ || pos.getZ() >= startZ + height) {
                    rebuildAll = true; //When the rail is outside the current map scope we need to increase the map size.
                }
            }
            if(!rebuildAll) {
                setColor(pos.getX(), pos.getZ(), color);
                setColors(colors, width, height, startX, startZ);
            }
        }
        if(rebuildAll) rebuildAll();
    }

    public static Map<Integer, NetworkController> rebuildAll(){
        Map<Integer, NetworkController> cache = new HashMap<>();

        Collection<NetworkObject<MCPos>> allObjects = RailNetworkManager.getInstance().getNetwork().railObjects.getAllNetworkObjects().values();
        Map<Integer, List<NetworkObject<MCPos>>> objsByDim = allObjects.stream().collect(Collectors.groupingBy(o -> o.pos.getDimID()));

        for(Map.Entry<Integer, List<NetworkObject<MCPos>>> entry : objsByDim.entrySet()) {
            NetworkController controller = new NetworkController(entry.getKey());
            controller.rebuild(entry.getValue());
            controller.setColors(controller.colors, controller.width, controller.height, controller.startX, controller.startZ);

            cache.put(entry.getKey(), controller);
        }

        return cache;
    }

    private void rebuild(Collection<NetworkObject<MCPos>> objects){
        startX = Integer.MAX_VALUE;
        startZ = Integer.MAX_VALUE;
        int endX = Integer.MIN_VALUE;
        int endZ = Integer.MIN_VALUE;
        for(NetworkObject<MCPos> obj : objects) {
            MCPos pos = obj.pos;
            startX = Math.min(startX, pos.getX());
            startZ = Math.min(startZ, pos.getZ());
            endX = Math.max(endX, pos.getX());
            endZ = Math.max(endZ, pos.getZ());
        }

        width = endX - startX + 1;
        height = endZ - startZ + 1;
        colors = new int[width * height];
        Arrays.fill(colors, NOTHING_COLOR);

        for(NetworkObject<MCPos> obj : objects) {
            setColor(obj.pos.getX(), obj.pos.getZ(), obj.getColor(), false);
        }
    }
}
