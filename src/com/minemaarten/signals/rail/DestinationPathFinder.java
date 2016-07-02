package com.minemaarten.signals.rail;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.minemaarten.signals.block.BlockSignalBase.EnumLampStatus;
import com.minemaarten.signals.lib.Log;
import com.minemaarten.signals.tileentity.TileEntitySignalBase;

public class DestinationPathFinder{
    public static class AStarRailNode implements Comparable<AStarRailNode>{
        private int distanceFromStart = Integer.MAX_VALUE;
        private AStarRailNode prevNode;
        private final RailWrapper goal;
        private final RailWrapper rail;
        private final EnumFacing pathDir;

        public AStarRailNode(RailWrapper rail, EnumFacing pathDir, RailWrapper goal){
            this.rail = rail;
            this.pathDir = pathDir;
            this.goal = goal;
        }

        public boolean checkImprovementAndUpdate(AStarRailNode node){
            int nodeDist = node.distanceFromStart + 1;
            if(nodeDist < distanceFromStart) {
                prevNode = node;
                distanceFromStart = nodeDist;
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
            return distanceFromStart + (goal != null ? getDistance(goal) : 0); //A*
        }

        @Override
        public int compareTo(AStarRailNode node){
            return Double.compare(getCost(), node.getCost());
        }

        private double getDistance(RailWrapper rail){
            return Math.sqrt(this.rail.distanceSq(rail));
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
        Queue<AStarRailNode> queue = new PriorityQueue<AStarRailNode>();
        Set<RailWrapper> traversedRails = new HashSet<RailWrapper>();
        Map<RailWrapper, AStarRailNode> railMap = new HashMap<RailWrapper, AStarRailNode>();

        Map<RailWrapper, AStarRailNode> firstSignals = getFirstSignals(start, direction, goals);
        Log.debug("Signals: " + firstSignals.size());
        if(firstSignals.isEmpty()) return null; //When no destination directly found or signals found, break right away.
        int minSignalDistance = getClosestSignal(firstSignals);

        AStarRailNode bestRoute = null;
        int bestDistance = Integer.MAX_VALUE;

        for(RailWrapper goal : goals) {
            AStarRailNode goalNode = new AStarRailNode(goal, null, start);
            goalNode.distanceFromStart = 0;
            queue.add(goalNode);
            railMap.put(goal, goalNode);
        }

        while(!queue.isEmpty()) {
            AStarRailNode node = queue.remove(); //Take the node with the highest priority of the queue.
            //  if(node.rail.world instanceof WorldServer) ((WorldServer)node.rail.world).spawnParticle(EnumParticleTypes.REDSTONE, node.rail.getX() + 0.5, node.rail.getY() + 0.5, node.rail.getZ() + 0.5, 1, 0, 0, 0, 0D);
            //   else Log.debug("World: " + node.rail.world);
            traversedRails.add(node.rail); //Mark it as traversed so we won't put it on the queue anymore.
            if(node.rail == start) continue;

            AStarRailNode signalNode = firstSignals.get(node.rail);
            if(signalNode != null) {
                int signalWeight = signalNode.distanceFromStart;
                firstSignals.remove(node.rail); //Only allow one path to end up on this signal.
                int totalWeight = signalWeight + node.distanceFromStart;
                if(totalWeight < bestDistance) {
                    bestDistance = totalWeight;
                    bestRoute = signalNode;
                    Log.debug("New best: " + signalWeight);
                }
                if(firstSignals.isEmpty()) {
                    Log.debug("No signals left: " + signalWeight);
                    return bestRoute;
                }
                minSignalDistance = getClosestSignal(firstSignals);
                continue;
            } else {
                int minBound = minSignalDistance + node.distanceFromStart;
                if(minBound > bestDistance) return bestRoute; //Branch and bound, return when we can't ever find a better solution in best case.
            }
            for(Map.Entry<RailWrapper, EnumFacing> entry : node.rail.getNeighbors().entrySet()) {
                RailWrapper neighborRail = entry.getKey();
                if(TileEntitySignalBase.getNeighborSignal(neighborRail, entry.getValue().getOpposite()) == null && !traversedRails.contains(neighborRail)) {
                    AStarRailNode neighborNode = railMap.get(neighborRail);
                    if(neighborNode == null) {
                        neighborNode = new AStarRailNode(neighborRail, entry.getValue(), start);
                        railMap.put(neighborRail, neighborNode);
                    }
                    if(neighborNode.checkImprovementAndUpdate(node)) {
                        queue.add(neighborNode);
                    }
                }
            }
        }
        Log.debug("Opting for the red signalled route (if available)");
        return bestRoute;
    }

    private static int getClosestSignal(Map<RailWrapper, AStarRailNode> signals){
        int closest = Integer.MAX_VALUE;
        for(AStarRailNode node : signals.values()) {
            if(node.distanceFromStart < closest) {
                closest = node.distanceFromStart;
            }
        }
        return closest;
    }

    private static Map<RailWrapper, AStarRailNode> getFirstSignals(RailWrapper start, EnumFacing dir, Collection<RailWrapper> goals){
        Map<RailWrapper, AStarRailNode> firstSignals = new HashMap<RailWrapper, AStarRailNode>();
        Set<RailWrapper> traversedRails = new HashSet<RailWrapper>();
        Queue<Map.Entry<RailWrapper, AStarRailNode>> traversingRails = new LinkedList<Map.Entry<RailWrapper, AStarRailNode>>();

        for(Map.Entry<RailWrapper, EnumFacing> entry : start.getNeighbors().entrySet()) {
            if(entry.getValue() != dir.getOpposite()) {
                AStarRailNode node = new AStarRailNode(entry.getKey(), entry.getValue(), null);
                node.distanceFromStart = 0;
                traversingRails.add(new ImmutablePair(entry.getKey(), node));
            }
        }
        traversedRails.add(start); //Make sure to consider this block as traversed already, prevents traversing the tracks in reverse direction

        while(!traversingRails.isEmpty()) {

            Map.Entry<RailWrapper, AStarRailNode> neighbor = traversingRails.poll();
            if(goals.contains(neighbor.getKey())) { //If we found the destination (without passing a signal)
                firstSignals.clear();
                firstSignals.put(neighbor.getKey(), neighbor.getValue());
                Log.debug("Found destination without passing signals!");
                return firstSignals;//Prefer that route
            }

            traversedRails.add(neighbor.getKey());
            TileEntitySignalBase signal = TileEntitySignalBase.getNeighborSignal(neighbor.getKey(), neighbor.getValue().pathDir.getOpposite());
            if(signal == null) { //If not connected to a signal
                if(neighbor.getKey().getSignals().size() != 1) {//Try to find a single opposing signal instead.
                    for(Map.Entry<RailWrapper, EnumFacing> entry : neighbor.getKey().getNeighbors().entrySet()) {
                        BlockPos nextNeighbor = entry.getKey();
                        if(!traversedRails.contains(nextNeighbor)) {
                            AStarRailNode nextNode = new AStarRailNode(entry.getKey(), entry.getValue(), null);
                            nextNode.checkImprovementAndUpdate(neighbor.getValue());

                            traversingRails.add(new ImmutablePair(entry.getKey(), nextNode));
                        }
                    }
                }
            } else { //When connected to a signal
                AStarRailNode node = neighbor.getValue();
                firstSignals.put(neighbor.getKey(), node);
                if(node != null && signal.getLampStatus() == EnumLampStatus.RED) node.distanceFromStart += 10000;
            }
        }
        return firstSignals;
    }
}
