package com.minemaarten.signals.rail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.inventory.ContainerNetworkController;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.PacketUpdateNetworkController;
import com.minemaarten.signals.tileentity.TileEntitySignalBase;
import com.minemaarten.signals.tileentity.TileEntityStationMarker;

public class NetworkController {
	private static final Map<Integer, NetworkController> SERVER_INSTANCES = new HashMap<Integer, NetworkController>();
	private static final Map<Integer, NetworkController> CLIENT_INSTANCES = new HashMap<Integer, NetworkController>();
	private static final int RAIL_COLOR = 0xFF666666;
	private static final int PATH_COLOR = 0xFFAAAAAA;
	private static final int TEXT_COLOR = 0xFFFFFF; //No alpha
	private static final int STATION_COLOR = 0xFFDDDD00;
	private static final int NOTHING_COLOR = 0xFF222222;
    
	public static NetworkController getInstance(World world){
        return getInstance(world.provider.getDimension(), world.isRemote);
    }

    public static NetworkController getInstance(int dimension, boolean client){
    	Map<Integer, NetworkController> cache = client ? CLIENT_INSTANCES : SERVER_INSTANCES;
        NetworkController controller = cache.get(dimension);
        if(controller == null) {
        	controller = new NetworkController(dimension);
        	if(client) controller.setColors(new int[]{0}, 1, 1, 0, 0);
        	cache.put(dimension, controller);
        	if(!client) controller.rebuildAll();
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
    	if(dynamicTexture == null || this.width != width || this.height != height){
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
    
    @SideOnly(Side.CLIENT)
    public void render(World world){
    	if(textureLoc == null) setColors(new int[1], 1, 1, 0, 0);
    	Minecraft mc = Minecraft.getMinecraft();
    	mc.getTextureManager().bindTexture(textureLoc);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        Gui.drawModalRectWithCustomSizedTexture(0, 0, 0, 0, width, height, width, height);
    	Tessellator t = Tessellator.getInstance();
    	VertexBuffer buffer = t.getBuffer();
    	for(EntityMinecart cart : world.getEntitiesWithinAABB(EntityMinecart.class, new AxisAlignedBB(startX, 0, startZ, startX + width, 255, startZ + height))){
    		List<BlockPos> path = cart.getCapability(CapabilityMinecartDestination.INSTANCE, null).getNBTPath();
        	if(path != null){
        		for(BlockPos pathPos : path){
        			int pathX = pathPos.getX() - startX;
        			int pathZ = pathPos.getZ() - startZ;
	        		Gui.drawRect(pathX, pathZ, pathX + 1, pathZ + 1, PATH_COLOR);
        		}
        	}
    	}
    	for(EntityMinecart cart : world.getEntitiesWithinAABB(EntityMinecart.class, new AxisAlignedBB(startX, 0, startZ, startX + width, 255, startZ + height))){
    		double x = cart.posX - startX - 0.5;
        	double y = cart.posZ - startZ - 0.5;
        	GlStateManager.color(0, 0, 1);
        	GlStateManager.disableTexture2D();
        	buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
            buffer.pos(x, y, 0).endVertex();
        	buffer.pos(x, y + 1, 0).endVertex();
        	buffer.pos(x + 1, y + 1, 0).endVertex();
        	buffer.pos(x + 1, y, 0).endVertex();
        	t.draw();
            GlStateManager.enableTexture2D();
            
            String dest = cart.getCapability(CapabilityMinecartDestination.INSTANCE, null).getCurrentDestination();
            if(dest != null){
            	GlStateManager.pushMatrix();
            	double scale = 1/4D;
            	GlStateManager.translate(x + 1.5, y, 0);
                GlStateManager.scale(scale, scale, scale);
            	mc.fontRendererObj.drawString(dest, 0, 0, TEXT_COLOR);
            	GlStateManager.popMatrix();
            }
        }
    	for(TileEntity te : world.loadedTileEntityList){
    		if(te instanceof TileEntityStationMarker){
    			double x = te.getPos().getX() - startX - 0.5;
            	double y = te.getPos().getZ() - startZ - 0.5;
    			
    			GlStateManager.pushMatrix();
            	double scale = 1/4D;
            	GlStateManager.translate(x + 2, y, 0);
                GlStateManager.scale(scale, scale, scale);
            	mc.fontRendererObj.drawString(((TileEntityStationMarker)te).getStationName(), 0, 0, 0xFFFFFF00);
            	GlStateManager.popMatrix();
    		}
    	}
    }
    
    private void setColor(int x, int z, int color){
    	setColor(x, z, color, true);
    }
    
    private void setColor(int x, int z, int color, boolean sendPacket){
    	int index = (x- startX) + (z - startZ) * width;
		if(colors[index] != color){
			colors[index] = color;
			if(sendPacket) sendUpdatePacket();
		}
    }
    
    private void sendUpdatePacket(){
    	PacketUpdateNetworkController packet = new PacketUpdateNetworkController(dimensionId, colors, width, height, startX, startZ);
    	for(EntityPlayer player : DimensionManager.getWorld(dimensionId).playerEntities){
    		if(shouldPlayerGetUpdates(player)){
    			NetworkHandler.sendTo(packet, (EntityPlayerMP) player);
    		}
    	}
    	
    }
    
    private boolean shouldPlayerGetUpdates(EntityPlayer player){
    	return player.openContainer instanceof ContainerNetworkController;
    }
    
    public void updateColor(RailWrapper rail, BlockPos pos){
    	updateColor(rail != null ? RAIL_COLOR : NOTHING_COLOR, pos);
    }
    
    public void updateColor(TileEntitySignalBase signal, BlockPos pos){
    	updateColor(signal != null ? signal.getLampStatus().color : NOTHING_COLOR, pos);
    }
    
    public void updateColor(TileEntityStationMarker station, BlockPos pos){
    	updateColor(station != null ? STATION_COLOR : NOTHING_COLOR, pos);
    }
    
    public void updateColor(int color, BlockPos... positions){
    	boolean rebuildAll = false;
    	for(BlockPos pos : positions){
	    	if(color == NOTHING_COLOR){
	    		if(pos.getX() == startX || pos.getX() == startX + width || pos.getZ() == startZ || pos.getZ() == startZ + height){
	    			rebuildAll = true; //When a border rail is removed it might decrease the total map size.
	    		}
	    	}else{
	    		if (pos.getX() < startX || pos.getX() >= startX + width || pos.getZ() < startZ || pos.getZ() >= startZ + height){
	    			rebuildAll = true; //When the rail is outside the current map scope we need to increase the map size.
	    		}
	    	}
	    	if(!rebuildAll) setColor(pos.getX(), pos.getZ(), color);
    	}
    	if(rebuildAll) rebuildAll();
    }
    
    public void rebuildAll(){
    	Iterable<RailWrapper> allRails = RailCacheManager.getInstance(dimensionId).getAllRails();
    	Map<BlockPos, Integer> posToColor = new HashMap<BlockPos, Integer>();
    	for(RailWrapper wrapper : allRails){
    		posToColor.put(wrapper, RAIL_COLOR);
    	}
    	for(TileEntity te : DimensionManager.getWorld(dimensionId).tickableTileEntities){
    		if(te instanceof TileEntitySignalBase){
    			posToColor.put(te.getPos(), ((TileEntitySignalBase) te).getLampStatus().color);
    		}else if(te instanceof TileEntityStationMarker){
    			posToColor.put(te.getPos(), STATION_COLOR);
    		}
    	}
    	
    	//allRails = Lists.newArrayList(allRails);
    	startX = Integer.MAX_VALUE;
    	startZ = Integer.MAX_VALUE;
    	int endX = Integer.MIN_VALUE;
    	int endZ = Integer.MIN_VALUE;
    	for(BlockPos pos : posToColor.keySet()){
    		startX = Math.min(startX, pos.getX());
    		startZ = Math.min(startZ, pos.getZ());
    		endX = Math.max(endX, pos.getX());
    		endZ = Math.max(endZ, pos.getZ());
    	}
    	
    	if(posToColor.isEmpty()){
    		width = 1;
    		height = 1;
    		colors = new int[1];
    		Arrays.fill(colors, NOTHING_COLOR);
    	}else{
    		width = endX - startX + 1;
        	height = endZ - startZ + 1;
        	colors = new int[width * height];
        	Arrays.fill(colors, NOTHING_COLOR);
        	for(Map.Entry<BlockPos, Integer> entry : posToColor.entrySet()){
        		setColor(entry.getKey().getX(), entry.getKey().getZ(), entry.getValue(), false);
        	}
    	}
    	
    	sendUpdatePacket();
    }
}
