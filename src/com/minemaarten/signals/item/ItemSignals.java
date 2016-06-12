package com.minemaarten.signals.item;

import java.util.List;

import com.minemaarten.signals.Signals;
import com.minemaarten.signals.lib.SignalsUtils;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemSignals extends Item {
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> curInfo, boolean extraInfo){
        super.addInformation(stack, player, curInfo, extraInfo);
        addTooltip(stack, player, curInfo);
    }

    public static void addTooltip(ItemStack stack, EntityPlayer player, List<String> curInfo){
        String info = "gui.tooltip." + stack.getItem().getUnlocalizedName();
        String translatedInfo = I18n.format(info);
        if(!translatedInfo.equals(info)) {
            if(Signals.proxy.isSneakingInGui()) {
                translatedInfo = TextFormatting.AQUA + translatedInfo;
                curInfo.addAll(SignalsUtils.convertStringIntoList(translatedInfo, 60));
            } else {
                curInfo.add(TextFormatting.AQUA + I18n.format("signals.gui.tooltip.sneakForInfo"));
            }
        }
    }
}
