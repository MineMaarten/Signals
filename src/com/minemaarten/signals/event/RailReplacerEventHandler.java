package com.minemaarten.signals.event;

import java.util.List;

import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRailBase.EnumRailDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import com.minemaarten.signals.api.IRail;
import com.minemaarten.signals.rail.RailManager;

public class RailReplacerEventHandler{

    /**
     * Makes it so when a player right clicks a rail block with a different rail item, it will be replaced, without having to remove and place a rail.
     * TODO: Replace signal types
     * @param e
     */
    @SubscribeEvent
    public void onBlockInteraction(RightClickBlock e){
        if(!e.getWorld().isRemote && e.getFace() == EnumFacing.UP) {
            ItemStack stack = e.getEntityPlayer().getHeldItemMainhand();
            BlockRailBase railBlock = getRailBlock(stack);
            if(railBlock != null) {
                IBlockState state = e.getWorld().getBlockState(e.getPos());
                IRail rail = RailManager.getInstance().getRail(e.getWorld(), e.getPos(), state);
                if(rail != null && state.getBlock() != railBlock) {
                    EnumRailDirection dir = rail.getDirection(e.getWorld(), e.getPos(), state);
                    e.getWorld().destroyBlock(e.getPos(), !e.getEntityPlayer().isCreative());
                    List<EntityItem> drops = e.getWorld().getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(e.getPos()));
                    for(EntityItem drop : drops) {
                        drop.setPickupDelay(0);
                        drop.onCollideWithPlayer(e.getEntityPlayer());
                    }

                    e.getWorld().setBlockState(e.getPos(), railBlock.getDefaultState());
                    if(!e.getEntityPlayer().isCreative()) stack.shrink(1);

                    //Set the rail orientation equal to the old rail, if possible.
                    if(railBlock.getShapeProperty().getAllowedValues().contains(dir)) {
                        IBlockState curState = e.getWorld().getBlockState(e.getPos());
                        e.getWorld().setBlockState(e.getPos(), curState.withProperty(railBlock.getShapeProperty(), dir));
                    }
                }
            }
        }
    }

    private static BlockRailBase getRailBlock(ItemStack stack){
        if(stack.getItem() instanceof ItemBlock) {
            ItemBlock item = (ItemBlock)stack.getItem();
            return item.getBlock() instanceof BlockRailBase ? (BlockRailBase)item.getBlock() : null; //Only allow simple registered IRails to be placed
        }
        return null;
    }
}
