package com.minemaarten.signals.api.access;

import java.util.List;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.tileentity.TileEntity;

import com.minemaarten.signals.api.tileentity.IDestinationProvider;

/**
 * Main entry point for accessing Signals information from (Tile)Entities in the world.
 * Obtain an instance of this interface by subscribing to the {@link SignalsAccessorProvidingEvent}.
 * In the post-init mod loading phase this event will be raised with an instance of ISignalsAccessor.
 * 
 * Example:
 * 
 * <pre>
 * import net.minecraftforge.fml.common.Optional;
 * import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
 * import com.minemaarten.signals.api.access.ISignalsAccessor;
 * import com.minemaarten.signals.api.access.SignalsAccessorProvidingEvent;
 *
 * public class SignalsAccessorTest{
 *   @SubscribeEvent
 *   @Optional.Method(modid = "signals")
 *   public void onSignalsAccessorProvided(SignalsAccessorProvidingEvent event){
 *      //Save this accessor somewhere to use when needed.
 *      ISignalsAccessor accessor = event.accessor;
 *   }
 * }
 * </pre>
 * 
 * @author Maarten
 *
 */
public interface ISignalsAccessor{
    public IDestinationAccessor getDestinationAccessor(EntityMinecart cart);

    /**
     * Gets the destination providers from a given tile entity.
     * For IItemHandlers this is {@link IDestinationProviderItems}
     * @param te
     * @return
     */
    public List<IDestinationProvider> getDestinationProviders(TileEntity te);

    /**
     * Helper method for retrieving the items destination provider.
     * @param te
     * @return the items destination provider, or null, if not applicable for this TileEntity.
     */
    public default IDestinationProviderItems getDestinationProviderItem(TileEntity te){
        for(IDestinationProvider d : getDestinationProviders(te)) {
            if(d instanceof IDestinationProviderItems) {
                return (IDestinationProviderItems)d;
            }
        }
        return null;
    }
}
