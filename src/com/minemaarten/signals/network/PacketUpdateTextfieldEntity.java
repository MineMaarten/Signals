package com.minemaarten.signals.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;

public class PacketUpdateTextfieldEntity extends AbstractPacket<PacketUpdateTextfieldEntity>{

    private int textFieldID;
    private int entityId;
    private String text;

    public PacketUpdateTextfieldEntity(){}

    public PacketUpdateTextfieldEntity(EntityMinecart cart, int textfieldID){
        textFieldID = textfieldID;
        entityId = cart.getEntityId();
        text = cart.getCapability(CapabilityMinecartDestination.INSTANCE, null).getText(textfieldID);
    }

    @Override
    public void toBytes(ByteBuf buffer){
        buffer.writeInt(textFieldID);
        buffer.writeInt(entityId);
        ByteBufUtils.writeUTF8String(buffer, text);
    }

    @Override
    public void fromBytes(ByteBuf buffer){
        textFieldID = buffer.readInt();
        entityId = buffer.readInt();
        text = ByteBufUtils.readUTF8String(buffer);
    }

    @Override
    public void handleClientSide(EntityPlayer player){}

    @Override
    public void handleServerSide(EntityPlayer player){
        Entity entity = player.world.getEntityByID(entityId);
        if(entity instanceof EntityMinecart) {
            entity.getCapability(CapabilityMinecartDestination.INSTANCE, null).setText(textFieldID, text);
        }
    }

}
