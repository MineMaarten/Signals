package com.minemaarten.signals.railcraft;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.nbt.NBTBase;

import com.minemaarten.signals.api.ICartLinker;
import com.minemaarten.signals.api.Signals;

@Signals
public class RailCraftTrainHandler implements ICartLinker{

    @Override
    public Set<EntityMinecart> getLinkedCarts(EntityMinecart cart){
        //Debug code:
        //return cart.world.loadedEntityList.stream().filter(x -> x instanceof EntityMinecart && x.getTags().contains("link")).map(x -> (EntityMinecart)x).collect(Collectors.toSet());

        //NBT methods based on https://github.com/brotazoa/Signals/commit/52e1466d78626e39df9db79ff48cf663ad8aa76e
        if(cart.getEntityData().hasKey("rcTrain")) // cart is part of an RC train
        {
            NBTBase tag = cart.getEntityData().getTag("rcTrain");

            Stream<EntityMinecart> allCarts = cart.world.loadedEntityList.stream().filter(entity -> entity instanceof EntityMinecart).map(entity -> (EntityMinecart)entity);
            return allCarts.filter(c -> c.getEntityData().hasKey("rcTrain") && c.getEntityData().getTag("rcTrain").equals(tag)).collect(Collectors.toSet());
        } else {
            return Collections.emptySet();
        }
    }

}
