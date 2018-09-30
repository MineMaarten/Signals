package com.minemaarten.signals.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.minemaarten.signals.rail.network.mc.MCPos;

public class TileEntityTeleportRail extends TileEntityRailLinkBase{
    /**
     * Only allow destination positions when:
     * 1. The position is similar to a portal position (it can't be used to cheese long distance travel)
     * 2. The receiving end has a teleport rail, to specify the destination direction.
     */
    @Override
    protected boolean isDestinationValid(MCPos destination, EntityPlayer player){
        Pair<MCPos, MCPos> allowedDestinationRange = getAllowedDestinationRange(destination.getWorld());
        if(allowedDestinationRange == null) {
            player.sendMessage(new TextComponentTranslation("signals.message.teleport_rail_failed_unloaded_destination_dimension", destination.getX(), destination.getY(), destination.getZ()));
            return false;
        } else if(destination.isInAABB(allowedDestinationRange.getLeft(), allowedDestinationRange.getRight())) {
            return true;
        } else {
            player.sendMessage(new TextComponentTranslation("signals.message.teleport_rail_failed_invalid_location", destination.getX(), destination.getY(), destination.getZ(), allowedDestinationRange.getLeft().getX(), allowedDestinationRange.getRight().getX(), allowedDestinationRange.getLeft().getZ(), allowedDestinationRange.getRight().getZ()));
            return false;
        }
    }

    public Pair<MCPos, MCPos> getAllowedDestinationRange(World destinationDimension){
        if(destinationDimension == null) return null;

        double moveFactor = getWorld().provider.getMovementFactor() / destinationDimension.provider.getMovementFactor();
        double destX = MathHelper.clamp(getPos().getX() * moveFactor, destinationDimension.getWorldBorder().minX() + 16.0D, destinationDimension.getWorldBorder().maxX() - 16.0D);
        double destZ = MathHelper.clamp(getPos().getZ() * moveFactor, destinationDimension.getWorldBorder().minZ() + 16.0D, destinationDimension.getWorldBorder().maxZ() - 16.0D);
        destX = MathHelper.clamp((int)destX, -29999872, 29999872);
        destZ = MathHelper.clamp((int)destZ, -29999872, 29999872);

        int maxDiff = 8;
        MCPos min = new MCPos(destinationDimension, new BlockPos(destX - maxDiff, 0, destZ - maxDiff));
        MCPos max = new MCPos(destinationDimension, new BlockPos(destX + maxDiff, destinationDimension.getActualHeight(), destZ + maxDiff));
        return new ImmutablePair<MCPos, MCPos>(min, max);
    }
}
