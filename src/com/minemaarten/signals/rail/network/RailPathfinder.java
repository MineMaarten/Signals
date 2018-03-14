package com.minemaarten.signals.rail.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import com.minemaarten.signals.rail.network.RailRoute.RailRouteNode;

public class RailPathfinder<TPos extends IPosition<TPos>> {
    private final RailNetwork<TPos> network;
    private final NetworkState<TPos> state;

    public RailPathfinder(RailNetwork<TPos> network, NetworkState<TPos> state){
        this.network = network;
        this.state = state;
    }

    public class AStarRailNode implements Comparable<AStarRailNode>{
        private int distanceFromGoal = Integer.MAX_VALUE;
        private AStarRailNode prevNode;
        private final TPos goal;
        private final TPos pos;
        public final RailEdge<TPos> edge;

        //  public final EnumHeading pathDir;

        public AStarRailNode(TPos pos, RailEdge<TPos> edge, /* EnumHeading pathDir,*/TPos goal){
            this.pos = pos;
            this.edge = edge;
            // this.pathDir = pathDir;
            this.goal = goal;
        }

        public boolean checkImprovementAndUpdate(AStarRailNode node, int edgeLength){
            int nodeDist = node.distanceFromGoal + edgeLength;
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

        public TPos getRail(){
            return pos;
        }

        private double getCost(){
            return distanceFromGoal + (goal != null ? getDistance(goal) : 0); //A*
        }

        /* public EnumHeading getPathDir(){
             return pathDir;
         }*/

        @Override
        public int compareTo(AStarRailNode node){
            return Double.compare(getCost(), node.getCost());
        }

        private double getDistance(TPos rail){
            return Math.sqrt(this.pos.distanceSq(rail));
        }
    }

    /**
     * @return Returns the first node that leads up to the best rated (distance, red signals) destination node.
     */
    public RailRoute<TPos> pathfindToDestination(TPos start, EnumHeading direction, Set<TPos> goals){
        if(goals.isEmpty()) return null;

        RailEdge<TPos> startPosEdge = null;// network.findEdge(start);
        RailEdge<TPos> startFakeEdge = startPosEdge != null ? startPosEdge.createExitPoint(start, direction) : null;
        TPos startIntersection = startFakeEdge != null ? startFakeEdge.other(start) : start;

        if(startPosEdge != null && startFakeEdge == null) {
            //When the start pos is in the middle of an edge, but we can't route to an end, we will never be able to route a train.
            //Example: A signal facing in the opposite direction
            return null;
        }

        Queue<AStarRailNode> queue = new PriorityQueue<>();
        Set<TPos> traversedRails = new HashSet<>();
        Map<TPos, AStarRailNode> nodeMap = new HashMap<>();

        AStarRailNode bestRoute = null; //In practice there will only be one route, as paths are created at a signal, which cannot be on an intersection.

        for(TPos goal : goals) {
            AStarRailNode goalNode = new AStarRailNode(goal, null, /*null,*/startIntersection);
            goalNode.distanceFromGoal = 0;
            queue.add(goalNode);
            nodeMap.put(goal, goalNode);
        }

        while(!queue.isEmpty()) {
            AStarRailNode node = queue.remove(); //Take the node with the highest priority of the queue.
            traversedRails.add(node.pos); //Mark it as traversed so we won't put it on the queue anymore.

            //Branch and bound as soon as we get nodes that cannot get better than our current solution.
            if(bestRoute != null && node.distanceFromGoal >= bestRoute.distanceFromGoal) {
                break;
            }

            //Find the edges that lead into this intersection
            Collection<RailEdge<TPos>> entryEdges = network.findConnectedEdgesBackwards(node.pos);

            //When nothing is found, check if we are currently checking the destination nodes, if so, we may not be on an intersection
            if(entryEdges.isEmpty() && goals.contains(node.pos)) {
                RailEdge<TPos> destEdge = network.findEdge(node.pos);
                entryEdges = destEdge.createEntryPoints(node.pos);
            }

            for(RailEdge<TPos> nextEdge : entryEdges) {
                TPos nextPos = nextEdge.other(node.pos);
                AStarRailNode neighborNode = nodeMap.get(nextPos);
                if(neighborNode == null) {
                    neighborNode = new AStarRailNode(nextPos, nextEdge, startIntersection);
                    nodeMap.put(nextPos, neighborNode);
                }
                if(neighborNode.checkImprovementAndUpdate(node, nextEdge.getPathLength(state))) {
                    queue.add(neighborNode);

                    if(neighborNode.pos.equals(startIntersection)) { //If we find the start, we have found a valid path from start to end.

                        //If it's a better solution
                        if(bestRoute == null || neighborNode.distanceFromGoal < bestRoute.distanceFromGoal) {
                            bestRoute = neighborNode;
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
            lastNode.checkImprovementAndUpdate(new AStarRailNode(start, startFakeEdge, start), startFakeEdge != null ? startFakeEdge.length : 0);
        }

        return bestRoute != null ? toRailRoute(bestRoute) : null;
    }

    private RailRoute<TPos> toRailRoute(AStarRailNode node){
        List<RailRouteNode<TPos>> route = new ArrayList<>();

        AStarRailNode prevNode = node;
        node = node.getNextNode();

        while(node != null && node.edge != null) {
            EnumHeading dirIn = prevNode.edge.headingForEndpoint(node.pos).getOpposite();
            EnumHeading dirOut = node.edge.headingForEndpoint(node.pos).getOpposite();
            route.add(new RailRouteNode<TPos>(node.pos, dirIn, dirOut));

            prevNode = node;
            node = node.getNextNode();
        }
        return new RailRoute<TPos>(route);
    }
}
