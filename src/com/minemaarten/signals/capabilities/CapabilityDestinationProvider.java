package com.minemaarten.signals.capabilities;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import com.minemaarten.signals.api.tileentity.IDestinationProvider;

public class CapabilityDestinationProvider{
    @CapabilityInject(CapabilityDestinationProvider.class)
    public static Capability<CapabilityDestinationProvider> INSTANCE;

    private List<IDestinationProvider> destinationProviders = new ArrayList<IDestinationProvider>();

    public static void register(){
        CapabilityManager.INSTANCE.register(CapabilityDestinationProvider.class, new Capability.IStorage<CapabilityDestinationProvider>(){
            @Override
            public NBTBase writeNBT(Capability<CapabilityDestinationProvider> capability, CapabilityDestinationProvider instance, EnumFacing side){
                NBTTagCompound tag = new NBTTagCompound();
                for(IDestinationProvider provider : instance.destinationProviders) {
                    provider.writeToNBT(tag);
                }
                return tag;
            }

            @Override
            public void readNBT(Capability<CapabilityDestinationProvider> capability, CapabilityDestinationProvider instance, EnumFacing side, NBTBase base){
                NBTTagCompound tag = (NBTTagCompound)base;
                for(IDestinationProvider provider : instance.destinationProviders) {
                    provider.readFromNBT(tag);
                }
            }

        }, new Callable<CapabilityDestinationProvider>(){
            @Override
            public CapabilityDestinationProvider call() throws Exception{
                return new CapabilityDestinationProvider();
            }
        });
    }

    public static class Provider implements ICapabilitySerializable<NBTBase>{
        private final CapabilityDestinationProvider cap = new CapabilityDestinationProvider();

        @Override
        public boolean hasCapability(Capability<?> capability, EnumFacing facing){
            return capability == INSTANCE;
        }

        @Override
        public <T> T getCapability(Capability<T> capability, EnumFacing facing){
            if(hasCapability(capability, facing)) {
                return (T)cap;
            } else {
                return null;
            }
        }

        @Override
        public NBTBase serializeNBT(){
            return INSTANCE.getStorage().writeNBT(INSTANCE, cap, null);
        }

        @Override
        public void deserializeNBT(NBTBase nbt){
            INSTANCE.getStorage().readNBT(INSTANCE, cap, null, nbt);
        }
    }

    public void addDestinationProvider(IDestinationProvider provider){
        destinationProviders.add(provider);
    }

    public List<IDestinationProvider> getApplicableDestinationProviders(){
        return destinationProviders;
    }

    public <T extends IDestinationProvider> T getProvider(Class<T> type){
        for(IDestinationProvider provider : destinationProviders) {
            if(type.isAssignableFrom(provider.getClass())) {
                return (T)provider;
            }
        }
        return null;
    }

    public boolean isCartApplicable(TileEntity te, EntityMinecart cart, Pattern destinationRegex){
        for(IDestinationProvider provider : destinationProviders) {
            if(provider.isCartApplicable(te, cart, destinationRegex)) {
                return true;
            }
        }
        return false;
    }
}
