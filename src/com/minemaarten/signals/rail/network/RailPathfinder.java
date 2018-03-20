package com.minemaarten.signals.rail.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
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
        public RailEdge<TPos> edge;

        //  public final EnumHeading pathDir;

        public AStarRailNode(TPos pos, RailEdge<TPos> edge, /* EnumHeading pathDir,*/TPos goal){
            this.pos = pos;
            this.edge = edge;
            // this.pathDir = pathDir;
            this.goal = goal;
        }

        public boolean checkImprovementAndUpdate(AStarRailNode node, RailEdge<TPos> edge, int edgeLength){
            int nodeDist = node.distanceFromGoal + edgeLength;
            if(nodeDist < distanceFromGoal) {
                prevNode = node;
                this.edge = edge;
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

        Map<TPos, RailEdge<TPos>> startToFirstIntersections;

        RailEdge<TPos> startPosEdge = network.findEdge(start);
        List<RailEdge<TPos>> startToFirstIntersectionList = startPosEdge != null && !startPosEdge.isAtStartOrEnd(start) ? startPosEdge.createExitPoints(start, direction) : null;
        if(startToFirstIntersectionList == null) {
            startToFirstIntersections = Collections.emptyMap();
        } else {
            if(startToFirstIntersectionList.isEmpty()) {
                //When the start pos is in the middle of an edge, but we can't route to an end, we will never be able to route a train.
                //Example: A signal facing in the opposite direction
                return null;
            } else {
                startToFirstIntersections = startToFirstIntersectionList.stream().collect(Collectors.toMap(e -> e.other(start), e -> e));
            }
        }

        Queue<AStarRailNode> queue = new PriorityQueue<>();
        Set<TPos> traversedRails = new HashSet<>();
        Map<TPos, AStarRailNode> nodeMap = new HashMap<>();

        AStarRailNode bestRoute = null; //In practice there will only be one route, as paths are created at a signal, which cannot be on an intersection.

        for(TPos goal : goals) {
            AStarRailNode goalNode = new AStarRailNode(goal, null, /*null,*/start);
            goalNode.distanceFromGoal = 0;
            queue.add(goalNode);
            nodeMap.put(goal, goalNode);

            //Special case: start is on the same edge as one of the goals.
            if(startPosEdge != null && startPosEdge.contains(goal)) {
                int startIndex = startPosEdge.getIndex(start);
                int endIndex = startPosEdge.getIndex(goal);
                bestRoute = new AStarRailNode(goal, startPosEdge.subEdge(startIndex, endIndex), goal);
                bestRoute.distanceFromGoal = endIndex - startIndex;
            }
        }

        while(!queue.isEmpty()) {
            AStarRailNode node = queue.remove(); //Take the node with the highest priority of the queue.
            traversedRails.add(node.pos); //Mark it as traversed so we won't put it on the queue anymore.

            //Branch and bound as soon as we get nodes that cannot get better than our current solution.
            if(bestRoute != null && node.distanceFromGoal >= bestRoute.distanceFromGoal) {
                break;
            }

            //Find the edges that lead into this intersection
            Collection<RailEdge<TPos>> networkEntryEdges = network.findConnectedEdgesBackwards(node.pos);

            //When nothing is found, check if we are currently checking the destination nodes, if so, we may not be on an intersection
            if(networkEntryEdges.isEmpty() && goals.contains(node.pos)) {
                RailEdge<TPos> destEdge = network.findEdge(node.pos);
                networkEntryEdges = destEdge.createEntryPoints(node.pos);
            }

            //Append the fake start edges, if applicable.
            Iterable<RailEdge<TPos>> entryEdges;
            RailEdge<TPos> startToIntersection = startToFirstIntersections.get(node.pos);
            if(startToIntersection != null) {
                entryEdges = Iterables.concat(networkEntryEdges, Collections.singleton(startToIntersection));
            } else {
                entryEdges = networkEntryEdges;
            }

            for(RailEdge<TPos> nextEdge : entryEdges) {
                TPos nextPos = nextEdge.other(node.pos);
                AStarRailNode neighborNode = nodeMap.get(nextPos);
                if(neighborNode == null) {
                    neighborNode = new AStarRailNode(nextPos, nextEdge, start);
                    nodeMap.put(nextPos, neighborNode);
                }
                if(neighborNode.checkImprovementAndUpdate(node, nextEdge, nextEdge.getPathLength(state))) {
                    queue.add(neighborNode);

                    if(neighborNode.pos.equals(start)) { //If we find the start, we have found a valid path from start to end.

                        //If it's a better solution
                        if(bestRoute == null || neighborNode.distanceFromGoal < bestRoute.distanceFromGoal) {
                            bestRoute = neighborNode;
                        }
                    }
                }
            }
        }

        return bestRoute != null ? toRailRoute(bestRoute) : null;
    }

    private RailRoute<TPos> toRailRoute(AStarRailNode node){
        List<RailRouteNode<TPos>> routeNodes = new ArrayList<>();
        List<RailEdge<TPos>> routeEdges = new ArrayList<>();
        LinkedHashSet<TPos> routeRails = new LinkedHashSet<>();

        routeRails.addAll(node.edge.traverseWithFirst(node.pos).stream().map(r -> r.pos).collect(Collectors.toList()));
        routeEdges.add(node.edge);
        routeNodes.addAll(node.edge.getIntersectionsWithFirst(node.pos));

        AStarRailNode prevNode = node;
        node = node.getNextNode();

        while(node != null && node.edge != null) {
            routeRails.addAll(node.edge.traverseWithFirst(node.pos).stream().map(r -> r.pos).collect(Collectors.toList()));
            routeEdges.add(node.edge);

            EnumHeading dirIn = EnumHeading.getOpposite(prevNode.edge.headingForEndpoint(node.pos));
            EnumHeading dirOut = EnumHeading.getOpposite(node.edge.headingForEndpoint(node.pos));
            routeNodes.add(new RailRouteNode<TPos>(node.pos, dirIn, dirOut));
            routeNodes.addAll(node.edge.getIntersectionsWithFirst(node.pos));

            prevNode = node;
            node = node.getNextNode();
        }

        return new RailRoute<TPos>(ImmutableList.copyOf(routeNodes), ImmutableList.copyOf(routeRails), ImmutableList.copyOf(routeEdges));
    }
}
