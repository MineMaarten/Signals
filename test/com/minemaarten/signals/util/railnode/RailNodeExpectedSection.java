package com.minemaarten.signals.util.railnode;

import org.junit.Assert;

import com.minemaarten.signals.rail.network.NetworkRail;
import com.minemaarten.signals.rail.network.NetworkState;
import com.minemaarten.signals.rail.network.RailSection;
import com.minemaarten.signals.util.Pos2D;
import com.minemaarten.signals.util.parsing.TestRailNetwork;

public class RailNodeExpectedSection extends DefaultRailNode implements IValidatingNode{

    public final char group;

    public RailNodeExpectedSection(Pos2D pos, char group){
        super(pos);
        this.group = group;
    }

    @Override
    public void validate(TestRailNetwork network, NetworkState<Pos2D> state){

        RailSection<Pos2D> expectedSection = network.findSection(getPos());
        Assert.assertNotNull("Not on an section at pos " + getPos(), expectedSection);

        //Assert right groupings (333  (somewhere else) 333 = AssertionError)
        network.railObjects.networkObjectsOfType(RailNodeExpectedSection.class).filter(node -> node.group == group).forEach(node -> {
            Assert.assertEquals("Section in group '" + group + "' for pos " + getPos() + " has an unexpected section.", expectedSection, network.findSection(node.getPos()));
        });

        //Assert every edge (33343 = AssertionError)
        for(NetworkRail<Pos2D> railOnSection : expectedSection) {
            if(railOnSection instanceof RailNodeExpectedSection) {
                RailNodeExpectedSection sectionValidator = (RailNodeExpectedSection)railOnSection;
                Assert.assertEquals("Section belonging to pos '" + getPos() + "' has a different group at " + sectionValidator.getPos() + ".", "" + group, "" + sectionValidator.group);
            }
        }
    }
}
