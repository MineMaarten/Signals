package com.minemaarten.signals.rail;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.EnumFacing;

import com.minemaarten.signals.block.BlockSignalBase.EnumLampStatus;
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
        if(goals.isEmpty()) return null;
        Queue<AStarRailNode> queue = new PriorityQueue<>();
        Set<RailWrapper> traversedRails = new HashSet<>();
        Map<RailWrapper, AStarRailNode> railMap = new HashMap<>();

        AStarRailNode bestRoute = null; //In practice there will only be one route, as paths are created at a signal, which cannot be on an intersection.

        for(RailWrapper goal : goals) {
            AStarRailNode goalNode = new AStarRailNode(goal, null, start);
            goalNode.distanceFromGoal = 0;
            queue.add(goalNode);
            railMap.put(goal, goalNode);
        }

        while(!queue.isEmpty()) {
            AStarRailNode node = queue.remove(); //Take the node with the highest priority of the queue.
            //if(node.rail.world instanceof WorldServer) ((WorldServer)node.rail.world).spawnParticle(EnumParticleTypes.REDSTONE, node.rail.getX() + 0.5, node.rail.getY() + 0.5, node.rail.getZ() + 0.5, 1, 0, 0, 0, 0D);
            //   else Log.debug("World: " + node.rail.world);
            traversedRails.add(node.rail); //Mark it as traversed so we won't put it on the queue anymore.

            //Branch and bound as soon as we get nodes that cannot get better than our current solution.
            if(bestRoute != null && node.distanceFromGoal >= bestRoute.distanceFromGoal) {
                break;
            }

            for(Map.Entry<RailWrapper, EnumFacing> entry : node.rail.getNeighborsForEntryDir(node.pathDir).entrySet()) {
                RailWrapper neighborRail = entry.getKey();
                if(TileEntitySignalBase.getNeighborSignal(neighborRail, entry.getValue().getOpposite()) == null && !traversedRails.contains(neighborRail)) {
                    AStarRailNode neighborNode = railMap.get(neighborRail);
                    if(neighborNode == null) {
                        neighborNode = new AStarRailNode(neighborRail, entry.getValue(), start);
                        railMap.put(neighborRail, neighborNode);
                    }
                    if(neighborNode.checkImprovementAndUpdate(node)) {
                        //Penalize red signals on the way
                        TileEntitySignalBase signal = TileEntitySignalBase.getNeighborSignal(neighborNode.rail, neighborNode.pathDir);
                        if(signal != null && signal.getLampStatus() == EnumLampStatus.RED) {
                            neighborNode.distanceFromGoal += RED_SIGNAL_PENALTY;
                        }

                        queue.add(neighborNode);

                        if(neighborNode.rail == start) { //If we find the start, we have found a valid path from start to end.

                            //If it's a better solution
                            if(bestRoute == null || neighborNode.distanceFromGoal < bestRoute.distanceFromGoal) {
                                bestRoute = neighborNode;
                            }
                        }
                    }
                }
            }
        }
        //Log.debug("Opting for the red signalled route (if available)");

        //Append the start node, so we have a complete path
        if(bestRoute != null) {
            AStarRailNode lastNode = bestRoute;
            while(lastNode.getNextNode() != null) {
                lastNode = lastNode.getNextNode();
            }
            lastNode.checkImprovementAndUpdate(new AStarRailNode(start, direction.getOpposite(), start));
        }

        return bestRoute;
    }
}
