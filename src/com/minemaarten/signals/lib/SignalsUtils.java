package com.minemaarten.signals.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class SignalsUtils {
    /** this method takes one very long string, and cuts it into lines which have
    a maxCharPerLine and returns it in a String list.
    it also preserves color formats. \n can be used to force a carriage
    return.
    */
   public static List<String> convertStringIntoList(String text, int maxCharPerLine){
       StringTokenizer tok = new StringTokenizer(text, " ");
       StringBuilder output = new StringBuilder(text.length());
       List<String> textList = new ArrayList<String>();
       String color = "";
       int lineLen = 0;
       while(tok.hasMoreTokens()) {
           String word = tok.nextToken();
           if(word.contains("\u00a7")) {// if there is a text formatter
                                        // present.
               for(int i = 0; i < word.length() - 1; i++)
                   if(word.substring(i, i + 2).contains("\u00a7")) color = word.substring(i, i + 2); // retrieve
                                                                                                     // the
                                                                                                     // color
                                                                                                     // format.
               lineLen -= 2;// don't count a color formatter with the line
                            // length.
           }
           if(lineLen + word.length() > maxCharPerLine || word.contains("\\n")) {
               word = word.replace("\\n", "");
               textList.add(output.toString());
               output.delete(0, output.length());
               output.append(color);
               lineLen = 0;
           } else if(lineLen > 0) {
               output.append(" ");
               lineLen++;
           }
           output.append(word);
           lineLen += word.length();
       }
       textList.add(output.toString());
       return textList;
   }
   
   public static void writeInventoryToNBT(NBTTagCompound tag, ItemStack[] stacks){
       writeInventoryToNBT(tag, stacks, "Items");
   }
   
   public static void writeInventoryToNBT(NBTTagCompound tag, IInventory inv){
       writeInventoryToNBT(tag, inv, "Items");
   }

   public static void writeInventoryToNBT(NBTTagCompound tag, IInventory inventory, String tagName){
       ItemStack[] stacks = new ItemStack[inventory.getSizeInventory()];
       for(int i = 0; i < stacks.length; i++) {
           stacks[i] = inventory.getStackInSlot(i);
       }
       writeInventoryToNBT(tag, stacks, tagName);
   }

   public static void writeInventoryToNBT(NBTTagCompound tag, ItemStack[] stacks, String tagName){
       NBTTagList tagList = new NBTTagList();
       for(int i = 0; i < stacks.length; i++) {
           if(stacks[i] != null) {
               NBTTagCompound itemTag = new NBTTagCompound();
               stacks[i].writeToNBT(itemTag);
               itemTag.setByte("Slot", (byte)i);
               tagList.appendTag(itemTag);
           }
       }
       tag.setTag(tagName, tagList);
   }

   public static void readInventoryFromNBT(NBTTagCompound tag, ItemStack[] stacks){
       readInventoryFromNBT(tag, stacks, "Items");
   }
   
   public static void readInventoryFromNBT(NBTTagCompound tag, IInventory inv){
       readInventoryFromNBT(tag, inv, "Items");
   }

   public static void readInventoryFromNBT(NBTTagCompound tag, IInventory inventory, String tagName){
       ItemStack[] stacks = new ItemStack[inventory.getSizeInventory()];
       readInventoryFromNBT(tag, stacks, tagName);
       for(int i = 0; i < stacks.length; i++) {
           inventory.setInventorySlotContents(i, stacks[i]);
       }
   }

   public static void readInventoryFromNBT(NBTTagCompound tag, ItemStack[] stacks, String tagName){
       for(int i = 0; i < stacks.length; i++) {
           stacks[i] = null;
       }
       NBTTagList tagList = tag.getTagList(tagName, 10);
       for(int i = 0; i < tagList.tagCount(); i++) {
           NBTTagCompound itemTag = tagList.getCompoundTagAt(i);
           int slot = itemTag.getByte("Slot");
           if(slot >= 0 && slot < stacks.length) {
               stacks[slot] = new ItemStack(itemTag);
           }
       }
   }

}
