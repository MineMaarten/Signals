package com.minemaarten.signals.rail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.BlockRailBase.EnumRailDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.minemaarten.signals.api.IRail;
import com.minemaarten.signals.tileentity.TileEntityRailLink;
import com.minemaarten.signals.tileentity.TileEntitySignalBase;
import com.minemaarten.signals.tileentity.TileEntityStationMarker;

public class RailWrapper extends BlockPos{
    private static Map<EnumSet<EnumFacing>, EnumRailDirection> DIRS_TO_RAIL_DIR = new HashMap<EnumSet<EnumFacing>, EnumRailDirection>(6);

    static {
        DIRS_TO_RAIL_DIR.put(EnumSet.of(EnumFacing.NORTH, EnumFacing.SOUTH), EnumRailDirection.NORTH_SOUTH);
        DIRS_TO_RAIL_DIR.put(EnumSet.of(EnumFacing.EAST, EnumFacing.WEST), EnumRailDirection.EAST_WEST);
        DIRS_TO_RAIL_DIR.put(EnumSet.of(EnumFacing.NORTH, EnumFacing.EAST), EnumRailDirection.NORTH_EAST);
        DIRS_TO_RAIL_DIR.put(EnumSet.of(EnumFacing.EAST, EnumFacing.SOUTH), EnumRailDirection.SOUTH_EAST);
        DIRS_TO_RAIL_DIR.put(EnumSet.of(EnumFacing.SOUTH, EnumFacing.WEST), EnumRailDirection.SOUTH_WEST);
        DIRS_TO_RAIL_DIR.put(EnumSet.of(EnumFacing.WEST, EnumFacing.NORTH), EnumRailDirection.NORTH_WEST);
    }

    public final IRail rail;
    public final World world;
    public final IBlockState state;
    private Map<RailWrapper, EnumFacing> neighbors;
    private List<TileEntityStationMarker> stationMarkers;
    private Map<EnumFacing, TileEntitySignalBase> signals;
    private Set<TileEntityRailLink> railLinks = Collections.emptySet();

    public RailWrapper(World world, BlockPos pos){
        this(world, pos, world.getBlockState(pos));
    }

    public RailWrapper(World world, BlockPos pos, IBlockState state){
        super(pos);
        this.world = world;
        this.state = state;
        rail = RailManager.getInstance().getRail(world, pos, state);
    }

    public boolean isRail(){
        return rail != null;
    }

    public boolean setRailDir(EnumRailDirection railDir){
        boolean valid = rail.getValidDirections(world, this, state).contains(railDir);
        if(valid) rail.setDirection(world, this, state, railDir);
        return valid;
    }

    public void invalidate(){
        if(neighbors != null) {
            for(RailWrapper neighbor : neighbors.keySet()) {
                if(neighbor.neighbors != null) neighbor.neighbors.remove(this); //Dereference itself
            }
        }
        for(TileEntityRailLink railLink : railLinks){
        	railLink.onLinkedRailInvalidated();
        }
        NetworkController.getInstance(world).updateColor((RailWrapper)null, this);
    }
    
    public void link(TileEntityRailLink link){
    	if(railLinks == Collections.EMPTY_SET){
    		railLinks = new HashSet<TileEntityRailLink>();
    	}
    	railLinks.add(link);
    }
    
    public void unlink(TileEntityRailLink link){
    	if(railLinks != Collections.EMPTY_SET){
    		railLinks.remove(link);
    	}
    }

    public void updateStationCache(){
        stationMarkers = null;
    }

    public Set<String> getStationNames(){
        if(stationMarkers == null) {
            for(EnumFacing d : EnumFacing.values()) {
                TileEntity te = world.getTileEntity(offset(d));
                if(te instanceof TileEntityStationMarker) {
                    if(stationMarkers == null) stationMarkers = new ArrayList<TileEntityStationMarker>(1); //Be conservative with instantiating, as not many rails usually have a station.
                    stationMarkers.add((TileEntityStationMarker)te);
                }
            }
            if(stationMarkers == null) stationMarkers = Collections.emptyList();
        }
        Set<String> stationNames = new HashSet<String>(stationMarkers.size());
        for(TileEntityStationMarker marker : stationMarkers) {
            stationNames.add(marker.getStationName());
        }
        return stationNames;
    }

    public void updateSignalCache(){
        signals = null;
    }

