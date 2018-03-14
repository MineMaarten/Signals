package com.minemaarten.signals.util.railnode;

import org.junit.Assert;

import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.rail.network.RailEdge;
import com.minemaarten.signals.util.Pos2D;
import com.minemaarten.signals.util.parsing.TestRailNetwork;

public class RailNodeExpectedEdge extends ValidatingRailNode{

    public final char group;

    public RailNodeExpectedEdge(Pos2D pos, char group){
        super(pos);
        this.group = group;
    }

    @Override
    public void validate(TestRailNetwork network){

        RailEdge<Pos2D> expectedEdge = network.findEdge(pos);
        Assert.assertNotNull("Not on an edge at pos " + pos, expectedEdge);

        //Assert right groupings (333  (somewhere else) 333 = AssertionError)
        network.railObjects.networkObjectsOfType(RailNodeExpectedEdge.class).filter(node -> node.group == group).forEach(node -> {
            Assert.assertEquals("Edge in group '" + group + "' for pos " + pos + " has an unexpected edge.", expectedEdge, network.findEdge(node.pos));
        });

        //Assert every edge (33343 = AssertionError)
        for(NetworkRail<Pos2D> railOnEdge : expectedEdge) {
            if(railOnEdge instanceof RailNodeExpectedEdge) {
                RailNodeExpectedEdge edgeValidator = (RailNodeExpectedEdge)railOnEdge;
                Assert.assertEquals("Edge belonging to pos '" + pos + "' has a different group at " + edgeValidator.pos + ".", "" + group, "" + edgeValidator.group);
            }
        }
    }
}
