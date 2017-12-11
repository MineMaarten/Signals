package com.minemaarten.signals.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import com.minemaarten.signals.capabilities.CapabilityMinecartDestination;
import com.minemaarten.signals.init.ModItems;
import com.minemaarten.signals.item.ItemTicket;

public class PacketUpdateTicket extends AbstractPacket<PacketUpdateTicket>{
    private String destinations;
    private String itemName;

    public PacketUpdateTicket(){}

    public PacketUpdateTicket(String destinations, String itemName){
        this.destinations = destinations;
        this.itemName = itemName;
    }

    @Override
    public void toBytes(ByteBuf buffer){
        ByteBufUtils.writeUTF8String(buffer, destinations);
        ByteBufUtils.writeUTF8String(buffer, itemName);
    }

    @Override
    public void fromBytes(ByteBuf buffer){
        destinations = ByteBufUtils.readUTF8String(buffer);
        itemName = ByteBufUtils.readUTF8String(buffer);
    }

    @Override
    public void handleClientSide(EntityPlayer player){}

    @Override
    public void handleServerSide(EntityPlayer player){
        ItemStack stack = player.getHeldItemMainhand();
        if(stack.getItem() == ModItems.TICKET) {
            CapabilityMinecartDestination cap = stack.getCapability(CapabilityMinecartDestination.INSTANCE, null);
            if(cap != null) {
                cap.setText(0, destinations);
                ItemTicket.writeNBTFromCap(stack);
                if(!"".equals(itemName)) stack.setStackDisplayName(itemName);
            }
        }
    }

}
