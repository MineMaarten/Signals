package com.minemaarten.signals.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import com.minemaarten.signals.client.glasses.GlassesHUD;
import com.minemaarten.signals.client.glasses.GlassesMessage;

public class PacketUpdateMessage extends LocationIntPacket<PacketUpdateMessage>{

    public int entityId;
    public String message;
    public String[] args;

    public PacketUpdateMessage(){

    }

    /* public PacketUpdateMessage(TileEntity te, EntityMinecart cart, String message, int... args){
         this(te, cart, message, toStringArray(args));
     }

     private static String[] toStringArray(int[] integers){
         String[] strings = new String[integers.length];
         for(int i = 0; i < strings.length; i++) {
             strings[i] = Integer.toString(integers[i]);
         }
         return strings;
     }*/

    public PacketUpdateMessage(TileEntity te, EntityMinecart cart, String message, String... args){
        super(te.getPos());
        entityId = cart.getEntityId();
        this.message = message;
        this.args = args;
    }

    @Override
    public void toBytes(ByteBuf buf){
        super.toBytes(buf);
        buf.writeInt(entityId);
        ByteBufUtils.writeUTF8String(buf, message);
        buf.writeInt(args.length);
        for(String arg : args)
            ByteBufUtils.writeUTF8String(buf, arg);
    }

    @Override
    public void fromBytes(ByteBuf buf){
        super.fromBytes(buf);
        entityId = buf.readInt();
        message = ByteBufUtils.readUTF8String(buf);
        args = new String[buf.readInt()];
        for(int i = 0; i < args.length; i++) {
            args[i] = ByteBufUtils.readUTF8String(buf);
        }
    }

    @Override
    public void handleClientSide(EntityPlayer player){
        GlassesHUD.getInstance().onNewMessage(new GlassesMessage(this, player.world));
    }

    @Override
    public void handleServerSide(EntityPlayer player){

    }

}
