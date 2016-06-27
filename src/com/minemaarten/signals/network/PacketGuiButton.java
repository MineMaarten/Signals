package com.minemaarten.signals.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;

import com.minemaarten.signals.tileentity.IGUIButtonSensitive;

public class PacketGuiButton extends AbstractPacket<PacketGuiButton>{
    private int[] data;

    public PacketGuiButton(){}

    public PacketGuiButton(int... data){
        this.data = data;
    }

    @Override
    public void toBytes(ByteBuf buffer){
    	buffer.writeInt(data.length);
    	for(int i : data){
    		 buffer.writeInt(i);
    	}
       
    }

    @Override
    public void fromBytes(ByteBuf buffer){
    	data = new int[buffer.readInt()];
    	for(int i = 0; i < data.length; i++){
    		data[i] = buffer.readInt();
    	}
    }

    @Override
    public void handleClientSide(EntityPlayer player){}

    @Override
    public void handleServerSide(EntityPlayer player){
        if(player.openContainer instanceof IGUIButtonSensitive) {
            ((IGUIButtonSensitive)player.openContainer).handleGUIButtonPress(player, data);
        }
    }

}
