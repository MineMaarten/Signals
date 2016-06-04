package com.minemaarten.signals.client.glasses;

import jline.internal.Log;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.minemaarten.signals.network.PacketUpdateMessage;

public class GlassesMessage{
    public final String localizedMessage;
    public final EntityMinecart associatedCart;
    public final BlockPos associatedRouter;

    public GlassesMessage(PacketUpdateMessage packet, World world){
        associatedRouter = packet.pos;
        Entity cart = world.getEntityByID(packet.entityId);
        associatedCart = cart instanceof EntityMinecart ? (EntityMinecart)cart : null;

        String message = packet.message;
        message = I18n.format(message);
        message = message.replaceAll("\\$ROUTER\\$", String.format("(%s, %s, %s)", associatedRouter.getX(), associatedRouter.getY(), associatedRouter.getZ()));

        if(associatedCart != null){
	        BlockPos cartPos = associatedCart.getPosition();
	        message = message.replaceAll("\\$CART\\$", String.format("(%s at (%s, %s, %s)", associatedCart.getName(), cartPos.getX(), cartPos.getY(), cartPos.getZ()));
        }else{
        	Log.warn("Cart is null!");
        }
        
        //Format the args
        for(int i = 0; i < packet.args.length; i++) {
            if(packet.args[i].startsWith("signals.")) {
                packet.args[i] = I18n.format(packet.args[i]);
            }
        }

        localizedMessage = String.format(message, packet.args);
    }
}