    public Map<EnumFacing, TileEntitySignalBase> getSignals(){
        if(signals == null) {
            EnumRailDirection railDir = rail.getDirection(world, this, state);
            if(isStraightTrack(railDir) && getNeighbors().size() <= 2) {
                for(EnumFacing d : getDirections(railDir)) {
                    d = d.rotateY(); //Check for signals perpendicular to the rail direction
                    TileEntity te = world.getTileEntity(offset(d));
                    if(te instanceof TileEntitySignalBase) {
                        if(signals == null) signals = new HashMap<EnumFacing, TileEntitySignalBase>(1); //Be conservative with instantiating, as not many rails usually have a signal.
                        signals.put(d, (TileEntitySignalBase)te);
                    }
                }
            }
            if(signals == null) signals = Collections.emptyMap();
        }
        return signals;
    }

    public void updateNeighborCache(){
        neighbors = null;
    }

    public Map<RailWrapper, EnumFacing> getNeighbors(){
        if(neighbors == null) {
            Map<RailWrapper, EnumFacing> neighbors = new HashMap<RailWrapper, EnumFacing>(6);

            EnumSet<EnumFacing> validDirs = EnumSet.noneOf(EnumFacing.class);
            for(EnumRailDirection railDir : rail.getValidDirections(world, this, state)) {
                validDirs.addAll(getDirections(railDir));
            }

            for(EnumFacing d : validDirs) {
                if(world.isBlockLoaded(offset(d))) {
                    RailWrapper rail = RailCacheManager.getInstance(world).getRail(world, offset(d));
                    if(rail != null) {
                        neighbors.put(rail, d);
                    } else {
                        RailWrapper rail2 = RailCacheManager.getInstance(world).getRail(world, offset(d).down());
                        if(rail2 != null) {
                            neighbors.put(rail2, d);
                        } else {
                            RailWrapper rail3 = RailCacheManager.getInstance(world).getRail(world, offset(d).up());
                            if(rail3 != null) neighbors.put(rail3, d);
                        }
                    }
                }
            }
            
            for(EnumFacing d : EnumFacing.values()) {
                if(world.isBlockLoaded(offset(d))) {
                	TileEntity te = world.getTileEntity(offset(d));
                	if(te instanceof TileEntityRailLink){
                		RailWrapper linkedNeighbor = ((TileEntityRailLink) te).getLinkedRail();
                		if(linkedNeighbor != null){
                			neighbors.put(linkedNeighbor, EnumFacing.DOWN);
                		}
                	}
                }
            }
            
            this.neighbors = neighbors;
        }
        return neighbors;
    }

    private static EnumSet<EnumFacing> getDirections(EnumRailDirection railDir){
        switch(railDir){
            case NORTH_SOUTH:
            case ASCENDING_NORTH:
            case ASCENDING_SOUTH:
                return EnumSet.of(EnumFacing.NORTH, EnumFacing.SOUTH);
            case EAST_WEST:
            case ASCENDING_EAST:
            case ASCENDING_WEST:
                return EnumSet.of(EnumFacing.EAST, EnumFacing.WEST);
            case SOUTH_EAST:
                return EnumSet.of(EnumFacing.SOUTH, EnumFacing.EAST);
            case SOUTH_WEST:
                return EnumSet.of(EnumFacing.SOUTH, EnumFacing.WEST);
            case NORTH_WEST:
                return EnumSet.of(EnumFacing.NORTH, EnumFacing.WEST);
            case NORTH_EAST:
                return EnumSet.of(EnumFacing.NORTH, EnumFacing.EAST);
            default:
                return EnumSet.noneOf(EnumFacing.class);
        }
    }

    private static int getHeightOffset(EnumRailDirection railDir, EnumFacing dir){
        switch(railDir){
            case ASCENDING_EAST:
                return dir == EnumFacing.EAST ? 1 : 0;
            case ASCENDING_WEST:
                return dir == EnumFacing.WEST ? 1 : 0;
            case ASCENDING_NORTH:
                return dir == EnumFacing.NORTH ? 1 : 0;
            case ASCENDING_SOUTH:
                return dir == EnumFacing.SOUTH ? 1 : 0;
            default:
                return 0;
        }
    }

    public boolean isStraightTrack(){
        return isStraightTrack(rail.getDirection(world, this, state));
    }

    public static EnumRailDirection getRailDir(EnumSet<EnumFacing> facings){
        return DIRS_TO_RAIL_DIR.get(facings);
    }

    private static boolean isStraightTrack(EnumRailDirection railDir){
        switch(railDir){
            case NORTH_SOUTH:
            case ASCENDING_NORTH:
            case ASCENDING_SOUTH:
            case EAST_WEST:
            case ASCENDING_EAST:
            case ASCENDING_WEST:
                return true;
        }
        return false;
    }
}
