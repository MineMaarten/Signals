package com.minemaarten.signals.api.access;

import com.minemaarten.signals.api.tileentity.IDestinationProvider;

/**
 * A way to interact with the settings of the items destination provider. Do not implement.
 * Retrieve this using ISignalsAccessor
 * @author Maarten
 *
 */
public interface IDestinationProviderItems extends IDestinationProvider{
    public void useBlacklist(boolean useBlacklist);

    public boolean isUsingBlacklist();

    public void checkDamage(boolean checkDamage);

    public boolean isCheckingDamage();

    public void checkNBT(boolean checkNBT);

    public boolean isCheckingNBT();

    public void checkModSimilarity(boolean checkModSimilarity);

    public boolean isCheckingModSimilarity();

    public void checkOreDictionary(boolean checkOreDictionary);

    public boolean isCheckingOreDictionary();
}
