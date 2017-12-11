package com.minemaarten.signals.dispenser;

import java.util.List;

import net.minecraft.block.BlockDispenser;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;

import com.minemaarten.signals.item.ItemTicket;

public class BehaviorDispenseTicket extends BehaviorDefaultDispenseItem{
    @Override
    protected ItemStack dispenseStack(IBlockSource source, ItemStack stack){
        EnumFacing facing = source.getBlockState().getValue(BlockDispenser.FACING);
        AxisAlignedBB aabb = new AxisAlignedBB(source.getBlockPos().offset(facing));
        List<EntityMinecart> carts = source.getWorld().getEntitiesWithinAABB(EntityMinecart.class, aabb);
        carts.forEach(cart -> ItemTicket.applyDestinations(cart, stack));
        return stack;
    }
}
