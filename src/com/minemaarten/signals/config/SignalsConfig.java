package com.minemaarten.signals.config;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.Name;
import net.minecraftforge.common.config.Config.Type;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import com.minemaarten.signals.block.BlockSignalBase;
import com.minemaarten.signals.init.ModBlocks;
import com.minemaarten.signals.init.ModItems;
import com.minemaarten.signals.lib.Constants;
import com.minemaarten.signals.lib.Log;

@Config(modid = Constants.MOD_ID)
@EventBusSubscriber(modid = Constants.MOD_ID)
public class SignalsConfig{

    public static ClientConfig client = new ClientConfig();
    @Name("Enable rail network")
    @Comment("ONLY SET TO FALSE IN CASE OF CRASHES. With this set to false, Signals will not work at all!")
    public static boolean enableRailNetwork = true;

    @Name("Cart blacklists")
    @Comment("Useful to disallow carts from Signals interaction. Cart names are how they would be used in a 'summon' command (<modid>:<entityName>).")
    public static CartBlacklists cartBlacklists = new CartBlacklists();

    @SubscribeEvent
    public static void onConfigChangedEvent(OnConfigChangedEvent event){
        if(event.getModID().equals(Constants.MOD_ID)) {
            ConfigManager.sync(Constants.MOD_ID, Type.INSTANCE);
            if(!enableRailNetwork) {
                Log.warning("RAIL NETWORK IS NOT FUNCTIONAL!");
            }
        }
    }

    public static class ClientConfig{
        @Name("Network visualization")
        public NetworkVisualization networkVisualization = new NetworkVisualization();
    }

    public static class NetworkVisualization{

        @Name("Valid items")
        @Comment("When one of these items is held, the rail network visualization will show.")
        public String[] validItems = new String[0];

        @Name("Not sneaking")
        @Comment("What is shown when the player is holding an applicable item, and is not sneaking")
        public NetworkVisualizationSettings notSneaking = new NetworkVisualizationSettings(EnumRenderType.SECTION);

        @Name("Sneaking")
        @Comment("What is shown when the player is holding an applicable item, is sneaking")
        public NetworkVisualizationSettings sneaking = new NetworkVisualizationSettings(EnumRenderType.PATHS);

        public void initDefaults(){
            if(validItems.length == 0) {
                List<Item> items = new ArrayList<>();
                items.add(ModItems.RAIL_CONFIGURATOR);
                items.add(Item.getItemFromBlock(ModBlocks.RAIL_LINK));

                for(Item item : Item.REGISTRY) {
                    if(item instanceof ItemBlock) {
                        Block block = ((ItemBlock)item).getBlock();
                        if(block instanceof BlockSignalBase || block instanceof BlockRailBase) {
                            items.add(item);
                        }
                    }
                }

                validItems = new String[items.size()];
                for(int i = 0; i < items.size(); i++) {
                    validItems[i] = Item.REGISTRY.getNameForObject(items.get(i)).toString();
                }
            }
        }

        public boolean isValid(Item item){
            String name = Item.REGISTRY.getNameForObject(item).toString();
            for(String validItem : validItems) {
                if(validItem.equals(name)) return true;
            }
            return false;
        }
    }

    public static class NetworkVisualizationSettings{
        @Name("Render type")
        public EnumRenderType renderType;

        @Name("Render directionality")
        @Comment("Whether or not to render the arrows indicating which way trains can travel in")
        public boolean renderDirectionality = true;

        public NetworkVisualizationSettings(){}

        public NetworkVisualizationSettings(EnumRenderType defaultRenderType){
            renderType = defaultRenderType;
        }
    }

    public static enum EnumRenderType{
        SECTION, PATHS, EDGES
    }

    public static class CartBlacklists{
        @Name("Cart Engine")
        @Comment("Engines cannot be applied to carts in this config option")
        public String[] cartEngines = new String[0];

        public static boolean isBlacklisted(EntityMinecart cart, String[] config){
            if(config.length == 0) return false;
            EntityEntry entry = EntityRegistry.getEntry(cart.getClass());
            ResourceLocation cartID = ForgeRegistries.ENTITIES.getKey(entry);
            for(String blacklist : config) {
                if(cartID.equals(new ResourceLocation(blacklist))) {
                    return true;
                }
            }
            return false;
        }
    }
}
