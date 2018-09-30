package com.minemaarten.signals.util.railnode;

import org.junit.Assert;

import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.rail.network.NetworkState;
import com.minemaarten.signals.rail.network.RailEdge;
import com.minemaarten.signals.util.Pos2D;
import com.minemaarten.signals.util.parsing.TestRailNetwork;

public class RailNodeExpectedEdge extends DefaultRailNode implements IValidatingNode{

    public final char group;

    public RailNodeExpectedEdge(Pos2D pos, char group){
        super(pos);
        this.group = group;
    }

    @Override
    public void validate(TestRailNetwork network, NetworkState<Pos2D> state){

        RailEdge<Pos2D> expectedEdge = network.findEdge(getPos());
        Assert.assertNotNull("Not on an edge at pos " + getPos(), expectedEdge);

        //Assert right groupings (333  (somewhere else) 333 = AssertionError)
        network.railObjects.networkObjectsOfType(RailNodeExpectedEdge.class).filter(node -> node.group == group).forEach(node -> {
            Assert.assertEquals("Edge in group '" + group + "' for pos " + getPos() + " has an unexpected edge.", expectedEdge, network.findEdge(node.getPos()));
        });

        //Assert every edge (33343 = AssertionError)
        for(NetworkRail<Pos2D> railOnEdge : expectedEdge) {
            if(railOnEdge instanceof RailNodeExpectedEdge) {
                RailNodeExpectedEdge edgeValidator = (RailNodeExpectedEdge)railOnEdge;
                Assert.assertEquals("Edge belonging to pos '" + getPos() + "' has a different group at " + edgeValidator.getPos() + ".", "" + group, "" + edgeValidator.group);
            }
        }
    }
}
