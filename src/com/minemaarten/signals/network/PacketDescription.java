package com.minemaarten.signals.network;

import io.netty.buffer.ByteBuf;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class PacketDescription extends LocationIntPacket<PacketDescription>{

    private byte[] types;
    private Object[] values;
    private NBTTagCompound extraData;
    private IDescSynced.Type type;

    public PacketDescription(){

    }

    public PacketDescription(IDescSynced te){

        super(te.getPosition());
        type = te.getSyncType();
        values = new Object[te.getDescriptionFields().size()];
        types = new byte[values.length];
        for(int i = 0; i < values.length; i++) {
            values[i] = te.getDescriptionFields().get(i).getValue();
            types[i] = PacketUpdateGui.getType(te.getDescriptionFields().get(i));
        }
        extraData = new NBTTagCompound();
        te.writeToPacket(extraData);
    }

    @Override
    public void toBytes(ByteBuf buf){

        super.toBytes(buf);
        buf.writeByte(type.ordinal());
        buf.writeInt(values.length);
        for(int i = 0; i < types.length; i++) {
            buf.writeByte(types[i]);
            PacketUpdateGui.writeField(buf, values[i], types[i]);
        }
        ByteBufUtils.writeTag(buf, extraData);
    }

    @Override
    public void fromBytes(ByteBuf buf){

        super.fromBytes(buf);
        type = IDescSynced.Type.values()[buf.readByte()];
        int dataAmount = buf.readInt();
        types = new byte[dataAmount];
        values = new Object[dataAmount];
        for(int i = 0; i < dataAmount; i++) {
            types[i] = buf.readByte();
            values[i] = PacketUpdateGui.readField(buf, types[i]);
        }
        extraData = ByteBufUtils.readTag(buf);
    }

    public static Object getSyncableForType(LocationIntPacket message, EntityPlayer player, IDescSynced.Type type, NBTTagCompound extraData){

        switch(type){
            case TILE_ENTITY:
                return message.getTileEntity(player.world);
        }
        return null;
    }

    @Override
    public void handleClientSide(EntityPlayer player){

        if(player.world.isBlockLoaded(pos)) {
            Object syncable = getSyncableForType(this, player, type, extraData);
            if(syncable instanceof IDescSynced) {
                IDescSynced descSynced = (IDescSynced)syncable;
                List<SyncedField> descFields = descSynced.getDescriptionFields();
                if(descFields != null && descFields.size() == types.length) {
                    for(int i = 0; i < descFields.size(); i++) {
                        descFields.get(i).setValue(values[i]);
                    }
                }
                descSynced.readFromPacket(extraData);
                descSynced.onDescUpdate();
            }
        }
    }

    @Override
    public void handleServerSide(EntityPlayer player){

    }

}
