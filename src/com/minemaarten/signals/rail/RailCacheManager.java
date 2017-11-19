package com.minemaarten.signals.rail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.EnumFacing.AxisDirection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.BlockEvent.NeighborNotifyEvent;

import com.google.common.collect.Iterables;
import com.minemaarten.signals.lib.Log;
import com.minemaarten.signals.network.NetworkHandler;
import com.minemaarten.signals.network.PacketSyncStationNames;
import com.minemaarten.signals.tileentity.TileEntityStationMarker;

public class RailCacheManager{
    private static Map<Integer, RailCacheManager> INSTANCES = new HashMap<Integer, RailCacheManager>();
    private final Map<Long, Map<BlockPos, RailWrapper>> railCache = new HashMap<Long, Map<BlockPos, RailWrapper>>(); //Chunk hash to cache (per chunk) mapping.
    private final Set<TileEntityStationMarker> stations = new HashSet<TileEntityStationMarker>();
    private String[] allStationNames = new String[0];

    public static RailCacheManager getInstance(World world){
        if(world.isRemote) {
            Log.warning("Can't be called client side!");
            new Throwable().printStackTrace();
            return new RailCacheManager(); // Give back a clean cache client side
        }
        return getInstance(world.provider.getDimension());
    }

    public static RailCacheManager getInstance(int dimension){
        RailCacheManager cache = INSTANCES.get(dimension);
        if(cache == null) {
            cache = new RailCacheManager();
            INSTANCES.put(dimension, cache);
        }
        return cache;
    }

    public RailWrapper getRail(World world, BlockPos pos){
        return getRail(world, pos, true);
    }

    private RailWrapper getRail(World world, BlockPos pos, boolean shouldLoadRail){
        long chunkHash = getChunkHashFromBlockPos(pos);
        Map<BlockPos, RailWrapper> cache = railCache.get(chunkHash);
        if(cache != null) {
            RailWrapper rail = cache.get(pos);
            if(rail != null) return rail;
        }
        if(shouldLoadRail) {
            RailWrapper rail = new RailWrapper(world, pos);
            if(rail.isRail()) { //A rail isn't cached yet
                if(cache == null) {
                    cache = new HashMap<BlockPos, RailWrapper>();
                    railCache.put(chunkHash, cache);
                }
                cache.put(pos, rail);
                for(RailWrapper neighbor : rail.getNeighbors().keySet()) {
                    neighbor.updateNeighborCache();
                }
                NetworkController.getInstance(world).updateColor(rail, pos);
                return rail;
            }
        }
        return null;
    }

    private static long getChunkHashFromBlockPos(BlockPos pos){
        return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
    }

    /**
     * When a chunk is loaded, we need to make sure to notify rails on a neighbor chunk, on a chunk border
     * @param chunk
     */
    public void onChunkLoad(Chunk chunk){
        for(EnumFacing d : EnumFacing.HORIZONTALS) {
            ChunkPos intPair = chunk.getPos();
            BlockPos offsetBlock = intPair.getBlock(0, 0, 0).offset(d, 16);
            if(chunk.getWorld().isBlockLoaded(offsetBlock)) {
                Chunk neighbor = chunk.getWorld().getChunkFromChunkCoords(chunk.x + d.getFrontOffsetX(), chunk.z + d.getFrontOffsetZ());
                Map<BlockPos, RailWrapper> chunkCache = railCache.get(ChunkPos.asLong(neighbor.x, neighbor.z));
                if(chunkCache != null) {
                    if(d.getAxis() == Axis.X) {
                        int borderX = (d.getAxisDirection() == AxisDirection.POSITIVE ? 0 : 15) + offsetBlock.getX();
                        for(RailWrapper rail : chunkCache.values()) {
                            if(rail.getX() == borderX) rail.updateNeighborCache();
                        }
                    } else {
                        int borderZ = (d.getAxisDirection() == AxisDirection.POSITIVE ? 0 : 15) + offsetBlock.getZ();
                        for(RailWrapper rail : chunkCache.values()) {
                            if(rail.getZ() == borderZ) rail.updateNeighborCache();
                        }
                    }
                }
            }

        }
    }

    public void onChunkUnload(Chunk chunk){
        long chunkHash = ChunkPos.asLong(chunk.x, chunk.z);
        Map<BlockPos, RailWrapper> cache = railCache.get(chunkHash);
        if(cache != null) {
            for(RailWrapper rail : cache.values()) {
                rail.invalidate();
            }
            railCache.remove(chunkHash);
        }
    }

    public void onWorldUnload(World world){
        INSTANCES.remove(world.provider.getDimension());
    }

    public void onNeighborChanged(NeighborNotifyEvent event){
        Map<BlockPos, RailWrapper> cache = railCache.get(getChunkHashFromBlockPos(event.getPos()));
        if(cache != null) {
            RailWrapper removedWrapper = cache.remove(event.getPos());
            if(removedWrapper != null) {
                removedWrapper.invalidate();
            }
        }
        RailWrapper rail = new RailWrapper(event.getWorld(), event.getPos(), event.getState());
        if(rail.isRail()) {
            for(EnumFacing d : EnumFacing.HORIZONTALS) {
                BlockPos neighborPos = event.getPos().offset(d);
                for(int i = -1; i <= 1; i++) {
                    RailWrapper neighbor = getRail(event.getWorld(), neighborPos.add(0, i, 0), false);
                    if(neighbor != null) neighbor.updateNeighborCache();
                }
            }
        }
    }

    public void addStationMarker(TileEntityStationMarker marker){
        stations.add(marker);
        NetworkController.getInstance(marker.getWorld()).updateColor(marker, marker.getPos());
    }

    public void removeStationMarker(TileEntityStationMarker marker){
        stations.remove(marker);
        NetworkController.getInstance(marker.getWorld()).updateColor((TileEntityStationMarker)null, marker.getPos());
    }

    public Collection<RailWrapper> getStationRails(EntityMinecart cart, Pattern destinationRegex){
        Set<RailWrapper> rails = new HashSet<RailWrapper>();
        Set<String> validNames = new HashSet<String>();
        for(TileEntityStationMarker station : stations) {
            if(station.isCartApplicable(cart, destinationRegex)) {
                rails.addAll(station.getNeighborRails());
                validNames.add(station.getStationName());
            }
        }

        //Make sure to include stations that don't match themselves, but other stations with the same name do.
        for(TileEntityStationMarker station : stations) {
            if(validNames.contains(station.getStationName())) {
                rails.addAll(station.getNeighborRails());
            }
        }
        return rails;
    }

    public Iterable<RailWrapper> getAllRails(){
        Iterable<RailWrapper> iterable = Collections.emptyList();
        for(Map<BlockPos, RailWrapper> map : railCache.values()) {
            iterable = Iterables.concat(iterable, map.values());
        }
        return iterable;
    }

    public static String[] getAllStationNames(){
        return getInstance(0).allStationNames;
    }

    public static void setAllStationNames(String[] stationNames){
        getInstance(0).allStationNames = stationNames;
    }

    public static void syncStationNames(EntityPlayerMP player){
        Set<String> stationNames = new HashSet<String>();
        stationNames.add("ITEM");
        for(RailCacheManager manager : INSTANCES.values()) {
            for(TileEntityStationMarker station : manager.stations) {
                if(!station.getStationName().equals("")) stationNames.add(station.getStationName());
            }
        }
        String[] array = stationNames.toArray(new String[stationNames.size()]);
        Arrays.sort(array);
        NetworkHandler.sendTo(new PacketSyncStationNames(array), player);
    }
}
