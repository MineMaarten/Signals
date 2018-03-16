package com.minemaarten.signals.rail;

import java.util.Collection;
import java.util.regex.Pattern;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.WorldServer;

import org.apache.commons.lang3.NotImplementedException;

import com.minemaarten.signals.rail.SignalsOnRouteIterable.SignalOnRoute;
import com.minemaarten.signals.tileentity.TileEntitySignalBase;

public class DestinationPathFinder{
    private static final double RED_SIGNAL_PENALTY = 10000;

    public static class AStarRailNode implements Comparable<AStarRailNode>{
        private int distanceFromGoal = Integer.MAX_VALUE;
        private AStarRailNode prevNode;
        private final RailWrapper goal;
        private final RailWrapper rail;
        public final EnumFacing pathDir;

        public AStarRailNode(RailWrapper rail, EnumFacing pathDir, RailWrapper goal){
            this.rail = rail;
            this.pathDir = pathDir;
            this.goal = goal;
        }

        public boolean checkImprovementAndUpdate(AStarRailNode node){
            int nodeDist = node.distanceFromGoal + 1;
            if(nodeDist < distanceFromGoal) {
                prevNode = node;
                distanceFromGoal = nodeDist;
                return true;
            } else {
                return false;
            }
        }

        /**
         * Next node from the caller's POV.
         * @return
         */
        public AStarRailNode getNextNode(){
            return prevNode;
        }

        public RailWrapper getRail(){
            return rail;
        }

        private double getCost(){
            return distanceFromGoal + (goal != null ? getDistance(goal) : 0); //A*
        }

        public EnumFacing getPathDir(){
            return pathDir;
        }

        @Override
        public int compareTo(AStarRailNode node){
            return Double.compare(getCost(), node.getCost());
        }

        private double getDistance(RailWrapper rail){
            return Math.sqrt(this.rail.distanceSq(rail));
        }

        public Iterable<SignalOnRoute> getSignalsOnRoute(){
            return new SignalsOnRouteIterable(this);
        }

        public TileEntitySignalBase getSignal(EnumFacing blacklistedDir){
            throw new NotImplementedException("");
        }

        public void showDebug(){
            showDebug(Vec3i.NULL_VECTOR);
        }

        public void showDebug(Vec3i offset){
            if(rail.world instanceof WorldServer) {
                Vec3i pos = rail.add(offset);
                ((WorldServer)rail.world).spawnParticle(EnumParticleTypes.REDSTONE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0, 0, 0, 0D);
            }
        }
    }

    /**
     * @param start
     * @param destination
     * @return Returns the first node starting with a signal (or destination), up to the 'start'.
     */
    public static AStarRailNode pathfindToDestination(RailWrapper start, EntityMinecart cart, Pattern destinationRegex, EnumFacing direction){
        return pathfindToDestination(start, RailCacheManager.getInstance(start.world).getStationRails(cart, destinationRegex), direction);
    }

    /**
     * @param start
     * @param goal
     * @return Returns the first node that leads up to the destination node.
     */
    public static AStarRailNode pathfindToDestination(RailWrapper start, Collection<RailWrapper> goals, EnumFacing direction){
        throw new NotImplementedException("");
    }
}
